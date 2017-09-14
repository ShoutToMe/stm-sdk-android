---
layout: home
---

# Create a Shout

There are two ways to create a shout using the Shout to Me SDK:

1. [Using the recording overlay activity](#record-overlay)
2. [Uploading a media file](#upload-file)

## <a name="record-overlay"></a>Recording Overlay
<img src="https://s3-us-west-2.amazonaws.com/sdk-public-images/sample-app-4.png" style="float:right" />
The StmRecorderActivity class provides client apps a way to capture audio and send it to the Shout to Me platform. It is
implemented as a native [Android Activity](http://developer.android.com/reference/android/app/Activity.html) to allow
developers to quickly and easily enable recording in a client app.  When the StmRecorderActivity is launched, the
following will occur:

1. The StmRecorderActivity is displayed to the user
2. Audio recording immediately begins
3. The user is presented with two options
    * A “Done” button; when pressed, this will stop the recording and send the recorded audio to the server.  The StmRecorderActivity will then be closed.
    * A “Cancel” icon; when pressed, the recording will be stopped and the StmRecorderActivity will be closed.

<div style="clear:right">&nbsp;</div>
There is also the ability to provide an optional callback if you would like to receive a Shout object following the creation of the shout.

```java
stmService.setShoutCreationCallback(new Callback<Shout>() {
    @Override
    public void onSuccess(StmResponse<Shout> stmResponse) {
        Shout shout = stmResponse.get();
    }

    @Override
    public void onFailure(StmError stmError) {
        // An error occurred during shout creation
    }
});
```

### Launching the StmRecorderActivity
Launching the StmRecorderActivity is done using standard Android Activity functionality.  You can pass in certain extras
to provide additional data.

The first piece of data, the maximum recording time in seconds, is required and will set
a hard limit for how long the user can recording their shout.  A countdown will also be shown on the overlay to let the
user know how long they have to record.

The next two pieces of data relate to the content of the shout.  Topic and Tags can be set to help a broadcaster
filter, sort and manage the data in the broadcaster application.  A Topic refers to the overall subject matter of the shout.
Tags refers to one or more keywords that reflect key points within the shout.  Topic and Tags are optional and if set,
they are assigned as metadata to the shout.  If they are not set, the Shout to Me system uses IBM Watson to make its
best guess of the shout content.

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

### Handling the Activity result of StmRecorderActivity
The StmRecorderActivity uses the standard Android Activity callback to indicate whether the Activity was closed OK, or whether
the action was cancelled.  In addition, the StmRecorderActivity will provide data to confirm whether or not the
recording process completed successfully.  The example below shows how to detect the
StmRecorderActivity.RECORD_AUDIO_PERMISSION_DENIED failure.

```java
protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (requestCode == MY_REQUEST_CODE) {
        if (resultCode == RESULT_OK){
            String recordingResult = data.getStringExtra(StmRecorderActivity.ACTIVITY_RESULT);

            if (recordingResult.equals(StmService.FAILURE)) {
                String failureReasonCode = data.getStringExtra(StmRecorderActivity.ACTIVITY_REASON);
                if (failureReasonCode.equals(StmRecorderActivity.RECORD_AUDIO_PERMISSION_DENIED)) {
                    // User has not granted the RECORD_AUDIO permission
                }
            }
       }
       if (resultCode == RESULT_CANCELED) {
           // Activity was cancelled.
       }
    }
}
```

## <a name="upload-file"></a>Uploading a Media File
The Shout to Me SDK allows for file uploads to create new shouts. To accomplish this, you pass the SDK a CreateShoutRequest
 object which contains a java <a href="https://developer.android.com/reference/java/io/File.html" target="_blank">File</a> object.
You can pass an optional callback if you wish to receive notification when the create shout process has completed.

```java
CreateShoutRequest createShoutRequest = new CreateShoutRequest();
createShoutRequest.setFile(file);
List<String> tags = new ArrayList<>();
tags.add("tag 1");
tags.add("tag 2");
createShoutRequest.setTags(tags);
createShoutRequest.setTopic("My topic");
createShoutRequest.setDescription("This is a description of the shout");

stmService.createShout(createShoutRequest, new Callback<Shout>() {
    @Override
    public void onSuccess(final StmResponse<Shout> stmResponse) {
        Shout shout = stmResponse.get();
        Log.d(TAG, shout.getId());
    }

    @Override
    public void onFailure(final StmError stmError) {
        // Could not create shout.
    }
});
```

For media files, there is a helper function in `CreateShoutRequest` that you can use to pass in a `Uri` instead of a `File`.
It requires a context in order to search the `MediaStore`.

```java
createShoutRequest.setFileFromMediaUri(mediaFileUri, context);
```

## Shout
The callback from StmRecorderActivity returns a Shout object.  The Shout object represents the recording a user created
and its metadata in the Shout to Me system.  This object may be used to allow the user the opportunity to review the
audio or delete the shout.

To delete a shout, you call the `delete()` method of a Shout object. You can provide a callback to inspect the status
and know when the delete process has completed.

```java
shout.delete(new Callback<String>() {
    @Override
    public void onSuccess(StmResponse<String> stmResponse) {
        if (stmResponse.get().equals("success")) {
            // Deletion of shout was successfull
        }
    }

    @Override
    public void onFailure(StmError stmError) {
        // An error occurred deleting shout
    }
});
```