package com.example.testprojmesh.api;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.support.ApiIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserProfileTest extends ApiIntegrationTest {

    UserEntity userWith(String balance) {
        return testUsers.create("Profile", LocalDate.of(1991, 2, 3), new BigDecimal(balance));
    }

    @Test
    void ownProfileByIdIncludesTheBalance() {
        UserEntity user = userWith("42.50");

        var result = send("GET", profileUri(user), loginAs(user), null);

        assertThat(result).hasStatusOk();
        String json = body(result);
        assertThat(JsonPath.<Number>read(json, "$.id").longValue()).isEqualTo(user.getId());
        assertThat(JsonPath.<String>read(json, "$.dateOfBirth")).isEqualTo("1991-02-03");
        assertThat(new BigDecimal(JsonPath.<Object>read(json, "$.balance").toString()))
            .isEqualByComparingTo("42.50");
    }

    @Test
    void anotherUsersProfileIsReadableButHidesTheBalance() {
        UserEntity viewer = userWith("10.00");
        UserEntity other = userWith("12345.67");

        var result = send("GET", profileUri(other), loginAs(viewer), null);

        assertThat(result).hasStatusOk();
        Map<String, Object> json = JsonPath.read(body(result), "$");
        assertThat(((Number) json.get("id")).longValue()).isEqualTo(other.getId());
        assertThat(JsonPath.<String>read(body(result), "$.emails[0].value"))
            .isEqualTo(other.getEmails().get(0).getValue());
        assertThat(json).doesNotContainKey("balance");
        assertThat(body(result)).doesNotContain("12345.67");
    }

    @Test
    void unknownUserIsNotFound() {
        UserEntity viewer = userWith("10.00");

        var result = send("GET", "/api/users/" + Long.MAX_VALUE, loginAs(viewer), null);

        assertThat(result).hasStatus(404);
        assertThat(JsonPath.<String>read(body(result), "$.type")).isEqualTo("urn:problem:not-found");
    }

    @Test
    void nonNumericUserIdIsABadRequest() {
        UserEntity viewer = userWith("10.00");

        assertThat(send("GET", "/api/users/abc", loginAs(viewer), null)).hasStatus(400);
    }

    @Test
    void theMeAliasNoLongerReturnsAProfile() {
        UserEntity viewer = userWith("10.00");

        assertThat(send("GET", "/api/users/me", loginAs(viewer), null)).hasStatus(400);
    }
}
