package org.injecto.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.injecto.core.Accountant;
import org.injecto.core.TransactionHandle;
import org.injecto.core.UserAccount;
import org.injecto.external.WithdrawalService;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Yes, it'd be better to use something better than jdk's server. And I was lazy to extract some common logic,
 * so the code is error-prone and lacks requests validation.
 */
public class Server {
    private final HttpServer server;

    public Server(int port, Accountant accountant) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        ConcurrentHashMap<UUID, Boolean> txResults = new ConcurrentHashMap<>();
        server.createContext(DepositHandler.CTX, new DepositHandler(accountant, txResults));
        server.createContext(WithdrawHandler.CTX, new WithdrawHandler(accountant, txResults));
        server.createContext(ResultHandler.CTX, new ResultHandler(txResults));
    }

    public void serve() {
        server.start();
    }

    private static class ResultHandler implements HttpHandler {
        private static final String CTX = "/result/";
        private final ConcurrentHashMap<UUID, Boolean> txResults;

        private ResultHandler(ConcurrentHashMap<UUID, Boolean> txResults) {
            this.txResults = txResults;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            var path = exchange.getRequestURI().getPath();
            var reqId = UUID.fromString(path.substring(CTX.length()));
            var result = txResults.remove(reqId);
            int respCode;
            if (result == null) {
                respCode = 404;
            } else if (result) {
                respCode = 200;
            } else {
                respCode = 502;
            }
            exchange.sendResponseHeaders(respCode, -1);
            exchange.close();
        }
    }

    private static class DepositHandler implements HttpHandler {
        private static final String CTX = "/tx/";
        private final Accountant accountant;
        private final ConcurrentHashMap<UUID, Boolean> txResults;

        private DepositHandler(Accountant accountant, ConcurrentHashMap<UUID, Boolean> txResults) {
            this.accountant = accountant;
            this.txResults = txResults;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Integer donator = null;
            Integer recipient = null;
            BigDecimal amount = null;
            String req;
            try (var s = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                req = URLDecoder.decode(s.next(), StandardCharsets.UTF_8);
            }

            for (var param : req.split("&")) {
                var kv = param.split("=");
                switch (kv[0]) {
                    case "donator" -> {
                        donator = Integer.parseInt(kv[1]);
                    }
                    case "recipient" -> {
                        recipient = Integer.parseInt(kv[1]);
                    }
                    case "amount" -> {
                        amount = new BigDecimal(kv[1]);
                    }
                }
            }
            Objects.requireNonNull(donator);
            Objects.requireNonNull(recipient);
            Objects.requireNonNull(amount);
            var reqId = UUID.randomUUID();
            exchange.getResponseHeaders().add("Location", ResultHandler.CTX + reqId);
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(303, 0);
            try (var out = new PrintWriter(exchange.getResponseBody())) {
                accountant.transfer(new UserAccount.User(donator), new UserAccount.User(recipient), amount, new TransactionHandle() {
                    @Override
                    public void onEvent(String description) {
                        out.println(description);
                    }

                    @Override
                    public void onSuccess(String comment) {
                        out.println(comment);
                        txResults.put(reqId, true);
                    }

                    @Override
                    public void onFailure(String error) {
                        out.println(error);
                        txResults.put(reqId, false);
                    }
                });
            }
        }
    }

    private static class WithdrawHandler implements HttpHandler {
        private static final String CTX = "/withdraw/";
        private final Accountant accountant;
        private final ConcurrentHashMap<UUID, Boolean> txResults;

        private WithdrawHandler(Accountant accountant, ConcurrentHashMap<UUID, Boolean> txResults) {
            this.accountant = accountant;
            this.txResults = txResults;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Integer donator = null;
            String recipientAddr = null;
            BigDecimal amount = null;
            String req;
            try (var s = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                req = URLDecoder.decode(s.next(), StandardCharsets.UTF_8);
            }

            for (var param : req.split("&")) {
                var kv = param.split("=");
                switch (kv[0]) {
                    case "donator" -> {
                        donator = Integer.parseInt(kv[1]);
                    }
                    case "addr" -> {
                        recipientAddr = kv[1];
                    }
                    case "amount" -> {
                        amount = new BigDecimal(kv[1]);
                    }
                }
            }
            Objects.requireNonNull(donator);
            Objects.requireNonNull(recipientAddr);
            Objects.requireNonNull(amount);
            var reqId = UUID.randomUUID();
            exchange.getResponseHeaders().add("Location", ResultHandler.CTX + reqId);
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(303, 0);
            OutputStream body = exchange.getResponseBody();
            try (var out = new PrintWriter(body, true)) {
                var resultFut = new CompletableFuture<Void>();
                accountant.withdraw(new UserAccount.User(donator), new WithdrawalService.Address(recipientAddr), amount, new TransactionHandle() {
                    @Override
                    public void onEvent(String description) {
                        out.println(description);
                    }

                    @Override
                    public void onSuccess(String comment) {
                        out.println(comment);
                        txResults.put(reqId, true);
                        resultFut.complete(null);
                    }

                    @Override
                    public void onFailure(String error) {
                        out.println(error);
                        txResults.put(reqId, false);
                        resultFut.complete(null);
                    }
                });
                resultFut.join();
            }
        }
    }
}
