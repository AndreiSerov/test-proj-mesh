package com.example.testprojmesh.service.impl;

import com.example.testprojmesh.api.error.ApiException;
import com.example.testprojmesh.config.CacheConfig;
import com.example.testprojmesh.dao.entity.ContactEntity;
import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.dao.repository.ContactRepository;
import com.example.testprojmesh.dao.repository.UserRepository;
import com.example.testprojmesh.service.ContactService;
import com.example.testprojmesh.service.model.ContactView;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public abstract class AbstractContactService<E extends ContactEntity> implements ContactService {

    private final ContactRepository<E> contacts;
    private final UserRepository users;
    private final String label;
    private final String uniqueConstraint;
    private final Supplier<E> factory;
    private final Function<String, String> normalizer;

    protected AbstractContactService(ContactRepository<E> contacts, UserRepository users, String label,
                             String uniqueConstraint, Supplier<E> factory, Function<String, String> normalizer) {
        this.contacts = contacts;
        this.users = users;
        this.label = label;
        this.uniqueConstraint = uniqueConstraint;
        this.factory = factory;
        this.normalizer = normalizer;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfig.USER_PROFILES, key = "#userId"),
        @CacheEvict(cacheNames = CacheConfig.USER_SEARCH, allEntries = true)})
    public ContactView add(long userId, String raw) {
        String value = normalizer.apply(raw);
        UserEntity user = lockUser(userId);
        if (contacts.existsByValueAndUserId(value, userId)) {
            throw ApiException.conflict("You already have this " + label);
        }
        if (contacts.existsByValue(value)) {
            throw inUse();
        }
        E contact = factory.get();
        contact.setUser(user);
        contact.setValue(value);
        E saved = save(contact);
        log.info("User {} added {} {}", userId, label, saved.getId());
        return view(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfig.USER_PROFILES, key = "#userId"),
        @CacheEvict(cacheNames = CacheConfig.USER_SEARCH, allEntries = true)})
    public ContactView change(long userId, long contactId, String raw) {
        String value = normalizer.apply(raw);
        lockUser(userId);
        E contact = owned(userId, contactId);
        if (contacts.existsByValueAndIdNot(value, contactId)) {
            throw inUse();
        }
        contact.setValue(value);
        E saved = save(contact);
        log.info("User {} changed {} {}", userId, label, contactId);
        return view(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfig.USER_PROFILES, key = "#userId"),
        @CacheEvict(cacheNames = CacheConfig.USER_SEARCH, allEntries = true)})
    public void delete(long userId, long contactId) {
        lockUser(userId);
        E contact = owned(userId, contactId);
        if (contacts.countByUserId(userId) <= 1) {
            throw ApiException.unprocessable("The last " + label + " cannot be deleted");
        }
        contacts.delete(contact);
        log.info("User {} deleted {} {}", userId, label, contactId);
    }

    /** Serializes all Contact writes of one User, so the "at least one" count cannot race. Locks users only. */
    private UserEntity lockUser(long userId) {
        return users.lockById(userId).orElseThrow(() -> ApiException.notFound("User not found: " + userId));
    }

    private E owned(long userId, long contactId) {
        return contacts.findByIdAndUserId(contactId, userId)
            .orElseThrow(() -> ApiException.notFound(capitalized() + " not found: " + contactId));
    }

    private E save(E contact) {
        try {
            return contacts.saveAndFlush(contact);
        } catch (DataIntegrityViolationException e) {
            if (violatesUniqueValue(e)) {
                throw inUse();
            }
            throw e;
        }
    }

    /** True only for the value-unique constraint (emails_value_key / phones_value_key), not e.g. the user FK. */
    private boolean violatesUniqueValue(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException cve
            && uniqueConstraint.equals(cve.getConstraintName());
    }

    private ApiException inUse() {
        return ApiException.conflict(capitalized() + " is already in use");
    }

    private String capitalized() {
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    private static ContactView view(ContactEntity contact) {
        return new ContactView(contact.getId(), contact.getValue());
    }
}
