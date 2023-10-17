package org.injecto.core;

import java.math.BigDecimal;

public interface Account {
    /**
     * Deposit the account from the `donator` by `amount`.
     */
    void deposit(Account donator, BigDecimal amount, TransactionHandle handle);
    /**
     * Hold the `amount` on the account.
     */
    void hold(BigDecimal amount, TransactionHandle handle);
    /**
     * Unhold the `amount` on the account.
     */
    void unhold(BigDecimal amount);
}
