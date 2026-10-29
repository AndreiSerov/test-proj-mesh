package com.example.testprojmesh.service.impl;

import com.example.testprojmesh.config.InterestProperties;
import com.example.testprojmesh.dao.repository.AccountRepository;
import com.example.testprojmesh.service.InterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterestServiceImpl implements InterestService {

    private final AccountRepository accounts;
    private final InterestProperties properties;

    @Override
    @Transactional
    public int accrue() {
        return accounts.accrueInterest(properties.growth(), properties.capFactor());
    }
}
