package org.injecto;

import org.injecto.core.Accountant;
import org.injecto.core.UserAccount;
import org.injecto.external.WithdrawalServiceStub;
import org.injecto.http.Server;

import java.io.IOException;
import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) throws IOException {
        var accountant = new Accountant(new WithdrawalServiceStub());
        accountant.register(new UserAccount(new UserAccount.User(0), BigDecimal.valueOf(100)));
        accountant.register(new UserAccount(new UserAccount.User(1), BigDecimal.valueOf(100)));
        var server = new Server(10080, accountant);
        server.serve();
    }
}