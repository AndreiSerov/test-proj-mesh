package com.example.testprojmesh.service;

import com.example.testprojmesh.service.model.PageView;
import com.example.testprojmesh.service.model.ProfileView;
import com.example.testprojmesh.service.model.UserSearchCriteria;
import com.example.testprojmesh.service.model.UserSummary;

public interface UserService {

    /** Any User may read any profile; the Balance is shown only to its owner. */
    ProfileView profile(long viewerId, long userId);

    PageView<UserSummary> search(UserSearchCriteria criteria, int page, int size);
}
