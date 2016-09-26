package me.shoutto.sdk;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shoutto.sdk.internal.http.StmEntityListRequestSync;
import me.shoutto.sdk.internal.NotificationManager;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.geofence.GeofenceManager;
import me.shoutto.sdk.internal.geofence.database.GeofenceDatabaseSchema;
import me.shoutto.sdk.internal.geofence.database.GeofenceDbHelper;

/**
 * The primary service class for Shout to Me functionality.
 */
public class StmService extends Service {

    private static final String TAG = "StmService";
    @Deprecated
    public static final String STM_SETTINGS_KEY = "stm_settings";
    public static final String DEFAULT_SERVER_URL = "https://app.shoutto.me/api/v1";
    private static final String CLIENT_TOKEN_KEY = "me.shoutto.sdk.clientToken";
    private final IBinder stmBinder = new StmBinder();
    private String accessToken;
    private User user;
    private StmHttpSender stmHttpSender;
    private StmCallback<Shout> shoutCreationCallback;
    private ExecutorService executorService;
    private LocationServicesClient locationServicesClient;
    private ProximitySensorClient proximitySensorClient;
    private List<HandWaveGestureListener> handWaveGestureListenerList = new ArrayList<>();
    private StmPreferenceManager stmPreferenceManager;
    private HandWaveGestureListener overlay;
    private ChannelManager channelManager;
    private GeofenceDbHelper geofenceDbHelper;
    private GeofenceManager geofenceManager;
    private MessageManager messageManager;

    public StmService() {}

