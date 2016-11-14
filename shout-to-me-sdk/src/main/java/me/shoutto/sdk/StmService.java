package me.shoutto.sdk;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shoutto.sdk.internal.MessageNotificationIntentWrapper;
import me.shoutto.sdk.internal.ProximitySensorClient;
import me.shoutto.sdk.internal.http.StmEntityListRequestSync;
import me.shoutto.sdk.internal.NotificationManager;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.location.LocationUpdateListener;
import me.shoutto.sdk.internal.location.geofence.GeofenceManager;
import me.shoutto.sdk.internal.location.geofence.LocationUtils;
import me.shoutto.sdk.internal.location.geofence.database.GeofenceDatabaseSchema;
import me.shoutto.sdk.internal.location.geofence.database.GeofenceDbHelper;
import me.shoutto.sdk.internal.http.StmHttpSender;
import me.shoutto.sdk.internal.http.StmRequestQueue;
import me.shoutto.sdk.internal.location.LocationServicesClient;

/**
 * The main entry point to interact with the Shout to Me platform.  <code>StmService</code> is implemented as
 * an Android service and can therefore be bound or started in accordance with standard Android
 * usage.
 * <p>
 *
 * @see <a href="https://developer.android.com/guide/components/services.html" target="_blank">Android Services</a>
 */
public class StmService extends Service implements LocationUpdateListener {

    /**
     * The shared preferences key that <code>StmService</code> uses.
     */
    @Deprecated
    public static final String STM_SETTINGS_KEY = "stm_settings";

    /**
     * The Shout to Me REST API url.
     */
    public static final String DEFAULT_SERVER_URL = "https://app.shoutto.me/api/v1";

    /**
     * For general usage in communicating a failure result.
     */
    public static final String FAILURE = "me.shoutto.sdk.FAILURE";

    /**
     * For general usage in communicating a successful result.
     */
    public static final String SUCCESS = "me.shoutto.sdk.SUCCESS";

    /**
     * The key for the Shout to Me channel ID in the Android manifest.
     */
    public static final String CHANNEL_ID = "me.shoutto.sdk.CHANNEL_ID";

    /**
     * The key for the Shout to Me client token in the Android manifest.
     */
    public static final String CLIENT_TOKEN = "me.shoutto.sdk.CLIENT_TOKEN";

    private static final String TAG = StmService.class.getSimpleName();
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

    public StmService() {
    }

    /**
     * The class to be used when binding via the Android bound service method.
     */
    public class StmBinder extends Binder {
        public StmService getService() {
            return StmService.this;
        }
    }

    /**
     * Gets the client access token that was set in the Android manifest.
     * @return The client access token.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Returns the Shout to Me channel ID from local storage.
     * @return The Shout to Me channel ID
     */
    public String getChannelId() {
        return stmPreferenceManager.getChannelId();
    }

