package com.example.testprojmesh.service.impl;

import com.example.testprojmesh.dao.entity.PhoneEntity;
import com.example.testprojmesh.dao.repository.PhoneRepository;
import com.example.testprojmesh.dao.repository.UserRepository;
import com.example.testprojmesh.service.PhoneService;
import com.example.testprojmesh.util.ContactNormalizer;
import org.springframework.stereotype.Service;

@Service
public class PhoneServiceImpl extends AbstractContactService<PhoneEntity> implements PhoneService {

    public PhoneServiceImpl(PhoneRepository phones, UserRepository users) {
        super(phones, users, "phone", "phones_value_key", PhoneEntity::new, ContactNormalizer::phone);
    }
}
