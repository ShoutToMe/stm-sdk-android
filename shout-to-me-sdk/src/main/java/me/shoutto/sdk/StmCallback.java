package me.shoutto.sdk;

/**
 * The interface used by {@link Callback}.
 * @param <T>
 */
public interface StmCallback<T> {
    /** Invoked on successful HTTP response. */
    void onResponse(T t);

    /** Invoked when a network or unexpected exception occurred during the HTTP request. */
    void onError(StmError stmError);
}