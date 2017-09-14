package me.shoutto.sdk.internal;

/**
 * Generic interface for observers of asynchronous operations
 */

public interface StmObserver {

    void update(StmObservableResults stmObservableResults);
}
