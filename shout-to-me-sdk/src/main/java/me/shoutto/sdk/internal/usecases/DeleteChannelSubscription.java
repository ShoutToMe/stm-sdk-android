package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.ChannelSubscription;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Unsubscribes the user from a channel
 */

public class DeleteChannelSubscription extends BaseUseCase<StmBaseEntity, Void> {

    private static final String TAG = DeleteChannelSubscription.class.getSimpleName();

    public DeleteChannelSubscription(StmRequestProcessor<StmBaseEntity> stmRequestProcessor) {
        super(stmRequestProcessor);
    }

    public void delete(String channelId, StmCallback<Void> callback) {
        if (channelId == null || "".equals(channelId)) {
            String validationErrorMessage = "channelId is required for deleting channel subscriptions";
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

        stmRequestProcessor.processRequest(HttpMethod.DELETE, channelSubscription);
    }
}
