package com.example.testprojmesh.service;

import com.example.testprojmesh.service.model.ContactView;

/** Add, change and delete one kind of Contact of a User; {@code userId} always comes from the token. */
public interface ContactService {

    ContactView add(long userId, String raw);

    ContactView change(long userId, long contactId, String raw);

    void delete(long userId, long contactId);
}
