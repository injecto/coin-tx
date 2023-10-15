package org.injecto.core;

import org.injecto.external.WithdrawalService;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

public class WithdrawalServiceCaller {
    private final WithdrawalService service;
    private final ScheduledExecutorService executor;
    private final ConcurrentMap<WithdrawalService.WithdrawalId, CompletableFuture<Boolean>> inFlight = new ConcurrentHashMap<>();

    public WithdrawalServiceCaller(WithdrawalService service) {
        this.service = service;
        this.executor = Executors.newScheduledThreadPool(1, new ThreadFactory());

        this.executor.scheduleWithFixedDelay(() -> {
            for (var id : inFlight.keySet()) {
                WithdrawalService.WithdrawalState state = service.getRequestState(id);
                switch (state) {
                    case PROCESSING -> {

                    }
                    case COMPLETED -> {
                        inFlight.remove(id).complete(true);
                    }
                    case FAILED -> {
                        inFlight.remove(id).complete(false);
                    }
                }
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Boolean> withdraw(WithdrawalService.Address address, BigDecimal amount) {
        var id = new WithdrawalService.WithdrawalId(UUID.randomUUID());
        var fut = new CompletableFuture<Boolean>();
        executor.execute(() -> {
            try {
                service.requestWithdrawal(id, address, amount);
                inFlight.put(id, fut);
            } catch (Exception e) {
                fut.complete(false);
            }
        });
        return fut;
    }

    static class ThreadFactory implements java.util.concurrent.ThreadFactory {
        private int counter;
        @Override
        public Thread newThread(Runnable r) {
            var t = new Thread(r);
            t.setName("withdraw-%d".formatted(counter++));
            return t;
        }
    }
}
