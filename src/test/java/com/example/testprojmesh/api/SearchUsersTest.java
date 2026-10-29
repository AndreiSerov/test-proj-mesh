package com.example.testprojmesh.api;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.support.ApiIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SearchUsersTest extends ApiIntegrationTest {

    String prefix;
    String token;

    @BeforeEach
    void setUp() {
        prefix = "Zq" + UUID.randomUUID().toString().substring(0, 8);
        token = loginAs(testUsers.create("Searcher"));
    }

    UserEntity user(String suffix, LocalDate dob) {
        return testUsers.create(prefix + " " + suffix, dob, new BigDecimal("42.00"));
    }

    String search(String query) {
        var result = send("GET", "/api/users/search?" + query, token, null);
        assertThat(result).hasStatusOk();
        return body(result);
    }

    List<String> names(String body) {
        return JsonPath.read(body, "$.content[*].name");
    }

    @Test
    void namePrefixMatchesCaseInsensitivelyAndOnlyAtTheStart() {
        user("Alpha", LocalDate.of(1990, 1, 1));
        user("Beta", LocalDate.of(1991, 1, 1));
        testUsers.create("Other " + prefix);

        String body = search("name=" + prefix.toLowerCase());

        assertThat(names(body)).containsExactly(prefix + " Alpha", prefix + " Beta");
    }

    @Test
    void percentAndUnderscoreInTheNameFilterAreLiteral() {
        user("Alpha", LocalDate.of(1990, 1, 1));

        assertThat(names(search("name=" + prefix + "%25"))).isEmpty();
        assertThat(names(search("name=" + prefix.substring(0, 3) + "_"))).isEmpty();
    }

    @Test
    void exactEmailFindsOneUserCaseInsensitively() {
        UserEntity target = user("Alpha", LocalDate.of(1990, 1, 1));
        user("Beta", LocalDate.of(1990, 1, 1));

        String body = search("email=" + target.getEmails().get(0).getValue().toUpperCase());

        assertThat(names(body)).containsExactly(prefix + " Alpha");
    }

    @Test
    void emailPrefixDoesNotMatch() {
        UserEntity target = user("Alpha", LocalDate.of(1990, 1, 1));
        String partial = target.getEmails().get(0).getValue().substring(0, 10);

        assertThat(names(search("email=" + partial))).isEmpty();
    }

    @Test
    void exactPhoneFindsOneUserEvenWhenWrittenWithFormatting() {
        UserEntity target = user("Alpha", LocalDate.of(1990, 1, 1));
        user("Beta", LocalDate.of(1990, 1, 1));
        String phone = target.getPhones().get(0).getValue();

        String body = search("phone=%2B" + phone.substring(0, 1) + "%20" + phone.substring(1));

        assertThat(names(body)).containsExactly(prefix + " Alpha");
    }

    @Test
    void dateOfBirthFilterIsStrictlyAfterTheGivenDate() {
        user("Old", LocalDate.of(1980, 6, 15));
        user("Same", LocalDate.of(1990, 6, 15));
        user("Young", LocalDate.of(2000, 6, 15));

        String body = search("name=" + prefix + "&dateOfBirth=1990-06-15");

        assertThat(names(body)).containsExactly(prefix + " Young");
    }

    @Test
    void filtersCombineWithAnd() {
        UserEntity both = user("Young Match", LocalDate.of(2000, 1, 1));
        user("Young Other", LocalDate.of(2000, 1, 1));
        user("Old Match", LocalDate.of(1970, 1, 1));

        String body = search("name=" + prefix + "%20Young&dateOfBirth=1999-01-01&email="
            + both.getEmails().get(0).getValue());

        assertThat(names(body)).containsExactly(prefix + " Young Match");
    }

    @Test
    void pagesAreStableSortedByIdAndCoverEveryUserExactlyOnce() {
        for (int i = 1; i <= 5; i++) {
            user("U" + i, LocalDate.of(1990, 1, 1));
        }

        String first = search("name=" + prefix + "&page=0&size=2");
        String second = search("name=" + prefix + "&page=1&size=2");
        String third = search("name=" + prefix + "&page=2&size=2");

        assertThat(names(first)).containsExactly(prefix + " U1", prefix + " U2");
        assertThat(names(second)).containsExactly(prefix + " U3", prefix + " U4");
        assertThat(names(third)).containsExactly(prefix + " U5");
        assertThat(JsonPath.<Number>read(first, "$.totalElements").longValue()).isEqualTo(5);
        assertThat(JsonPath.<Number>read(first, "$.totalPages").intValue()).isEqualTo(3);
    }

    @Test
    void defaultPageSizeIsTwentyAndOversizedPagesAreRejected() {
        assertThat(JsonPath.<Number>read(search("name=" + prefix), "$.size").intValue()).isEqualTo(20);

        assertThat(send("GET", "/api/users/search?size=101", token, null)).hasStatus(400);
        assertThat(send("GET", "/api/users/search?size=0", token, null)).hasStatus(400);
        assertThat(send("GET", "/api/users/search?page=-1", token, null)).hasStatus(400);
    }

    @Test
    void invalidPhoneFilterIsRejectedWithAFieldErrorInsteadOfMatchingEveryone() {
        for (String bad : List.of("abc", "123")) {
            var result = send("GET", "/api/users/search?phone=" + bad, token, null);

            assertThat(result).as(bad).hasStatus(400);
            assertThat(JsonPath.<List<String>>read(body(result), "$.errors[*].field")).as(bad).contains("phone");
        }
    }

    @Test
    void invalidDateIsRejectedAndNoMatchIsAnEmptyPage() {
        assertThat(send("GET", "/api/users/search?dateOfBirth=yesterday", token, null)).hasStatus(400);

        String body = search("name=" + prefix);
        assertThat(names(body)).isEmpty();
        assertThat(JsonPath.<Number>read(body, "$.totalElements").longValue()).isZero();
    }

    @Test
    void resultsShowContactsButNeverPasswordOrBalance() {
        user("Alpha", LocalDate.of(1990, 1, 1));

        String body = search("name=" + prefix);

        assertThat(JsonPath.<List<String>>read(body, "$.content[0].emails[*].value")).hasSize(1);
        assertThat(JsonPath.<List<String>>read(body, "$.content[0].phones[*].value")).hasSize(1);
        assertThat((String) JsonPath.read(body, "$.content[0].dateOfBirth")).isEqualTo("1990-01-01");
        assertThat(body).doesNotContain("password").doesNotContain("$2a$").doesNotContain("balance");
    }

    @Test
    void searchRequiresAuthentication() {
        assertThat(mvc.get().uri("/api/users/search").exchange()).hasStatus(401);
    }
}
