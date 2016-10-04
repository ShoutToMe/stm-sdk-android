# Shout to Me Android SDK
*Version 1.0.6*
## Quickstart Guide
This guide will show you how to get up and running with the Shout to Me Android SDK in minutes.
### Prerequisites
- A Shout to Me client access token
- [Android Studio](http://developer.android.com/tools/studio/index.html) and all its dependencies. This quickstart guide was developed using Android Studio version 2.1.2

### Create an Android application
Run through the Android Studio’s Create New Project wizard.  The minimum Android SDK required is **API 15: Android 4.0.3 (IceCreamSandwich)**.  Choose "Empty Activity" on the "Add an Activity to Mobile" screen.  For the rest of the options, you can leave them set to their defaults.

![Create new project step 1](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-1.png)

![Create new project step 2](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-2.png)

![Create new project step 3](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-3.png)

![Create new project step 4](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-4.png)
### Add the Shout to Me Android SDK

1. In Android Studio, navigate to **File > New > New Module**
2. Select **Import .JAR/.AAR Package** then click **Next**
3. Enter the location of the **shout-to-me-sdk-release.aar** file and then click **Finish**

Add the following to your app/build.gradle file dependencies section:

```gradle
dependencies {
    ...
    
    compile project(":shout-to-me-sdk-release")
    compile 'com.google.android.gms:play-services:9.6.0'
    compile 'com.android.volley:volley:1.0.0'
    compile 'com.amazonaws:aws-android-sdk-core:2.2.+'
    compile 'com.amazonaws:aws-android-sdk-sns:2.2.+'
}
```

(Note the additional dependency on Google Play Services, Volley, and AWS)

Then click:  **Tools > Android > Sync Project with Gradle Files**

### Add your Shout to Me client token to AndroidManifest.xml

Add the following section into the &lt;application&gt; node of your AndroidManifest.xml.

```xml
<service
    android:name="me.shoutto.sdk.StmService"
    android:exported="false">
    
    <meta-data
        android:name="me.shoutto.sdk.CLIENT_TOKEN"
        android:value="@string/client_token" />
        
    <meta-data
        android:name="me.shoutto.sdk.CHANNEL_ID"
        android:value="@string/channel_id" />
</service>
```

And then add the following to your app/src/main/res/values/strings.xml file:

```xml
<string name="client_token">[Your client token]</string>
<string name="channel_id">s2m-sandbox</string>
```

### Use the Shout to Me Android SDK
You are now able to begin coding with the Shout to Me Android SDK.  Assuming you used the default names when creating the projects, modify the following files so they look like these:

**MainActivity.java**

```java
package com.mycompany.teststmsdk;

import android.Manifest;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import me.shoutto.sdk.Callback;
import me.shoutto.sdk.Shout;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmRecorderActivity;
import me.shoutto.sdk.StmResponse;
import me.shoutto.sdk.StmService;
import me.shoutto.sdk.User;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private StmService stmService;
    private Boolean isStmServiceBound = false;
    private Shout newlyCreatedShout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Intent to bind to the Shout to Me service
        Intent intent = new Intent(this, StmService.class);
        bindService(intent, stmServiceConnection, Context.BIND_AUTO_CREATE);

        // Show user a Dialog to update Google Play Services if required version is not installed
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int val = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (val != ConnectionResult.SUCCESS) {
            Dialog gpsErrorDialog = googleApiAvailability.getErrorDialog(this, val, 2);
            gpsErrorDialog.show();
        }

        final EditText editText = (EditText) findViewById(R.id.editTextUserHandle);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.setError(null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isStmServiceBound) {
            unbindService(stmServiceConnection);
        }
    }

    public void launchRecordingOverlay(View view) {
        if (isStmServiceBound) {
            Log.d(TAG, "Launching overlay");
            stmService.setShoutCreationCallback(new Callback<Shout>() {
                @Override
                public void onSuccess(StmResponse<Shout> stmResponse) {
                    Log.d(TAG, "Shout created successfully. ID = " + stmResponse.get().getId());
                    newlyCreatedShout = stmResponse.get();
                    showDeleteButton();
                }

                @Override
                public void onFailure(StmError stmError) {
                    Log.e(TAG, "An error occurred during shout creation. Message is " + stmError.getMessage());
                }
            });

            Intent intent = new Intent(this, StmRecorderActivity.class);

            // REQUIRED: Set the maximum length of recording time allowed in seconds.
            intent.putExtra(StmRecorderActivity.MAX_RECORDING_TIME_IN_SECONDS, 15);

            startActivityForResult(intent, 1);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                String result = data.getStringExtra(StmRecorderActivity.ACTIVITY_RESULT);
                Log.d(TAG, "The recording overlay has closed successfully. Result is: " + result);

                if (result.equals(StmService.FAILURE)) {
                    String failureReasonCode = data.getStringExtra(StmRecorderActivity.ACTIVITY_REASON);
                    Log.d(TAG, "Failure code: " + failureReasonCode);
                    if (failureReasonCode.equals(StmRecorderActivity.RECORD_AUDIO_PERMISSION_DENIED)) {

                        // User has not granted access to record audio.  Ask the user for permission now.
                        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO }, 0);
                    }
                }
            }
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Recording was cancelled");
            }
        }
    }

    public void setUserHandle(View view) {
        if (isStmServiceBound) {
            final EditText editText = (EditText)findViewById(R.id.editTextUserHandle);
            String newHandle = editText.getText().toString();

            // Calling getUser() without a Callback does not guarantee that the object will be
            // instantiated from the server, but is useful for update-only functions.
            User user = stmService.getUser();
            user.setHandle(newHandle);
            user.save(new Callback<User>() {
                @Override
                public void onSuccess(final StmResponse<User> stmResponse) {
                    Log.d(TAG, "User handle update was successful. Handle is " + stmResponse.get().getHandle());
                    Log.d(TAG, "stmReponse.get() && stmService.getUser() point to the same object. "
                            + (stmResponse.get() == stmService.getUser()));
                    editText.setError(null);
                    editText.setText(stmService.getUser().getHandle());
                }

                @Override
                public void onFailure(final StmError stmError) {
                    editText.setError(stmError.getMessage());
                    editText.setText(stmService.getUser().getHandle());
                }
            });
        }
    }

    public void deleteShout(View view) {
        if (newlyCreatedShout != null) {
            Log.d(TAG, "Deleting shout " + newlyCreatedShout.getId());
            newlyCreatedShout.delete(new Callback<String>() {
                @Override
                public void onSuccess(StmResponse<String> stmResponse) {
                    if (stmResponse.get().equals(StmService.SUCCESS)) {
                        Log.d(TAG, "Deletion of shout succeeded.");
                        hideDeleteButton();
                    }
                }

                @Override
                public void onFailure(StmError stmError) {
                    Log.e(TAG, "Error occurred deleting shout. Error message: " + stmError.getMessage());
                }
            });
        }
    }

    private void showDeleteButton() {
        Button deleteButton = (Button) findViewById(R.id.deleteShoutButton);
        deleteButton.setVisibility(View.VISIBLE);
    }

    private void hideDeleteButton() {
        Button deleteButton = (Button) findViewById(R.id.deleteShoutButton);
        deleteButton.setVisibility(View.INVISIBLE);
    }

    private ServiceConnection stmServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to StmService, cast the IBinder and get StmService instance
            Log.d(TAG, "in onServiceConnected");
            StmService.StmBinder binder = (StmService.StmBinder) service;
            stmService = binder.getService();
            isStmServiceBound = true;

            // You can also set the channel programmatically if you have access to more than one channel
            // stmService.setChannelId("s2m-sandbox");

            // Get a reference to the UI text box
            final EditText handleEditText = (EditText) findViewById(R.id.editTextUserHandle);

            // Calling getUser() with a Callback will ensure you get an instantiated user object from the server
            stmService.getUser(new Callback<User>() {
                @Override
                public void onSuccess(final StmResponse<User> stmResponse) {
                    Log.d(TAG, "Shout to Me user has been loaded");
                    handleEditText.setText(stmResponse.get().getHandle());
                }

                @Override
                public void onFailure(final StmError stmError) {
                    Log.w(TAG, "Could not retrieve Shout to Me user.");
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isStmServiceBound = false;
        }
    };
}
```

**activity_main.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:paddingBottom="@dimen/activity_vertical_margin"
    android:paddingLeft="@dimen/activity_horizontal_margin"
    android:paddingRight="@dimen/activity_horizontal_margin"
    android:paddingTop="@dimen/activity_vertical_margin"
    tools:context="com.mycompany.teststmsdk.MainActivity">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello World!"
        android:id="@+id/textView"/>

    <EditText
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/editTextUserHandle"
        android:inputType="textNoSuggestions"
        android:layout_below="@id/textView"
        android:layout_alignParentLeft="true"
        android:layout_alignParentStart="true"
        android:layout_marginTop="118dp" />

    <Button
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Change Handle"
        android:id="@+id/button"
        android:layout_below="@id/editTextUserHandle"
        android:layout_alignParentLeft="true"
        android:layout_alignParentStart="true"
        android:onClick="setUserHandle" />

    <Button
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Delete Last Shout"
        android:id="@+id/deleteShoutButton"
        android:layout_toRightOf="@id/button"
        android:layout_marginTop="71dp"
        android:onClick="deleteShout"
        android:visibility="invisible" />

    <Button
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Record a Shout"
        android:id="@+id/startRecording"
        android:layout_alignParentLeft="true"
        android:layout_alignParentStart="true"
        android:layout_marginTop="71dp"
        android:onClick="launchRecordingOverlay" />

</RelativeLayout>
```

After the code has been modified, click **Run -> Run 'app'** to build and start the app.  You should see the initial Activity with the Start Recording button enabled.  When you press that button, it will launch the Shout to Me recording overlay as seen in the following images and immediately begin recording. Pressing the Done button on the overlay will transmit the recorded audio to the Shout to Me service for processing.

![Sample app](https://s3-us-west-2.amazonaws.com/sdk-public-images/sample-app-3.png)
![Shout to me overlay](https://s3-us-west-2.amazonaws.com/sdk-public-images/sample-app-4.png)

## SDK Documentation

### Dependencies

#### Google Play Services
The Shout to Me SDK utilizes [Google Play Services Location Services](https://developers.google.com/android/reference/com/google/android/gms/location/package-summary).  If the mobile user does not have the required Google Play Services, the application may not function properly.  Google Play Services provides a convenient mechanism to prompt the user to install or upgrade their Google Play Services if applicable.

```java
GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
int val = googleApiAvailability.isGooglePlayServicesAvailable(this);
if (val != ConnectionResult.SUCCESS) {
    Dialog gpsErrorDialog = googleApiAvailability.getErrorDialog(this, val, 2);
    gpsErrorDialog.show();
}
```

#### Volley
The Shout to Me SDK utilizes Volley to optimize asynchronous communication with the API service.  For more information about volley, see the [Android Volley documentation](https://developer.android.com/training/volley/index.html).

#### Amazon Web Services
The Shout to Me SDK utilizes AWS to send push notifications to mobile devices.  For more information about the Amazon Android AWS SDK, see the [Amazon mobile SDK documentation](https://aws.amazon.com/mobile/sdk/).

### Permissions
The Shout to Me SDK requests the following permissions in the manifest:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Beginning with API level 23 (6.0), [Android requires that certain permissions be requested at run time](https://developer.android.com/training/permissions/requesting.html). The Shout to Me SDK uses two permissions that fall into this category:

1. Mic/Record Audio (Required)
2. Location (Optional)

#### Record Audio
Being that Shout to Me is an audio-based platform, this permission is considered required.  Launching the recording overlay without the permission will result in a failure response indicating that the record audio permission is denied.  Here is an example of one way to check for this permission and requesting it if not granted:
```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
    // User has not granted access to record audio.  Ask the user for permission now.
    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO }, 0);
}
```

#### Location
Use of location functionality is optional in the Shout to Me platform.  However, if location permission is enabled, the coordinates (lat/lon) of the person shouting are included with the Shout creation request and broadcasters will be able to see the location of the user.  
```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION }, 0);
}
```

If the user rejects the permission request, there is a way to explain the rationale for the request.  This is beyond the scope of this document.  More information can be found in the [Android - Request Permissions documentation](https://developer.android.com/training/permissions/requesting.html#perm-request). 

### StmService
The StmService class is the object developers will use to establish integration with the Shout to Me system.  It is implemented as a [bound service](http://developer.android.com/guide/components/bound-services.html) in Android to provide developers with a convenient, native way to integrate with. Here is an example of one way to check for this permission and requesting it if not granted:

```java
/**
 * StmService
 * Main class to access Shout to Me SDK functionality
 * Requires a client access token
 */
