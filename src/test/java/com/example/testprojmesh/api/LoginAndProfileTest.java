package com.example.testprojmesh.api;

import com.example.testprojmesh.support.BaseIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import static org.assertj.core.api.Assertions.assertThat;

class LoginAndProfileTest extends BaseIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Value("${app.jwt.secret}")
    String secret;

    String login(String identifier, String password) {
        var result = mvc.post().uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, password))
            .exchange();
        assertThat(result).hasStatusOk();
        return JsonPath.read(bodyOf(result), "$.token");
    }

    String bodyOf(org.springframework.test.web.servlet.assertj.MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void userLogsInByEmailAndReadsOwnProfileWithBalance() {
        String token = login("alice@example.com", SEED_PASSWORD);

        var me = mvc.get().uri("/api/users/1").header("Authorization", "Bearer " + token).exchange();

        assertThat(me).hasStatusOk();
        String body = bodyOf(me);
        assertThat((String) JsonPath.read(body, "$.name")).isEqualTo("Alice Johnson");
        assertThat((String) JsonPath.read(body, "$.dateOfBirth")).isEqualTo("1990-05-14");
        assertThat(JsonPath.<java.util.List<String>>read(body, "$.emails[*].value"))
            .containsExactlyInAnyOrder("alice@example.com", "alice.j@example.com");
        assertThat(JsonPath.<java.util.List<String>>read(body, "$.phones[*].value"))
            .containsExactly("79990000001");
        assertThat(((Number) JsonPath.read(body, "$.balance")).doubleValue()).isEqualTo(1000.00);
        assertThat(body).doesNotContain("passwordHash").doesNotContain("$2a$");
    }

    @Test
    void userLogsInByFormattedPhone() {
        String token = login("+7 (999) 000-00-02", SEED_PASSWORD);

        var me = mvc.get().uri("/api/users/2").header("Authorization", "Bearer " + token).exchange();

        assertThat(me).hasStatusOk();
        assertThat((String) JsonPath.read(bodyOf(me), "$.name")).isEqualTo("Bob Smith");
    }

    @Test
    void emailLoginIsCaseInsensitive() {
        assertThat(login("ALICE@Example.com", SEED_PASSWORD)).isNotBlank();
    }

    @Test
    void tokenCarriesOnlyTheUserIdClaim() throws Exception {
        String token = login("alice@example.com", SEED_PASSWORD);

        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        Map<String, Object> claims = JsonPath.read(payload, "$");

        assertThat(claims.keySet()).containsExactlyInAnyOrder("USER_ID", "exp");
        assertThat(((Number) claims.get("USER_ID")).longValue()).isEqualTo(1L);
        long ttl = ((Number) claims.get("exp")).longValue() - Instant.now().getEpochSecond();
        assertThat(ttl).isBetween(3590L, 3600L);
    }

    @Test
    void wrongPasswordAndUnknownIdentifierGiveTheSameGenericUnauthorized() {
        var wrongPassword = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"identifier\":\"alice@example.com\",\"password\":\"nope\"}").exchange();
        var unknownUser = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"identifier\":\"ghost@example.com\",\"password\":\"password123\"}").exchange();

        assertThat(wrongPassword).hasStatus(401);
        assertThat(unknownUser).hasStatus(401);
        assertThat(bodyOf(wrongPassword)).isEqualTo(bodyOf(unknownUser));
        assertThat((String) JsonPath.read(bodyOf(wrongPassword), "$.type")).isEqualTo("urn:problem:unauthorized");
    }

    @Test
    void blankLoginFieldsAreRejectedWithFieldErrors() {
        var result = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"identifier\":\"\",\"password\":\"\"}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(JsonPath.<java.util.List<String>>read(bodyOf(result), "$.errors[*].field"))
            .contains("identifier", "password");
    }

    @Test
    void protectedEndpointsRejectMissingTamperedAndExpiredTokens() {
        assertThat(mvc.get().uri("/api/users/1").exchange()).hasStatus(401);

        String token = login("alice@example.com", SEED_PASSWORD);
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        assertThat(mvc.get().uri("/api/users/1").header("Authorization", "Bearer " + tampered).exchange())
            .hasStatus(401);
        assertThat(mvc.get().uri("/api/users/1").header("Authorization", "Bearer not-a-jwt").exchange())
            .hasStatus(401);

        assertThat(mvc.get().uri("/api/users/1")
            .header("Authorization", "Bearer " + expiredToken()).exchange()).hasStatus(401);
    }

    @Test
    void correctlySignedTokenWithoutANumericUserIdIsRejectedWith401NotAServerError() {
        for (String token : new String[] {validTokenWith("OTHER", "x"), validTokenWith("USER_ID", "abc")}) {
            var result = mvc.get().uri("/api/users/1").header("Authorization", "Bearer " + token).exchange();

            assertThat(result).hasStatus(401);
            assertThat((String) JsonPath.read(bodyOf(result), "$.type")).isEqualTo("urn:problem:unauthorized");
        }
    }

    @Test
    void loginDocsAndHealthAreOpen() {
        assertThat(mvc.get().uri("/actuator/health").exchange()).hasStatusOk();
        assertThat(mvc.get().uri("/v3/api-docs").exchange()).hasStatusOk();
    }

    private String expiredToken() {
        Instant past = Instant.now().minusSeconds(7200);
        return signed(JwtClaimsSet.builder().claim("USER_ID", 1L).issuedAt(past).expiresAt(past.plusSeconds(3600)));
    }

    private String validTokenWith(String claim, Object value) {
        Instant now = Instant.now();
        var builder = JwtClaimsSet.builder().issuedAt(now).expiresAt(now.plusSeconds(3600));
        return signed(value == null ? builder : builder.claim(claim, value));
    }

    private String signed(JwtClaimsSet.Builder claims) {
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
            .getTokenValue();
    }
}
