package me.shoutto.sdk.internal.usecases;

import java.util.List;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Gets messages for the user
 */

public class GetMessages extends BaseUseCase {

    private StmCallback<List<Message>> callback;

    public GetMessages(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
    }

    public void get(StmCallback<List<Message>> callback) {
        this.callback = callback;
        stmEntityRequestProcessor.processRequest(HttpMethod.GET, new Message());
    }

    @Override
    void processCallback(StmObservableResults stmObservableResults) {
        if (callback != null) {
            callback.onResponse((List<Message>)stmObservableResults.getResult());
        }
    }

    @Override
    void processCallbackError(StmObservableResults stmObservableResults) {
        if (callback != null) {
            StmError error = new StmError(stmObservableResults.getErrorMessage(), false, StmError.SEVERITY_MAJOR);
            callback.onError(error);
        }
    }
}
