package me.shoutto.sdk.internal;

/**
 * Results from an StmObserver process
 */

public class StmObservableResults<T> {

    private boolean error = false;
    private String errorMessage;
    private StmObservableType stmObservableType;
    private T result;

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public StmObservableType getStmObservableType() {
        return stmObservableType;
    }

    public void setStmObservableType(StmObservableType stmObservableType) {
        this.stmObservableType = stmObservableType;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
