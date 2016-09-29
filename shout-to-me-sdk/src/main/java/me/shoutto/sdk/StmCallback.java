package me.shoutto.sdk;

public interface StmCallback<T> {
    /** Successful HTTP response. */
    void onResponse(T t);

    /** Invoked when a network or unexpected exception occurred during the HTTP request. */
    void onError(StmError stmError);
}