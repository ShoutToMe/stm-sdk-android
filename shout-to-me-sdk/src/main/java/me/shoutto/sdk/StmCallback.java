package me.shoutto.sdk;


interface StmCallback<T> {
    /** Invoked on successful HTTP response. */
    void onResponse(T t);

    /** Invoked when a network or unexpected exception occurred during the HTTP request. */
    void onError(StmError stmError);
}