package me.shoutto.sdk.internal.usecases;

import me.shoutto.sdk.ChannelSubscription;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Unsubscribes the user from a channel
 */

public class DeleteChannelSubscription extends BaseUseCase {

    private StmCallback<Void> callback;

    public DeleteChannelSubscription(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
    }

    public void delete(String channelId, StmCallback<Void> callback) {
        if (channelId == null || "".equals(channelId)) {
            StmError error = new StmError("channelId is required for deleting channel subscriptions",
                    false, StmError.SEVERITY_MINOR);
            callback.onError(error);
            return;
        }

        this.callback = callback;

        ChannelSubscription channelSubscription = new ChannelSubscription();
        channelSubscription.setChannelId(channelId);

        stmEntityRequestProcessor.processRequest(HttpMethod.DELETE, channelSubscription);
    }

    @Override
    void processCallback(StmObservableResults stmObservableResults) {
        if (callback != null) {
            callback.onResponse(null);
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
