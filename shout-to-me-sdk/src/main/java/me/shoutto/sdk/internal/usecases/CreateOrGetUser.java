package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Creates or gets a user account from the Shout to Me service using the affiliate's client token
 */

public class CreateOrGetUser extends BaseUseCase<StmBaseEntity, User> {

    private static final String TAG = CreateOrGetUser.class.getSimpleName();

    public CreateOrGetUser(StmRequestProcessor<StmBaseEntity> stmRequestProcessor) {
        super(stmRequestProcessor);
    }

    public void createOrGet(User user, StmCallback<User> callback) {
        if (user == null) {
            Log.e(TAG, "Cannot create or get user from Shout to Me service.  Passed in User is null");
            if (callback != null) {
                StmError stmError = new StmError("Cannot create or get user from Shout to Me service.  Passed in User is null", true, StmError.SEVERITY_MAJOR);
                callback.onError(stmError);
            }
            return;
        }

        this.callback = callback;

        stmRequestProcessor.processRequest(HttpMethod.POST, user);
    }
}
