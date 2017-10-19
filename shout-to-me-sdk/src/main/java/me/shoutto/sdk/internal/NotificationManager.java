package me.shoutto.sdk.internal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * NotificationManager provides business logic for incoming notifications.
 */
public class NotificationManager {

    private static final String TAG = NotificationManager.class.getSimpleName();
    private Context context;

    public NotificationManager(Context context) {
        this.context = context;
    }

    public void processIncomingNotification(Bundle bundle, String serverUrl, String authToken, String userId) {

        NotificationData notificationData = new NotificationData(bundle);

        if (!notificationData.isBundleValid()) {
            Log.w(TAG, "Cannot process incoming notification.  Bundle data is invalid.");
            return;
        }

        // This is a user directed notification message.  Deliver immediately
        deliverNotification(notificationData, userId, authToken, serverUrl);
    }

    private void deliverNotification(NotificationData notificationData, String userId, String authToken, String serverUrl) {

        if (notificationData.getMessageId() != null) {
            // Alert client of message notification received
            MessageNotificationIntentWrapper messageNotificationIntentWrapper
                    = new MessageNotificationIntentWrapper(notificationData.getChannelId(),
                    notificationData.getMessageId(), notificationData.getNotificationBody(),
                    notificationData.getNotificationType(), notificationData.getCategory());
            Intent intent = messageNotificationIntentWrapper.getIntent();
            context.sendBroadcast(intent);
        }
    }

    private class NotificationData {

        private boolean isBundleValid = false;
        private String category;
        private String channelId;
        private String messageId;
        private String notificationBody;
        private String notificationType;

        NotificationData(Bundle bundle) {
            notificationBody = bundle.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY);
            category = bundle.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_CATEGORY);
            channelId = bundle.getString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID);
            messageId = bundle.getString(MessageNotificationIntentWrapper.EXTRA_MESSAGE_ID);
            notificationType = bundle.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE);

            isBundleValid = notificationBody != null && channelId != null;
        }

        boolean isBundleValid() {
            return isBundleValid;
        }

        public String getCategory() {
            return category;
        }

        public String getChannelId() {
            return channelId;
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

        String getNotificationType() {
            return notificationType;
        }
    }
}
