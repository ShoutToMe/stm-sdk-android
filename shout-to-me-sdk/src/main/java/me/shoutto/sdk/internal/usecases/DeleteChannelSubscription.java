package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.ChannelSubscription;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Unsubscribes the user from a channel
 */

public class DeleteChannelSubscription extends BaseUseCase<Void> {

    private static final String TAG = DeleteChannelSubscription.class.getSimpleName();

    public DeleteChannelSubscription(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
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
            stmEntityRequestProcessor.deleteObserver(this);
            return;
        }

        this.callback = callback;

        ChannelSubscription channelSubscription = new ChannelSubscription();
        channelSubscription.setChannelId(channelId);

        stmEntityRequestProcessor.processRequest(HttpMethod.DELETE, channelSubscription);
    }
}
