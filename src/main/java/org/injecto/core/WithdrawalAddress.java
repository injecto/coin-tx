package org.injecto.core;

import org.injecto.external.WithdrawalService;

import java.math.BigDecimal;
import java.util.UUID;

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
                handle.onEvent("[%s] %s trying withdraw %g".formatted(address, donator, amount));
                service.withdraw(address, amount).thenAccept(success -> {
                    if (success) {
                        handle.onSuccess("withdrew %g from %s to %s".formatted(amount, donator, address));
                    } else {
                        donator.unhold(amount);
                        handle.onFailure("withdrawal of %g from %s to %s failed".formatted(amount, donator, address));
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                handle.onFailure(error);
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
