package me.shoutto.sdk.internal;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.List;

import me.shoutto.sdk.Conversation;
import me.shoutto.sdk.Message;
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

    private static final String TAG = NotificationManager.class.getCanonicalName();
    private Context context;
    private GeofenceManager geofenceManager;
    private boolean isBundleValid;
    private String channelId;
    private String channelImageUrl;
    private String conversationId;
    private String notificationBody;
    private String notificationTitle;
    private String notificationType;

    public NotificationManager(Context context, Bundle bundle) {
        this.context = context;
        geofenceManager = new GeofenceManager(context, new GeofenceDbHelper(context));
        processBundle(bundle);
    }

    private void processBundle(Bundle bundle) {
        notificationBody = bundle.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY);
        channelId = bundle.getString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID);
        channelImageUrl = bundle.getString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_IMAGE_URL);
        conversationId = bundle.getString(MessageNotificationIntentWrapper.EXTRA_CONVERSATION_ID);
        notificationTitle = bundle.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TITLE);
        notificationType = bundle.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE);

        isBundleValid = (notificationBody != null && channelId != null && notificationType != null);
    }

    public void processIncomingNotification(String serverUrl, String authToken, String userId) {

        if (!isBundleValid) {
            Log.w(TAG, "Cannot process incoming notification.  Bundle data is invalid.");
            return;
        }

        if (conversationId != null) {
            List<Message> messages = getMessagesByConversationId(conversationId, authToken, serverUrl);
            if (messages == null || messages.size() == 0) {

                // Get the conversation
                Conversation conversation = getConversation(conversationId, authToken, serverUrl);
                if (conversation == null) {
                    Log.w(TAG, "Conversation is null for conversationId: " + conversationId
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
                                deliverNotification(userId, authToken, serverUrl);

                            } else {

                                // User is not in Radius of conversation. Create geofence
                                createOrUpdateGeofence(conversation, userLocation);
                            }

                        } else {
                            // Create message immediately. No location data means channel wide
                            deliverNotification(userId, authToken, serverUrl);
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
                        deliverNotification(userId, authToken, serverUrl);
                    }
                }
            } else {
                Log.d(TAG, "User has received this notification (" + conversationId + ").  Do not present it again.");
            }
        } else {
            // This is a user directed notification message.  Deliver immediately
            deliverNotification(userId, authToken, serverUrl);
        }
    }

    private List<Message> getMessagesByConversationId(String conversationId, String authToken, String serverUrl) {

        String endpointUrl = serverUrl + Message.BASE_ENDPOINT + "?conversation_id=" + conversationId;
        StmEntityListRequestSync<Message> stmEntityRequestSync = new StmEntityListRequestSync<>();
        return stmEntityRequestSync.process("GET", authToken, endpointUrl, null,
                Message.getSerializationListType(), Message.LIST_JSON_KEY);
    }

    private Conversation getConversation(String conversationId, String authToken, String serverUrl) {

        String conversationEndpointUrl = serverUrl + Conversation.BASE_ENDPOINT + "/" + conversationId;
        StmEntityRequestSync<Conversation> stmEntityRequestSync = new StmEntityRequestSync<>();
        return stmEntityRequestSync.process("GET", authToken, conversationEndpointUrl, null,
                Conversation.getSerializationType(), Conversation.OBJECT_JSON_KEY);
    }

    private boolean isUserInConversationRadius(Location conversationLocation, Location userLocation,
                                               float radiusInMeters) {

        float distanceBetweenConversationAndUser = conversationLocation.distanceTo(userLocation);
        return distanceBetweenConversationAndUser < radiusInMeters;
    }

    private void deliverNotification(String userId, String authToken, String serverUrl) {

        // Create a message.  This notification was published from a conversation and the
        // created message will represent the "instance" of the notification for this
        // specific user/recipient
        String messageId = createMessage(authToken, userId, serverUrl);

        if (messageId != null) {
            // Alert client of message notification received
            MessageNotificationIntentWrapper messageNotificationIntentWrapper
                    = new MessageNotificationIntentWrapper(channelId, channelImageUrl, conversationId,
                        messageId, notificationBody, notificationTitle, notificationType);
            Intent intent = messageNotificationIntentWrapper.getIntent();
            context.sendBroadcast(intent);
        }
    }

    private String createMessage(String authToken, String userId, String serverUrl) {

        String newMessageId = null;

        if (!"".equals(authToken) && !"".equals(userId)) {
            CreateMessageData createMessageData =
                    new CreateMessageData(channelId, conversationId, notificationBody, userId);

            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();
            String createMessageDataJson = gson.toJson(createMessageData, CreateMessageData.class);

            String messageEndpointUrl = serverUrl + Message.BASE_ENDPOINT;
            StmEntityRequestSync<Message> stmEntityRequestSync = new StmEntityRequestSync<>();
            Message message = stmEntityRequestSync.process("POST", authToken, messageEndpointUrl,
                    createMessageDataJson, Message.getSerializationType(), Message.OBJECT_JSON_KEY);
            if (message != null) {
                newMessageId = message.getId();
            }
        } else {
            Log.w(TAG, "Cannot create message due to no authToken or userId. authToken="
                    + authToken + " userId=" + userId);
        }

        return newMessageId;
    }

    private void createOrUpdateGeofence(Conversation conversation, Location userLocation) {

        MessageGeofence messageGeofence = new MessageGeofence();
        messageGeofence.setChannelId(channelId);
        messageGeofence.setChannelImageUrl(channelImageUrl);
        messageGeofence.setConversationId(conversationId);
        messageGeofence.setExpirationDate(conversation.getExpirationDate());
        messageGeofence.setId(conversation.getId());
        messageGeofence.setLatitude(conversation.getLocation().getLat());
        messageGeofence.setLongitude(conversation.getLocation().getLon());
        messageGeofence.setMessageBody(notificationBody);
        messageGeofence.setRadius(conversation.getLocation().getRadiusInMeters());
        messageGeofence.setTitle(notificationTitle);
        messageGeofence.setType(notificationType);

        geofenceManager.addGeofence(messageGeofence, userLocation);
    }

    @SuppressWarnings("unused")
    private class CreateMessageData {

        private String channelId;
        private String conversationId;
        private String message;
        private String recipientId;

        public CreateMessageData(String channelId, String conversationId, String message,
                                 String recipientId) {
            this.channelId = channelId;
            this.conversationId = conversationId;
            this.message = message;
            this.recipientId = recipientId;
        }
    }
}
