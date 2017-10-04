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

public class GetMessages extends BaseUseCase<List<Message>> {

    public GetMessages(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
    }

    public void get(StmCallback<List<Message>> callback) {
        this.callback = callback;
        stmEntityRequestProcessor.processRequest(HttpMethod.GET, new Message());
    }
}
