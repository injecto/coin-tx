package org.injecto.core;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class UserAccount implements Account {
    private final User user;
    private final AtomicReference<BigDecimal> balance;

    public UserAccount(User user, BigDecimal initialBalance) {
        this.user = user;
        this.balance = new AtomicReference<>(initialBalance);
    }

    @Override
    public void deposit(Account donator, BigDecimal amount, TransactionHandle handle) {
        if (amount.equals(BigDecimal.ZERO)) {
            handle.onFailure("zero amount deposit requested");
            return;
        }

        var self = this;
        donator.hold(amount, new TransactionHandle() {
            @Override
            public void onEvent(String description) {
                handle.onEvent(description);
            }

            @Override
            public void onSuccess(String comment) {
                handle.onEvent(comment);
                var result = balance.updateAndGet(b -> b.add(amount));
                handle.onSuccess(String.format("[%s] balance increased by %g: total %g", self, amount, result));
            }

            @Override
            public void onFailure(String error) {
                handle.onFailure(error);
            }
        });
    }

    @Override
    public void hold(BigDecimal amount, TransactionHandle handle) {
        BigDecimal balance;
        BigDecimal result;
        do {
            handle.onEvent(String.format("[%s] trying hold %g", this, amount));
            balance = this.balance.get();
            result = balance.subtract(amount);
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                handle.onFailure(String.format("[%s] cannot hold %g: %g available", this, amount, balance));
                return;
            }
        } while (!this.balance.compareAndSet(balance, result));
        handle.onSuccess(String.format("[%s] %g held: %g available", this, amount, result));
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getBalance() {
        return balance.get();
    }

    @Override
    public void unhold(BigDecimal amount) {
        balance.updateAndGet(b -> b.add(amount));
    }

    @Override
    public String toString() {
        return user.toString();
    }

    public record User(int id) {}
}
