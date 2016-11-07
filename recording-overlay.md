---
layout: home
---

# Recording

The StmRecorderActivity class provides client apps a way to capture audio and send it to the Shout to Me platform. It is
implemented as a native [Android Activity](http://developer.android.com/reference/android/app/Activity.html) to allow
developers to quickly and easily enable recording in a client app.  When the StmRecorderActivity is launched, the
following will occur:

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

## Launching the StmRecorderActivity
Launching the StmRecorderActivity is done using standard Android Activity functionality.  You can pass in certain extras to provide additional data.

1. StmRecorderActivity.MAX_RECORDING_TIME_IN_SECONDS - This extra is required. It defines the maximum amount of time the
  user has to record their shout. Please note that the Shout to Me system currently does not support recording times over 1 minute.
2. StmRecorderActivity.TAGS - A comma separated list of tags that will flow through to the Broadcaster Application.
2. StmRecorderActivity.TOPIC - A topic that will flow through to the Broadcaster Application.

```java
Intent intent = new Intent(this, StmRecorderActivity.class);
intent.putExtra(StmRecorderActivity.MAX_RECORDING_TIME_IN_SECONDS, maxRecordingLengthSeconds);  // Required
intent.putExtra(StmRecorderActivity.TAGS, tags);                                                // Optional
intent.putExtra(StmRecorderActivity.TOPIC, topic);                                              // Optional
startActivityForResult(intent, 1);
```

## Handling the Activity result of StmRecorderActivity
The StmRecorderActivity uses the standard Android Activity callback to indicate whether the Activity was closed OK, or whether
the action was cancelled.  In addition, the StmRecorderActivity will provide data to confirm whether or not the
recording process completed successfully or not.  The example below shows how to detect the
StmRecorderActivity.RECORD_AUDIO_PERMISSION_DENIED failure.

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

## Shout
The callback from StmRecorderActivity returns a Shout object.  The Shout object represents the recording a user created
and its metadata in the Shout to Me system.  This object may be used to allow the user the opportunity to review the
audio or delete the shout.

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