package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.ChannelSubscription;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Gets a channel subscription for the user. The callback returns true if subscribed or false if not.
 */

public class GetChannelSubscription extends BaseUseCase<Boolean> {

    private static final String TAG = GetChannelSubscription.class.getSimpleName();
    private String channelId;

    public GetChannelSubscription(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
    }

    public void get(String channelId, String userId, StmCallback<Boolean> callback) {
        if (channelId == null || "".equals(channelId) || userId == null || "".equals(userId)) {
            String validationErrorMessage = "Invalid channelId or userId";
            if (channelId == null || "".equals(channelId)) {
                validationErrorMessage = "channelId is required for getting channel subscription";
            } else if (userId == null || "".equals(userId)) {
                validationErrorMessage = "userId is required for getting channel subscription";
            }
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
        this.channelId = channelId;

        User user = new User();
        user.setId(userId);

        stmEntityRequestProcessor.processRequest(HttpMethod.GET, user);
    }

    @Override
    public void processCallback(StmObservableResults stmObservableResults) {
        if (callback != null) {
            User user = (User)stmObservableResults.getResult();

            if (user.getChannelSubscriptions() == null || user.getChannelSubscriptions().size() == 0) {
                callback.onResponse(false);
                return;
            }

            for (String subscribedChannelId : user.getChannelSubscriptions()) {
                if (channelId.equals(subscribedChannelId)) {
                    callback.onResponse(true);
                    return;
                }
            }

            callback.onResponse(false);
        }
    }
}
