package com.example.testprojmesh.service;

public interface InterestService {

    /** Applies one Interest accrual to every Account; returns how many Balances changed. */
    int accrue();
}
