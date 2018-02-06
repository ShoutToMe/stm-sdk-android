package me.shoutto.sdk.internal.usecases;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import java.util.Date;

import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.UserLocation;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;
import me.shoutto.sdk.internal.location.geofence.GeofenceManager;

/**
 * Process an update to the user's newLocation by creating a new geofence and sending a newLocation
 * update to the Shout to Me service.
 */

public class UpdateUserLocation extends BaseUseCase<Void> {

    private static final String TAG = UpdateUserLocation.class.getSimpleName();
    private static final Object lock = new Object();
    private static final long MINIMUM_UPDATE_PERIOD = 15000;
    private static final String BROADCAST_ACTION = "me.shoutto.sdk.action.UpdateUserLocation";
    private static final String PACKAGE_VOIGO = "me.shoutto.voigo";
    private GeofenceManager geofenceManager;
    private StmPreferenceManager stmPreferenceManager;
    private Context context;
    private String triggeringEvent;

    public UpdateUserLocation(StmEntityRequestProcessor stmEntityRequestProcessor,
                              GeofenceManager geofenceManager,
                              StmPreferenceManager stmPreferenceManager,
                              Context context, String triggeringEvent) {
        super(stmEntityRequestProcessor);
        this.geofenceManager = geofenceManager;
        this.stmPreferenceManager = stmPreferenceManager;
        this.context = context;
        this.triggeringEvent = triggeringEvent;
    }

    public void update(Location newLocation, StmCallback<Void> callback) {
        if (newLocation == null) {
            Log.w(TAG, "Cannot process location update. Location is null");
            if (callback != null) {
                StmError stmError = new StmError("Cannot process location update. Location is null", false, StmError.SEVERITY_MAJOR);
                callback.onError(stmError);
            }
            return;
        }

        this.callback = callback;

        boolean shouldUpdateUserLocation = true;
        Float distanceSinceLastUpdate = null;

        synchronized (lock) {
            Double lastUserLocationLat = stmPreferenceManager.getUserLocationLat();
            Double lastUserLocationLon = stmPreferenceManager.getUserLocationLon();
            Long lastUserLocationTime = stmPreferenceManager.getUserLocationTime();

            if (lastUserLocationLat != null && lastUserLocationLon != null) {
                Location lastUserLocation = new Location("");
                lastUserLocation.setLatitude(lastUserLocationLat);
                lastUserLocation.setLongitude(lastUserLocationLon);

                distanceSinceLastUpdate = lastUserLocation.distanceTo(newLocation);

                if (distanceSinceLastUpdate < (GeofenceManager.GEOFENCE_RADIUS_IN_METERS * 0.9)) {
                    shouldUpdateUserLocation = false;
                } else if (lastUserLocationTime != null) {
                    if ((newLocation.getTime() - lastUserLocationTime) < MINIMUM_UPDATE_PERIOD) {
                        shouldUpdateUserLocation = false;
                    }
                }
            }

            if (!shouldUpdateUserLocation) {
                if (callback != null) {
                    callback.onResponse(null);
                }
                return;
            }

            stmPreferenceManager.setUserLocationLat(newLocation.getLatitude());
            stmPreferenceManager.setUserLocationLon(newLocation.getLongitude());
            stmPreferenceManager.setUserLocationTime(newLocation.getTime());
        }

        sendLocationUpdateBroadcast(newLocation, distanceSinceLastUpdate);
        updateGeofence(newLocation);
        processUpdateRequest(newLocation, distanceSinceLastUpdate);
    }

    private void updateGeofence(Location newLocation) {
        try {
            geofenceManager.addUserLocationGeofence(newLocation);
        } catch (Exception e) {
            Log.w(TAG, "Unable to create user newLocation geofence");
        }
    }

    private void processUpdateRequest(Location newLocation, Float distanceSinceLastUpdate) {
        Double[] coordinates = { newLocation.getLongitude(), newLocation.getLatitude() };
        UserLocation userLocation = new UserLocation();
        userLocation.setLocation(new UserLocation.Location(coordinates));
        userLocation.setDate(new Date());

        if (distanceSinceLastUpdate != null) {
            userLocation.setMetersSinceLastUpdate(distanceSinceLastUpdate);
        }

        stmEntityRequestProcessor.processRequest(HttpMethod.PUT, userLocation);
    }

    private void sendLocationUpdateBroadcast(Location newLocation, Float distanceSinceLastUpdate) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION);
        intent.putExtra("timestamp", newLocation.getTime());
        intent.putExtra("lat",newLocation.getLatitude());
        intent.putExtra("lon", newLocation.getLongitude());
        intent.putExtra("accuracy", newLocation.getAccuracy());
        intent.putExtra("distanceSinceLastUpdate", distanceSinceLastUpdate);
        intent.putExtra("triggeringEvent", triggeringEvent);
        intent.setPackage(PACKAGE_VOIGO);
        context.sendBroadcast(intent);
    }
}
