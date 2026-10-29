package com.example.testprojmesh.service;

public interface AuthService {

    /** Returns a JWT for the User with this email or phone; one generic 401 for any failure. */
    String login(String identifier, String password);
}
