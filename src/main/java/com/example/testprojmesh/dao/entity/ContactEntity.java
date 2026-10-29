package com.example.testprojmesh.dao.entity;

public interface ContactEntity {

    Long getId();

    String getValue();

    void setValue(String value);

    UserEntity getUser();

    void setUser(UserEntity user);
}
