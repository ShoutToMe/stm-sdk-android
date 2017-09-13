package me.shoutto.sdk.internal;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.shoutto.sdk.Channel;
import me.shoutto.sdk.Conversation;
import me.shoutto.sdk.Message;
import me.shoutto.sdk.Subscription;
import me.shoutto.sdk.internal.http.StmEntityListRequestSync;
import me.shoutto.sdk.internal.http.StmEntityRequestSync;
import me.shoutto.sdk.internal.location.geofence.GeofenceManager;
import me.shoutto.sdk.internal.location.geofence.LocationUtils;
import me.shoutto.sdk.internal.location.geofence.MessageGeofence;
import me.shoutto.sdk.internal.location.geofence.database.GeofenceDbHelper;

/**
 * NotificationManager provides business logic for incoming notifications.
 */
public class NotificationManager {

    private static final String TAG = NotificationManager.class.getSimpleName();
    private static final String CONVERSATION_MESSAGE = "conversation message";
    private static final String MESSAGE_SYNC = "message sync";
    private static final String USER_MESSAGE = "user message";
    private static final Object syncNotificationsLock = new Object();
    private Context context;
    private GeofenceManager geofenceManager;
    private StmPreferenceManager stmPreferenceManager;

    public NotificationManager(Context context) {
        this.context = context;
        geofenceManager = new GeofenceManager(context, new GeofenceDbHelper(context),
                new StmPreferenceManager(context));
        stmPreferenceManager = new StmPreferenceManager(context);
    }

    public void processIncomingNotification(Bundle bundle, String serverUrl, String authToken, String userId) {

        NotificationData notificationData = new NotificationData(bundle);

        if (!notificationData.isBundleValid()) {
            Log.w(TAG, "Cannot process incoming notification.  Bundle data is invalid.");
            return;
        }

        if (notificationData.isMessageSyncNotification()) {
            syncNotifications();
        } else if (notificationData.isMessageNotification()) {
            if (notificationData.getConversationId() != null) {
                List<Message> messages = getMessagesByConversationId(notificationData.getConversationId(), authToken, serverUrl);
                if (messages == null || messages.size() == 0) {

                    // Get the conversation
                    Conversation conversation = getConversation(notificationData.getConversationId(), authToken, serverUrl);
                    if (conversation == null) {
                        Log.w(TAG, "Conversation is null for conversationId: " + notificationData.getConversationId()
                                + ". Can't deliver notification");
                        return;
                    }

                    Location conversationLocation = null;

                    // Does the conversation have lat/lon? If so, create an Android Location object
                    if (conversation.getLocation() != null
                            && conversation.getLocation().getLat() != 0.0
                            && conversation.getLocation().getLon() != 0.0) {
                        conversationLocation = new Location("");
                        conversationLocation.setLatitude(conversation.getLocation().getLat());
                        conversationLocation.setLongitude(conversation.getLocation().getLon());
                    }

                    // Get users location. Can't use listener because may be run from notification listener service.
                    // userLocation will be null if user location services are disabled.
                    Location userLocation = LocationUtils.getLastKnownCoordinates(context);
                    if (userLocation == null) {
                        Log.w(TAG, "LocationUtils.getLastKnownCoordinates is null.  Message will be treated as channel wide.");
                    }

                    // Boolean that represents all the required location data. If any of this data
                    // is missing, then this is considered a channel-wide notification
                    boolean hasRequiredLocationData =  conversationLocation != null  // Does not have lat/lon
                            && conversation.getLocation().getRadiusInMeters() != 0          // Does not have radius
                            && userLocation != null;                                // Does not have location services enabled

                    // Does conversation have expiration date? If no, then immediate publishing only
                    if (conversation.getExpirationDate() != null) {

                        // Is conversation expired? Yes, then skip. No, then continue processing
                        if (conversation.getExpirationDate().getTime() - new Date().getTime() > 0) {

                            // Does conversation have lat,lon & radius? If no, then channel wide publishing
                            if (hasRequiredLocationData) {

                                // Is user within the conversation's radius? If so, create message immediately.
                                // If not, create a geofence
                                if (isUserInConversationRadius(conversationLocation, userLocation,
                                        conversation.getLocation().getRadiusInMeters())) {

                                    // User is in Radius of conversation. Publish immediately
                                    // TODO: (If update, need to make sure geofence is deleted)
                                    deliverNotification(notificationData, userId, authToken, serverUrl);

                                } else {

                                    // User is not in Radius of conversation. Create geofence
                                    createOrUpdateGeofence(notificationData, conversation, userLocation);
                                }

                            } else {
                                // Create message immediately. No location data means channel wide
                                deliverNotification(notificationData, userId, authToken, serverUrl);
                            }
                        }
                    } else { // No exp date means conversation was published immediately. Deliver message

                        // Is there the required location data? No, then it's treated as channel wide.
                        // OR
                        // Is user within the conversation's radius? If so, create message immediately.
                        // If not, just skip. User will not ever get a notification
                        if (!hasRequiredLocationData
                                || isUserInConversationRadius(conversationLocation, userLocation,
                                conversation.getLocation().getRadiusInMeters())) {
                            // User is in Radius of conversation. Publish immediately
                            deliverNotification(notificationData, userId, authToken, serverUrl);
                        }
                    }
                } else {
                    Log.d(TAG, "User has received this notification (" + notificationData.getConversationId() + ").  Do not present it again.");
                }
            } else {
                // This is a user directed notification message.  Deliver immediately
                deliverNotification(notificationData, userId, authToken, serverUrl);
            }
        }
    }

