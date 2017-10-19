package me.shoutto.sdk.internal.location.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

/**
 * GeofenceManager handles all geofence Google Play API and Database activities.
 */
public class GeofenceManager {

    private final static String TAG = GeofenceManager.class.getName();
    private final static String USER_LOCATION_GEOFENCE_ID = "me.shoutto.sdk.geofence.UserLocation";
    public final static float GEOFENCE_RADIUS_IN_METERS = 3219.0f;

    private Context context;
    private GeofencingClient geofencingClient;
    private PendingIntent pendingIntent;

    public GeofenceManager(Context context) {
        this.context = context;
        geofencingClient = LocationServices.getGeofencingClient(context);
    }

    public void addUserLocationGeofence(Location userLocation) throws SecurityException, IllegalArgumentException {

        int finePermissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (finePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Cannot add Geofence. ACCESS_FINE_LOCATION not granted.");
        }

        if (userLocation.getLatitude() == 0.0 || userLocation.getLongitude() == 0.0) {
            throw new IllegalArgumentException("Invalid location object");
        }

        geofencingClient.addGeofences(getGeofencingRequest(userLocation), getGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User location geofence added successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Failed to add Shout to Me user location geofence");
                    }
                });
    }

    private GeofencingRequest getGeofencingRequest(Location userLocation) {
        Geofence geofence = new Geofence.Builder()
                .setCircularRegion(userLocation.getLatitude(), userLocation.getLongitude(), GEOFENCE_RADIUS_IN_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setRequestId(USER_LOCATION_GEOFENCE_ID)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        return new GeofencingRequest.Builder()
                .addGeofence(geofence)
                .build();
    }

    private PendingIntent getGeofencePendingIntent() {

        if (pendingIntent != null) {
            return pendingIntent;
        }

        Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
        pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public void removeUserLocationGeofence(List<String> geofenceIdsToRemove) {
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(USER_LOCATION_GEOFENCE_ID);
        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Removed geofence: " + USER_LOCATION_GEOFENCE_ID);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Failed to remove geofence: " + USER_LOCATION_GEOFENCE_ID);
                    }
                });
    }
}
