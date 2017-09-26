package me.shoutto.sdk.internal.usecases;

import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Base class for Shout to Me service use case classes
 */

abstract class BaseUseCase implements StmObserver {

    protected StmEntityRequestProcessor stmEntityRequestProcessor;

    protected BaseUseCase(StmEntityRequestProcessor stmEntityRequestProcessor) {
        this.stmEntityRequestProcessor = stmEntityRequestProcessor;
        this.stmEntityRequestProcessor.addObserver(this);
    }

    abstract void processCallback(StmObservableResults stmObservableResults);

    @Override
    public void update(StmObservableResults stmObservableResults) {
        processCallback(stmObservableResults);
    }
}
