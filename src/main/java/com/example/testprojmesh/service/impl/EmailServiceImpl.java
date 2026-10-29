package com.example.testprojmesh.service.impl;

import com.example.testprojmesh.dao.entity.EmailEntity;
import com.example.testprojmesh.dao.repository.EmailRepository;
import com.example.testprojmesh.dao.repository.UserRepository;
import com.example.testprojmesh.service.EmailService;
import com.example.testprojmesh.util.ContactNormalizer;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl extends AbstractContactService<EmailEntity> implements EmailService {

    public EmailServiceImpl(EmailRepository emails, UserRepository users) {
        super(emails, users, "email", "emails_value_key", EmailEntity::new, ContactNormalizer::email);
    }
}
