package me.shoutto.sdk.internal.usecases;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Gets the count of number of messages for the user
 */

public class GetMessageCount extends BaseUseCase {

    private StmCallback<Integer> callback;

    public GetMessageCount(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
    }

    public void get(StmCallback<Integer> callback) {
        this.callback = callback;
        stmEntityRequestProcessor.processRequest(HttpMethod.GET, new Message());
    }

    @Override
    void processCallback(StmObservableResults stmObservableResults) {
        if (callback != null) {
            callback.onResponse((Integer)stmObservableResults.getResult());
        }
    }

    @Override
    void processCallbackError(StmObservableResults stmObservableResults) {
        if (callback != null) {
            StmError error = new StmError(stmObservableResults.getErrorMessage(), false, StmError.SEVERITY_MINOR);
            callback.onError(error);
        }
    }
}
