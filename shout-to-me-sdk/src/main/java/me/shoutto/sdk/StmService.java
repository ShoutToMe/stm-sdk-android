package me.shoutto.sdk;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class StmService extends Service {

    public static final String STM_SETTINGS_KEY = "stm_settings";
    private static final String TAG = "StmService";
    public static String STM_BASE_API_URL = "https://app.shoutto.me/api/v1";
    private final IBinder stmBinder = new StmBinder();
    private String accessToken;
    private String deviceId;
    private String userAuthToken;
    private StmUser user;
    private String channelId;
    private StmHttpSender stmHttpSender;
    private StmCallback<StmShout> shoutCreationCallback;
    private ExecutorService executorService;
    private LocationServicesClient locationServicesClient;
    private ProximitySensorClient proximitySensorClient;
    private List<HandWaveGestureListener> handWaveGestureListenerList = new ArrayList<>();
    private SharedPreferences settings;
    private HandWaveGestureListener overlay;

    public StmService() {}

    public class StmBinder extends Binder {
        public StmService getService() {
            // Return this instance of LocalService so clients can call public methods
            return StmService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        accessToken = intent.getStringExtra("stmClientToken");
        if (intent.getStringExtra("baseUrl") != null) {
            STM_BASE_API_URL = intent.getStringExtra("baseUrl");
        }

        if (accessToken != null) {
            // Initialize the RequestQueue
            StmRequestQueue.setInstance(this);

            // Create or get user
            this.user = new StmUser(this);

            this.stmHttpSender = new StmHttpSender(this);
            locationServicesClient = new LocationServicesClient(this);
        } else {
            Log.w(TAG, "Access token is null. Please make sure to include the access token when binding.");
        }

        executorService = Executors.newFixedThreadPool(10);

        locationServicesClient.connectToService();
        proximitySensorClient = new ProximitySensorClient(this);

        settings = getSharedPreferences(STM_SETTINGS_KEY, 0);

        return stmBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        locationServicesClient.disconnectFromService();
        proximitySensorClient.stopListening();
        return super.onUnbind(intent);
    }

    public Context getContext() {
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public StmUser getUser() {
        return user;
    }

    public void getUser(StmCallback<StmUser> callback) {
        synchronized (this) {
            if (!user.isInitialized()) {
                user.get(callback);
                user.setIsInitialized(true);
            } else {
                callback.onResponse(user);
            }
        }
    }

    void handleHandWaveGesture() {
        if (overlay != null) {
            overlay.onHandWaveGesture();
        } else if (handWaveGestureListenerList.size() > 0) {
            for (HandWaveGestureListener handWaveGestureListener : handWaveGestureListenerList) {
                handWaveGestureListener.onHandWaveGesture();
            }
        }
    }

    public String getChannelId() {
        if (channelId == null) {
            channelId = settings.getString("channelId", "");
        }
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("channelId", this.channelId);
        editor.commit();
    }

    public String getUserAuthToken() throws Exception {
        synchronized (this) {
            if (userAuthToken == null) {
                String userIdFromSharedPrefs = settings.getString("userId", "");
                String authTokenFromSharedPrefs = settings.getString("authToken", "");
                if (!"".equals(userIdFromSharedPrefs) && !"".equals(authTokenFromSharedPrefs)) {
                    Log.i(TAG, "Loaded user ID & authToken from shared prefs");
                    user.setId(userIdFromSharedPrefs);
                    user.setAuthToken(authTokenFromSharedPrefs);
                    userAuthToken = authTokenFromSharedPrefs;
                } else {
                    stmHttpSender.getUserWithClientToken(user);

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("userId", user.getId());
                    editor.putString("authToken", user.getAuthToken());
                    editor.commit();
                    userAuthToken = user.getAuthToken();
                    Log.d(TAG, "channel " + channelId);
                }
            }
        }
        return userAuthToken;
    }

    public StmHttpSender getStmHttpSender() {
        return stmHttpSender;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public LocationServicesClient getLocationServicesClient() {
        return locationServicesClient;
    }

    public void setShoutCreationCallback(StmCallback<StmShout> shoutCreationCallback) {
        this.shoutCreationCallback = shoutCreationCallback;
    }

    StmCallback<StmShout> getShoutCreationCallback() {
        return shoutCreationCallback;
    }

    public void registerHandGestureListener(HandWaveGestureListener handWaveGestureListener) {
        if (!handWaveGestureListenerList.contains(handWaveGestureListener)) {
            handWaveGestureListenerList.add(handWaveGestureListener);
        }
        if (handWaveGestureListenerList.size() == 1) {
            proximitySensorClient.startListening();
        }
    }

    public void unregisterHandGestureListener(HandWaveGestureListener handWaveGestureListener) {
        handWaveGestureListenerList.remove(handWaveGestureListener);
        if (handWaveGestureListenerList.size() == 0) {
            proximitySensorClient.stopListening();
        }
    }

    public void setOverlay(HandWaveGestureListener overlay) {
        this.overlay = overlay;
    }

    /**
     * getDeviceId
     * Android does not have a way to get a guaranteed unique device ID.  This method is
     * based on this stackoverflow: http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id
     * @param
     * @return
     */
    public String getDeviceId() {

        if (deviceId == null) {
            SharedPreferences settings = getSharedPreferences(STM_SETTINGS_KEY, 0);
            String deviceIdFromSharedPrefs = settings.getString("deviceId", "");
            if (!"".equals(deviceIdFromSharedPrefs)) {
                Log.d(TAG, "Got device ID from shared prefs");
                Log.d(TAG, deviceIdFromSharedPrefs);
                deviceId = deviceIdFromSharedPrefs;
            } else {
                try {
                    final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    final String tmDevice, tmSerial, androidId;
                    tmDevice = "" + tm.getDeviceId();
                    tmSerial = "" + tm.getSimSerialNumber();
                    androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
                    String generatedDeviceId = deviceUuid.toString();

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("deviceId", generatedDeviceId);
                    editor.commit();

                    deviceId = generatedDeviceId;
                } catch(SecurityException ex) {
                    Log.e(TAG, "Shout to Me does not have sufficient permissions. ", ex);
                }
            }
        }

        return deviceId;
    }
}
