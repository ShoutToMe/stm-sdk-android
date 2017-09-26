package me.shoutto.sdk.internal.usecases;

import java.util.List;

import me.shoutto.sdk.ChannelSubscription;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Subscribes the user to the specified channel
 */

public class CreateChannelSubscription extends BaseUseCase {

    private StmCallback<Void> callback;

    public CreateChannelSubscription(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
    }

    public void create(String channelId, StmCallback<Void> callback) {

        if (channelId == null || "".equals(channelId)) {
            StmError error = new StmError("channelId is required for creating channel subscriptions",
                    false, StmError.SEVERITY_MINOR);
            callback.onError(error);
            return;
        }

        this.callback = callback;

        ChannelSubscription channelSubscription = new ChannelSubscription();
        channelSubscription.setChannelId(channelId);

        stmEntityRequestProcessor.processRequest(HttpMethod.POST, channelSubscription);
    }

    @Override
    void processCallback(StmObservableResults stmObservableResults) {
        callback.onResponse(null);
    }
}
