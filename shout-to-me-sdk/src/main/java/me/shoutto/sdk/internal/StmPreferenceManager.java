package me.shoutto.sdk.internal;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * An internal class to manage the shared preference data
 */
public class StmPreferenceManager {

    private static final String STM_PREFERENCES = "me.shoutto.sdk.STM_PREFERENCES";
    private static final String PREF_AUTH_TOKEN = "me.shoutto.sdk.PREF_AUTH_TOKEN";
    private static final String PREF_CHANNEL_ID = "me.shoutto.sdk.PREF_CHANNEL_ID";
    private static final String PREF_DEVICE_ID = "me.shoutto.sdk.PREF_DEVICE_ID";
    private static final String PREF_MAX_GEOFENCES = "me.shoutto.sdk.PREF_MAX_GEOFENCES";
    private static final String PREF_SERVER_URL = "me.shoutto.sdk.PREF_SERVER_URL";
    private static final String PREF_USER_ID = "me.shoutto.sdk.USER_ID";
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

    public String getDeviceId() {
        return sharedPreferences.getString(PREF_DEVICE_ID, null);
    }

    public void setDeviceId(String deviceId) {
        setPreferenceString(PREF_DEVICE_ID, deviceId);
    }

    public int getMaxGeofences() { return sharedPreferences.getInt(PREF_MAX_GEOFENCES, -1); }

    public void setMaxGeofences(Integer maxGeofences) { setPreferenceInt(PREF_MAX_GEOFENCES, maxGeofences); }

    public String getServerUrl() {
        return sharedPreferences.getString(PREF_SERVER_URL, null);
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

    private void setPreferenceString(String key, String value) {
        if (value == null) {
            sharedPreferences.edit().remove(key).apply();
        } else {
            sharedPreferences.edit().putString(key, value).apply();
        }
    }

    private void setPreferenceInt(String key, Integer value) {
        if (value == null) {
            sharedPreferences.edit().remove(key).apply();
        } else {
            sharedPreferences.edit().putInt(key, value).apply();
        }
    }
}
