---
layout: home
---

# StmService

[StmService](http://localhost:4000/stm-sdk-android/reference/me/shoutto/sdk/StmService.html) is the primary class in the
Shout to Me SDK for Android that a developer will interact with. It is implemented as an
[Android Service](https://developer.android.com/reference/android/app/Service.html) to provide developers a convenient,
native integration. When you start or bind to the StmService, it reads the access token and channel ID values from
AndroidManifest.xml and initializes itself with the Shout To Me service.

## Connecting to StmService

The following shows how to bind and unbind to StmService.

```java
private ServiceConnection stmServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className,
                                   IBinder service) {
        // We've bound to StmService, cast the IBinder and get StmService instance
        StmService.StmBinder binder = (StmService.StmBinder) service;
        stmService = binder.getService();
        isStmServiceBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        isStmServiceBound = false;
    }
};

// Binding to StmService
Intent intent = new Intent(this, StmService.class);
bindService(intent, stmServiceConnection, Context.BIND_AUTO_CREATE);

// Unbinding
unbindService(stmServiceConnection);
```

The following sections describe some of the functionality that the SDK provides.

## User Authentication
The Shout to Me Android SDK will generally handle user authentication behind the scenes so you don't need to worry
about it.  In some cases, you may wish to call the Shout to Me REST API directly to accomplish something outside the SDK.
If you wish to send direct requests to the Shout to Me REST API, you will need the user's authentication token.
To get the auth token, call the following method on StmService.  The first time this method is called,
it blocks until the auth token is retrieved.  Therefore, either call this method on a background thread,
or be prepared to handle an error in the event that the auth token has not yet been retrieved.

Once retrieved from the server, the auth token is stored in the device shared preferences to save time on
future retrievals and across app sessions.

```java
stmService.getUserAuthToken();
```

## Hand Wave Gesture Initiated Shout Recording
The Shout to Me SDK includes a usability feature design to help make an app safe for driving.  When
enabled, a driver need only wave their hand in front of the phone to launch the
[Recording Overlay](create-shout).

The hand wave gesture functionality utilizes the phone's proximity sensor.  Therefore, if a phone
does not have a proximity sensor, or the user has revoked proximity sensor permission, this
functionality will not work.

To enable the hand wave gesture functionality, simply register a listener to the following StmService method.

```java
// Implement the HandWaveGestureListener interface
public class MainActivity extends Activity implements HandWaveGestureListener

// Register the listener with StmService
stmService.registerHandGestureListener(this);
```

Once enabled, the Shout to Me SDK continues to listen for events from the proximity sensor.
Un-registering the listener will turn off the proximity event listening, thereby conserving device
resources.

```java
// Unregistering a listener
stmService.unregisterHandGestureListener(this);
```

## Callbacks

The SDK uses the concept of callbacks to handle asynchronous processing and therefore StmService accepts callbacks
in many of its methods.  The SDK includes a Callback class.  The Callback class is used to provide methods you would
like to have executed following the asynchronous calls to the Shout to Me service.  Callback is an abstract class with
two methods that can be overridden.  Simply inherit the class or use an anonymous class to define functionality you
wish to have executed following the asynchronous processing.  Below is the definition of the Callback class.

```java
public abstract class Callback<T> implements StmCallback<T> {

    public abstract void onSuccess(StmResponse<T> stmResponse);
    public abstract void onFailure(StmError stmError);
}
```

As you can see from the Callback definition, the class offers two methods for `onSuccess` or `onFailure`.  `onSuccess`
will return StmResponse which is a wrapper object.  To get the typed response object, call the following.

```java
// In this example, the typed object is a User
User stmUser = stmResponse.get();
```

In the `onFailure` method, an instance of the StmError class is returned.  The StmError class encapsulates information
in the event of an error during asynchronous processing.   A "blocking error" occurs when the system determines that the
client application cannot continue using Shout to Me functionality, such as if the Shout to Me service is unreachable.
Below is the definition of the StmError class.

```java
public class StmError {

    public static final String SEVERITY_MAJOR = "major";
    public static final String SEVERITY_MINOR = "minor";

    public String getMessage() {
        return message;
    }

    public String getSeverity() {
        return severity;
    }

    public boolean isBlocking() {
        return blocking;
    }
}
```

## User
The User object represents the Shout to Me user entity that is bound to your application.  Although you may have your
own user domain objects, Shout to Me still needs a context in which to create shouts, accumulate statistics, etc.  A
Shout to Me user created by your mobile app will be unique to your system.  For example, if a mobile user has
installed two apps that utilize the Shout to Me SDK on the same phone, there will be two distinct Shout to Me users,
one for each app.  Much of the generic code around creating and authenticating the user is hidden from you by the SDK
to make your life easier.  However, there are a few items that do need to be exposed, such as setting a user handle
to match the handle in your system.

Retrieving the user object from StmService can be done with or without a Callback. Retrieving the user
object without a Callback does not guarantee that the user object will have been initialized by the service, however,
it is convenient for certain write-only scenarios such as wanting to change the user's handle.

```java
// Without a callback
User user = stmService.getUser()

// With a callback
stmService.getUser(new Callback<User>() {
    @Override
    public void onSuccess(final StmResponse<User> stmResponse) {
	    User user = stmResponse.get();
    }

    @Override
    public void onFailure(final StmError stmError) {
        // Error occurred retrieving user
    }
});
```

To update a user, instantiate a `UpdateUserRequest` object, complete the desired update fields, and then submit the
object to the `stmService.updateUser()` method with an optional callback.

```java
UpdateUserRequest updateUserRequest = new UpdateUserRequest();
updateUserRequest.setEmail("user@example.com");
updateUserRequest.setHandle("BobSmith");
updateUserRequest.setPhone("8885551212");

stmService.updateUser(updateUserRequest, new Callback<User>() {
    @Override
    public void onSuccess(final StmResponse<User> stmResponse) {
        User updatedUser = stmResponse.get();
    }

    @Override
    public void onFailure(final StmError stmError) {
        // Could not save changes to user
    }
});
```

## Subscribing to a Channel

A subscription to a channel indicates that push notifications will be sent to the app when a broadcaster
publishes a channel-wide message.  The SDK provides a way for the client app to subscribe, unsubscribe and
tell if the user is currently subscribed or not.  This can be used in an app setting to allow the user
control over their subscription status. (For more information about notifications, see the [Notifications](notifications)
section).

Determining subscription status

```java
stmService.isSubscribedToChannel("[channel ID]", new Callback<Boolean>() {
    @Override
    public void onSuccess(StmResponse<Boolean> isSubscribedResponse) {
        if (isSubscribedResponse.get()) {
            // User is subscribed to channel
        } else {
            // User is not subscribed to channel
        }
    }

    @Override
    public void onFailure(StmError stmError) {
        // An error occurred checking user's subscribed status
    }
});
```

Subscribing to a channel

```java
stmService.subscribeToChannel("[channel ID]", new Callback<Void>() {
    @Override
    public void onSuccess(StmResponse<Void> subscribeResponse) {
        // User is now subscribed
    }

    @Override
    public void onFailure(StmError stmError) {
        // An error occurred subscribing to channel
    }
});
```

Unsubscribing to a channel

```java
stmService.unsubscribeFromChannel("[channel ID]", new Callback<Void>() {
    @Override
    public void onSuccess(StmResponse<Void> unsubscribeResponse) {
        // User is now unsubscribed.
    }

    @Override
    public void onFailure(StmError stmError) {
        // An error occurred unsubscribing to channel
    }
});
```