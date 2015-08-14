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
    }
    
Then click:  `Tools -> Android -> Sync Project with Gradle Files`
### Use the Shout to Me Android SDK
You are now able to begin coding with the Shout to Me Android SDK.  Assuming you used the default names when creating the projects, modify the following files so they look like these:

**MainActivity.java**

    package com.mycompany.teststmsdk;
    
    import android.content.ComponentName;
    import android.content.Context;
    import android.content.Intent;
    import android.content.ServiceConnection;
    import android.os.IBinder;
    import android.support.v7.app.AppCompatActivity;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.Menu;
    import android.view.MenuItem;
    import android.view.View;
    import android.widget.Button;
    
    import me.shoutto.sdk.android.StmError;
    import me.shoutto.sdk.android.StmOverlayActivity;
    import me.shoutto.sdk.android.StmService;
    import me.shoutto.sdk.android.StmServiceListener;
    
    public class MainActivity extends AppCompatActivity implements StmServiceListener {
    
       private StmService stmService;
       private Boolean isStmServiceBound = false;
    
       private ServiceConnection stmServiceConnection = new ServiceConnection() {
    
           @Override
           public void onServiceConnected(ComponentName className,
                                          IBinder service) {
               // We've bound to StmService, cast the IBinder and get StmService instance
               StmService.StmBinder binder = (StmService.StmBinder) service;
               stmService = binder.getService();
               registerListener();
               isStmServiceBound = true;
           }
    
           @Override
           public void onServiceDisconnected(ComponentName arg0) {
               isStmServiceBound = false;
           }
       };
    
       private void registerListener() {
           stmService.setServiceListener(this);
       }
    
       public void launchRecordingOverlay(View view) {
           Intent intent = new Intent(this, StmOverlayActivity.class);
           startActivityForResult(intent, 1);
           overridePendingTransition(R.anim.stm_slidedown_anim, R.anim.stm_hold);
       }
    
       protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    
           if (requestCode == 1) {
               if(resultCode == RESULT_OK){
                   Log.d("MainActivity", "Recording completed successfully");
               }
               if (resultCode == RESULT_CANCELED) {
                   //Write your code if there's no result
                   Log.d("MainActivity", "Recording cancelled");
               }
           }
       }
    
       @Override
       public void stmInitialized(StmError stmError) {
           if (stmError == null) {
               Button startRecordingBtn = (Button)findViewById(R.id.startRecordingBtn);
               startRecordingBtn.setEnabled(true);
           } else {
               Log.e("MainActivity", "Oops.. something happened initializing Shout to Me");
           }
       }
    
       @Override
       protected void onCreate(Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);
           setContentView(R.layout.activity_main);
    
           // Bind to StmService
           Intent intent = new Intent(this, StmService.class);
           intent.putExtra("stmClientToken", "***********client_token**********");
           bindService(intent, stmServiceConnection, Context.BIND_AUTO_CREATE);
       }
    
       @Override
       protected void onDestroy() {
           super.onDestroy();
    
           // Unbind from the service
           if (isStmServiceBound) {
               unbindService(stmServiceConnection);
               isStmServiceBound = false;
           }
       }
    
       @Override
       public boolean onCreateOptionsMenu(Menu menu) {
           // Inflate the menu; this adds items to the action bar if it is present.
           getMenuInflater().inflate(R.menu.menu_main, menu);
           return true;
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
    }

**activity_main.xml**

    <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
       xmlns:tools="http://schemas.android.com/tools" android:layout_width="match_parent"
       android:layout_height="match_parent" android:paddingLeft="@dimen/activity_horizontal_margin"
       android:paddingRight="@dimen/activity_horizontal_margin"
       android:paddingTop="@dimen/activity_vertical_margin"
       android:paddingBottom="@dimen/activity_vertical_margin" tools:context=".MainActivity">
    
       <TextView android:text="Shout to Me SDK Sample App" android:layout_width="wrap_content"
           android:layout_height="wrap_content"
           android:id="@+id/textView" />
    
       <Button
           android:layout_width="wrap_content"
           android:layout_height="wrap_content"
           android:text="Start Recording"
           android:id="@+id/startRecordingBtn"
           android:enabled="false"
           android:layout_below="@+id/textView"
           android:layout_alignParentLeft="true"
           android:layout_alignParentStart="true"
           android:onClick="launchRecordingOverlay" />
    </RelativeLayout>

After the code has been modified, click `Run -> Run 'app'` to build and start the app.  You should see the initial Activity with the Start Recording button enabled.  When you press that button, it will launch the Shout to Me recording overlay as seen in the following images and immediately begin recording. Pressing the Done button on the overlay will transmit the recorded audio to the Shout to Me service for processing.

