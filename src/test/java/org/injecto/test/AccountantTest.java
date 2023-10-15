package org.injecto.test;

import org.injecto.core.*;
import org.injecto.external.WithdrawalService;
import org.injecto.external.WithdrawalServiceStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class AccountantTest {
    @Test
    void accountTest() {
        var acc0 = new UserAccount(new UserAccount.User(0), BigDecimal.valueOf(5));
        var acc1 = new UserAccount(new UserAccount.User(1), BigDecimal.valueOf(5));
        WithdrawalServiceCaller service = new WithdrawalServiceCaller(new WithdrawalServiceStub());
        var addr0 = new WithdrawalAddress(service, new WithdrawalService.Address("addr0"));

        {
            var fut = new CompletableFuture<Void>();
            acc1.deposit(acc0, BigDecimal.valueOf(4), new TransactionHandle() {
                @Override
                public void onEvent(String description) {
                }

                @Override
                public void onSuccess(String comment) {
                    fut.complete(null);
                }

                @Override
                public void onFailure(String error) {
                    Assertions.fail(error);
                }
            });
            fut.join();
        }

        {
            var fut = new CompletableFuture<Void>();
            acc1.deposit(acc0, BigDecimal.valueOf(2), new TransactionHandle() {
                @Override
                public void onEvent(String description) {
                }

                @Override
                public void onSuccess(String comment) {
                    Assertions.fail(comment);
                }

                @Override
                public void onFailure(String error) {
                    fut.complete(null);
                }
            });
            fut.join();
        }

        {
            var fut = new CompletableFuture<Void>();
            addr0.deposit(acc0, BigDecimal.valueOf(1), new TransactionHandle() {
                @Override
                public void onEvent(String description) {
                }

                @Override
                public void onSuccess(String comment) {
                    fut.complete(null);
                }

                @Override
                public void onFailure(String error) {
                    fut.complete(null);
                }
            });
            fut.join();
        }
    }

    @Test
    void concurrentTest() throws InterruptedException {
        var accountant = new Accountant(new WithdrawalServiceStub());
        var deposited = BigDecimal.ZERO;
        var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        var accounts = 5;
        for (int i = 0; i < accounts; i++) {
            BigDecimal deposit = BigDecimal.valueOf(100);
            accountant.register(new UserAccount(new UserAccount.User(i), deposit));
            deposited = deposited.add(deposit);
        }
        var withdrew = new AtomicReference<>(BigDecimal.ZERO);

        var tasks = 1000;
        var latch = new CountDownLatch(tasks);
        for (int i = 0; i < tasks; i++) {
            executor.execute(() -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                if (rnd.nextBoolean()) {
                    accountant.transfer(new UserAccount.User(rnd.nextInt(accounts)), new UserAccount.User(rnd.nextInt(accounts)), BigDecimal.valueOf(rnd.nextLong(1, 10)), new TransactionHandle() {
                        @Override
                        public void onEvent(String description) {
                            System.out.printf("%s %s%n", Thread.currentThread().getName(), description);
                        }

                        @Override
                        public void onSuccess(String comment) {
                            System.out.printf("%s %s%n", Thread.currentThread().getName(), comment);
                            latch.countDown();
                        }

                        @Override
                        public void onFailure(String error) {
                            System.out.printf("%s %s%n", Thread.currentThread().getName(), error);
                            latch.countDown();
                        }
                    });
                } else {
                    var amount = BigDecimal.valueOf(rnd.nextLong(1, 10));
                    accountant.withdraw(new UserAccount.User(rnd.nextInt(accounts)), new WithdrawalService.Address("addr"), amount, new TransactionHandle() {
                        @Override
                        public void onEvent(String description) {
                            System.out.printf("%s %s%n", Thread.currentThread().getName(), description);
                        }

                        @Override
                        public void onSuccess(String comment) {
                            System.out.printf("%s %s%n", Thread.currentThread().getName(), comment);
                            withdrew.updateAndGet(w -> w.add(amount));
                            latch.countDown();
                        }

                        @Override
                        public void onFailure(String error) {
                            System.out.printf("%s %s%n", Thread.currentThread().getName(), error);
                            latch.countDown();
                        }
                    });
                }
            });
        }

        latch.await();
        executor.shutdown();
        if (executor.awaitTermination(1, TimeUnit.SECONDS)) {
            var total = accountant.accounts().stream().map(UserAccount::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
            Assertions.assertEquals(deposited.subtract(withdrew.get()), total);
        } else {
            throw new IllegalStateException("executor didn't stop");
        }
    }
}
