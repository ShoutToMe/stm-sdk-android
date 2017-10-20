---
layout: home
---

# Setting up the Shout to Me Android SDK

The following describes how to set up your Android project to use the Shout to Me Android SDK.

## Prerequisites
* A Shout to Me client access token
* A Shout to Me channel ID
* A Shout to Me notification app ID if using the Shout to Me notification system
* [Android Studio](https://developer.android.com/studio/intro/index.html) and all its dependencies

## Add the Shout to Me SDK into your Project

### JCenter
The Shout to Me Android SDK is available in the JCenter repository.  By default, Android Studio includes a reference to
JCenter. If you do not currently have a reference to JCenter, add it to your app's `build.gradle` file.

```gradle
repositories {
    jcenter()
}
```

### Add the Shout to Me SDK dependency
Add the following to your app's `build.gradle` file.

```gradle
dependencies {
    compile 'me.shoutto.sdk:shout-to-me-sdk:1.0.+'
}
```

## Client Access Token and Channel ID
Developers will need to get a client access token and a channel ID from Shout to Me in order to use this SDK.  A client
access token is used to authorize the client app in HTTP calls.  The channel ID represents a Shout to Me channel which
is linked to the broadcaster/podcaster's account.  You will need to [contact Shout to Me](http://www.shoutto.me/contact) in order to get the client access
  token and channel ID. After you receive the client access token and channel ID from Shout to Me, add the
  following &lt;service&gt; element to the &lt;application&gt; element in your AndroidManifest.xml:

```xml
<application>
    ...
    <service
        android:name="me.shoutto.sdk.StmService"
        android:exported="false">
        <meta-data
            android:name="me.shoutto.sdk.CLIENT_TOKEN"
            android:value="[Your client token]" />
        <meta-data
            android:name="me.shoutto.sdk.CHANNEL_ID"
            android:value="[Your channel ID]" />
    </service>
    ...
</application>
```

## Dependencies
If you included the Shout to Me SDK by adding it to your gradle file, the dependencies will be automatically included in your build.
Here is a list of dependencies that the Shout to Me SDK uses.

### Google Play Services
The Shout to Me SDK Android SDK uses [Google Play Services](https://developers.google.com/android/guides/overview) for
the following functionality.

* Determining the user's location when they send a message
* Keeping track of the user's location in order to send geo-targeted notifications
* Registering the user for Google Cloud Messaging


If Google Play Services is not installed on a user's phone, certain functionality may not work correctly.
Most modern Android phones come bundled with Google Play Services, however it is
recommended to check for the availability of it to support older phones.
Google Play Services provides a convenient mechanism to prompt the user to install or upgrade their Google Play
Services if applicable.  The following code provides an example of the call to make to prompt the user to install
Google Play Services if they do not already have it.  This code is normally placed in the first Activity so that the
user sees it after installing your app and the app can immediately be registered to receive notifications.

```java
GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
int val = googleApiAvailability.isGooglePlayServicesAvailable(this);
if (val != ConnectionResult.SUCCESS) {
    Dialog gpsErrorDialog = googleApiAvailability.getErrorDialog(this, val, 2);
    gpsErrorDialog.show();
}
```

### Volley
The Shout to Me SDK utilizes Volley to optimize asynchronous communication with the API service. For more information about volley, see the [Android Volley documentation](https://developer.android.com/training/volley/index.html).


### Amazon Web Services
The Shout to Me SDK utilizes AWS to send push notifications to mobile devices and transmit media files to cloud storage. For more information about the Amazon Android AWS SDK, see the [Amazon mobile SDK documentation](https://aws.amazon.com/mobile/sdk/).


## Permissions

The Shout to Me SDK requests the following permissions in the manifest:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
```

Beginning with API level 23 (6.0), [Android requires that certain permissions be requested at run time](https://developer.android.com/training/permissions/requesting.html). The Shout to Me SDK uses three permissions that fall into this category:

1. Mic/record audio (Optional)
2. Read external storage (Optional)
2. Location (Optional)

### Record Audio
If you plan to use Shout to Me's recording overlay to create shouts, then the record audio permission is required.
Launching the recording overlay without the permission will result in a failure response indicating that the record
audio permission is denied.

```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
    // User has not granted access to record audio.  Ask the user for permission now.
    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO }, 0);
}
```

### Read External Storage
If you plan to upload media files to create new shouts, then the read external storage permission is required.  Without
permission to read external storage, you may not have access to certain media files.

```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
    // User has not granted access to read external storage.  Ask the user for permission now.
    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 0);
}
```

### Location
Use of location functionality is optional in the Shout to Me platform. However, if location permission is enabled, shouts
will contain location information and the user will be able to receive geo-targeted notifications.

```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION }, 0);
}
```

If the user rejects the permission request, there is a way to explain the rationale for the request. This is beyond the scope of this document. More information can be found in the [Android - Request Permissions documentation](https://developer.android.com/training/permissions/requesting.html#perm-request).