package me.shoutto.sdk.internal.usecases;

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
    private GeofenceManager geofenceManager;
    private Location newLocation;
    private StmPreferenceManager stmPreferenceManager;
    private Double lastUserLocationLat;
    private Double lastUserLocationLon;
    private float distanceSinceLastUpdate;

    public UpdateUserLocation(StmEntityRequestProcessor stmEntityRequestProcessor,
                              GeofenceManager geofenceManager,
                              StmPreferenceManager stmPreferenceManager) {
        super(stmEntityRequestProcessor);
        this.geofenceManager = geofenceManager;

        this.stmPreferenceManager = stmPreferenceManager;
        lastUserLocationLat = stmPreferenceManager.getUserLocationLat();
        lastUserLocationLon = stmPreferenceManager.getUserLocationLon();
    }

    public void update(Location newLocation, StmCallback<Void> callback, boolean forceUpdate) {
        if (newLocation == null) {
            Log.w(TAG, "Cannot process location update. Location is null");
            if (callback != null) {
                StmError stmError = new StmError("Cannot process location update. Location is null", false, StmError.SEVERITY_MAJOR);
                callback.onError(stmError);
            }
            return;
        }

        this.callback = callback;

        if (lastUserLocationLat != null && lastUserLocationLon != null) {
            Location lastUserLocation = new Location("");
            lastUserLocation.setLatitude(lastUserLocationLat);
            lastUserLocation.setLongitude(lastUserLocationLon);

            distanceSinceLastUpdate = lastUserLocation.distanceTo(newLocation);

            if (!forceUpdate && distanceSinceLastUpdate < GeofenceManager.GEOFENCE_RADIUS_IN_METERS) {
                Log.d(TAG, "User is still in Geofence. Ignore location update");
                if (callback != null) {
                    callback.onResponse(null);
                }
                return;
            }
        }

        this.newLocation = newLocation;

        updateGeofence();
        processUpdateRequest();
    }

    private void updateGeofence() {
        try {
            geofenceManager.addUserLocationGeofence(newLocation);
        } catch (Exception e) {
            Log.w(TAG, "Unable to create user newLocation geofence");
        }
    }

    private void processUpdateRequest() {
        Double[] coordinates = { newLocation.getLongitude(), newLocation.getLatitude() };
        UserLocation userLocation = new UserLocation();
        userLocation.setLocation(new UserLocation.Location(coordinates));
        userLocation.setDate(new Date());

        if (lastUserLocationLat != null && lastUserLocationLon != null) {
            userLocation.setMetersSinceLastUpdate(distanceSinceLastUpdate);
        }

        stmPreferenceManager.setUserLocationLat(newLocation.getLatitude());
        stmPreferenceManager.setUserLocationLon(newLocation.getLongitude());

        stmEntityRequestProcessor.processRequest(HttpMethod.PUT, userLocation);
    }
}
