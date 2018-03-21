package me.shoutto.sdk.internal.usecases;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Gets the count of number of messages for the user
 */

public class GetMessageCount extends BaseUseCase<StmBaseEntity, Integer> {

    public GetMessageCount(StmRequestProcessor<StmBaseEntity> stmRequestProcessor) {
        super(stmRequestProcessor);
    }

    public void get(StmCallback<Integer> callback) {
        this.callback = callback;
        stmRequestProcessor.processRequest(HttpMethod.GET, new Message());
    }
}
