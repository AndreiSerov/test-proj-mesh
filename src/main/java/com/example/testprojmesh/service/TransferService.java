package com.example.testprojmesh.service;

import com.example.testprojmesh.service.model.TransferView;
import java.math.BigDecimal;

public interface TransferService {

    /** Moves {@code amount} from the sender's Account to the recipient's and records the Transfer. */
    TransferView transfer(long fromUserId, long toUserId, BigDecimal amount);
}
