package me.shoutto.sdk;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import android.content.Intent;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import me.shoutto.sdk.internal.MessageNotificationIntentWrapper;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MessageNotificationIntentWrapperTest {

    @Test
    public void testIntent() {
        String channelId = "channel123";
        String messageId = "message123";
        String notificationBody = "Notification body";
        String notificationType = "Notification type";
        String notificationCategory = "Notification category";

        MessageNotificationIntentWrapper messageNotificationIntentWrapper
                = new MessageNotificationIntentWrapper(channelId, messageId,
                notificationBody, notificationType, notificationCategory);
        Intent intent = messageNotificationIntentWrapper.getIntent();
        assertThat(intent, not(equalTo(null)));
        assertThat(intent.getStringExtra(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID), equalTo(channelId));
        assertThat(intent.getStringExtra(MessageNotificationIntentWrapper.EXTRA_MESSAGE_ID), equalTo(messageId));
        assertThat(intent.getStringExtra(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY), equalTo(notificationBody));
        assertThat(intent.getStringExtra(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE), equalTo(notificationType));
        assertThat(intent.getStringExtra(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_CATEGORY), equalTo(notificationCategory));
    }
}