    public class StmBinder extends Binder {
        public StmService getService() {
            // Return this instance of LocalService so clients can call public methods
            return StmService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying StmService");
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {

        if (stmPreferenceManager == null) {
            stmPreferenceManager = new StmPreferenceManager(this);
        }

        try {
            ServiceInfo serviceInfo = getPackageManager().getServiceInfo(new ComponentName(this, this.getClass()), PackageManager.GET_META_DATA);
            Bundle bundle = serviceInfo.metaData;
            accessToken = bundle.getString(CLIENT_TOKEN_KEY);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Package name not found. Cannot start StmService.", ex);
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

        geofenceDbHelper = new GeofenceDbHelper(this);
        geofenceManager = new GeofenceManager(this, geofenceDbHelper);

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
        return stmPreferenceManager.getChannelId();
    }

    public void setChannelId(String channelId) {
        stmPreferenceManager.setChannelId(channelId);
    }

    public void getChannels(final StmCallback<List<Channel>> callback) {
        if (channelManager == null) {
            channelManager = new ChannelManager(this);
        }
        channelManager.getChannels(callback);
    }

    public void getMessages(final StmCallback<List<Message>> callback) {
        if (messageManager == null) {
            messageManager = new MessageManager(this);
        }
        messageManager.getMessages(callback);
    }

    public void getUnreadMessageCount(final StmCallback<Integer> callback) {
        if (messageManager == null) {
            messageManager = new MessageManager(this);
        }
        messageManager.getUnreadMessageCount(callback);
    }

    public String getUserAuthToken() {
        synchronized (this) {
            initializeUserSession();
            return stmPreferenceManager.getAuthToken();
        }
    }

    private void initializeUserSession() {
        String userId = stmPreferenceManager.getUserId();
        String authToken = stmPreferenceManager.getAuthToken();
        if (userId == null || authToken == null) {
            try {
                stmHttpSender.getUserWithClientToken(user);
                stmPreferenceManager.setAuthToken(user.getAuthToken());
                stmPreferenceManager.setUserId(user.getId());
            } catch (Exception ex) {
                Log.e(TAG, "Could not initialize user session.", ex);
            }
        } else {
            user.setId(userId);
            user.setAuthToken(authToken);
        }
    }

    public void refreshUserAuthToken() throws Exception {
        synchronized (this) {
            stmPreferenceManager.setAuthToken(null);
            stmPreferenceManager.setUserId(null);
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

    public String getDeviceId() {

        String deviceIdFromSharedPrefs = stmPreferenceManager.getDeviceId();
        if (deviceIdFromSharedPrefs == null) {
            stmPreferenceManager.setDeviceId(generateDeviceId());
        }

        return stmPreferenceManager.getDeviceId();
    }

    private String generateDeviceId() {
        String generatedDeviceId = null;
        try {
            final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            final String tmDevice, tmSerial, androidId;
            tmDevice = "" + tm.getDeviceId();
            tmSerial = "" + tm.getSimSerialNumber();
            androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
            generatedDeviceId = deviceUuid.toString();

        } catch(SecurityException ex) {
            Log.e(TAG, "Shout to Me does not have sufficient permissions. ", ex);
        }
        return generatedDeviceId;
    }

    public void synchronizeNotifications() {
        // TODO: Add callback functionality
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Conversation> activeConversations = new ArrayList<>();

                // Get subscribed channels
                StmEntityListRequestSync<Subscription> subscriptionRequest = new StmEntityListRequestSync<>();
                List<Subscription> subscriptionList = subscriptionRequest.process("GET", getUserAuthToken(),
                        getServerUrl() + Subscription.BASE_ENDPOINT, null, Subscription.getListSerializationType(),
                        Subscription.LIST_JSON_KEY);

                // Get active conversations for those channels
                if (subscriptionList != null) {
                    for (Subscription subscription : subscriptionList) {
                        String conversationRequestUrl = getServerUrl() + Conversation.BASE_ENDPOINT
                                + "?channel_id=" + subscription.getChannelId() + "&date_field=expiration_date"
                                + "&hours=0";
                        StmEntityListRequestSync<Conversation> conversationRequest = new StmEntityListRequestSync<>();
                        List<Conversation> conversationList = conversationRequest.process("GET", getUserAuthToken(),
                                conversationRequestUrl, null, Conversation.getListSerializationType(),
                                Conversation.LIST_JSON_KEY);
                        if (conversationList != null && conversationList.size() > 0) {
                            activeConversations.addAll(conversationList);
                        }
                    }
                }

                // Remove geofences not in the list
                List<String> conversationsToRemove = new ArrayList<>();
                SQLiteDatabase readableDatabase = geofenceDbHelper.getReadableDatabase();
                Cursor cursor = readableDatabase.query(
                        GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME,
                        new String[] {GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID},
                        null,
                        null,
                        null,
                        null,
                        null
                );
                if (cursor.moveToFirst()) {
                    do {
                        String conversationId = cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID));
                        boolean isConversationActive = false;
                        for (Conversation conversation : activeConversations) {
                            if (conversation.getId().equals(conversationId)) {
                                isConversationActive = true;
                            }
                        }
                        if (!isConversationActive) {
                            conversationsToRemove.add(conversationId);
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
                readableDatabase.close();

                if (conversationsToRemove.size() > 0) {
                    geofenceManager.removeGeofencesByIds(conversationsToRemove);
                }

                // Process the notifications
                for (Conversation conversation : activeConversations) {
                    Bundle bundle = new Bundle();
                    bundle.putString(MessageNotificationIntentWrapper.EXTRA_CONVERSATION_ID, conversation.getId());
                    bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY, conversation.getPublishingMessage());
                    bundle.putString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID, conversation.getChannelId());
                    bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE, "conversation message");

                    NotificationManager notificationManager = new NotificationManager(StmService.this, bundle);
                    notificationManager.processIncomingNotification(getServerUrl(), getUserAuthToken(), user.getId());
                }

            }
        }).start();
    }

    public String getServerUrl() {
        String serverUrl = stmPreferenceManager.getServerUrl();
        if (serverUrl == null) {
            return DEFAULT_SERVER_URL;
        } else {
            return serverUrl;
        }
    }

    public void setServerUrl(String serverUrl) {
        stmPreferenceManager.setServerUrl(serverUrl);
    }

    public void setMaxGeofences(Integer maxGeofences) {
        stmPreferenceManager.setMaxGeofences(maxGeofences);
    }

    public void removeAllGeofences() {
        geofenceManager.removeAllGeofences();
    }
}
