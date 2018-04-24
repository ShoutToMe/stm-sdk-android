package me.shoutto.sdk;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shoutto.sdk.internal.ChannelManager;
import me.shoutto.sdk.internal.ProximitySensorClient;
import me.shoutto.sdk.internal.S3Client;
import me.shoutto.sdk.internal.http.BasicAuthHeaderProvider;
import me.shoutto.sdk.internal.http.ChannelSubscriptionUrlProvider;
import me.shoutto.sdk.internal.http.CountResponseAdapter;
import me.shoutto.sdk.internal.http.CreateUserUrlProvider;
import me.shoutto.sdk.internal.http.DefaultEntityRequestProcessorSync;
import me.shoutto.sdk.internal.http.GsonListResponseAdapter;
import me.shoutto.sdk.internal.http.GsonUserResponseAdapter;
import me.shoutto.sdk.internal.http.NullResponseAdapter;
import me.shoutto.sdk.internal.http.MessageCountUrlProvider;
import me.shoutto.sdk.internal.http.TopicUrlProvider;
import me.shoutto.sdk.internal.location.LocationServicesClient;
import me.shoutto.sdk.internal.location.UserLocationListener;
import me.shoutto.sdk.internal.usecases.CreateChannelSubscription;
import me.shoutto.sdk.internal.usecases.CreateOrGetUser;
import me.shoutto.sdk.internal.usecases.CreateTopicPreference;
import me.shoutto.sdk.internal.usecases.DeleteChannelSubscription;
import me.shoutto.sdk.internal.usecases.DeleteTopicPreference;
import me.shoutto.sdk.internal.usecases.GetChannelSubscription;
import me.shoutto.sdk.internal.usecases.GetMessage;
import me.shoutto.sdk.internal.usecases.GetMessageCount;
import me.shoutto.sdk.internal.usecases.GetMessages;
import me.shoutto.sdk.internal.usecases.GetUser;
import me.shoutto.sdk.internal.usecases.UpdateUser;
import me.shoutto.sdk.internal.usecases.UploadShout;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.http.DefaultUrlProvider;
import me.shoutto.sdk.internal.http.GsonRequestAdapter;
import me.shoutto.sdk.internal.http.GsonObjectResponseAdapter;
import me.shoutto.sdk.internal.http.DefaultEntityRequestProcessorAsync;
import me.shoutto.sdk.internal.http.StmHttpSender;
import me.shoutto.sdk.internal.http.StmRequestQueue;

/**
 * The main entry point to interact with the Shout to Me platform.  <code>StmService</code> is implemented as
 * an Android service and can therefore be bound or started in accordance with standard Android
 * usage.
 * <p>
 *
 * @see <a href="https://developer.android.com/guide/components/services.html" target="_blank">Android Services</a>
 */
public class StmService extends Service {

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

    /**
     * AWS Cognito Identity Pool ID for use with the AWS SDK
     */
    public static final String AWS_COGNITO_IDENTITY_POOL_ID = "us-east-1:4ec2b44e-0dde-43e6-a279-6ee1cf241b05";

    private static final String TAG = StmService.class.getSimpleName();
    private static final Object initializationLock = new Object();
    private final IBinder stmBinder = new StmBinder();
    private String accessToken;
    private User user;
    private StmHttpSender stmHttpSender;
    private StmCallback<Shout> shoutCreationCallback;
    private ExecutorService executorService;
    private ProximitySensorClient proximitySensorClient;
    private List<HandWaveGestureListener> handWaveGestureListenerList = new ArrayList<>();
    private StmPreferenceManager stmPreferenceManager;
    private HandWaveGestureListener overlay;
    private ChannelManager channelManager;
    private UserLocationListener userLocationListener;

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

