package com.example.testprojmesh.api;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.support.ApiIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ManagePhonesTest extends ApiIntegrationTest {

    static String uniqueDigits() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(80_000_000_000L, 89_999_999_999L));
    }

    List<String> phonesOf(UserEntity user, String token) {
        return JsonPath.read(body(send("GET", profileUri(user), token, null)), "$.phones[*].value");
    }

    String json(String value) {
        return "{\"value\":\"%s\"}".formatted(value);
    }

    @Test
    void userAddsAPhoneAndItIsStoredAsDigitsOnly() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        String digits = uniqueDigits();
        String formatted = "+%s (%s) %s-%s".formatted(digits.substring(0, 1), digits.substring(1, 4),
            digits.substring(4, 7), digits.substring(7));

        var added = send("POST", phonesUri(user), token, json(formatted));

        assertThat(added).hasStatus(201);
        assertThat(phonesOf(user, token)).contains(digits);
    }

    @Test
    void phonesWithTooFewOrTooManyDigitsOrLettersAreRejectedWithAFieldError() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);

        for (String bad : List.of("12345", "1234567890123456", "abc1234567890", "")) {
            var result = send("POST", phonesUri(user), token, json(bad));
            assertThat(result).as(bad).hasStatus(400);
            assertThat(JsonPath.<List<String>>read(body(result), "$.errors[*].field")).contains("value");
        }
    }

    @Test
    void phoneUsedByAnotherUserIsRejectedEvenWhenFormattedDifferently() {
        UserEntity owner = testUsers.create("Owner");
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        String taken = owner.getPhones().get(0).getValue();
        String formatted = "+" + taken.substring(0, 1) + " " + taken.substring(1);

        var result = send("POST", phonesUri(user), token, json(formatted));

        assertThat(result).hasStatus(409);
        assertThat(JsonPath.<String>read(body(result), "$.detail")).isEqualTo("Phone is already in use");
    }

    @Test
    void addingAPhoneTheUserAlreadyOwnsIsRejectedWithoutCreatingADuplicate() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        String own = user.getPhones().get(0).getValue();

        var result = send("POST", phonesUri(user), token, json(own));
        assertThat(result).hasStatus(409);
        assertThat(JsonPath.<String>read(body(result), "$.detail")).isEqualTo("You already have this phone");
        assertThat(phonesOf(user, token)).containsOnlyOnce(own);
    }

    @Test
    void userChangesAnOwnPhone() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        long id = user.getPhones().get(0).getId();
        String replacement = uniqueDigits();

        var result = send("PUT", phonesUri(user) + "/" + id, token, json(replacement));

        assertThat(result).hasStatusOk();
        assertThat(phonesOf(user, token)).containsExactly(replacement);
    }

    @Test
    void changingAPhoneToOneUsedByAnotherUserIsRejected() {
        UserEntity other = testUsers.create("Other");
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);

        var result = send("PUT", phonesUri(user) + "/" + user.getPhones().get(0).getId(), token,
            json(other.getPhones().get(0).getValue()));

        assertThat(result).hasStatus(409);
    }

    @Test
    void userDeletesOneOfTwoPhonesButNotTheLastOne() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        long first = user.getPhones().get(0).getId();
        String second = uniqueDigits();
        send("POST", phonesUri(user), token, json(second));

        assertThat(send("DELETE", phonesUri(user) + "/" + first, token, null)).hasStatus(204);
        assertThat(phonesOf(user, token)).containsExactly(second);

        long remaining = JsonPath.<Number>read(body(send("GET", profileUri(user), token, null)), "$.phones[0].id")
            .longValue();
        assertThat(send("DELETE", phonesUri(user) + "/" + remaining, token, null)).hasStatus(422);
        assertThat(phonesOf(user, token)).containsExactly(second);
    }

    @Test
    void anotherUsersPhoneIdUnderTheAttackersOwnPathIsNotFound() {
        UserEntity victim = testUsers.create("Victim");
        UserEntity mallory = testUsers.create("Mallory");
        String attacker = loginAs(mallory);
        long victimPhoneId = victim.getPhones().get(0).getId();

        assertThat(send("PUT", phonesUri(mallory) + "/" + victimPhoneId, attacker,
            json(uniqueDigits()))).hasStatus(404);
        assertThat(send("DELETE", phonesUri(mallory) + "/" + victimPhoneId, attacker, null))
            .hasStatus(404);
        assertThat(phonesOf(victim, loginAs(victim))).containsExactly(victim.getPhones().get(0).getValue());
    }

    @Test
    void writesUnderAnotherUsersIdAreForbiddenEvenWithAnInvalidBody() {
        UserEntity victim = testUsers.create("Victim");
        String attacker = loginAs(testUsers.create("Mallory"));
        String path = phonesUri(victim);
        long victimPhoneId = victim.getPhones().get(0).getId();

        var added = send("POST", path, attacker, json(uniqueDigits()));
        assertThat(added).hasStatus(403);
        assertThat(JsonPath.<String>read(body(added), "$.type")).isEqualTo("urn:problem:forbidden");
        assertThat(send("POST", path, attacker, json("123"))).hasStatus(403);
        assertThat(send("PUT", path + "/" + victimPhoneId, attacker, json(uniqueDigits()))).hasStatus(403);
        assertThat(send("DELETE", path + "/" + victimPhoneId, attacker, null)).hasStatus(403);
        assertThat(phonesOf(victim, loginAs(victim))).containsExactly(victim.getPhones().get(0).getValue());
    }
}
