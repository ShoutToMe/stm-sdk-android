package me.shoutto.sdk.internal.usecases;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.location.UserLocationListener;

/**
 * Initializes the location services, creates a geofence, and sends the location to the Shout to Me
 * service following a boot
 */

public class InitializeLocationAfterBoot implements StmObserver {

    private static final String TAG = InitializeLocationAfterBoot.class.getSimpleName();
    private static final int LOCATION_LISTENING_TIMEOUT = 10000;
    private Context context;
    private UserLocationListener userLocationListener;
    private StmCallback<Void> callback;
    private Handler locationListeningHandler;
    private Runnable cancelLocationListeningRunnable;
    private String authToken;
    private String serverUrl;
    private User user;

    public InitializeLocationAfterBoot(Context context, UserLocationListener userLocationListener) {
        this.context = context;
        this.userLocationListener = userLocationListener;
        locationListeningHandler = new Handler();
        cancelLocationListeningRunnable = new Runnable() {
            @Override
            public void run() {
                cancelLocationUpdate();
            }
        };

        StmPreferenceManager stmPreferenceManager = new StmPreferenceManager(context);
        authToken = stmPreferenceManager.getAuthToken();
        serverUrl = stmPreferenceManager.getServerUrl();
        user = new User();
        user.setId(stmPreferenceManager.getUserId());
    }

    public void initialize(StmCallback<Void> callback) {
        this.callback = callback;

        if (authToken == null || serverUrl == null || user.getId() == null) {
            processCallbackError(String.format("Required data not available for posting location " +
                            "to server. authToken=%s, serverUrl=%s, userId=%s",
                    authToken, serverUrl, user.getId()));
            return;
        }

        userLocationListener.addObserver(this);
        userLocationListener.updateUserLocation(context);

        locationListeningHandler.postDelayed(cancelLocationListeningRunnable, LOCATION_LISTENING_TIMEOUT);
    }

    @Override
    public void update(StmObservableResults stmObservableResults) {
        locationListeningHandler.removeCallbacks(cancelLocationListeningRunnable);

        if (!stmObservableResults.isError()) {
            if (callback != null) {
                callback.onResponse(null);
            }
        } else {
            if (callback != null) {
                StmError stmError = new StmError(stmObservableResults.getErrorMessage(), false, StmError.SEVERITY_MAJOR);
                callback.onError(stmError);
            }
        }

        userLocationListener.deleteObserver(this);
    }

    private void cancelLocationUpdate() {
        userLocationListener.deleteObserver(this);
        processCallbackError("Location cannot be retrieved");
    }

    private void processCallbackError(String message) {
        Log.w(TAG, "Error occurred initializing location after boot. " + message);
        if (callback != null) {
            StmError stmError = new StmError(message, false, StmError.SEVERITY_MINOR);
            callback.onError(stmError);
        }
        userLocationListener.deleteObserver(this);
    }
}
