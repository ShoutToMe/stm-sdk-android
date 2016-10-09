package me.shoutto.sdk.internal.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for LocationServices.
 */
public class LocationServicesClient implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = LocationServicesClient.class.getSimpleName();
    private static LocationServicesClient instance;

    private Context context;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private LocationRequest locationRequest;
    private double latitude;
    private double longitude;
    private List<LocationUpdateListener> locationUpdateListeners;

    // Location updates intervals in sec
    private static final int UPDATE_INTERVAL = 20000; // 20 sec
    private static final int FASTEST_INTERVAL = 60000; // 1 min
    private static final int DISPLACEMENT = 500; // 500 meters

    private LocationServicesClient(Context context) {
        this.context = context;
        locationUpdateListeners = new ArrayList<>();
        buildGoogleApiClient();
        createLocationRequest();
    }

    public static LocationServicesClient getInstance(Context context) {
        if (instance == null) {
            instance = new LocationServicesClient(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (lastLocation != null) {
                latitude = lastLocation.getLatitude();
                longitude = lastLocation.getLongitude();
            }
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.w(TAG, "Google location services was suspended. Cause: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Integer errorCode = result.getErrorCode();
        Log.e(TAG, "Google play location services failed. Check the ConnectionResult error code: "
                + errorCode);
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        if (lastLocation != null) {
            latitude = lastLocation.getLatitude();
            longitude = lastLocation.getLongitude();
        }

        for (LocationUpdateListener locationUpdateListener : locationUpdateListeners) {
            locationUpdateListener.onLocationUpdate(location);
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void connectToService() {
        googleApiClient.connect();
    }

    public void disconnectFromService() {
        stopLocationUpdates();
        googleApiClient.disconnect();
    }

    public void registerLocationUpdateListener(LocationUpdateListener locationUpdateListener) {
        if (locationUpdateListener != null) {
            locationUpdateListeners.add(locationUpdateListener);
        }
    }

    public void unregisterLocationUpdateListener(LocationUpdateListener locationUpdateListener) {
        if (locationUpdateListener != null) {
            locationUpdateListeners.remove(locationUpdateListener);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }
}
