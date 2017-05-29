---
layout: home
---

# Messages and Notifications

The Shout to Me platform allows you to enable your app to receive communications sent from Shout to Me's Broadcaster
Application.  Messages and notifications are two separate concepts in the Shout to Me system.  A **Message** is text or
audio content that is sent to mobile users to convey a communication.  A **Notification** is the delivery mechanism used to transport a Message.

## Messages
The Message object represents a text or audio message that was sent from a broadcaster.  A
user may receive messages from more than one channel if the client app supports multiple channels.  Some messages have
audio associated with them. When they do, you can use the conversation ID to get access to the audio.

```java
public class Message {

    public String getId()

    public Channel getChannel()

    // The actual message text
    public String getMessage()

    // The name of the sender.  May be null if was sent via a channel-wide notification
    public String getSenderName()

    public Date getSentDate()

    // A reference to a Shout to Me conversation.  May be null if message was not associated with a conversation
    public String getConversationId()
}
```

### Retrieving messages

A maximum of 1000 messages will be returned.

```java
stmService.getMessages(new Callback<List<Message>>() {
    @Override
    public void onSuccess(StmResponse<List<Message>> messagesResponse) {
        List<Message> messageList = messagesResponse.get();
    }

    @Override
    public void onFailure(StmError stmError) {
        // Could not retrieve message list
    }
});
```

## Notifications
The Shout to Me SDK supports receiving push notifications from the Shout to Me platform.  The SDK will only handle
  notifications sent from the Shout to Me system.  There are a number of technologies used in receiving notifications,
  and consequently, there are a number of items that need to be wired up. The following high level steps occur in the
  notifications system:

1. A message is sent from the Shout to Me Broadcaster Application
2. The message is delivered to the mobile app as a Google Cloud Messaging notification
3. The Shout to Me SDK receives the notification and broadcasts a message
4. A listener in the client app receives a broadcast and can take further action using the data

![Notifications](https://s3-us-west-2.amazonaws.com/sdk-public-images/android-notifications.png)

### Google Cloud Messaging
The Shout to Me system uses [Google Cloud Messaging (GCM)](https://developers.google.com/cloud-messaging/) to send and receive messages. Add the following to your AndroidManifest.xml if you wish to receive notifications.  Be sure to set your own values for the string resource references.  Check with Shout to Me support for specific values to use.

```xml
<service
    android:name="me.shoutto.sdk.GcmNotificationRegistrationIntentService"
    android:exported="false">
    <meta-data android:name="me.shoutto.sdk.NotificationAppId" android:value="@string/notification_app_id" />
</service>

<!-- [START gcm_receiver] -->
<receiver
    android:name="com.google.android.gms.gcm.GcmReceiver"
    android:exported="true"
    android:permission="com.google.android.c2dm.permission.SEND">
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        <category android:name="me.shoutto.sdk" />
    </intent-filter>
</receiver>
<!-- [END gcm_receiver] -->


<!-- [START gcm_listener] -->
<service
    android:name="me.shoutto.sdk.StmGcmListenerService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
    </intent-filter>
</service>
<!-- [END gcm_listener] -->


<!-- [START instanceId_listener] -->
<service
    android:name="me.shoutto.sdk.GcmInstanceIDListenerService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.android.gms.iid.InstanceID" />
    </intent-filter>
</service>
<!-- [END instanceId_listener] -->
```

### Geofencing
Location based notifications will be created as [geofences](https://developers.google.com/android/reference/com/google/android/gms/location/Geofence) in the Shout to Me SDK.  Add this to your AndroidManifest.xml to allow the SDK to listen for geofence events:

```xml
<service android:name="me.shoutto.sdk.GeofenceTransitionsIntentService" />
```

**Note:** Please contact Shout to Me support if you are already using geofences within your application.

### Shout to Me Broadcasts
The Shout to Me SDK uses a standard Android broadcast to send the processed message data to client apps.  Add the following to your AndroidManifest.xml to listen for these broadcasts.

```xml
<receiver
    android:name=".StmNotificationReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="me.shoutto.sdk.EVENT_MESSAGE_NOTIFICATION_RECEIVED" />
    </intent-filter>
</receiver>
```

Of course, you will need to supply your own listener class. In this example, it is called StmNotificationReceiver.

The broadcast receiver class should include something similar to the following to retrieve the broadcast data:

```java
@Override
public void onReceive(Context context, Intent intent) {
    Bundle data = intent.getExtras();
    body = data.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY);
    channelId = data.getString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID);
    channelImageUrl = data.getString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_IMAGE_URL);
    title = data.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TITLE);
    type = data.getString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE);
}
```