package me.shoutto.sdk.internal.usecases;

import java.util.List;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Gets messages for the user
 */

public class GetMessages extends BaseUseCase<StmBaseEntity, List<Message>> {

    public GetMessages(StmRequestProcessor<StmBaseEntity> stmRequestProcessor) {
        super(stmRequestProcessor);
    }

    public void get(StmCallback<List<Message>> callback) {
        this.callback = callback;
        stmRequestProcessor.processRequest(HttpMethod.GET, new Message());
    }
}
