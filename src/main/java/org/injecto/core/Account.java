package org.injecto.core;

import java.math.BigDecimal;

public interface Account {
    void deposit(Account donator, BigDecimal amount, TransactionHandle handle);
    void hold(BigDecimal amount, TransactionHandle handle);
    void unhold(BigDecimal amount);
}