![Sample app 1](https://s3-us-west-2.amazonaws.com/sdk-public-images/sample-app-1.png)
![Sample app 2](https://s3-us-west-2.amazonaws.com/sdk-public-images/sample-app-2.png)

## SDK Documentation
### StmService
The StmService class is the object developers will use to establish integration with the Shout to Me system.  It is implemented as a [bound service](http://developer.android.com/guide/components/bound-services.html) in Android to provide developers with a convenient, native way to integrate with.  

    /**
    * StmService
    * Main class to access Shout to Me SDK functionality
    * Requires a client access token
    */
    public class StmService extends Service

Sample binding with StmService through the Android ServiceConnection class

    private ServiceConnection stmServiceConnection = new ServiceConnection() {
    
       @Override
       public void onServiceConnected(ComponentName className,
                                          IBinder service) {
           // We've bound to StmService, cast the IBinder and get StmService instance
           StmService.StmBinder binder = (StmService.StmBinder) service;
           stmService = binder.getService();
           registerListener();
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
    
Unbinding
    
    unbindService(stmServiceConnection);

### StmServiceListener
This is the listener related to application level callbacks.  Application level callbacks and domain objects callbacks have been split into separate listeners so that the functionality is more modular and flexible.  You only need to implement the callbacks you need.  Of note in StmServiceListener, the callback stmInitialized is called when the Shout to Me SDK system is fully ready to receive requests on behalf of the user.  You cannot make calls to the Shout to Me system prior to receiving that callback.

    /**
    * StmServiceListener
    * For application level callbacks
    */
    public interface StmServiceListener {
    
       /**
        * stmInitialized
        * Called after all initialization activities have been completed. 
        * This includes the preparation of a Shout to Me User object and
        * receiving the authentication token to make calls on the user's
        * behalf. No calls can be made to the Shout to Me system until
        * this callback occurs.
        *
        * @param stmError - An StmError object or null if no error
        */
       void stmInitialized(StmError stmError);
    }

### StmUser
The StmUser object represents the Shout to Me user entity that is bound to your application.  Although you may have your own user domain objects, Shout to Me still needs a context in which to create shouts, accumulate statistics, etc.  A Shout to Me user created by your mobile app will be unique to your system.  For example, if a mobile user has installed two apps that utilize the Shout to Me SDK on the same phone, there will be two distinct Shout to Me users, one for each app.  Much of the generic code around creating and authenticating the user is hidden from you by the SDK to make your life easier.  However, there are a few items that do need to be exposed, such as setting a user handle to match the handle in your system.

Retrieving the user object from StmService

    StmUser stmUser = stmService.getUser()

Retrieving and updating the user’s handle

    String handle = stmUser.getHandle();
    stmUser.setHandle(“NewGuy”);

### StmUserListener
The StmUserListener can be implemented by objects that need to be able to receive callbacks on User related asynchronous functions.

    /**
    * StmUserListener
    * For callbacks related to operations on the StmUser
    */
    public interface StmUserListener {
    
       /**
        * handleUpdateUserHandleResponse
        * Called following an update to the user's handle.  Possible
        * errors include a non-unique handle and invalid format.
        * Only letters, numbers, and underscores are valid
        * characters for the user handle.
        * @param stmError - An StmError object or null if no error
        */
       void handleUpdateUserHandleResponse(StmError stmError);
    }

### StmShout
The StmShout object represents the recording a user created and its metadata in the Shout to Me system.  Although generally a read-only object to the mobile user, an administrative user can make updates to it, such as making it public or private, and queuing it to be played on-air.

Retrieving a shout from StmService

    StmShout stmShout = stmService.getShout(“1434374231436-5096fa7a0a6c99e2489b06dcee8ca4c0”);

Deleting a shout

    stmShout.delete();

### StmShoutListener
The StmShoutListener can be implemented by objects that need to be able to receive callbacks on Shout related asynchronous functions.  

    /**
    * StmShoutListener
    * For callbacks related to operations on a StmShout object
    */
    public interface StmShoutListener {
    
       /**
        * handleCreateShoutResponse
        * Called after the response is received from a call to the create
        * shout server endpoint.  This includes the recorded audio being sent to 
        * the server.
        * @param stmShout - a StmShout object or null if an error occurred
        * @param stmError - a StmError object or null if no error
        */
       void handleCreateShoutResponse(StmShout stmShout, StmError stmError);
    
       /**
        * handleDeleteShoutResponse
        * Called after the response is received from a call to the delete
        * shout server endpoint
        * @param stmError - a StmError object or null if no error
        */
       void handleDeleteShoutResponse(StmError stmError);
    }

### StmRecorderActivity
The StmRecorderActivity is a native Android [Activity](http://developer.android.com/reference/android/app/Activity.html) used to quickly and easily enable recording in a client app.  When the StmRecorderActivity is launched, the following will occur:

1. The StmRecorderActivity is displayed to the user
2. Audio recording immediately begins
3. The user is presented with two options
    * A “Done” button; when pressed, this will stop the recording and send the recorded audio to the server.  The StmRecorderActivity will then be closed.
    * A “Cancel” icon; when pressed, the recording will be stopped and the StmRecorderActivity will be closed.

Launching the StmRecorderActivity

    Intent intent = new Intent(this, StmRecorderActivity.class);
    startActivityForResult(intent, 1); 
    // The 1 is an arbitrary identifier used to identify
    // the activity during the result callback

Handling the result callback of StmRecorderActivity

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



