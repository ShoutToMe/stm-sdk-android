package me.shoutto.sdk;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import me.shoutto.sdk.internal.NotificationManager;
import me.shoutto.sdk.internal.StmPreferenceManager;

public class StmGcmListenerService extends GcmListenerService {

    private static final String TAG = StmGcmListenerService.class.getCanonicalName();

    /**
     * Called when message is received.  This class must use synchronous calls to STM API because
     * trying to start the async StmService when the app is closed causes a memory leak.
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
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY, data.getString("body"));
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID, data.getString("channel_id"));
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_IMAGE_URL, data.getString("channel_image_url"));
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_CONVERSATION_ID, data.getString("conversation_id"));
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TITLE, data.getString("title"));
        notificationData.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE, data.getString("type"));

        StmPreferenceManager stmPreferenceManager = new StmPreferenceManager(this);
        NotificationManager notificationManager = new NotificationManager(this, notificationData);
        notificationManager.processIncomingNotification(stmPreferenceManager.getServerUrl(),
                stmPreferenceManager.getAuthToken(), stmPreferenceManager.getUserId());
    }
    // [END receive_message]
}