    /**
     * Calls the service asynchronously to get the available list of channels and returns
     * that list in the callback.
     * @param callback The callback to be executed or null.
     */
    public void getChannels(final StmCallback<List<Channel>> callback) {
        if (channelManager == null) {
            channelManager = new ChannelManager(this);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                channelManager.getChannels(callback);
            }
        }).start();
    }

    ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Gets the Shout to ME SDK installation ID.  Creates one if it didn't previously exist.
     * @return The Shout to Me SDK installation ID.
     */
    public String getInstallationId() {

        String installationId = stmPreferenceManager.getInstallationId();
        if (installationId == null) {
            stmPreferenceManager.setInstallationId(UUID.randomUUID().toString());
        }

        return stmPreferenceManager.getInstallationId();
    }

    /**
     * Returns the <code>LocationServicesClient</code>.
     * @return The LocationServicesClient.
     */
    public LocationServicesClient getLocationServicesClient() {
        return locationServicesClient;
    }

    /**
     * Calls the service to get the list of user's messages and returns the list in the callback.
     * Currently only returns 1000 records.
     * @param callback The callback to execute or null.
     */
    public void getMessages(final StmCallback<List<Message>> callback) {
        if (messageManager == null) {
            messageManager = new MessageManager(this);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                messageManager.getMessages(callback);
            }
        }).start();
    }

    /**
     * Returns the Shout to Me API URL.
     * @return The Shout to Me API URL.
     */
    public String getServerUrl() {
        return stmPreferenceManager.getServerUrl();
    }

    StmCallback<Shout> getShoutCreationCallback() {
        return shoutCreationCallback;
    }

    StmHttpSender getStmHttpSender() {
        return stmHttpSender;
    }

    /**
     * Calls the service to get the unread message count and returns the count to the callback.
     * @param callback The callback to execute or null.
     */
    public void getUnreadMessageCount(final StmCallback<Integer> callback) {
        if (messageManager == null) {
            messageManager = new MessageManager(this);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                messageManager.getUnreadMessageCount(callback);
            }
        }).start();
    }

    /**
     * Gets the in memory User object. If not previously instantiated, returns an empty
     * User object.
     * @return The User object.
     */
    public User getUser() {
        return user;
    }

    /**
     * Instantiates a User object asynchronously and returns it in the Callback.
     * @param callback The Callback to be executed or null.
     */
    public void getUser(final StmCallback<User> callback) {
        synchronized (this) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!user.isInitialized()) {
                        initializeUserSession();
                    }
                    user.get(callback);
                }
            }).start();
        }
    }

    private void initializeUserSession() {
        String userId = stmPreferenceManager.getUserId();
        String authToken = stmPreferenceManager.getAuthToken();
        if (userId == null || authToken == null) {
            createOrGetUserAccount();
            authToken = stmPreferenceManager.getAuthToken();
            userId = stmPreferenceManager.getUserId();
        }
        user.setId(userId);
        user.setAuthToken(authToken);
        user.setIsInitialized(true);
        Log.d(TAG, "User has been initialized");
    }

    private void createOrGetUserAccount() {
        try {
            stmHttpSender.getUserWithClientToken(user);
            stmPreferenceManager.setAuthToken(user.getAuthToken());
            stmPreferenceManager.setUserId(user.getId());
        } catch (Exception ex) {
            Log.e(TAG, "Could not create or get user account.", ex);
        }
    }

    /**
     * Returns the user auth token from local storage or gets it from the service if not in local
     * storage.  This token can be used to make calls to the Shout to Me REST API outside of the SDK.
     * @return The user auth token.
     */
    public String getUserAuthToken() {
        synchronized (this) {
            initializeUserSession();
            return stmPreferenceManager.getAuthToken();
        }
    }

    /**
     * Calls registered listeners when a hand wave gesture occurs.
     */
    public void handleHandWaveGesture() {
        if (overlay != null) {
            overlay.onHandWaveGesture();
        } else if (handWaveGestureListenerList.size() > 0) {
            for (HandWaveGestureListener handWaveGestureListener : handWaveGestureListenerList) {
                handWaveGestureListener.onHandWaveGesture();
            }
        }
    }

    /**
     * Sends request to the Shout to Me service to see if the user is subscribed to the specified
     * channel.
     * @param channelId The channel ID to check subscription status.
     * @param callback The callback to be executed or null.
     */
    public void isSubscribedToChannel(String channelId, final StmCallback<Boolean> callback) {

        if (channelId == null) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        Channel channel = new Channel(this);
        channel.setId(channelId);
        channel.isSubscribed(callback);
    }

    /**
     * Handles the Android bind lifecycle event. This is where most of the initialization takes place.
     * @param   intent The Intent that was used to bind to the service.
     * @return  The IBinder through which clients can call on to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {

        if (stmPreferenceManager == null) {
            stmPreferenceManager = new StmPreferenceManager(this);
        }

        try {
            ServiceInfo serviceInfo = getPackageManager().getServiceInfo(new ComponentName(this, this.getClass()), PackageManager.GET_META_DATA);
            Bundle bundle = serviceInfo.metaData;
            if (bundle == null) {
                Log.e(TAG, "Metadata with client token is missing. Please make sure to include the client token metadata in AndroidManifest.xml");
            } else {
                accessToken = bundle.getString(CLIENT_TOKEN);
                if (accessToken == null) {
                    Log.w(TAG, "Access token is null. Please make sure to include the access token when binding.");
                }

                String channelId = bundle.getString(CHANNEL_ID);
                Log.d(TAG, "Channel ID from manifest: " + channelId);
                if (channelId != null) {
                    setChannelId(channelId);
                }
            }
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Package name not found. Cannot start StmService.", ex);
        }

        // Initialize the RequestQueue
        StmRequestQueue.setInstance(this);

        // Create or get user
        this.user = new User(this);

        this.stmHttpSender = new StmHttpSender(this);

        geofenceDbHelper = new GeofenceDbHelper(this);
        geofenceManager = new GeofenceManager(this, geofenceDbHelper, new StmPreferenceManager(this));

        locationServicesClient = LocationServicesClient.getInstance(this);
        locationServicesClient.registerLocationUpdateListener(this);

        executorService = Executors.newFixedThreadPool(10);

        locationServicesClient.connectToService();
        proximitySensorClient = new ProximitySensorClient(this);

        return stmBinder;
    }

    /**
     * Handles location updates.
     * @param location The updated Location object.
     */
    @Override
    public void onLocationUpdate(final Location location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                geofenceManager.rebuildGeofencesInPlayServices(location);
            }
        }).start();
    }

    /**
     * Handles the Android <code>startService</code> event.
     * @param   intent The Intent used to the start the service.
     * @param   flags The flags passed into the start service process.
     * @param   startId The unique ID used to identify the start service request.
     * @return  The behavior of how to continue the service if it was killed.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_REDELIVER_INTENT;
    }

    /**
     * Handles the Android unbind lifecycle event.  Performs some clean up tasks.
     * @param   intent The Intent that was used to bind to this service.
     * @return  Return true if you would like to have the service's <code>onRebind(Intent)</code>
     *          method later called when new clients bind to it.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        locationServicesClient.unregisterLocationUpdateListener(this);
        locationServicesClient.disconnectFromService();
        proximitySensorClient.stopListening();
        return super.onUnbind(intent);
    }

    /**
     * Clears the user auth token from local storage and gets it from the service.
     * @throws Exception The exception that occurred.
     */
    public void refreshUserAuthToken() throws Exception {
        synchronized (this) {
            stmPreferenceManager.setAuthToken(null);
            stmPreferenceManager.setUserId(null);
            getUserAuthToken();
        }
    }

    /**
     * Registers a listener for hand wave gestures and starts listening if the first registration.
     * @param handWaveGestureListener The listener for hand wave gestures.
     */
    public void registerHandGestureListener(HandWaveGestureListener handWaveGestureListener) {
        if (!handWaveGestureListenerList.contains(handWaveGestureListener)) {
            handWaveGestureListenerList.add(handWaveGestureListener);
        }
        if (handWaveGestureListenerList.size() == 1) {
            proximitySensorClient.startListening();
        }
    }

    /**
     * Reinitializes the user by making a call to the service. Overrides previous values stored
     * in Shared Preferences following the call to the service.
     *
     * @param callback The callback to be executed or null
     */
    public void reloadUser(final StmCallback<User> callback) {
        synchronized (this) {
            user.setIsInitialized(false);
            stmPreferenceManager.setAuthToken(null);
            stmPreferenceManager.setUserId(null);
            getUser(callback);
        }
    }

    /**
     * Removes all geofences from the Android geofencing API and local storage. Not for general use.
     * Contact Shout to Me for more information.
     */
    public void removeAllGeofences() {
        geofenceManager.removeAllGeofences();
    }

    /**
     * Sets the Shout to Me channel ID in local storage.
     * @param channelId The Shout to Me channel ID.
     */
    public void setChannelId(String channelId) {
        stmPreferenceManager.setChannelId(channelId);
    }

    /**
     * Sets the maximum number of geofences to override the Android default in Shout to Me
     * processing.  Not for general use. Contact Shout to Me for more information.
     * @param maxGeofences The maximum number of geofences to create.
     */
    public void setMaxGeofences(Integer maxGeofences) {
        stmPreferenceManager.setMaxGeofences(maxGeofences);
    }

    void setOverlay(HandWaveGestureListener overlay) {
        this.overlay = overlay;
    }

    /**
     * Sets the Shout to Me API URL.  This method is not normally used for production releases.
     * It can be used to point to testing environments.  Contact Shout to Me for more information.
     * @param serverUrl The Shout to Me API server URL to use for HTTP calls.
     */
    public void setServerUrl(String serverUrl) {
        stmPreferenceManager.setServerUrl(serverUrl);
    }

    /**
     * Sets the callback to be executed following the creation of a shout.
     * @param shoutCreationCallback The callback to be executed following the creation of a shout or null.
     */
    public void setShoutCreationCallback(StmCallback<Shout> shoutCreationCallback) {
        this.shoutCreationCallback = shoutCreationCallback;
    }

    /**
     * Registers the user to receive notifications from the specified channel.
     * @param channelId The channel ID to subscribe to.
     * @param callback The callback to be executed or null.
     */
    public void subscribeToChannel(final String channelId, final StmCallback<Void> callback) {

        if (channelId == null) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        Channel channel = new Channel(this);
        channel.setId(channelId);
        channel.subscribe(callback);
    }

    /**
     * Synchronizes Shout to Me user notifications.  For each channel that the user is subscribed to,
     * this method will retrieve active messages from the service and either display them to the
     * user or create geofences.
     */
    public void synchronizeNotifications() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Conversation> activeConversations = new ArrayList<>();

                // Get subscribed channels
                StmEntityListRequestSync<Subscription> subscriptionRequest = new StmEntityListRequestSync<>();
                List<Subscription> subscriptionList = subscriptionRequest.process("GET", getUserAuthToken(),
                        getServerUrl() + Subscription.BASE_ENDPOINT, null, Subscription.getListSerializationType(),
                        Subscription.LIST_SERIALIZATION_KEY);

                // Get active conversations for those channels
                if (subscriptionList != null) {
                    for (Subscription subscription : subscriptionList) {
                        String conversationRequestUrl = getServerUrl() + Conversation.BASE_ENDPOINT
                                + "?channel_id=" + subscription.getChannelId() + "&date_field=expiration_date"
                                + "&hours=0";
                        StmEntityListRequestSync<Conversation> conversationRequest = new StmEntityListRequestSync<>();
                        List<Conversation> conversationList = conversationRequest.process("GET", getUserAuthToken(),
                                conversationRequestUrl, null, Conversation.getListSerializationType(),
                                Conversation.LIST_SERIALIZATION_KEY);
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
                        new String[]{GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID},
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
                    geofenceManager.removeGeofencesByIds(conversationsToRemove, LocationUtils.getLastKnownCoordinates(StmService.this));
                }

                // Process the notifications. Get channel information to provide channel name.
                getChannels(new Callback<List<Channel>>() {
                    @Override
                    public void onSuccess(StmResponse<List<Channel>> stmResponse) {
                        for (final Conversation conversation : activeConversations) {
                            String channelName = "";
                            for (Channel channel : stmResponse.get()) {
                                if (channel.getId().equals(conversation.getChannelId())) {
                                    channelName = channel.getName();
                                }
                            }

                            Bundle bundle = new Bundle();
                            bundle.putString(MessageNotificationIntentWrapper.EXTRA_CONVERSATION_ID, conversation.getId());
                            bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY, conversation.getPublishingMessage());
                            bundle.putString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID, conversation.getChannelId());
                            bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE, "conversation message");
                            bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TITLE, channelName);
                            processNotificationBundle(bundle);
                        }
                    }

                    @Override
                    public void onFailure(StmError stmError) {
                        Log.d(TAG, "Could not get channel info to process notifications. " + stmError.getMessage());

                        for (final Conversation conversation : activeConversations) {
                            Bundle bundle = new Bundle();
                            bundle.putString(MessageNotificationIntentWrapper.EXTRA_CONVERSATION_ID, conversation.getId());
                            bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY, conversation.getPublishingMessage());
                            bundle.putString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID, conversation.getChannelId());
                            bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE, "conversation message");
                            processNotificationBundle(bundle);
                        }
                    }
                });
            }
        }).start();
    }

    private void processNotificationBundle(final Bundle bundle) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NotificationManager notificationManager = new NotificationManager(StmService.this, bundle);
                notificationManager.processIncomingNotification(getServerUrl(), getUserAuthToken(), user.getId());
            }
        }).start();
    }

    /**
     * Unregisters a previously registered <code>HandWaveGestureListener</code>.
     * @param handWaveGestureListener The <code>HandWaveGestureListener</code> to unregister.
     */
    public void unregisterHandGestureListener(HandWaveGestureListener handWaveGestureListener) {
        handWaveGestureListenerList.remove(handWaveGestureListener);
        if (handWaveGestureListenerList.size() == 0) {
            proximitySensorClient.stopListening();
        }
    }

    /**
     * Unsubscribes the user from receiving notifications from the specified channel.
     * @param channelId The channel ID to unsubscribed from.
     * @param callback The callback to be executed or null.
     */
    public void unsubscribeFromChannel(final String channelId, final StmCallback<Void> callback) {

        if (channelId == null) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        Channel channel = new Channel(this);
        channel.setId(channelId);
        channel.unsubscribe(callback);
    }
}
