package me.shoutto.sdk.internal.usecases;

import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Base class for Shout to Me service use case classes
 */

abstract class BaseUseCase<T> implements StmObserver {

    protected StmEntityRequestProcessor stmEntityRequestProcessor;
    protected StmCallback<T> callback;

    protected BaseUseCase(StmEntityRequestProcessor stmEntityRequestProcessor) {
        this.stmEntityRequestProcessor = stmEntityRequestProcessor;
        this.stmEntityRequestProcessor.addObserver(this);
    }

    @Override
    public void update(StmObservableResults stmObservableResults) {

        if (stmObservableResults.isError()) {
            processCallbackError(stmObservableResults);
            return;
        }

        processCallback(stmObservableResults);

        stmEntityRequestProcessor.deleteObserver(this);
    }

    /**
     * Default implementation of processCallback. Subclasses should override if they need more
     * specific functionality
     * @param stmObservableResults The observer result object
     */
    @SuppressWarnings("unchecked")
    public void processCallback(StmObservableResults stmObservableResults) {
        if (callback != null) {
            callback.onResponse((T)stmObservableResults.getResult());
        }
    }

    /**
     * Default implementation of processCallbackError. Subclasses should override if the need more
     * specific functionality
     * @param stmObservableResults The observer result object that contains error info
     */
    public void processCallbackError(StmObservableResults stmObservableResults) {
        if (callback != null) {
            StmError error = new StmError(stmObservableResults.getErrorMessage(), false, StmError.SEVERITY_MINOR);
            callback.onError(error);
        }
    }
}
