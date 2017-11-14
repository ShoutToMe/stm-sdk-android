package me.shoutto.sdk.internal.location.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.sample.geofencing.GeofenceErrorMessages;

import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.http.DefaultSyncEntityRequestProcessor;
import me.shoutto.sdk.internal.http.GsonRequestAdapter;
import me.shoutto.sdk.internal.http.NullResponseAdapter;
import me.shoutto.sdk.internal.http.UserLocationUrlProvider;
import me.shoutto.sdk.internal.usecases.UpdateUserLocation;

/**
 * Handles events from geofence transitions
 */

public class GeofenceTransitionsIntentService extends IntentService {

    private static final String TAG = GeofenceTransitionsIntentService.class.getSimpleName();

    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event was null. Cannot process geofence transition");
            return;
        } else if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Location location = geofencingEvent.getTriggeringLocation();
            if (location == null) {
                Log.e(TAG, "Location is null. Cannot process geofence transition");
                return;
            }

            Log.i(TAG,String.format("Geofence exit detected %f, %f", location.getLatitude(), location.getLongitude()));

            StmPreferenceManager stmPreferenceManager = new StmPreferenceManager(this);
            String userId = stmPreferenceManager.getUserId();
            String authToken = stmPreferenceManager.getAuthToken();
            String serverUrl = stmPreferenceManager.getServerUrl();

            User user = new User();
            user.setId(userId);

            DefaultSyncEntityRequestProcessor<Void> defaultSyncEntityRequestProcessor = new DefaultSyncEntityRequestProcessor<>(
                    new GsonRequestAdapter(),
                    new NullResponseAdapter(),
                    authToken,
                    new UserLocationUrlProvider(serverUrl, user)
            );

            UpdateUserLocation updateUserLocation
                    = new UpdateUserLocation(defaultSyncEntityRequestProcessor, new GeofenceManager(this), this);
            updateUserLocation.update(location);
        }
    }
}
