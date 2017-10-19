package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmService;
import me.shoutto.sdk.UpdateUserRequest;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Updates a Shout to Me user object.
 */

public class UpdateUser extends BaseUseCase<User> {

    private static final String TAG = UpdateUser.class.getSimpleName();
    private StmService stmService;

    public UpdateUser(StmEntityRequestProcessor stmEntityRequestProcessor, StmService stmService) {
        super(stmEntityRequestProcessor);
        this.stmService = stmService;
    }

    public void update(UpdateUserRequest updateUserRequest, String userId, StmCallback<User> callback) {

        String errorMessage = "";

        if (!updateUserRequest.isValid()) {
            errorMessage = "UpdateUserRequest object is invalid";
        } else if (userId == null || "".equals(userId)) {
            errorMessage = "Invalid user ID. Cannot update user";
        }

        if (!"".equals(errorMessage)) {
            if (callback != null) {
                StmError error = new StmError(errorMessage, false, StmError.SEVERITY_MAJOR);
                callback.onError(error);
            } else {
                Log.w(TAG, errorMessage);
            }
            stmEntityRequestProcessor.deleteObserver(this);
            return;
        }

        this.callback = callback;

        User user = (User)updateUserRequest.adaptToBaseEntity();
        user.setId(userId);

        stmEntityRequestProcessor.processRequest(HttpMethod.PUT, user);
    }

    @Override
    public void processCallback(StmObservableResults stmObservableResults) {
        User user = (User)stmObservableResults.getResult();

        if (user != null && stmService != null) {
            stmService.getUser().setChannelSubscriptions(user.getChannelSubscriptions());
            stmService.getUser().setEmail(user.getEmail());
            stmService.getUser().setHandle(user.getHandle());
            stmService.getUser().setPlatformEndpointEnabled(user.getPlatformEndpointEnabled());
            stmService.getUser().setPhone(user.getPhone());
            stmService.getUser().setTopicPreferences(user.getTopicPreferences());
        }

        if (callback != null) {
            callback.onResponse(user);
        }
    }

    @Override
    public void processCallbackError(StmObservableResults stmObservableResults) {
        if (callback != null) {
            StmError error = new StmError(stmObservableResults.getErrorMessage(), false, StmError.SEVERITY_MAJOR);
            callback.onError(error);
        }
    }
}
