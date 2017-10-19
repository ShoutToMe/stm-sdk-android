package me.shoutto.sdk.internal;

import android.content.Intent;
import android.util.Log;

/**
 * MessageNotificationIntentWrapper
 */
public class MessageNotificationIntentWrapper {

    public static final String EVENT_MESSAGE_NOTIFICATION_RECEIVED = "me.shoutto.sdk.EVENT_MESSAGE_NOTIFICATION_RECEIVED";
    public static final String EXTRA_CHANNEL_ID = "me.shoutto.sdk.EXTRA_CHANNEL_ID";
    public static final String EXTRA_CHANNEL_IMAGE_URL = "me.shoutto.sdk.EXTRA_CHANNEL_IMAGE_URL";
    public static final String EXTRA_CONVERSATION_ID = "me.shoutto.sdk.EXTRA_CONVERSATION_ID";
    public static final String EXTRA_MESSAGE_ID = "me.shoutto.sdk.EXTRA_MESSAGE_ID";
    public static final String EXTRA_NOTIFICATION_BODY = "me.shoutto.sdk.EXTRA_NOTIFICATION_BODY";
    public static final String EXTRA_NOTIFICATION_CATEGORY = "me.shoutto.sdk.EXTRA_NOTIFICATION_CATEGORY";
    public static final String EXTRA_NOTIFICATION_TITLE = "me.shoutto.sdk.EXTRA_NOTIFICATION_TITLE";
    public static final String EXTRA_NOTIFICATION_TYPE = "me.shoutto.sdk.EXTRA_NOTIFICATION_TYPE";

    private String category;
    private String channelId;
    private String messageId;
    private String notificationBody;
    private String notificationType;
    private Intent intent;

    public MessageNotificationIntentWrapper(String channelId,
                                            String messageId, String notificationBody,
                                            String notificationType, String category) {
        this.category = category;
        this.channelId = channelId;
        this.messageId = messageId;
        this.notificationBody = notificationBody;
        this.notificationType = notificationType;
        intent = new Intent(MessageNotificationIntentWrapper.EVENT_MESSAGE_NOTIFICATION_RECEIVED);
    }

    public Intent getIntent() {
        intent.putExtra(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_CATEGORY, category);
        intent.putExtra(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(MessageNotificationIntentWrapper.EXTRA_MESSAGE_ID, messageId);
        intent.putExtra(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY, notificationBody);
        intent.putExtra(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE, notificationType);
        return intent;
    }
}
