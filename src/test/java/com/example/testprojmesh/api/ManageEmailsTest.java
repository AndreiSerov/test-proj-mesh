package com.example.testprojmesh.api;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.support.ApiIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class ManageEmailsTest extends ApiIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    static String unique() {
        return UUID.randomUUID().toString().substring(0, 8) + "@mail.example";
    }

    List<String> emailsOf(UserEntity user, String token) {
        return JsonPath.read(body(send("GET", profileUri(user), token, null)), "$.emails[*].value");
    }

    @Test
    void userAddsAnEmailAndItIsStoredLowercased() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        String raw = "New." + unique().toUpperCase();

        var added = send("POST", emailsUri(user), token, "{\"value\":\"%s\"}".formatted(raw));

        assertThat(added).hasStatus(201);
        assertThat(emailsOf(user, token)).contains(raw.toLowerCase());
    }

    @Test
    void tokenOfAUserThatNoLongerExistsGetsNotFoundNotAConflict() {
        UserEntity user = testUsers.create("Ghost");
        String token = loginAs(user);
        long id = user.getId();
        jdbc.update("delete from emails where user_id = ?", id);
        jdbc.update("delete from phones where user_id = ?", id);
        jdbc.update("delete from accounts where user_id = ?", id);
        jdbc.update("delete from users where id = ?", id);

        var result = send("POST", emailsUri(user), token, "{\"value\":\"%s\"}".formatted(unique()));

        assertThat(result).hasStatus(404);
    }

    @Test
    void invalidEmailIsRejectedWithAFieldError() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);

        var result = send("POST", emailsUri(user), token, "{\"value\":\"not-an-email\"}");

        assertThat(result).hasStatus(400);
        assertThat(JsonPath.<List<String>>read(body(result), "$.errors[*].field")).contains("value");
    }

    @Test
    void emailUsedByAnotherUserIsRejectedEvenWithDifferentCase() {
        UserEntity owner = testUsers.create("Owner");
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        String taken = owner.getEmails().get(0).getValue().toUpperCase();

        var result = send("POST", emailsUri(user), token, "{\"value\":\"%s\"}".formatted(taken));

        assertThat(result).hasStatus(409);
        assertThat((String) JsonPath.read(body(result), "$.type")).isEqualTo("urn:problem:conflict");
        assertThat(JsonPath.<String>read(body(result), "$.detail")).isEqualTo("Email is already in use");
    }

    @Test
    void addingAnEmailTheUserAlreadyOwnsIsRejectedWithoutCreatingADuplicate() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        String own = user.getEmails().get(0).getValue();

        var result = send("POST", emailsUri(user), token, "{\"value\":\"%s\"}".formatted(own));

        assertThat(result).hasStatus(409);
        assertThat(JsonPath.<String>read(body(result), "$.detail")).isEqualTo("You already have this email");
        assertThat(emailsOf(user, token)).containsOnlyOnce(own);
    }

    @Test
    void userChangesAnOwnEmail() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        long id = user.getEmails().get(0).getId();
        String replacement = unique();

        var result = send("PUT", emailsUri(user) + "/" + id, token,
            "{\"value\":\"%s\"}".formatted(replacement));

        assertThat(result).hasStatusOk();
        assertThat(emailsOf(user, token)).containsExactly(replacement);
    }

    @Test
    void changingAnEmailToOneUsedByAnotherUserIsRejected() {
        UserEntity other = testUsers.create("Other");
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        long id = user.getEmails().get(0).getId();

        var result = send("PUT", emailsUri(user) + "/" + id, token,
            "{\"value\":\"%s\"}".formatted(other.getEmails().get(0).getValue()));

        assertThat(result).hasStatus(409);
    }

    @Test
    void changingAnEmailToItsOwnCurrentValueSucceeds() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        var email = user.getEmails().get(0);

        var result = send("PUT", emailsUri(user) + "/" + email.getId(), token,
            "{\"value\":\"%s\"}".formatted(email.getValue()));

        assertThat(result).hasStatusOk();
    }

    @Test
    void userDeletesOneOfTwoEmailsButNotTheLastOne() {
        UserEntity user = testUsers.create("Eve");
        String token = loginAs(user);
        long first = user.getEmails().get(0).getId();
        String second = unique();
        send("POST", emailsUri(user), token, "{\"value\":\"%s\"}".formatted(second));

        assertThat(send("DELETE", emailsUri(user) + "/" + first, token, null)).hasStatus(204);
        assertThat(emailsOf(user, token)).containsExactly(second);

        long remaining = JsonPath.<Number>read(body(send("GET", profileUri(user), token, null)), "$.emails[0].id")
            .longValue();
        var last = send("DELETE", emailsUri(user) + "/" + remaining, token, null);
        assertThat(last).hasStatus(422);
        assertThat(emailsOf(user, token)).containsExactly(second);
    }

    @Test
    void anotherUsersEmailIdUnderTheAttackersOwnPathIsNotFound() {
        UserEntity victim = testUsers.create("Victim");
        UserEntity mallory = testUsers.create("Mallory");
        String attacker = loginAs(mallory);
        long victimEmailId = victim.getEmails().get(0).getId();

        assertThat(send("PUT", emailsUri(mallory) + "/" + victimEmailId, attacker,
            "{\"value\":\"%s\"}".formatted(unique()))).hasStatus(404);
        assertThat(send("DELETE", emailsUri(mallory) + "/" + victimEmailId, attacker, null))
            .hasStatus(404);
        assertThat(emailsOf(victim, loginAs(victim))).containsExactly(victim.getEmails().get(0).getValue());
    }

    @Test
    void writesUnderANonNumericUserIdAreForbiddenNotAServerError() {
        String token = loginAs(testUsers.create("Eve"));

        assertThat(send("POST", "/api/users/abc/emails", token, "{\"value\":\"%s\"}".formatted(unique())))
            .hasStatus(403);
    }

    @Test
    void writesUnderAnotherUsersIdAreForbiddenEvenWithAnInvalidBody() {
        UserEntity victim = testUsers.create("Victim");
        String attacker = loginAs(testUsers.create("Mallory"));
        String path = emailsUri(victim);
        long victimEmailId = victim.getEmails().get(0).getId();

        var added = send("POST", path, attacker, "{\"value\":\"%s\"}".formatted(unique()));
        assertThat(added).hasStatus(403);
        assertThat(JsonPath.<String>read(body(added), "$.type")).isEqualTo("urn:problem:forbidden");
        assertThat(send("POST", path, attacker, "{\"value\":\"not-an-email\"}")).hasStatus(403);
        assertThat(send("PUT", path + "/" + victimEmailId, attacker,
            "{\"value\":\"%s\"}".formatted(unique()))).hasStatus(403);
        assertThat(send("DELETE", path + "/" + victimEmailId, attacker, null)).hasStatus(403);
        assertThat(emailsOf(victim, loginAs(victim))).containsExactly(victim.getEmails().get(0).getValue());
    }
}
