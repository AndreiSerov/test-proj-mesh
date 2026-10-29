package com.example.testprojmesh.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
    "app.jwt.secret=test-secret-test-secret-test-secret-32",
    "app.interest.enabled=false"
})
@AutoConfigureMockMvc
@Import(IntegrationTestContainers.class)
public abstract class BaseIntegrationTest {

    public static final String SEED_PASSWORD = "password123";
}
