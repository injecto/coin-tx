package org.injecto.core;

import org.injecto.external.WithdrawalService;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class WithdrawalAddress implements Account {
    private final WithdrawalServiceCaller service;
    private final WithdrawalService.Address address;

    public WithdrawalAddress(WithdrawalServiceCaller service, WithdrawalService.Address address) {
        this.service = service;
        this.address = address;
    }

    @Override
    public void deposit(Account donator, BigDecimal amount, TransactionHandle handle) {
        if (amount.equals(BigDecimal.ZERO)) {
            handle.onFailure("zero amount withdrawal requested");
            return;
        }

        donator.hold(amount, new TransactionHandle() {
            @Override
            public void onEvent(String description) {
                handle.onEvent(description);
            }

            @Override
            public void onSuccess(String comment) {
                handle.onEvent(comment);
                withdraw_with_retry(new AtomicInteger(3), donator, amount, handle);
            }

            @Override
            public void onFailure(String error) {
                handle.onFailure(error);
            }
        });
    }

    private void withdraw_with_retry(AtomicInteger attemptsLeft, Account donator, BigDecimal amount, TransactionHandle handle) {
        if (attemptsLeft.decrementAndGet() < 0) {
            donator.unhold(amount);
            handle.onFailure("operation failed");
            return;
        }
        handle.onEvent("[%s] %s trying withdraw %g".formatted(address, donator, amount));
        service.withdraw(address, amount).thenAccept(success -> {
            if (success) {
                handle.onSuccess("withdrew %g from %s to %s".formatted(amount, donator, address));
            } else {
                handle.onEvent("withdrawal of %g from %s to %s failed".formatted(amount, donator, address));
                withdraw_with_retry(attemptsLeft, donator, amount, handle);
            }
        });
    }

    @Override
    public void hold(BigDecimal amount, TransactionHandle handle) {
        // oops, leaked abstraction
        throw new IllegalArgumentException("[%s] Cannot hold external address".formatted(address));
    }

    @Override
    public void unhold(BigDecimal amount) {
        throw new IllegalArgumentException("[%s] Cannot unhold external address".formatted(address));
    }

    @Override
    public String toString() {
        return "Addr %s".formatted(address);
    }
}
