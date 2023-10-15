package org.injecto.core;

public interface TransactionHandle {
    void onEvent(String description);
    void onSuccess(String comment);
    void onFailure(String error);
}
