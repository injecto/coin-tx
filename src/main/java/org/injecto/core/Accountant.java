package org.injecto.core;

import org.injecto.external.WithdrawalService;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Accountant {
    private final ConcurrentMap<UserAccount.User, UserAccount> accounts = new ConcurrentHashMap<>();
    private final WithdrawalServiceCaller withdrawalService;

    public Accountant(WithdrawalService service) {
        withdrawalService = new WithdrawalServiceCaller(service);
    }

    public void transfer(UserAccount.User donator, UserAccount.User recipient, BigDecimal amount, TransactionHandle handle) {
        var donatorAcc = Objects.requireNonNull(accounts.get(donator), () -> "%s not registered".formatted(donator));
        var recipientAcc = Objects.requireNonNull(accounts.get(recipient), () -> "%s not registered".formatted(recipient));
        recipientAcc.deposit(donatorAcc, amount, handle);
    }

    public void withdraw(UserAccount.User donator, WithdrawalService.Address address, BigDecimal amount, TransactionHandle handle) {
        var donatorAcc = Objects.requireNonNull(accounts.get(donator), () -> "%s not registered".formatted(donator));
        new WithdrawalAddress(withdrawalService, address).deposit(donatorAcc, amount, handle);
    }

    public void register(UserAccount account) {
        if (accounts.putIfAbsent(account.getUser(), account) != null) {
            throw new IllegalArgumentException("%s account already exists".formatted(account.getUser()));
        }
    }

    public Collection<UserAccount> accounts() {
        return accounts.values();
    }
}
