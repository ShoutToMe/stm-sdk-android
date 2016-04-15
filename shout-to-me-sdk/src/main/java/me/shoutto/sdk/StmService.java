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
    private final IBinder stmBinder = new StmBinder();
    private String accessToken;
    private String deviceId;
    private String userAuthToken;
    private User user;
    private String channelId;
    private StmHttpSender stmHttpSender;
    private StmCallback<Shout> shoutCreationCallback;
    private ExecutorService executorService;
    private LocationServicesClient locationServicesClient;
    private ProximitySensorClient proximitySensorClient;
    private List<HandWaveGestureListener> handWaveGestureListenerList = new ArrayList<>();
    private SharedPreferences settings;
    private HandWaveGestureListener overlay;
    private String serverUrl = "https://app.shoutto.me/api/v1";
    private Channels channels;
    private int maxRecordingTimeInSeconds;

    public StmService() {
        maxRecordingTimeInSeconds = 0;
    }

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
            serverUrl = intent.getStringExtra("baseUrl");
        }

        if (accessToken != null) {
            // Initialize the RequestQueue
            StmRequestQueue.setInstance(this);

            // Create or get user
            this.user = new User(this);

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

    public User getUser() {
        return user;
    }

    public void getUser(StmCallback<User> callback) {
        synchronized (this) {
            if (!user.isInitialized()) {
                user.get(callback);
                user.setIsInitialized(true);
            } else {
                callback.onResponse(user);
            }
        }
    }

    public void reloadUser(final StmCallback<User> callback) {
        synchronized (this) {
            user.setIsInitialized(false);
            getUser(callback);
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
        if (this.channelId != channelId) {
            this.channelId = channelId;
            SharedPreferences.Editor editor = settings.edit();
            if (channelId == null) {
                editor.remove("channelId");
            } else {
                editor.putString("channelId", this.channelId);
            }
            editor.commit();
        }
    }

    public void getChannels(final StmCallback<List<Channel>> callback) {
        if (channels == null) {
            channels = new Channels(this, callback);
        }
    }

    public int getMaxRecordingTimeInSeconds() {
        Channel channel = channels.getChannel(channelId);
        if (channel == null) {
            Log.w(TAG, "No selected channel");
            return maxRecordingTimeInSeconds;
        } else {
            if (maxRecordingTimeInSeconds > channel.getDefaultMaxRecordingLengthSeconds()) {
                return maxRecordingTimeInSeconds;
            } else {
                return channel.getDefaultMaxRecordingLengthSeconds();
            }
        }
    }

    public void setMaxRecordingTimeInSeconds(int maxRecordingTimeInSeconds) {
        this.maxRecordingTimeInSeconds = maxRecordingTimeInSeconds;
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

    public void refreshUserAuthToken() throws Exception {
        synchronized (this) {
            SharedPreferences.Editor editor = settings.edit();
            editor.remove("userId");
            editor.remove("authToken");
            editor.commit();
            userAuthToken = null;
            getUserAuthToken();
        }
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

    public void setShoutCreationCallback(StmCallback<Shout> shoutCreationCallback) {
        this.shoutCreationCallback = shoutCreationCallback;
    }

    StmCallback<Shout> getShoutCreationCallback() {
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

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
