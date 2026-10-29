package com.example.testprojmesh.api;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.support.ApiIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Caching seen from outside (ADR 0004): a change written straight to the database, bypassing the services, stays
 * invisible while an entry is cached; a Contact write through the API evicts it.
 */
class CachingTest extends ApiIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    List<String> emailsSeenBy(String token, UserEntity user) {
        return JsonPath.read(body(send("GET", profileUri(user), token, null)), "$.emails[*].value");
    }

    void renameEmailBehindTheServices(UserEntity user, String value) {
        jdbc.update("update emails set value = ? where user_id = ?", value, user.getId());
    }

    @Test
    void anotherUsersProfileIsServedFromTheCache() {
        UserEntity owner = testUsers.create("Owner");
        String viewer = loginAs(testUsers.create("Viewer"));
        String original = owner.getEmails().get(0).getValue();

        assertThat(emailsSeenBy(viewer, owner)).containsExactly(original);
        renameEmailBehindTheServices(owner, "changed." + original);

        assertThat(emailsSeenBy(viewer, owner)).containsExactly(original);
    }

    @Test
    void ownProfileAndBalanceAreNeverCached() {
        UserEntity owner = testUsers.create("Owner");
        String token = loginAs(owner);

        send("GET", profileUri(owner), token, null);
        jdbc.update("update accounts set balance = 77.77 where user_id = ?", owner.getId());

        String json = body(send("GET", profileUri(owner), token, null));
        assertThat(new BigDecimal(JsonPath.<Object>read(json, "$.balance").toString())).isEqualByComparingTo("77.77");
    }

    @Test
    void addingChangingOrDeletingAContactEvictsTheCachedProfile() {
        UserEntity owner = testUsers.create("Owner");
        String ownerToken = loginAs(owner);
        String viewer = loginAs(testUsers.create("Viewer"));
        String first = owner.getEmails().get(0).getValue();
        String second = "second." + first;
        String renamed = "renamed." + first;

        emailsSeenBy(viewer, owner);
        var added = send("POST", emailsUri(owner), ownerToken, "{\"value\":\"%s\"}".formatted(second));
        assertThat(emailsSeenBy(viewer, owner)).containsExactlyInAnyOrder(first, second);

        long secondId = JsonPath.<Number>read(body(added), "$.id").longValue();
        send("PUT", emailsUri(owner) + "/" + secondId, ownerToken, "{\"value\":\"%s\"}".formatted(renamed));
        assertThat(emailsSeenBy(viewer, owner)).containsExactlyInAnyOrder(first, renamed);

        send("DELETE", emailsUri(owner) + "/" + secondId, ownerToken, null);
        assertThat(emailsSeenBy(viewer, owner)).containsExactly(first);
    }

    List<String> emailsFound(String token, String query) {
        return JsonPath.read(body(send("GET", "/api/users/search?" + query, token, null)),
            "$.content[*].emails[*].value");
    }

    @Test
    void aRepeatedSearchIsServedFromTheCacheUntilAContactIsWritten() {
        String name = "Cached" + UUID.randomUUID().toString().substring(0, 8);
        UserEntity found = testUsers.create(name);
        String token = loginAs(found);
        String original = found.getEmails().get(0).getValue();
        String query = "name=" + name;

        assertThat(emailsFound(token, query)).containsExactly(original);
        renameEmailBehindTheServices(found, "changed." + original);
        assertThat(emailsFound(token, query)).as("same query, cached").containsExactly(original);
        assertThat(emailsFound(token, query + "&size=5")).as("other key, fresh")
            .containsExactly("changed." + original);

        String added = "added." + original;
        send("POST", emailsUri(found), token, "{\"value\":\"%s\"}".formatted(added));
        assertThat(emailsFound(token, query)).as("evicted by a Contact write")
            .containsExactlyInAnyOrder("changed." + original, added);
    }
}