    public void syncNotifications() {
        synchronized (syncNotificationsLock) {
            String userAuthCode = stmPreferenceManager.getAuthToken();
            if (userAuthCode == null || userAuthCode.equals("")) {
                return;
            }

            final List<Conversation> activeConversations = new ArrayList<>();

            // Get subscribed channels
            SubscriptionManager subscriptionManager = new SubscriptionManager(context);
            List<Subscription> subscriptionList = subscriptionManager.getSubscriptions();

            // Get active conversations for those channels
            if (subscriptionList != null) {
                ConversationManager conversationManager = new ConversationManager(context);
                for (Subscription subscription : subscriptionList) {
                    List<Conversation> conversationList = conversationManager.getActiveConversations(subscription.getChannelId());
                    if (conversationList != null && conversationList.size() > 0) {
                        activeConversations.addAll(conversationList);
                    }
                }
            }

            // Remove geofences not in the list
            List<String> conversationsToRemove = new ArrayList<>();
            List<String> geofenceIds = geofenceManager.getGeofenceIds();
            for (String geofenceId : geofenceIds) {
                boolean isConversationActive = false;
                for (Conversation conversation : activeConversations) {
                    if (conversation.getId().equals(geofenceId)) {
                        isConversationActive = true;
                    }
                }
                if (!isConversationActive) {
                    conversationsToRemove.add(geofenceId);
                }
            }
            if (conversationsToRemove.size() > 0) {
                geofenceManager.removeGeofencesByIds(conversationsToRemove, LocationUtils.getLastKnownCoordinates(context));
            }

            // Get channel information to provide channel name.
            ChannelManager channelManager = new ChannelManager(context);
            List<Channel> channelList = channelManager.getChannels();

            // Process the notifications.
            for (Conversation conversation : activeConversations) {
                String channelName = "";
                for (Channel channel : channelList) {
                    if (channel.getId().equals(conversation.getChannelId())) {
                        channelName = channel.getName();
                        break;
                    }
                }

                Bundle bundle = new Bundle();
                bundle.putString(MessageNotificationIntentWrapper.EXTRA_CONVERSATION_ID, conversation.getId());
                bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY, conversation.getPublishingMessage());
                bundle.putString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID, conversation.getChannelId());
                bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE, "conversation message");
                bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TITLE, channelName);

                processIncomingNotification(bundle, stmPreferenceManager.getServerUrl(),
                        stmPreferenceManager.getAuthToken(), stmPreferenceManager.getUserId());
            }
        }
    }

    private List<Message> getMessagesByConversationId(String conversationId, String authToken, String serverUrl) {

        String endpointUrl = serverUrl + Message.BASE_ENDPOINT + "?conversation_id=" + conversationId;
        StmEntityListRequestSync<Message> stmEntityRequestSync = new StmEntityListRequestSync<>();
        return stmEntityRequestSync.process("GET", authToken, endpointUrl, null,
                Message.getSerializationListType(), Message.LIST_SERIALIZATION_KEY);
    }

    private Conversation getConversation(String conversationId, String authToken, String serverUrl) {

        String conversationEndpointUrl = serverUrl + Conversation.BASE_ENDPOINT + "/" + conversationId;
        StmEntityRequestSync<Conversation> stmEntityRequestSync = new StmEntityRequestSync<>();
        return stmEntityRequestSync.process("GET", authToken, conversationEndpointUrl, null,
                Conversation.getSerializationType(), Conversation.SERIALIZATION_KEY);
    }

    private boolean isUserInConversationRadius(Location conversationLocation, Location userLocation,
                                               float radiusInMeters) {

        float distanceBetweenConversationAndUser = conversationLocation.distanceTo(userLocation);
        return distanceBetweenConversationAndUser < radiusInMeters;
    }

    private void deliverNotification(NotificationData notificationData, String userId, String authToken, String serverUrl) {

        // If the notification was published from a conversation, then create a message
        // via the service that will represent the "instance" of the notification for this
        // specific user/recipient
        if (notificationData.getNotificationType() != null && notificationData.getNotificationType().equals("conversation message")) {
            notificationData.setMessageId(createMessage(notificationData, authToken, userId, serverUrl));
        }

        if (notificationData.getMessageId() != null) {
            // Alert client of message notification received
            MessageNotificationIntentWrapper messageNotificationIntentWrapper
                    = new MessageNotificationIntentWrapper(notificationData.getChannelId(),
                    notificationData.getChannelImageUrl(), notificationData.getConversationId(),
                    notificationData.getMessageId(), notificationData.getNotificationBody(),
                    notificationData.getNotificationTitle(), notificationData.getNotificationType());
            Intent intent = messageNotificationIntentWrapper.getIntent();
            context.sendBroadcast(intent);
        }
    }

    private String createMessage(NotificationData notificationData, String authToken, String userId, String serverUrl) {

        String newMessageId = null;

        if (!"".equals(authToken) && !"".equals(userId)) {
            CreateMessageData createMessageData =
                    new CreateMessageData(notificationData.getChannelId(), notificationData.getConversationId(),
                            notificationData.getNotificationBody(), userId);

            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();
            String createMessageDataJson = gson.toJson(createMessageData, CreateMessageData.class);

            String messageEndpointUrl = serverUrl + Message.BASE_ENDPOINT;
            StmEntityRequestSync<Message> stmEntityRequestSync = new StmEntityRequestSync<>();
            Message message = stmEntityRequestSync.process("POST", authToken, messageEndpointUrl,
                    createMessageDataJson, Message.getSerializationType(), Message.SERIALIZATION_KEY);
            if (message != null) {
                newMessageId = message.getId();
            }
        } else {
            Log.w(TAG, "Cannot create message due to no authToken or userId. authToken="
                    + authToken + " userId=" + userId);
        }

        return newMessageId;
    }

    private void createOrUpdateGeofence(NotificationData notificationData, Conversation conversation, Location userLocation) {

        MessageGeofence messageGeofence = new MessageGeofence();
        messageGeofence.setChannelId(notificationData.getChannelId());
        messageGeofence.setChannelImageUrl(notificationData.getChannelImageUrl());
        messageGeofence.setConversationId(notificationData.getConversationId());
        messageGeofence.setExpirationDate(conversation.getExpirationDate());
        messageGeofence.setId(conversation.getId());
        messageGeofence.setLatitude(conversation.getLocation().getLat());
        messageGeofence.setLongitude(conversation.getLocation().getLon());
        messageGeofence.setMessageBody(notificationData.getNotificationBody());
        messageGeofence.setRadius(conversation.getLocation().getRadiusInMeters());
        messageGeofence.setTitle(notificationData.getNotificationTitle());
        messageGeofence.setType(notificationData.getNotificationType());

        try {
            geofenceManager.addGeofence(messageGeofence, userLocation);
        } catch (SecurityException ex) {
            Log.e(TAG, "Could not add new geofence.", ex);
        }
    }

    @SuppressWarnings("unused")
    private class CreateMessageData {

        private String channelId;
        private String conversationId;
        private String message;
        private String recipientId;

        CreateMessageData(String channelId, String conversationId, String message,
                                 String recipientId) {
            this.channelId = channelId;
            this.conversationId = conversationId;
            this.message = message;
            this.recipientId = recipientId;
        }
    }

    private class NotificationData {

        private boolean isBundleValid = false;
        private boolean isMessageNotification = false;
        private boolean isMessageSyncNotification = false;
        private String channelId;
        private String channelImageUrl;
        private String conversationId;
        private String messageId;
        private String notificationBody;
        private String notificationTitle;
        private String notificationType;

        NotificationData(Bundle bundle) {
            notificationBody = bundle.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY);
            channelId = bundle.getString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID);
            channelImageUrl = bundle.getString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_IMAGE_URL);
            conversationId = bundle.getString(MessageNotificationIntentWrapper.EXTRA_CONVERSATION_ID);
            messageId = bundle.getString(MessageNotificationIntentWrapper.EXTRA_MESSAGE_ID);
            notificationTitle = bundle.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TITLE);
            notificationType = bundle.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE);

            isMessageNotification = notificationType != null
                    && (notificationType.equals(CONVERSATION_MESSAGE) || notificationType.equals(USER_MESSAGE))
                    && notificationBody != null
                    && channelId != null;

            isMessageSyncNotification = notificationType != null && notificationType.equals(MESSAGE_SYNC);

            isBundleValid = isMessageNotification || isMessageSyncNotification;
        }

        boolean isBundleValid() {
            return isBundleValid;
        }

        boolean isMessageNotification() {
            return isMessageNotification;
        }

        boolean isMessageSyncNotification() {
            return isMessageSyncNotification;
        }

        public String getChannelId() {
            return channelId;
        }

        String getChannelImageUrl() {
            return channelImageUrl;
        }

        String getConversationId() {
            return conversationId;
        }

        String getMessageId() {
            return messageId;
        }

        void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        String getNotificationBody() {
            return notificationBody;
        }

        String getNotificationTitle() {
            return notificationTitle;
        }

        String getNotificationType() {
            return notificationType;
        }
    }
}
