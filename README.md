# Shout to Me Android SDK
*Version 1*
## Quickstart Guide
This guide will show you how to get up and running with the Shout to Me Android SDK in minutes.
### Prerequisites
- A Shout to Me client access token
- [Android Studio](http://developer.android.com/tools/studio/index.html) and all its dependencies. This quickstart guide was developed using Android Studio version 1.3

### Create an Android application
Run through the Android Studio’s Create New Project wizard.  The minimum Android SDK required is **API 11: Android 3.0 (Honeycomb)**.  For the rest of the options, you can leave them set to their defaults.

![Create new project step 1](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-1.png)
![Create new project step 2](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-2.png)
![Create new project step 3](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-3.png)
![Create new project step 4](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-4.png)
### Add the Shout to Me Android SDK
Add the stm-sdk.aar file into the new app by dragging it into the libs directory.
![Add stm-sdk.aar to libs](https://s3-us-west-2.amazonaws.com/sdk-public-images/stm-aar-libs.png)

Add the following highlighted lines to the build.gradle under your app module directory (or whatever name you used when setting up the project.)

    repositories{
       flatDir{
           dirs 'libs'
       }
    }

    dependencies {
       compile fileTree(dir: 'libs', include: ['*.jar'])
       compile 'com.android.support:appcompat-v7:22.2.1'
       compile(name:'stm-sdk', ext:'aar')
       compile 'com.mcxiaoke.volley:library:1.0.18'
       compile 'com.google.android.gms:play-services-location:7.8.0'
    }
    
Then click:  `Tools -> Android -> Sync Project with Gradle Files`
### Use the Shout to Me Android SDK
You are now able to begin coding with the Shout to Me Android SDK.  Assuming you used the default names when creating the projects, modify the following files so they look like these:

**MainActivity.java**

```java
package me.shoutto.androidsdk;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import me.shoutto.sdk.android.Callback;
import me.shoutto.sdk.android.StmResponse;
import me.shoutto.sdk.android.StmError;
import me.shoutto.sdk.android.StmRecorderActivity;
import me.shoutto.sdk.android.StmService;
import me.shoutto.sdk.android.StmShout;
import me.shoutto.sdk.android.StmUser;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private StmService stmService;
    private Boolean isStmServiceBound = false;
    private StmShout newlyCreatedShout;

    private ServiceConnection stmServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to StmService, cast the IBinder and get StmService instance
            Log.d(TAG, "in onServiceConnected");
            StmService.StmBinder binder = (StmService.StmBinder) service;
            stmService = binder.getService();
            isStmServiceBound = true;

            // Example:  Get user data as soon as service is connected

            // Get a reference to the UI text box
            final EditText handleEditText = (EditText) findViewById(R.id.editTextUserHandle);

            // Calling getUser() with a Callback will ensure you get an instantiated user object from the server
            stmService.getUser(new Callback<StmUser>() {
                @Override
                public void onSuccess(final StmResponse<StmUser> stmResponse) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind to the Shout to Me service
        Intent intent = new Intent(this, StmService.class);
        intent.putExtra("stmClientToken", getString(R.string.stm_client_token));
        bindService(intent, stmServiceConnection, Context.BIND_AUTO_CREATE);

        // Show user a Dialog to update Google Play Services if required version is not installed
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int val = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (val != ConnectionResult.SUCCESS) {
            Dialog gpsErrorDialog = googleApiAvailability.getErrorDialog(this, val, 2);
            gpsErrorDialog.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind from the Shout to Me service
        if (isStmServiceBound) {
            unbindService(stmServiceConnection);
            isStmServiceBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void launchRecordingOverlay(View view) {

        Log.d(TAG, "launching overlay");
        stmService.setShoutCreationCallback(new Callback<StmShout>() {
            @Override
            public void onSuccess(StmResponse<StmShout> stmResponse) {
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
        startActivityForResult(intent, 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                String result=data.getStringExtra("result");
                Log.d(TAG, "The recording overlay has closed successfully. Result is: " + result);
            }
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Recording was cancelled");
            }
        }
    }

    public void setUserHandle(View view) {
        final EditText editText = (EditText)findViewById(R.id.editTextUserHandle);
        String newHandle = editText.getText().toString();

        // Calling getUser() without a Callback does not guarantee that the object will be
        // instantiated from the server, but is useful for update-only functions.
        StmUser stmUser = stmService.getUser();
        stmUser.setHandle(newHandle);
        stmUser.save(new Callback<StmUser>() {
            @Override
            public void onSuccess(final StmResponse<StmUser> stmResponse) {
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

    public void deleteShout(View view) {
        Log.d(TAG, "Deleting shout");
        if (newlyCreatedShout != null) {
            newlyCreatedShout.delete(new Callback<String>() {
                @Override
                public void onSuccess(StmResponse<String> stmResponse) {
                    if (stmResponse.get().equals("success")) {
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
}
```

**activity_main.xml**

```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:background="#FFFFFF"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:paddingLeft="@dimen/activity_horizontal_margin"
    android:paddingRight="@dimen/activity_horizontal_margin"
    android:paddingTop="@dimen/activity_vertical_margin"
    android:paddingBottom="@dimen/activity_vertical_margin" tools:context=".MainActivity">

    <TextView android:text="@string/hello_world" android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/textView" />

    <EditText
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/editTextUserHandle"
        android:layout_below="@+id/textView"
        android:layout_alignParentLeft="true"
        android:layout_alignParentStart="true"
        android:layout_marginTop="118dp" />

    <Button
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Change Handle"
        android:id="@+id/button"
        android:layout_below="@+id/editTextUserHandle"
        android:layout_alignParentLeft="true"
        android:layout_alignParentStart="true"
        android:onClick="setUserHandle" />

    <Button
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Delete Last Shout"
        android:id="@+id/deleteShoutButton"
        android:layout_toRightOf="@+id/button"
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

After the code has been modified, click `Run -> Run 'app'` to build and start the app.  You should see the initial Activity with the Start Recording button enabled.  When you press that button, it will launch the Shout to Me recording overlay as seen in the following images and immediately begin recording. Pressing the Done button on the overlay will transmit the recorded audio to the Shout to Me service for processing.

![Sample app 1](https://s3-us-west-2.amazonaws.com/sdk-public-images/sample-app-1.png)
![Sample app 2](https://s3-us-west-2.amazonaws.com/sdk-public-images/sample-app-2.png)

## SDK Documentation

### Google Play Services
The Shout to Me SDK utilizes [Google Play Services Location Services](https://developers.google.com/android/reference/com/google/android/gms/location/package-summary).  If the mobile user does not have the required Google Play Services, the application may not function properly.  Google Play Services provides a convenient mechanism to prompt the user to install or upgrade their Google Play Services if applicable.

```java
GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
int val = googleApiAvailability.isGooglePlayServicesAvailable(this);
if (val != ConnectionResult.SUCCESS) {
    Dialog gpsErrorDialog = googleApiAvailability.getErrorDialog(this, val, 2);
    gpsErrorDialog.show();
}
```

### StmService
The StmService class is the object developers will use to establish integration with the Shout to Me system.  It is implemented as a [bound service](http://developer.android.com/guide/components/bound-services.html) in Android to provide developers with a convenient, native way to integrate with.

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
intent.putExtra("stmClientToken", "***********client_token**********");
bindService(intent, stmServiceConnection, Context.BIND_AUTO_CREATE);
```

Unbinding

```java    
unbindService(stmServiceConnection);
```

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

### StmUser
The StmUser object represents the Shout to Me user entity that is bound to your application.  Although you may have your own user domain objects, Shout to Me still needs a context in which to create shouts, accumulate statistics, etc.  A Shout to Me user created by your mobile app will be unique to your system.  For example, if a mobile user has installed two apps that utilize the Shout to Me SDK on the same phone, there will be two distinct Shout to Me users, one for each app.  Much of the generic code around creating and authenticating the user is hidden from you by the SDK to make your life easier.  However, there are a few items that do need to be exposed, such as setting a user handle to match the handle in your system.

Retrieving the user object from StmService can be done with or without a [Callback](#callback). Retrieving the user object without a [Callback](#callback) does not guarantee that the user object will have been initialized by the service, however, it is convenient for certain scenarios such as wanting to change the user's handle.

```java
// Without a callback
StmUser stmUser = stmService.getUser()

// With a callback
stmService.getUser(new Callback<StmUser>() {
    @Override
    public void onSuccess(final StmResponse<StmUser> stmResponse) {
        Log.d(TAG, "Shout to Me user has been loaded");
		
	StmUser stmUser = stmResponse.get();
	Log.d(TAG, "User's handle is: " + stmUser.getHandle());
    }

    @Override
    public void onFailure(final StmError stmError) {
        Log.w(TAG, "Could not retrieve Shout to Me user.");
    }
});
```

Retrieving and updating the user’s handle.  Calling `save(Callback<StmUser>)` is required to persist the new handle to the Shout to Me service.

```java
String handle = stmUser.getHandle();

StmUser stmUser = stmService.getUser();
stmUser.setHandle("BobSmith");
stmUser.save(new Callback<StmUser>() {
    @Override
    public void onSuccess(final StmResponse<StmUser> stmResponse) {
        Log.d(TAG, "User handle update was successful. Handle is " + stmResponse.get().getHandle());
    }

    @Override
    public void onFailure(final StmError stmError) {
        Log.w(TAG, "Could not save changes to user. Message is: " + stmError.getMessage());
    }
});
```

### StmShout
The StmShout object represents the recording a user created and its metadata in the Shout to Me system.  Although generally a read-only object to the mobile user, an administrative user can make updates to it, such as making it public or private, and queuing it to be played on-air.

Currently the only time a client app will interact with a Shout is in the [StmRecorderActivity](#stm-recorder-activity) callback.  

Deleting a shout

```java
stmShout.delete(new Callback<String>() {
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

### <a name="stm-recorder-activity"></a>StmRecorderActivity
The StmRecorderActivity is a native Android [Activity](http://developer.android.com/reference/android/app/Activity.html) used to quickly and easily enable recording in a client app.  When the StmRecorderActivity is launched, the following will occur:

1. The StmRecorderActivity is displayed to the user
2. Audio recording immediately begins
3. The user is presented with two options
    * A “Done” button; when pressed, this will stop the recording and send the recorded audio to the server.  The StmRecorderActivity will then be closed.
    * A “Cancel” icon; when pressed, the recording will be stopped and the StmRecorderActivity will be closed.

Optional callback if you would like to receive an StmShout object following the creation of the shout.

```java
stmService.setShoutCreationCallback(new Callback<StmShout>() {
    @Override
    public void onSuccess(StmResponse<StmShout> stmResponse) {
        Log.d(TAG, "Shout created successfully. ID = " + stmResponse.get().getId());
        stmShout = stmResponse.get();
    }

    @Override
    public void onFailure(StmError stmError) {
        Log.e(TAG, "An error occurred during shout creation. Message is " + stmError.getMessage());
    }
});
```

Launching the StmRecorderActivity

```java
Intent intent = new Intent(this, StmRecorderActivity.class);
startActivityForResult(intent, 1); 
```

Handling the Activity result of StmRecorderActivity

```java
protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (requestCode == 1) {
       if(resultCode == RESULT_OK){
           // The recording completed and the async server call was sent
           Log.d(TAG, result);
       }
       if (resultCode == RESULT_CANCELED) {
           // Write your code if there's no result
           Log.d(TAG, "recording cancelled");
       }
    }
}
```


