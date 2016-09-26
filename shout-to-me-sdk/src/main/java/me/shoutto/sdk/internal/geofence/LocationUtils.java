package me.shoutto.sdk.internal.geofence;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

/**
 * Utilities to help handle LocationManager related information.
 */
public class LocationUtils {

    private static final String TAG = "LocationUtils";
    private static final long TWO_MINUTES_IN_MILLIS = 1000 * 60 * 2;

    public static Location getLastKnownCoordinates(Context context) {

        Location location = null;

        int finePermissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int coursePermissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if ((finePermissionCheck == PackageManager.PERMISSION_GRANTED || coursePermissionCheck == PackageManager.PERMISSION_GRANTED)
                && isLocationProviderEnabled(context)) {

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location gpsLocation, networkLocation;
            double gpsLat = 0 , gpsLon = 0, networkLat = 0, networkLon = 0;
            float gpsAccuracy = 0.0f, networkAccuracy = 0.0f;
            long gpsTime = 0, networkTime = 0;
            boolean isGpsSignificantlyNewer = false, isNetworkSignificantlyNewer = false;

            try {
                gpsLocation = locationManager.getLastKnownLocation("gps");
                gpsAccuracy = gpsLocation.getAccuracy();
                gpsLat = gpsLocation.getLatitude();
                gpsLon = gpsLocation.getLongitude();
                gpsTime = gpsLocation.getTime();
            } catch (Exception ex) {
                // Ignore
            }

            try {
                networkLocation = locationManager.getLastKnownLocation("network");
                networkAccuracy = networkLocation.getAccuracy();
                networkLat = networkLocation.getLatitude();
                networkLon = networkLocation.getLongitude();
                networkTime = networkLocation.getTime();
            } catch (Exception ex) {
                // Ignore
            }

            if (gpsTime - networkTime > TWO_MINUTES_IN_MILLIS) {
                isGpsSignificantlyNewer = true;
            } else if (networkTime - gpsTime > TWO_MINUTES_IN_MILLIS) {
                isNetworkSignificantlyNewer = true;
            }

            if (isGpsSignificantlyNewer) {
                location = buildLocation(LocationManager.GPS_PROVIDER, gpsLat, gpsLon);
            } else if (isNetworkSignificantlyNewer) {
                location = buildLocation(LocationManager.NETWORK_PROVIDER, networkLat, networkLon);
            } else {
                if (gpsAccuracy > 0 && networkAccuracy > 0) {
                    if (gpsAccuracy < networkAccuracy) {
                        location = buildLocation(LocationManager.GPS_PROVIDER, gpsLat, gpsLon);
                    } else {
                        location = buildLocation(LocationManager.NETWORK_PROVIDER, networkLat, networkLon);
                    }
                } else if (gpsAccuracy > 0) {
                    location = buildLocation(LocationManager.GPS_PROVIDER, gpsLat, gpsLon);
                } else if (networkAccuracy > 0) {
                    location = buildLocation(LocationManager.NETWORK_PROVIDER, networkLat, networkLon);
                }
            }

        } else {
            Log.w(TAG, "User does not have location permissions or providers enabled.  Cannot get last known location.");
        }

        return location;
    }

    private static Location buildLocation(String provider, double lat, double lon) {
        Location location = new Location(provider);
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }

    public static boolean isLocationProviderEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }
}
