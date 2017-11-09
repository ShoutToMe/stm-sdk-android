package me.shoutto.sdk;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import me.shoutto.sdk.internal.MessageNotificationIntentWrapper;
import me.shoutto.sdk.internal.NotificationManager;
import me.shoutto.sdk.internal.StmPreferenceManager;

/**
 * Implementation of <code>GcmListenerService</code> that handles notifications from the Shout to Me
 * platform.
 * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/gcm/GcmListenerService" target="_blank">GcmListenerService</a>
 */
public class StmGcmListenerService extends GcmListenerService {

    private static final String TAG = StmGcmListenerService.class.getSimpleName();
    private static final String CONVERSATION_MESSAGE_TYPE = "conversation";
    private static final String BODY_KEY = "body";
    private static final String CATEGORY_KEY = "category";
    private static final String CHANNEL_ID_KEY = "channel_id";
    private static final String MESSAGE_ID_KEY = "stm_message_id";
    private static final String MESSAGE_TYPE_KEY = "stm_message_type";

    /**
     * Called when a notification is received from the Shout to Me platform.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "Message received.");

        Bundle notificationData = new Bundle();
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY, data.getString(BODY_KEY));
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID, data.getString(CHANNEL_ID_KEY));
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_CATEGORY, data.getString(CATEGORY_KEY));

        String messageId = data.getString(MESSAGE_ID_KEY);
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_MESSAGE_ID, messageId);

        String messageType = data.getString(MESSAGE_TYPE_KEY);
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE, messageType);
        if (CONVERSATION_MESSAGE_TYPE.equals(messageType)) {
            notificationData.putString(MessageNotificationIntentWrapper.EXTRA_CONVERSATION_ID, messageId);
        }

        StmPreferenceManager stmPreferenceManager = new StmPreferenceManager(this);
        NotificationManager notificationManager = new NotificationManager(this);
        notificationManager.processIncomingNotification(notificationData, stmPreferenceManager.getServerUrl(),
                stmPreferenceManager.getAuthToken(), stmPreferenceManager.getUserId());
    }
    // [END receive_message]
}
