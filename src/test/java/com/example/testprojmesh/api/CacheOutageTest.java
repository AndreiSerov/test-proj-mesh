package com.example.testprojmesh.api;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.support.ApiIntegrationTest;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

/** Own context (and Redis container), because the test stops Redis: a cache outage must not be an API outage. */
@TestPropertySource(properties = "test.context=redis-outage")
class CacheOutageTest extends ApiIntegrationTest {

    @Autowired
    RedisContainer redis;

    UserEntity owner;
    String ownerToken;
    String viewer;

    @BeforeEach
    void usersThenStopRedis() {
        owner = testUsers.create("Owner");
        ownerToken = loginAs(owner);
        viewer = loginAs(testUsers.create("Viewer"));
        redis.stop();
    }

    @Test
    void cachedReadsAndEvictingWritesStillWorkWithoutRedis() {
        assertThat(send("GET", profileUri(owner), viewer, null)).hasStatusOk();
        assertThat(send("GET", "/api/users/search?name=Owner", viewer, null)).hasStatusOk();
        assertThat(send("POST", emailsUri(owner), ownerToken, "{\"value\":\"outage.%s\"}"
            .formatted(owner.getEmails().get(0).getValue()))).hasStatus(201);
    }
}
