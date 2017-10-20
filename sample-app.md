---
layout: home
---

# Sample App

After completing the steps in [Setup](setup), creating a sample app to use the Shout to Me functionality is simple.
This page will walk you through that process.

## Create an Android Application
Run through the Android Studio’s Create New Project wizard.

![Create new project step 1](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-1.png)

Set the minimum Android SDK to  **API 15: Android 4.0.3 (IceCreamSandwich)** on the "Target Android Devices" screen.

![Create new project step 2](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-2.png)

Choose "Empty Activity" on the "Add an Activity to Mobile" screen.

![Create new project step 3](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-3.png)

Leave the Activity and Layout names set to the default.

![Create new project step 4](https://s3-us-west-2.amazonaws.com/sdk-public-images/as-new-project-4.png)

## Set Up the Shout to Me SDK

Follow the steps in [Setup](setup) to incorporate the Shout to Me Android SDK into this new project.

## Use the Shout to Me Android SDK

You are now able to begin coding with the Shout to Me Android SDK.  Start by adding the following code into `MainActivity`.

### MainActivity.java

Modify the `MainActivity` class so it looks like below.

```java
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
            stmService.setShoutCreationCallback(new Callback<Shout>() {
                @Override
                public void onSuccess(StmResponse<Shout> stmResponse) {
                    newlyCreatedShout = stmResponse.get();
                    showDeleteButton();
                }

                @Override
                public void onFailure(StmError stmError) {
                    // An error occurred during shout creation
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

                if (result.equals(StmService.FAILURE)) {
                    String failureReasonCode = data.getStringExtra(StmRecorderActivity.ACTIVITY_REASON);
                    if (failureReasonCode.equals(StmRecorderActivity.RECORD_AUDIO_PERMISSION_DENIED)) {

                        // User has not granted access to record audio.  Ask the user for permission now.
                        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO }, 0);
                    }
                }
            }
            if (resultCode == RESULT_CANCELED) {
                // Recording was cancelled
            }
        }
    }

    public void setUserHandle(View view) {
        if (isStmServiceBound) {
            final EditText editText = (EditText)findViewById(R.id.editTextUserHandle);
            String newHandle = editText.getText().toString();

            UpdateUserRequest updateUserRequest = new UpdateUserRequest();
            updateUserRequest.setHandle(newHandle);

            stmService.updateUser(updateUserRequest, new Callback<User>() {
                @Override
                public void onSuccess(final StmResponse<User> stmResponse) {
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
            newlyCreatedShout.delete(new Callback<String>() {
                @Override
                public void onSuccess(StmResponse<String> stmResponse) {
                    if (stmResponse.get().equals(StmService.SUCCESS)) {
                        hideDeleteButton();
                    }
                }

                @Override
                public void onFailure(StmError stmError) {
                    // An error occurred deleting shout
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
            StmService.StmBinder binder = (StmService.StmBinder) service;
            stmService = binder.getService();
            isStmServiceBound = true;

            // You can also set the channel programmatically if you have access to more than one channel
            // stmService.setChannelId("[channel ID]");

            // Get a reference to the UI text box
            final EditText handleEditText = (EditText) findViewById(R.id.editTextUserHandle);

            stmService.getUser(new Callback<User>() {
                @Override
                public void onSuccess(final StmResponse<User> stmResponse) {
                    handleEditText.setText(stmResponse.get().getHandle());
                }

                @Override
                public void onFailure(final StmError stmError) {
                    // Could not retrieve Shout to Me user.
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

### activity_main.xml

Next modify the `activity_main.xml` file to look like the following.

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

## Run the App

After the code has been modified, click **Run -> Run 'app'** to build and start the app.  You should see the initial Activity with the **RECORD A SHOUT** button enabled.  When you press that button, it will launch the Shout to Me recording overlay as seen in the following images and immediately begin recording. Pressing the Done button on the overlay will transmit the recorded audio to the Shout to Me service for processing.

You can also set the user's handle.  The user's handle is seen with the shout metadata in the Shout to Me Broadcaster Application.

![Sample app](https://s3-us-west-2.amazonaws.com/sdk-public-images/sample-app-3.png)
![Shout to me overlay](https://s3-us-west-2.amazonaws.com/sdk-public-images/sample-app-4.png)