public class StmService extends Service
```

Sample binding with StmService through the Android ServiceConnection class

```java
private ServiceConnection stmServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className,
                                   IBinder service) {
        // We've bound to StmService, cast the IBinder and get StmService instance
        Log.d(TAG, "in onServiceConnected");
        StmService.StmBinder binder = (StmService.StmBinder) service;
        stmService = binder.getService();
        isStmServiceBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        isStmServiceBound = false;
    }
};
    
// Bind to StmService
Intent intent = new Intent(this, StmService.class);
bindService(intent, stmServiceConnection, Context.BIND_AUTO_CREATE);
```

Unbinding

```java    
unbindService(stmServiceConnection);
```

#### AndroidManifest.xml
The StmService gets the Shout to Me client token from metadata in the AndroidManifest.xml.  Optionally, you can wire up the channel ID if your app will only use one channel. Be sure to set your own string resources.
```xml
<service
    android:name="me.shoutto.sdk.StmService"
    android:exported="false">
    
    <meta-data
        android:name="me.shoutto.sdk.CLIENT_TOKEN"
        android:value="@string/client_token" />
    
    <!-- Optional -->
    <meta-data
        android:name="me.shoutto.sdk.CHANNEL_ID"
        android:value="@string/channel_id" />
</service>
```

#### Getting the user's authentication token
While the SDK is meant to provide easy access to the Shout to Me API service, if you wish to send direct 
requests to the Shout to Me REST API, you will need the user's authentication token.
To get the auth token, call the following method on StmService.  The first time this method is called,
it blocks until the auth token is retrieved.  Therefore, either call this method on a background thread,
or be prepared to handle an error in the event that the auth token has not yet been retrieved.

Once retrieved from the server, the auth token is stored in the device shared preferences to save time on
future retrievals and across app sessions.

```java
stmService.getUserAuthToken();
```

#### Refreshing the user's authentication token
The SDK will abstract out much of the authentication tasks for you.  In the rare instance that requests are
returned as unauthorized or when switching between test and production URLs, you can call the following method 
to refresh the user's authentication token.  This is a synchronous method and should be done on a background thread.

```java
stmService.refreshUserAuthToken();
```

#### Hand wave gesture initiated Shout recording
The Shout to Me SDK includes a usability feature design to help make the app safe for driving.  When
enabled, a driver need only wave their hand in front of the phone to launch the
[StmRecorderActivity](#stm-recorder-activity).

The hand wave gesture functionality utilizes the phone's proximity sensor.  Therefore, if a phone
does not have a proximity sensor, or the user has revoked proximity sensor permission, the
functionality will not work.

To enable the hand wave gesture functionality, simply register a listener to the following StmService method:

```java
public class MainActivity extends Activity implements HandWaveGestureListener {

