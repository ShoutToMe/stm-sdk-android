package me.shoutto.sdk.internal.usecases;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.UserLocation;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.database.UserLocationDao;
import me.shoutto.sdk.internal.database.UserLocationDaoImpl;
import me.shoutto.sdk.internal.database.UserLocationRecord;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;
import me.shoutto.sdk.internal.location.UserLocationListener;
import me.shoutto.sdk.internal.location.geofence.GeofenceManager;

/**
 * Process an update to the user's newLocation by creating a new geofence and sending a newLocation
 * update to the Shout to Me service.
 */

public class UpdateUserLocation extends BaseUseCase<SortedSet<? extends StmBaseEntity>, Void> {

    private static final String TAG = UpdateUserLocation.class.getSimpleName();
    private static final Object lock = new Object();
    private static final long MINIMUM_UPDATE_PERIOD = 15000;
    private static final String BROADCAST_ACTION = "me.shoutto.sdk.action.UpdateUserLocation";
    private static final String PACKAGE_VOIGO = "me.shoutto.voigo";
    private GeofenceManager geofenceManager;
    private StmPreferenceManager stmPreferenceManager;
    private UserLocationDao userLocationDao;
    private Context context;
    private String triggeringEvent;
    private UserLocation userLocation;

    public UpdateUserLocation(StmRequestProcessor<SortedSet<? extends StmBaseEntity>> stmRequestProcessor,
                              GeofenceManager geofenceManager,
                              StmPreferenceManager stmPreferenceManager,
                              UserLocationDao userLocationDao,
                              Context context, String triggeringEvent) {
        super(stmRequestProcessor);
        this.geofenceManager = geofenceManager;
        this.stmPreferenceManager = stmPreferenceManager;
        this.userLocationDao = userLocationDao;
        this.context = context;
        this.triggeringEvent = triggeringEvent;
    }

    public void update(Location location, StmCallback<Void> callback) {
        if (location == null) {
            Log.w(TAG, "Cannot process location update. Location is null");
            if (callback != null) {
                StmError stmError = new StmError("Cannot process location update. Location is null", false, StmError.SEVERITY_MAJOR);
                callback.onError(stmError);
            }
            return;
        }

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

                distanceSinceLastUpdate = lastUserLocation.distanceTo(location);

                if (lastUserLocationTime != null && (location.getTime() - lastUserLocationTime) < MINIMUM_UPDATE_PERIOD) {
                    // TODO: May need to be able to handle older dates with project_until_date at some point
                    shouldUpdateUserLocation = false;
                }
            }

            if (!shouldUpdateUserLocation) {
                Log.d(TAG, "User is still within minimum time for update. Location not updated.");
                if (callback != null) {
                    callback.onResponse(null);
                }
                return;
            }

            Log.d(TAG, "Location requires updating. Updating now.");
            stmPreferenceManager.setUserLocationLat(location.getLatitude());
            stmPreferenceManager.setUserLocationLon(location.getLongitude());
            stmPreferenceManager.setUserLocationTime(location.getTime());

            this.callback = callback;

            sendLocationUpdateBroadcast(location, distanceSinceLastUpdate);
            updateGeofence(location);
            processUpdateRequest(location, distanceSinceLastUpdate);
        }

    }

    /**
     * Broadcast for Shout to Me's internal app. Not relevant for other clients.
     * @param newLocation the new Location
     * @param distanceSinceLastUpdate the distance in meters since the last update
     */
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

    private void updateGeofence(Location newLocation) {
        try {
            geofenceManager.addUserLocationGeofence(newLocation);
        } catch (Exception e) {
            Log.w(TAG, "Unable to create user newLocation geofence");
        }
    }

    private void processUpdateRequest(Location location, Float distanceSinceLastUpdate) {

        SortedSet<UserLocation> userLocationSortedSet = new TreeSet<>(new UserLocationDateComparator());

        // Get any previously saved user locations
        userLocationSortedSet.addAll(userLocationDao.getAllUserLocations());

        // Add the current location
        Double[] coordinates = { location.getLongitude(), location.getLatitude() };
        userLocation = new UserLocation();
        userLocation.setLocation(new UserLocation.Location(coordinates));
        if (location.getTime() > 0) {
            userLocation.setDate(new Date(location.getTime()));
        } else {
            userLocation.setDate(new Date());
        }

        if (distanceSinceLastUpdate != null) {
            userLocation.setMetersSinceLastUpdate(distanceSinceLastUpdate);
        }
        userLocationSortedSet.add(userLocation);

        stmRequestProcessor.processRequest(HttpMethod.PUT, userLocationSortedSet);
    }

    @Override
    public void processCallback(StmObservableResults stmObservableResults) {
        Log.d(TAG, "User location was updated.");
        userLocationDao.deleteAllUserLocationRecords();
        super.processCallback(stmObservableResults);
    }

    @Override
    public void processCallbackError(StmObservableResults stmObservableResults) {

        Log.w(TAG, "An error occurred during user location update. " + stmObservableResults.getErrorMessage());
        // Insert the failed object into the db
        UserLocationRecord userLocationRecord = new UserLocationRecord();
        userLocationRecord.setDate(userLocation.getDate());
        userLocationRecord.setLat(userLocation.getLocation().getCoordinates()[1]);
        userLocationRecord.setLon(userLocation.getLocation().getCoordinates()[0]);
        userLocationRecord.setRadius(userLocation.getLocation().getRadius());
        userLocationRecord.setType(userLocation.getLocation().getType());
        if (userLocation.getMetersSinceLastUpdate() != null) {
            userLocationRecord.setMetersSinceLastUpdate(userLocation.getMetersSinceLastUpdate());
        }
        userLocationDao.addUserLocationRecord(userLocationRecord);

        userLocationDao.truncateTable();

        super.processCallbackError(stmObservableResults);
    }

    class UserLocationDateComparator implements Comparator<UserLocation> {
        public int compare(UserLocation ul1, UserLocation ul2) {
            return ul2.getDate().compareTo(ul1.getDate());
        }
    }
}
