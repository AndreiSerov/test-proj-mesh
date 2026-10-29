package com.example.testprojmesh.service.impl;

import com.example.testprojmesh.config.CacheConfig;
import com.example.testprojmesh.dao.repository.UserRepository;
import com.example.testprojmesh.service.model.ProfileView;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Another User's profile without the Balance, cached in {@link CacheConfig#USER_PROFILES}. Its own bean so that
 * {@link UserServiceImpl} calls it through the caching proxy. An unknown User throws 404 and is not cached.
 */
@Component
@RequiredArgsConstructor
public class PublicProfileReader {

    private final UserRepository users;

    @Cacheable(cacheNames = CacheConfig.USER_PROFILES, key = "#userId")
    @Transactional(readOnly = true)
    public ProfileView publicProfile(long userId) {
        return UserServiceImpl.profileOf(UserServiceImpl.find(users, userId), null);
    }
}
