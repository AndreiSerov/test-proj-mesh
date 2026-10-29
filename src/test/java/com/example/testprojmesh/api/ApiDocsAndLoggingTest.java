package com.example.testprojmesh.api;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.support.ApiIntegrationTest;
import com.example.testprojmesh.support.TestUsers;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class ApiDocsAndLoggingTest extends ApiIntegrationTest {

    @Test
    void apiDocsDescribeEveryEndpointAndHowToPassTheJwt() {
        var result = mvc.get().uri("/v3/api-docs").exchange();
        assertThat(result).hasStatusOk();
        String json = body(result);

        Map<String, Object> paths = JsonPath.read(json, "$.paths");
        assertThat(paths.keySet()).contains(
            "/api/auth/login", "/api/users/{userId}", "/api/users/search",
            "/api/users/{userId}/emails", "/api/users/{userId}/emails/{id}",
            "/api/users/{userId}/phones", "/api/users/{userId}/phones/{id}", "/api/transfers");
        assertThat(paths.keySet()).noneMatch(path -> path.startsWith("/api/users/me"));
        assertThat((String) JsonPath.read(json, "$.components.securitySchemes.bearerAuth.scheme"))
            .isEqualTo("bearer");
        assertThat((String) JsonPath.read(json, "$.components.securitySchemes.bearerAuth.bearerFormat"))
            .isEqualTo("JWT");
        assertThat(JsonPath.<Object>read(json, "$.paths['/api/transfers'].post.security[0].bearerAuth"))
            .isNotNull();
        assertThat(paths).doesNotContainKey("/actuator/health");
    }

    @Test
    void swaggerUiIsReachableWithoutAToken() {
        assertThat(mvc.get().uri("/swagger-ui/index.html").exchange()).hasStatusOk();
    }

    @Test
    void loginsAndTransfersAreLoggedWithoutLeakingSecrets(CapturedOutput output) {
        UserEntity payer = testUsers.create("Logger", LocalDate.of(1990, 1, 1), new BigDecimal("77.77"));
        UserEntity payee = testUsers.create("Receiver");
        mvc.post().uri("/api/auth/login").contentType("application/json")
            .content("{\"identifier\":\"%s\",\"password\":\"wrong-secret-pw\"}"
                .formatted(payer.getEmails().get(0).getValue())).exchange();
        String token = loginAs(payer);

        send("POST", "/api/transfers", token,
            "{\"toUserId\":%d,\"amount\":12.34}".formatted(payee.getId()));
        send("POST", "/api/transfers", token,
            "{\"toUserId\":%d,\"amount\":9999.00}".formatted(payee.getId()));

        String logs = output.getAll();
        assertThat(logs)
            .contains("Failed login attempt")
            .contains("User " + payer.getId() + " logged in")
            .contains("completed: user " + payer.getId() + " -> user " + payee.getId())
            .contains("insufficient funds");
        assertThat(logs)
            .doesNotContain(token)
            .doesNotContain(TestUsers.PASSWORD)
            .doesNotContain("wrong-secret-pw")
            .doesNotContain("$2a$")
            .doesNotContain(payer.getEmails().get(0).getValue())
            .doesNotContain("77.77");
    }
}
