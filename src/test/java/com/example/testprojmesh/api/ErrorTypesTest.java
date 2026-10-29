package com.example.testprojmesh.api;

import com.example.testprojmesh.support.ApiIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorTypesTest extends ApiIntegrationTest {

    static String typeOf(MvcTestResult result) {
        return JsonPath.read(body(result), "$.type");
    }

    @Test
    void nonNumericUserIdIsAnInvalidInputProblem() {
        String token = loginAs(testUsers.create("Eve"));

        var result = send("GET", "/api/users/abc", token, null);

        assertThat(result).hasStatus(400);
        assertThat(typeOf(result)).isEqualTo("urn:problem:bad-request");
    }

    @Test
    void malformedJsonBodyIsAnInvalidInputProblem() {
        String token = loginAs(testUsers.create("Eve"));

        var result = send("POST", "/api/transfers", token, "{\"toUserId\":");

        assertThat(result).hasStatus(400);
        assertThat(typeOf(result)).isEqualTo("urn:problem:bad-request");
    }

    @Test
    void badDateFilterIsAnInvalidInputProblem() {
        String token = loginAs(testUsers.create("Eve"));

        var result = send("GET", "/api/users/search?dateOfBirth=not-a-date", token, null);

        assertThat(result).hasStatus(400);
        assertThat(typeOf(result)).isEqualTo("urn:problem:bad-request");
    }

    @Test
    void securityErrorsHaveTheSameProblemShapeAsEveryOtherError() {
        var victim = testUsers.create("Victim");
        String attacker = loginAs(testUsers.create("Mallory"));

        var forbidden = send("DELETE", emailsUri(victim) + "/1", attacker, null);
        var unauthorized = mvc.get().uri(profileUri(victim)).exchange();

        assertThat(forbidden).hasStatus(403);
        assertThat(JsonPath.<String>read(body(forbidden), "$.title")).isEqualTo("Forbidden");
        assertThat(JsonPath.<Integer>read(body(forbidden), "$.status")).isEqualTo(403);
        assertThat(JsonPath.<String>read(body(forbidden), "$.instance"))
            .isEqualTo(emailsUri(victim) + "/1");
        assertThat(typeOf(forbidden)).isEqualTo("urn:problem:forbidden");
        assertThat(unauthorized).hasStatus(401);
        assertThat(JsonPath.<String>read(body(unauthorized), "$.instance")).isEqualTo(profileUri(victim));
        assertThat(typeOf(unauthorized)).isEqualTo("urn:problem:unauthorized");
    }

    @Test
    void wrongMethodGetsAStableTypeForItsStatus() {
        String token = loginAs(testUsers.create("Eve"));

        var result = send("GET", "/api/transfers", token, null);

        assertThat(result).hasStatus(405);
        assertThat(typeOf(result)).isEqualTo("urn:problem:method-not-allowed");
    }
}
