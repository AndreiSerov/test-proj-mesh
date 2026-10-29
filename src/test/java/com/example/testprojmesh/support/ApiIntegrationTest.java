package com.example.testprojmesh.support;

import com.example.testprojmesh.dao.entity.UserEntity;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

public abstract class ApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected TestUsers testUsers;

    protected String loginAs(UserEntity user) {
        String email = user.getEmails().get(0).getValue();
        MvcTestResult result = mvc.post().uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(email, TestUsers.PASSWORD))
            .exchange();
        return JsonPath.read(body(result), "$.token");
    }

    protected static String profileUri(UserEntity user) {
        return "/api/users/" + user.getId();
    }

    protected static String emailsUri(UserEntity user) {
        return profileUri(user) + "/emails";
    }

    protected static String phonesUri(UserEntity user) {
        return profileUri(user) + "/phones";
    }

    protected MvcTestResult send(String method, String uri, String token, String json) {
        var request = switch (method) {
            case "POST" -> mvc.post();
            case "PUT" -> mvc.put();
            case "DELETE" -> mvc.delete();
            default -> mvc.get();
        };
        request.uri(java.net.URI.create(uri)).header("Authorization", "Bearer " + token);
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return request.exchange();
    }

    protected static String body(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
