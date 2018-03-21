package me.shoutto.sdk.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Date;

import me.shoutto.sdk.StmService;

/**
 * An internal class to manage the shared preference data
 */
public class StmPreferenceManager {

    private static final String TAG = StmPreferenceManager.class.getSimpleName();
    private static final String STM_PREFERENCES = "me.shoutto.sdk.STM_PREFERENCES";
    private static final String PREF_AUTH_TOKEN = "me.shoutto.sdk.PREF_AUTH_TOKEN";
    private static final String PREF_CHANNEL_ID = "me.shoutto.sdk.PREF_CHANNEL_ID";
    private static final String PREF_INSTALLATION_ID = "me.shoutto.sdk.PREF_INSTALLATION_ID";
    private static final String PREF_SERVER_URL = "me.shoutto.sdk.PREF_SERVER_URL";
    private static final String PREF_USER_ID = "me.shoutto.sdk.USER_ID";
    private static final String PREF_USER_LOCATION_LAT = "me.shoutto.sdk.USER_LOCATION_LAT";
    private static final String PREF_USER_LOCATION_LON = "me.shoutto.sdk.USER_LOCATION_LON";
    private static final String PREF_USER_LOCATION_TIME = "me.shoutto.sdk.USER_LOCATION_TIME";
    private SharedPreferences sharedPreferences;

    public StmPreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(STM_PREFERENCES, Context.MODE_PRIVATE);
    }

    public String getAuthToken() {
        return sharedPreferences.getString(PREF_AUTH_TOKEN, null);
    }

    public void setAuthToken(String authToken) {
        setPreferenceString(PREF_AUTH_TOKEN, authToken);
    }

    public String getChannelId() {
        return sharedPreferences.getString(PREF_CHANNEL_ID, null);
    }

    public void setChannelId(String channelId) {
        setPreferenceString(PREF_CHANNEL_ID, channelId);
    }

    public String getInstallationId() {
        return sharedPreferences.getString(PREF_INSTALLATION_ID, null);
    }

    public void setInstallationId(String installationId) {
        setPreferenceString(PREF_INSTALLATION_ID, installationId);
    }

    public String getServerUrl() {
        String serverUrl = sharedPreferences.getString(PREF_SERVER_URL, null);
        if (serverUrl == null) {
            return StmService.DEFAULT_SERVER_URL;
        } else {
            return serverUrl;
        }
    }

    public void setServerUrl(String serverUrl) {
        setPreferenceString(PREF_SERVER_URL, serverUrl);
    }

    public String getUserId() {
        return sharedPreferences.getString(PREF_USER_ID, null);
    }

    public void setUserId(String userId) {
        setPreferenceString(PREF_USER_ID, userId);
    }

    public Double getUserLocationLat() {
        Double lat = null;
        String latString = sharedPreferences.getString(PREF_USER_LOCATION_LAT, null);
        if (latString != null) {
            try {
                lat = Double.parseDouble(latString);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Could not parse user lat: " + latString);
            }
        }
        return lat;
    }

    public void setUserLocationLat(Double lat) {
        setPreferenceString(PREF_USER_LOCATION_LAT, Double.toString(lat));
    }

    public Double getUserLocationLon() {
        Double lon = null;
        String lonString = sharedPreferences.getString(PREF_USER_LOCATION_LON, null);
        if (lonString != null) {
            try {
                lon = Double.parseDouble(lonString);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Could not parse user lon: " + lonString);
            }
        }
        return lon;
    }

    public void setUserLocationLon(Double lon) {
        setPreferenceString(PREF_USER_LOCATION_LON, Double.toString(lon));
    }

    public Long getUserLocationTime() {
        Long userLocationTime = sharedPreferences.getLong(PREF_USER_LOCATION_TIME, 0L);
        return (userLocationTime == 0L) ? null : userLocationTime;
    }

    public void setUserLocationTime(Long userLocationTime) {
        if (userLocationTime != null) {
            sharedPreferences.edit().putLong(PREF_USER_LOCATION_TIME, userLocationTime).apply();
        }
    }

    private void setPreferenceString(String key, String value) {
        if (value == null) {
            sharedPreferences.edit().remove(key).apply();
        } else {
            sharedPreferences.edit().putString(key, value).apply();
        }
    }
}