    private ServiceConnection stmServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            StmService.StmBinder binder = (StmService.StmBinder) service;
            stmService = binder.getService();
            isStmServiceBound = true;

            regsiterHandWaveGestureListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isStmServiceBound = false;
        }
    };

    @Override
    public void onHandWaveGesture() {
        launchRecordingOverlay(null);
    }

    private void regsiterHandWaveGestureListener() {
        stmService.registerHandGestureListener(this);
    }
}
```

Once enabled, the Shout to Me SDK continues to listen for events from the proximity sensor.
Un-registering the listener will turn off the proximity event listening, thereby conserving device
resources.

```java
stmService.unregisterHandGestureListener(this);
```

Note that you can specify more than one hand wave gesture listener if you want to take advantage
of the functionality, however, be sure to un-register them all to effectively turn off the
proximity sensor event listening.

### <a name="callback"></a>Callback
The Android system [does not allow asynchronous calls to be made on the main (UI) thread](http://developer.android.com/guide/components/processes-and-threads.html). The Callback class is used to provide methods you would like to have executed following the asynchronous calls to the Shout to Me service.  Callback is an abstract class with two methods that can be overridden.  

```java
public abstract class Callback<T> implements StmCallback<T> {

    public abstract void onSuccess(StmResponse<T> stmResponse);
    public abstract void onFailure(StmError stmError);
}
```

### StmError
The StmError class encapsulates information in the event of an error during asynchronous processing.  It is provided to the Callback `onFailure` method.  A "blocking error" occurs when the system determines that the client application cannot continue using Shout to Me functionality, such as if the Shout to Me service is unreachable.

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

    public boolean isBlockingError() {
        return blockingError;
    }
}
```

