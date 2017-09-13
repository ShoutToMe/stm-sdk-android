package me.shoutto.sdk.internal;

/**
 * Generic observable interface for asynchronous operations
 */

public interface StmObservable {

    void addObserver(StmObserver o);
    void deleteObserver(StmObserver o);
    void notifyObservers(StmObservableResults stmObserverResults);
}
