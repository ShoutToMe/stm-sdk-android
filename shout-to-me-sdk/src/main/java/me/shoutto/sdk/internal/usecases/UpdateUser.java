package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmService;
import me.shoutto.sdk.UpdateUserRequest;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Updates a Shout to Me user object.
 */

public class UpdateUser extends BaseUseCase<StmBaseEntity, User> {

    private static final String TAG = UpdateUser.class.getSimpleName();
    private static final int STATE_GET_USER = 1;
    private static final int STATE_UPDATE_USER = 2;
    private static final int STATE_COMPLETED = 3;
    private StmService stmService;
    private int processingState;
    private User user;

    public UpdateUser(StmRequestProcessor<StmBaseEntity> stmRequestProcessor, StmService stmService) {
        super(stmRequestProcessor);
        this.stmService = stmService;
    }

    public void update(UpdateUserRequest updateUserRequest, String userId, boolean requiresGet, StmCallback<User> callback) {

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
            stmRequestProcessor.deleteObserver(this);
            return;
        }

        this.callback = callback;
        user = (User)updateUserRequest.adaptToBaseEntity();
        user.setId(userId);

        if (requiresGet) {
            processingState = STATE_GET_USER;
            executeGetUser();
        } else {
            processingState = STATE_UPDATE_USER;
            executeUpdateUser();
        }
    }

    private void executeGetUser() {
        stmRequestProcessor.processRequest(HttpMethod.GET, user);
    }

    private void executeUpdateUser() {
        stmRequestProcessor.processRequest(HttpMethod.PUT, user);
    }

    @Override
    public void update(StmObservableResults stmObservableResults) {

        if (stmObservableResults.isError()) {
            processCallbackError(stmObservableResults);
            return;
        }

        processCallback(stmObservableResults);

        if (processingState == STATE_COMPLETED)
            stmRequestProcessor.deleteObserver(this);
    }

    @Override
    public void processCallback(StmObservableResults stmObservableResults) {
        User userFromResults = (User)stmObservableResults.getResult();

        if (processingState == STATE_GET_USER) {
            if (userFromResults != null) {
                if (user.getMetaInfo() != null) {
                    User.MetaInfo metaInfoFromResults = userFromResults.getMetaInfo();
                    if (metaInfoFromResults == null) {
                        metaInfoFromResults = new User.MetaInfo();
                    }

                    if (user.getMetaInfo().getGender() != null) {
                        if ("".equals(user.getMetaInfo().getGender())) {
                            metaInfoFromResults.setGender(null);
                        } else {
                            metaInfoFromResults.setGender(user.getMetaInfo().getGender());
                        }
                    }

                    user.setMetaInfo(metaInfoFromResults);
                }

                processingState = STATE_UPDATE_USER;
                executeUpdateUser();
            } else {
                Log.e(TAG, "Did not successfully retrieve user from server");
                if (callback != null) {
                    StmError error = new StmError("Did not successfully retrieve user from server",
                            false, StmError.SEVERITY_MAJOR);
                    callback.onError(error);
                }
            }
        } else if (processingState == STATE_UPDATE_USER) {
            if (userFromResults != null && stmService != null) {
                stmService.getUser().setChannelSubscriptions(userFromResults.getChannelSubscriptions());
                stmService.getUser().setEmail(userFromResults.getEmail());
                stmService.getUser().setHandle(userFromResults.getHandle());
                stmService.getUser().setPlatformEndpointEnabled(userFromResults.getPlatformEndpointEnabled());
                stmService.getUser().setPhone(userFromResults.getPhone());
                stmService.getUser().setTopicPreferences(userFromResults.getTopicPreferences());
            }

            if (callback != null) {
                callback.onResponse(userFromResults);
            }

            processingState = STATE_COMPLETED;
        } else {
            Log.e(TAG, "Unknown UpdateUser processing state " + String.valueOf(processingState));
            if (callback != null) {
                StmError error = new StmError("Unknown UpdateUser processing state "
                        + String.valueOf(processingState),false, StmError.SEVERITY_MAJOR);
                callback.onError(error);
            }
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
