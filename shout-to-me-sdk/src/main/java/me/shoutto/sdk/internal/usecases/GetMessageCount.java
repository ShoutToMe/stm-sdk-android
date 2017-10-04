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

public class GetMessageCount extends BaseUseCase<Integer> {

    public GetMessageCount(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
    }

    public void get(StmCallback<Integer> callback) {
        this.callback = callback;
        stmEntityRequestProcessor.processRequest(HttpMethod.GET, new Message());
    }
}
