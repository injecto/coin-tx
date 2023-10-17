package org.injecto.core;

public interface TransactionHandle {
    /**
     * Called with processing operation progress `description`. Might be called multiple times.
     */
    void onEvent(String description);
    /**
     * Called when operation finishes successfully.
     */
    void onSuccess(String comment);
    /**
     * Called when operation finishes with failure.
     */
    void onFailure(String error);
}