### User
The User object represents the Shout to Me user entity that is bound to your application.  Although you may have your own user domain objects, Shout to Me still needs a context in which to create shouts, accumulate statistics, etc.  A Shout to Me user created by your mobile app will be unique to your system.  For example, if a mobile user has installed two apps that utilize the Shout to Me SDK on the same phone, there will be two distinct Shout to Me users, one for each app.  Much of the generic code around creating and authenticating the user is hidden from you by the SDK to make your life easier.  However, there are a few items that do need to be exposed, such as setting a user handle to match the handle in your system.

Retrieving the user object from StmService can be done with or without a [Callback](#callback). Retrieving the user object without a [Callback](#callback) does not guarantee that the user object will have been initialized by the service, however, it is convenient for certain scenarios such as wanting to change the user's handle.

```java
// Without a callback
User user = stmService.getUser()

// With a callback
stmService.getUser(new Callback<User>() {
    @Override
    public void onSuccess(final StmResponse<User> stmResponse) {
        Log.d(TAG, "Shout to Me user has been loaded");
		
	User user = stmResponse.get();
	Log.d(TAG, "User's handle is: " + user.getHandle());
    }

    @Override
    public void onFailure(final StmError stmError) {
        Log.w(TAG, "Could not retrieve Shout to Me user.");
    }
});
```

Retrieving and updating the user’s handle.  Calling `save(Callback<User>)` is required to persist the new handle to the Shout to Me service.

```java
String handle = user.getHandle();

User user = stmService.getUser();
user.setHandle("BobSmith");
user.save(new Callback<User>() {
    @Override
    public void onSuccess(final StmResponse<User> stmResponse) {
        Log.d(TAG, "User handle update was successful. Handle is " + stmResponse.get().getHandle());
    }

    @Override
    public void onFailure(final StmError stmError) {
        Log.w(TAG, "Could not save changes to user. Message is: " + stmError.getMessage());
    }
});
```

### Shout
The Shout object represents the recording a user created and its metadata in the Shout to Me system.  Although generally a read-only object to the mobile user, an administrative user can make updates to it, such as making it public or private, and queuing it to be played on-air.

Currently the only time a client app will interact with a Shout is in the [StmRecorderActivity](#stm-recorder-activity) callback.  

Deleting a shout

```java
shout.delete(new Callback<String>() {
    @Override
    public void onSuccess(StmResponse<String> stmResponse) {
        if (stmResponse.get().equals("success")) {
            Log.d(TAG, "Deletion of shout succeeded.");
        }
    }

    @Override
    public void onFailure(StmError stmError) {
        Log.e(TAG, "Error occurred deleting shout. Error message: " + stmError.getMessage());
    }
});
```

### <a name="channel"></a>Channel
The Channel object represents one or more channels that you may have configured in your Shout to Me account.  The Channel object contains metadata and default configuration values that can be used to display information to your users.

```java
public class Channel {

    public String getId()

    public String getName()

    public String getDescription()

    // The URL of an image that can be used to show the user what channel they are on
    public String getImageUrl()

    // A smaller version of the channel image to display in lists
    public String getListImageUrl()

    // The maximum recording length in seconds allowed for the channel
    public int getDefaultMaxRecordingLengthSeconds()
}
```

Retrieving channels

```java
stmService.getChannels(new Callback<List<Channel>>() {
    @Override
    public void onSuccess(StmResponse<List<Channel>> stmResponse) {
        List<Channel> channels = stmResponse.get();
        for (Channel channel : channels) {
            Log.d(TAG, "Image URL for " + channel.getName() + " is " + channel.getImageUrl());
        }
    }

    @Override
    public void onFailure(StmError stmError) {
        Log.w(TAG, "Could not retrieve channel list");
    }
});
```

#### Channel Subscriptions
A subscription to a channel indicates that push notifications will be sent to the app when a broadcaster
publishes a channel-wide message.  The SDK provides a way for the client app to subscribe, unsubscribe and
tell if the user is currently subscribed or not.  This can be used in an app setting to allow the user
control over their subscription status.

Determining subscription status

```java
channel.isSubscribed(new Callback<Boolean>() {
    @Override
    public void onSuccess(StmResponse<Boolean> isSubscribedResponse) {
        if (isSubscribedResponse.get()) {
            Log.d(TAG, "User is subscribed to channel");
        } else {
            Log.d(TAG, "User is not subscribed to channel");
        }
    }

    @Override
    public void onFailure(StmError stmError) {
        Log.w(TAG, "An error occurred checking user's subscribed status. " + stmError.getMessage());
    }
});
```

Subscribing to a channel

```java
channel.subscribe(new Callback<Void>() {
    @Override
    public void onSuccess(StmResponse<Void> subscribeResponse) {
        Log.d(TAG, "User is now subscribed");
    }

    @Override
    public void onFailure(StmError stmError) {
        Log.w(TAG, "An error occurred subscribing to channel. " + stmError.getMessage());
    }
});
```

Unsubscribing to a channel

```java
channel.unsubscribe(new Callback<Void>() {
    @Override
    public void onSuccess(StmResponse<Void> unsubscribeResponse) {
        Log.d(TAG, "User is now unsubscribed.");
    }

    @Override
    public void onFailure(StmError stmError) {
        Log.w(TAG, "An error occurred unsubscribing to channel. " + stmError.getMessage());
    }
});
```

### Message
The Message object represents text or audio messages that can be sent from broadcasters to users.  A
user may receive messages from more than one channel if the client app supports multiple channels.

```java
public class Message {

    public String getId()

    public Channel getChannel()

    // The actual message text
    public String getMessage()

    // The name of the sender.  May be null if was sent via a channel-wide notification
    public String getSenderName()

    public Date getSentDate()

    // A reference to a Shout to Me conversation.  May be null
    public String getConversationId()
}
```

Retrieving messages

A maximum of 1000 messages will be returned.

```java
stmService.getMessages(new Callback<List<Message>>() {
    @Override
    public void onSuccess(StmResponse<List<Message>> messagesResponse) {
        List<Message> messageList = messagesResponse.get();
        Log.d(TAG, "Number of messages = " + String.valueOf(messageList.size()));
    }

    @Override
    public void onFailure(StmError stmError) {
        Log.w(TAG, "Could not retrieve message list");
    }
});
```

Creating a message

At certain times a client app may need to create a message for persistence in the user's message records.
A Builder class is provided to help with this.

```java
stmService.getMessageBuilder()
    .channelId(channelId)
    .conversationId(conversationId)
    .message(messageBody)
    .recipientId(stmService.getUser().getId())
    .create(new Callback<Message>() {
        @Override
        public void onSuccess(StmResponse<Message> response) {
            Message message = response.get();
            Log.d(TAG, "Message created successfully " + message.getId());
        }

        @Override
        public void onFailure(StmError stmError) {
            Log.w(TAG, "Error creating message " + stmError.getMessage());
        }
    });
```

### <a name="stm-recorder-activity"></a>StmRecorderActivity
The StmRecorderActivity is a native Android [Activity](http://developer.android.com/reference/android/app/Activity.html) used to quickly and easily enable recording in a client app.  When the StmRecorderActivity is launched, the following will occur:

1. The StmRecorderActivity is displayed to the user
2. Audio recording immediately begins
3. The user is presented with two options
    * A “Done” button; when pressed, this will stop the recording and send the recorded audio to the server.  The StmRecorderActivity will then be closed.
    * A “Cancel” icon; when pressed, the recording will be stopped and the StmRecorderActivity will be closed.

There is also the ability to provide an optional callback if you would like to receive an Shout object following the creation of the shout.

```java
stmService.setShoutCreationCallback(new Callback<Shout>() {
    @Override
    public void onSuccess(StmResponse<Shout> stmResponse) {
        Log.d(TAG, "Shout created successfully. ID = " + stmResponse.get().getId());
        shout = stmResponse.get();
    }

    @Override
    public void onFailure(StmError stmError) {
        Log.e(TAG, "An error occurred during shout creation. Message is " + stmError.getMessage());
    }
});
```

#### Launching the StmRecorderActivity
Launching the StmRecorderActivity is done using standard Android Activity functionality.  You can pass in certain extras to provide additional data.

1. StmRecorderActivity.MAX_RECORDING_TIME_IN_SECONDS - This extra is required. Maximum recording times can be found in [Channel](#channel) data.  Please note that the Shout to Me system currently does not support recording times over 1 minute.
2. StmRecorderActivity.TAGS - A comma separated list of tags that will flow through to the Broadcaster Application.
2. StmRecorderActivity.TOPIC - A topic that will flow through to the Broadcaster Application.

```java
Intent intent = new Intent(this, StmRecorderActivity.class);
intent.putExtra(StmRecorderActivity.MAX_RECORDING_TIME_IN_SECONDS, maxRecordingLengthSeconds);  // Required
intent.putExtra(StmRecorderActivity.TAGS, tags);                                                // Optional
intent.putExtra(StmRecorderActivity.TOPIC, topic);                                              // Optional
startActivityForResult(intent, 1); 
```

#### Handling the Activity result of StmRecorderActivity
The StmRecorderActivity uses the standard Android callback to indicate whether the Activity was closed OK, or whether the action was cancelled.  In addition, the StmRecorderActivity will provide data to confirm whether or not the recording process completed successfully or not.  The example below shows how to detect the StmRecorderActivity.RECORD_AUDIO_PERMISSION_DENIED failure.

```java
protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (requestCode == MY_REQUEST_CODE) {
        if (resultCode == RESULT_OK){
            String recordingResult = data.getStringExtra(StmRecorderActivity.ACTIVITY_RESULT);
            Log.d(TAG, "The recording overlay has closed successfully. Result is: " + recordingResult);
            
            if (recordingResult.equals(StmService.FAILURE)) {
                String failureReasonCode = data.getStringExtra(StmRecorderActivity.ACTIVITY_REASON);
                Log.d(TAG, "Failure code: " + failureReasonCode);
                    if (failureReasonCode.equals(StmRecorderActivity.RECORD_AUDIO_PERMISSION_DENIED)) {

                        Log.w(TAG, "User has not granted the RECORD_AUDIO permission");
                    }
                } 
       }
       if (resultCode == RESULT_CANCELED) {
           // Write your code if there's no result
           Log.d(TAG, "recording cancelled");
       }
    }
}
```

### Notifications
The Shout to Me SDK supports receiving push notifications from the Shout to Me platform.  There are a number of technologies used in receiving notifications, and consequently, there are a number of items that need to be wired up. The following high level steps occur in the notifications system:

1. A notification is received from GCM
2. The SDK processes the notification and may do one of two things:
    a. Immediately broadcast a message to the client app
    b. Create a geofence which may be triggered later if and when a user enters the geofence area
3. A listener in the client app receives a broadcast and displays data to the mobile user

#### GCM
The Shout to Me system uses (GCM)[https://developers.google.com/cloud-messaging/] to send and receive messages. Add the following to your AndroidManifest.xml if you wish to receive notifications.  Be sure to set your own values for the string resource references.  Check with Shout to Me support for specific values to use.

```xml
<service
    android:name="me.shoutto.sdk.GcmNotificationRegistrationIntentService"
    android:exported="false">
    <meta-data android:name="me.shoutto.sdk.GcmDefaultSenderId" android:value="@string/gcm_default_sender_id" />
    <meta-data android:name="me.shoutto.sdk.PlatformApplicationArn" android:value="@string/platform_application_arn" />
    <meta-data android:name="me.shoutto.sdk.IdentityPoolId" android:value="@string/identity_pool_id" />
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

#### Geofencing
Location based notifications will be created as (geofences)[https://developers.google.com/android/reference/com/google/android/gms/location/Geofence] in the Shout to Me SDK.  Add this to your AndroidManifest.xml to allow the SDK to listen for geofence events:

```xml
<service android:name="me.shoutto.sdk.GeofenceTransitionsIntentService" />
```

#### Shout to Me Broadcasts
The Shout to Me SDK uses a standard Android broadcast to send the processed message data to client apps.  Add the following to your AndroidManifest.xml to listen for these broadcasts.  Of course, you will need to supply your own listener class. In this example, it is called StmNotificationReceiver.

```xml
<receiver
    android:name=".StmNotificationReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="me.shoutto.sdk.EVENT_MESSAGE_NOTIFICATION_RECEIVED" />
    </intent-filter>
</receiver>
```

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