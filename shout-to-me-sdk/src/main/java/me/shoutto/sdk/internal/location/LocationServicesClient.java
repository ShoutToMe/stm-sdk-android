package me.shoutto.sdk.internal.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Wrapper class for LocationServices.
 */
public class LocationServicesClient {

    private static final String TAG = LocationServicesClient.class.getSimpleName();
    private static final int LOCATION_UPDATE_DELAY_MILLIS = 2000;
    private static LocationServicesClient instance;

    // Location updates intervals in sec
    private static final int LONG_DELAY_UPDATE_INTERVAL = 120000; // 2 min
    private static final int LONG_DELAY_FASTEST_INTERVAL = 60000; // 1 min
    private static final int LONG_DELAY_DISPLACEMENT = 1610; // 1 mile
    private static final int SHORT_DELAY_INTERVAL = 100; // 100 ms

    private Location lastLocation;
    private Location pendingLocation;
    private double latitude;
    private double longitude;
    private List<LocationUpdateListener> locationUpdateListeners;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean isListeningForLocation = false;

    // TODO: Make this a Pending Intent callback to remove Context in static instance of UpdateUserLocationController
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (isListeningForLocation) {
                processShortDelayLocationUpdate(locationResult.getLastLocation());
            } else {
                processLongDelayLocationUpdate(locationResult.getLastLocation());
            }
        }
    };

    private LocationServicesClient() {
        locationUpdateListeners = new ArrayList<>();
    }

    public static LocationServicesClient getInstance() {
        if (instance == null) {
            instance = new LocationServicesClient();
        }
        return instance;
    }

    double getLatitude() {
        return latitude;
    }

    double getLongitude() {
        return longitude;
    }

    void connectToService(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getFusedLocationClient(context).getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    processLongDelayLocationUpdate(location);
                }
            });
        }

        startLongDelayLocationUpdates(context);
    }

    public void disconnectFromService() {
        stopLocationUpdates();
        mFusedLocationClient = null;
    }

    void refreshLocation(final Context context) {
        if (!isListeningForLocation) {
            processTimerStart(context);

            Handler handler = new Handler();
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    processTimerEnd(context);
                }
            };
            handler.postDelayed(r, LOCATION_UPDATE_DELAY_MILLIS);
        }
    }

    private FusedLocationProviderClient getFusedLocationClient(Context context) {
        if (mFusedLocationClient == null) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        }
        return  mFusedLocationClient;
    }

    private void processTimerStart(Context context) {
        if (!isListeningForLocation) {
            isListeningForLocation = true;
            startShortDelayLocationUpdates(context);
        }
    }

    private void processTimerEnd(Context context) {
        processLongDelayLocationUpdate(pendingLocation);
        isListeningForLocation = false;
        pendingLocation = null;
        startLongDelayLocationUpdates(context);
    }

    private void startLongDelayLocationUpdates(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getFusedLocationClient(context).requestLocationUpdates(createLongDelayLocationRequest(), locationCallback, null);
        }
    }

    private void startShortDelayLocationUpdates(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getFusedLocationClient(context).requestLocationUpdates(createShortDelayLocationRequest(), locationCallback, null);
        }
    }

    private LocationRequest createLongDelayLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(LONG_DELAY_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(LONG_DELAY_FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setSmallestDisplacement(LONG_DELAY_DISPLACEMENT);
        return locationRequest;
    }

    private LocationRequest createShortDelayLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(SHORT_DELAY_INTERVAL);
        locationRequest.setFastestInterval(SHORT_DELAY_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void processLongDelayLocationUpdate(Location location) {
        if (location != null) {
            long timeSinceLastUpdate = -1;

            if (lastLocation != null) {
                timeSinceLastUpdate = new Date().getTime() - lastLocation.getTime();
            }

            if (timeSinceLastUpdate == -1 || timeSinceLastUpdate > 15000) {
                lastLocation = location;
                latitude = lastLocation.getLatitude();
                longitude = lastLocation.getLongitude();

                Log.d(TAG, "Long delay location update " + String.valueOf(latitude) + "," + String.valueOf(longitude));

                for (LocationUpdateListener locationUpdateListener : locationUpdateListeners) {
                    locationUpdateListener.onLocationUpdate(lastLocation);
                }
            }
        }
    }

    private void processShortDelayLocationUpdate(Location location) {
        pendingLocation = location;
    }

    private void stopLocationUpdates() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    void registerLocationUpdateListener(LocationUpdateListener locationUpdateListener) {
        if (locationUpdateListener != null) {
            locationUpdateListeners.add(locationUpdateListener);
        }
    }

    public void unregisterLocationUpdateListener(LocationUpdateListener locationUpdateListener) {
        if (locationUpdateListener != null) {
            locationUpdateListeners.remove(locationUpdateListener);
        }
    }
}
