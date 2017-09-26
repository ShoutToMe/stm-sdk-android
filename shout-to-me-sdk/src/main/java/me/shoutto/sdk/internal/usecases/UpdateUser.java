package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.UpdateUserRequest;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Updates a Shout to Me user object.
 */

public class UpdateUser implements StmObserver {

    private static final String TAG = UpdateUser.class.getSimpleName();
    private StmEntityRequestProcessor stmEntityRequestProcessor;
    private StmCallback<User> callback;

    public UpdateUser(StmEntityRequestProcessor stmEntityRequestProcessor) {
        this.stmEntityRequestProcessor = stmEntityRequestProcessor;
        this.stmEntityRequestProcessor.addObserver(this);
    }

    public void update(UpdateUserRequest updateUserRequest, String userId, StmCallback<User> callback) {

        if (!updateUserRequest.isValid()) {
            String errorMessage = "UpdateUserRequest object is invalid";
            if (callback != null) {
                StmError error = new StmError(errorMessage, false, StmError.SEVERITY_MAJOR);
                callback.onError(error);
            } else {
                Log.w(TAG, errorMessage);
            }
        }

        this.callback = callback;

        User user = (User)updateUserRequest.adaptToBaseEntity();
        user.setId(userId);

        stmEntityRequestProcessor.processRequest(HttpMethod.PUT, user);
    }

    @Override
    public void update(StmObservableResults stmObservableResults) {
        if (callback != null) {
            callback.onResponse((User)stmObservableResults.getResult());
        }
    }
}
