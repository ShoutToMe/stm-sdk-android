package me.shoutto.sdk;

/**
 * Created by tracyrojas on 9/20/15.
 */
public abstract class Callback<T> implements StmCallback<T> {

    @Override
    public final void onResponse(T t) {
        onSuccess(new StmResponse<T>(t));
    }

    @Override
    public final void onError(StmError stmError) {
        onFailure(stmError);
    }

    public abstract void onSuccess(StmResponse<T> stmResponse);
    public abstract void onFailure(StmError stmError);
}
