package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.ChannelSubscription;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Subscribes the user to the specified channel
 */

public class CreateChannelSubscription extends BaseUseCase<StmBaseEntity, Void> {

    private static final String TAG = CreateChannelSubscription.class.getSimpleName();

    public CreateChannelSubscription(StmRequestProcessor<StmBaseEntity> stmRequestProcessor) {
        super(stmRequestProcessor);
    }

    public void create(String channelId, StmCallback<Void> callback) {

        if (channelId == null || "".equals(channelId)) {
            String validationErrorMessage = "channelId is required for creating channel subscriptions";
            if (callback != null) {
                StmError error = new StmError(validationErrorMessage, false, StmError.SEVERITY_MINOR);
                callback.onError(error);
            } else {
                Log.w(TAG, validationErrorMessage);
            }
            stmRequestProcessor.deleteObserver(this);
            return;
        }

        this.callback = callback;

        ChannelSubscription channelSubscription = new ChannelSubscription();
        channelSubscription.setChannelId(channelId);

        stmRequestProcessor.processRequest(HttpMethod.PUT, channelSubscription);
    }
}
