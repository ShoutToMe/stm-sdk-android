package me.shoutto.sdk.internal.usecases;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.Date;

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
    private Context context;
    private Location newLocation;

    public UpdateUserLocation(StmEntityRequestProcessor stmEntityRequestProcessor,
                              GeofenceManager geofenceManager,
                              Context context) {
        super(stmEntityRequestProcessor);
        this.geofenceManager = geofenceManager;
        this.context = context;
    }

    public void update(Location newLocation) {
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

        StmPreferenceManager stmPreferenceManager = new StmPreferenceManager(context);
        Double oldLat = stmPreferenceManager.getUserLocationLat();
        Double oldLon = stmPreferenceManager.getUserLocationLon();
        if (oldLat != null && oldLon != null) {
            Location oldLocation = new Location("");
            oldLocation.setLatitude(oldLat);
            oldLocation.setLongitude(oldLon);
            userLocation.setMetersSinceLastUpdate(oldLocation.distanceTo(newLocation));
        }

        stmPreferenceManager.setUserLocationLat(newLocation.getLatitude());
        stmPreferenceManager.setUserLocationLon(newLocation.getLongitude());

        stmEntityRequestProcessor.processRequest(HttpMethod.PUT, userLocation);
    }
}
