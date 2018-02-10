package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Gets a user from the Shout to Me service.
 */

public class GetUser extends BaseUseCase<StmBaseEntity, User> {

    private static final String TAG = GetUser.class.getSimpleName();

    public GetUser(StmRequestProcessor<StmBaseEntity> stmRequestProcessor) {
        super(stmRequestProcessor);
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

        stmRequestProcessor.processRequest(HttpMethod.GET, user);
    }
}
