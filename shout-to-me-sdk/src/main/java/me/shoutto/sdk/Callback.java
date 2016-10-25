package me.shoutto.sdk;

/**
 * The base class to use for callbacks when calling asynchronous SDK methods.
 */
public abstract class Callback<T> implements StmCallback<T> {

    /**
     * The method the SDK calls to pass through the successful response object.  A client app will
     * not call this method.
     * @param t The response object.
     */
    @Override
    public final void onResponse(T t) {
        onSuccess(new StmResponse<>(t));
    }

    /**
     * The method the SDK calls to pass through the error.  A client app will not call this method.
     * @param stmError The error object that contains information about the problem.
     */
    @Override
    public final void onError(StmError stmError) {
        onFailure(stmError);
    }

    /**
     * The method called on a successful response.
     * @param stmResponse The response object that contains a returned object.
     */
    public abstract void onSuccess(StmResponse<T> stmResponse);

    /**
     * The method called when an error occurs during the request.
     * @param stmError The error object that contains information about the problem.
     */
    public abstract void onFailure(StmError stmError);
}