    private Handler connectToLocationServicesHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(android.os.Message msg) {
            userLocationListener = new UserLocationListener(LocationServicesClient.getInstance(), StmService.this);
            userLocationListener.startTrackingUserLocation(StmService.this);
        }
    };

    /**
     * Adds a topic preference to the user's record. This will result in a user only receiving
     * notifications for the specified topics.
     * @param topic The topic
     * @param callback An optional callback or null
     */
    public void addTopicPreference(String topic, StmCallback<Void> callback) {

        if (topic == null) {
            String validationErrorMessage = "topic cannot be null";
            if (callback != null) {
                StmError error = new StmError(validationErrorMessage, false, StmError.SEVERITY_MINOR);
                callback.onError(error);
                return;
            } else {
                throw new IllegalArgumentException(validationErrorMessage);
            }
        }

        DefaultEntityRequestProcessorAsync<Void> defaultEntityRequestProcessorAsync = new DefaultEntityRequestProcessorAsync<>(
                new GsonRequestAdapter<StmBaseEntity>(),
                StmRequestQueue.getInstance(),
                new NullResponseAdapter(),
                getUserAuthToken(),
                new TopicUrlProvider(getServerUrl(), user)
        );

        CreateTopicPreference createTopicPreference = new CreateTopicPreference(defaultEntityRequestProcessorAsync);
        createTopicPreference.create(topic, callback);
    }

    /**
     * The method to create a new shout programmatically, as opposed to through the Shout to Me
     * Recording Overlay
     * @param createShoutRequest A CreateShoutRequest object with all required fields
     * @param callback An optional callback or null
     */
    public void createShout(CreateShoutRequest createShoutRequest, StmCallback<Shout> callback) {
        refreshUserLocation();

        DefaultEntityRequestProcessorAsync<Shout> defaultEntityRequestProcessorAsync = new DefaultEntityRequestProcessorAsync<>(
                new GsonRequestAdapter<StmBaseEntity>(),
                StmRequestQueue.getInstance(),
                new GsonObjectResponseAdapter<Shout>(Shout.SERIALIZATION_KEY, Shout.getSerializationType()),
                getUserAuthToken(),
                new DefaultUrlProvider(this.getServerUrl())
        );
        UploadShout shoutUploader = new UploadShout(this, new S3Client(this), defaultEntityRequestProcessorAsync);
        shoutUploader.upload(createShoutRequest, callback);
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
                channelManager.getChannels(StmService.this, callback);
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
     * Returns the <code>UserLocationListener</code>.
     * @return The UserLocationListener.
     */
    //TODO: Only used in one place. Refactor so only lat & lon are passed back, not an object.
    public UserLocationListener getUserLocationListener() {
        return userLocationListener;
    }

    /**
     * Gets a single message from the Shout to Me service
     * @param messageId The message ID
     * @param callback An optional callback or null
     */
    public void getMessage(String messageId, StmCallback<Message> callback) {
        DefaultEntityRequestProcessorAsync<Message> defaultEntityRequestProcessorAsync = new DefaultEntityRequestProcessorAsync<>(
                null,
                StmRequestQueue.getInstance(),
                new GsonObjectResponseAdapter<Message>(Message.SERIALIZATION_KEY, Message.getSerializationType()),
                getUserAuthToken(),
                new DefaultUrlProvider(getServerUrl())
        );

        GetMessage getMessage = new GetMessage(defaultEntityRequestProcessorAsync);
        getMessage.get(messageId, callback);
    }

    /**
     * Calls the service to get the list of user's messages and returns the list in the callback.
     * Currently only returns 1000 records.
     * @param callback The callback to execute or null.
     */
    public void getMessages(final StmCallback<List<Message>> callback) {
        DefaultEntityRequestProcessorAsync<List<Message>> defaultEntityRequestProcessorAsync
                = new DefaultEntityRequestProcessorAsync<>(
                null,
                StmRequestQueue.getInstance(),
                new GsonListResponseAdapter<List<Message>, Message>(
                        Message.LIST_SERIALIZATION_KEY,
                        Message.SERIALIZATION_KEY,
                        Message.getSerializationListType(),
                        Message.class
                ),
                getUserAuthToken(),
                new DefaultUrlProvider(getServerUrl())
        );

        GetMessages getMessages = new GetMessages(defaultEntityRequestProcessorAsync);
        getMessages.get(callback);
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
        DefaultEntityRequestProcessorAsync<Integer> defaultEntityRequestProcessorAsync = new DefaultEntityRequestProcessorAsync<>(
                null,
                StmRequestQueue.getInstance(),
                new CountResponseAdapter(),
                getUserAuthToken(),
                new MessageCountUrlProvider(getServerUrl(), true)
        );

        GetMessageCount getUnreadMessageCount = new GetMessageCount(defaultEntityRequestProcessorAsync);
        getUnreadMessageCount.get(callback);
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

        if (user.getId() == null) {
            StmError stmError = new StmError("User has not been initialized", true, StmError.SEVERITY_MINOR);
            callback.onError(stmError);
            return;
        }

        DefaultEntityRequestProcessorAsync<User> defaultEntityRequestProcessorAsync = new DefaultEntityRequestProcessorAsync<>(
                new GsonRequestAdapter<StmBaseEntity>(),
                StmRequestQueue.getInstance(),
                new GsonUserResponseAdapter(),
                getUserAuthToken(),
                new DefaultUrlProvider(getServerUrl())
        );
        GetUser getUser = new GetUser(defaultEntityRequestProcessorAsync);
        getUser.get(user.getId(), callback);
    }

    private void initializeUserSession() {
        synchronized (initializationLock) {
            if (!user.isInitialized()) {
                String userId = stmPreferenceManager.getUserId();
                String authToken = stmPreferenceManager.getAuthToken();
                if (userId == null || authToken == null) {

                    User user = new User();
                    user.setDeviceId(getInstallationId());

                    DefaultEntityRequestProcessorSync<User> stmRequestProcessor = new DefaultEntityRequestProcessorSync<>(
                            new GsonRequestAdapter<StmBaseEntity>(),
                            new GsonUserResponseAdapter(),
                            new BasicAuthHeaderProvider(getAccessToken()),
                            new CreateUserUrlProvider(stmPreferenceManager.getServerUrl())
                    );
                    CreateOrGetUser createOrGetUser = new CreateOrGetUser(stmRequestProcessor);
                    createOrGetUser.createOrGet(user, new Callback<User>() {
                        @Override
                        public void onSuccess(StmResponse<User> stmResponse) {
                            User userFromResponse = stmResponse.get();
                            stmPreferenceManager.setAuthToken(userFromResponse.getAuthToken());
                            stmPreferenceManager.setUserId(userFromResponse.getId());

                            getUser().setIsInitialized(true);
                            getUser().setId(userFromResponse.getId());
                            getUser().setAuthToken(userFromResponse.getAuthToken());

                            Log.d(TAG, "User has been initialized from Shout to Me service");
                        }

                        @Override
                        public void onFailure(StmError stmError) {
                            Log.e(TAG, "Could not create or get user. " + stmError.getMessage());
                        }
                    });
                } else {
                    user.setId(userId);
                    user.setAuthToken(authToken);
                    user.setIsInitialized(true);

                    Log.d(TAG, "User has been initialized via Shared Preferences");
                }
            } else {
                Log.d(TAG, "User is already initialized");
            }
        }
    }

    /**
     * Returns the user auth token from local storage or gets it from the service if not in local
     * storage.  This token can be used to make calls to the Shout to Me REST API outside of the SDK.
     * @return The user auth token.
     */
    public String getUserAuthToken() {
        initializeUserSession();
        return stmPreferenceManager.getAuthToken();
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
            String validationErrorMessage = "channelId cannot be null";
            if (callback != null) {
                StmError error = new StmError(validationErrorMessage, false, StmError.SEVERITY_MINOR);
                callback.onError(error);
                return;
            } else {
                throw new IllegalArgumentException(validationErrorMessage);
            }
        }

        DefaultEntityRequestProcessorAsync<User> defaultEntityRequestProcessorAsync = new DefaultEntityRequestProcessorAsync<>(
                new GsonRequestAdapter<StmBaseEntity>(),
                StmRequestQueue.getInstance(),
                new GsonUserResponseAdapter(),
                getUserAuthToken(),
                new DefaultUrlProvider(getServerUrl())
        );

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(defaultEntityRequestProcessorAsync);
        getChannelSubscription.get(channelId, user.getId(), callback);
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

        this.stmHttpSender = new StmHttpSender(this);

        // Create or get user
        this.user = new User(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Needs to be done in background thread
                initializeUserSession();

                // Needs to be done in main thread
                connectToLocationServicesHandler.sendEmptyMessage(0);
            }
        }).start();


        executorService = Executors.newFixedThreadPool(10);

        proximitySensorClient = new ProximitySensorClient(this);

        return stmBinder;
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

    @Override
    public void onDestroy() {
        proximitySensorClient.stopListening();
    }

    /**
     * Clears the user auth token from local storage and gets it from the service.
     * @throws Exception The exception that occurred.
     */
    public void refreshUserAuthToken() throws Exception {
        stmPreferenceManager.setAuthToken(null);
        stmPreferenceManager.setUserId(null);
        getUserAuthToken();
    }

    public void refreshUserLocation() {
        userLocationListener.updateUserLocation(this);
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
        user.setIsInitialized(false);
        stmPreferenceManager.setAuthToken(null);
        stmPreferenceManager.setUserId(null);
        initializeUserSession();
    }

    /**
     * Removes a topic preference from the user's record.  If additional topics are still in the
     * user's record, they will no longer receive shouts with the specified topic. If removing the
     * last topic preference and the user has no more topic preferences, then the user will
     * receive shouts from all topics.
     * @param topic The topic to remove
     * @param callback An optional callback or null
     */
    public void removeTopicPreference(String topic, StmCallback<Void> callback) {
        if (topic == null) {
            String validationErrorMessage = "topic cannot be null";
            if (callback != null) {
                StmError error = new StmError(validationErrorMessage, false, StmError.SEVERITY_MINOR);
                callback.onError(error);
                return;
            } else {
                throw new IllegalArgumentException(validationErrorMessage);
            }
        }

        DefaultEntityRequestProcessorAsync<Void> defaultEntityRequestProcessorAsync = new DefaultEntityRequestProcessorAsync<>(
                new GsonRequestAdapter<StmBaseEntity>(),
                StmRequestQueue.getInstance(),
                new NullResponseAdapter(),
                getUserAuthToken(),
                new TopicUrlProvider(getServerUrl(), user)
        );

        DeleteTopicPreference deleteTopicPreference = new DeleteTopicPreference(defaultEntityRequestProcessorAsync);
        deleteTopicPreference.delete(topic, callback);
    }

    /**
     * Sets the Shout to Me channel ID in local storage.
     * @param channelId The Shout to Me channel ID.
     */
    public void setChannelId(String channelId) {
        stmPreferenceManager.setChannelId(channelId);
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
            String validationErrorMessage = "channelId cannot be null";
            if (callback != null) {
                StmError error = new StmError(validationErrorMessage, false, StmError.SEVERITY_MINOR);
                callback.onError(error);
                return;
            } else {
                throw new IllegalArgumentException(validationErrorMessage);
            }
        }

        DefaultEntityRequestProcessorAsync<Void> defaultEntityRequestProcessorAsync = new DefaultEntityRequestProcessorAsync<>(
                new GsonRequestAdapter<StmBaseEntity>(),
                StmRequestQueue.getInstance(),
                new NullResponseAdapter(),
                getUserAuthToken(),
                new ChannelSubscriptionUrlProvider(getServerUrl(), user)
        );

        CreateChannelSubscription createChannelSubscription =
                new CreateChannelSubscription(defaultEntityRequestProcessorAsync);
        createChannelSubscription.create(channelId, callback);
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
            String validationErrorMessage = "channelId cannot be null";
            if (callback != null) {
                StmError error = new StmError(validationErrorMessage, false, StmError.SEVERITY_MINOR);
                callback.onError(error);
                return;
            } else {
                throw new IllegalArgumentException(validationErrorMessage);
            }
        }

        DefaultEntityRequestProcessorAsync<Void> defaultEntityRequestProcessorAsync = new DefaultEntityRequestProcessorAsync<>(
                new GsonRequestAdapter<StmBaseEntity>(),
                StmRequestQueue.getInstance(),
                new NullResponseAdapter(),
                getUserAuthToken(),
                new ChannelSubscriptionUrlProvider(getServerUrl(), user)
        );

        DeleteChannelSubscription deleteChannelSubscription = new DeleteChannelSubscription(defaultEntityRequestProcessorAsync);
        deleteChannelSubscription.delete(channelId, callback);
    }

    /**
     * Updates a user with properties from a {@link UpdateUserRequest} object
     * @param updateUserRequest The object containing the updated properties.
     * @param callback The callback to be executed or null.
     */
    public void updateUser(UpdateUserRequest updateUserRequest, StmCallback<User> callback) {

        if (user.getId() == null) {
            String validationErrorMessage = "Shout to Me user not initialized";
            if (callback != null) {
                StmError error = new StmError(validationErrorMessage, false, StmError.SEVERITY_MAJOR);
                callback.onError(error);
                return;
            } else {
                throw new IllegalArgumentException(validationErrorMessage);
            }
        }

        DefaultEntityRequestProcessorAsync<User> defaultEntityRequestProcessorAsync = new DefaultEntityRequestProcessorAsync<>(
                new GsonRequestAdapter<StmBaseEntity>(),
                StmRequestQueue.getInstance(),
                new GsonUserResponseAdapter(),
                getUserAuthToken(),
                new DefaultUrlProvider(this.getServerUrl())
        );

        UpdateUser updateUser = new UpdateUser(defaultEntityRequestProcessorAsync, this);
        updateUser.update(updateUserRequest, user.getId(), true, callback);
    }
}
