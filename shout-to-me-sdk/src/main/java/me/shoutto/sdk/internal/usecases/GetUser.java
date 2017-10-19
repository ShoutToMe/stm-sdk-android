package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Gets a user from the Shout to Me service.
 */

public class GetUser extends BaseUseCase {

    private static final String TAG = GetUser.class.getSimpleName();

    public GetUser(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
    }

    public void get(String userId, StmCallback<User> callback) {

        if (userId == null || "".equals(userId)) {
            if (callback != null) {
                StmError error = new StmError("Invalid user ID", false, StmError.SEVERITY_MINOR);
                callback.onError(error);
            } else {
                Log.w(TAG, "Invalid user ID");
            }
            return;
        }

        this.callback = callback;

        User user = new User();
        user.setId(userId);

        stmEntityRequestProcessor.processRequest(HttpMethod.GET, user);
    }
}
