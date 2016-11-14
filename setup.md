---
layout: home
---

# Setting up the Shout to Me Android SDK

The following describes how to set up your Android project to use the Shout to Me Android SDK.

## Prerequisites
* A Shout to Me client access token
* A Shout to Me channel ID
* [Android Studio](https://developer.android.com/studio/intro/index.html) and all its dependencies

## Import the Shout to Me SDK into your Project
1. [Download the most recent version of the Shout to Me SDK](https://github.com/ShoutToMe/stm-sdk-android/releases)
2. In Android Studio, navigate to **File > New > New Module**
3. Select **Import .JAR/.AAR Package** then click **Next**
4. Enter the location of the **shout-to-me-sdk-release.aar** file that you downloaded in Step 1. and then click **Finish**

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
            android:value="@string/client_token" />
        <meta-data
            android:name="me.shoutto.sdk.CHANNEL_ID"
            android:value="@string/channel_id" />
    </service>
    ...
</application>
```

Be sure to place the appropriate client token and channel ID values into your strings.xml file.

```xml
<string name="client_token">[Your client token]</string>
<string name="channel_id">[Your channel ID]</string>
```

## Dependencies
The Shout to Me Android SDK has the following dependencies:

### Google Play Services
The Shout to Me SDK Android SDK uses [Google Play Services](https://developers.google.com/android/guides/overview) for
the following functionality.

* Determining the user's location
* Registering the user for Google Cloud Messaging
* Managing geotargeted notifications with geofences

Add the following to your gradle file in the dependencies section if you have not already integrated to Google Play Services.

```gradle
dependencies {
    ...
    compile 'com.google.android.gms:play-services:9.6.+'
    ...
}
```

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
The Shout to Me SDK utilizes Volley to optimize asynchronous communication with the API service. For more information about volley, see the [Android Volley documentation](https://developer.android.com/training/volley/index.html).  Add the following to your gradle file in the dependencies section to incorporate Volley.

```gradle
dependencies {
    ...
    compile 'com.android.volley:volley:1.0.+'
    ...
}
```

### Amazon Web Services
The Shout to Me SDK utilizes AWS to send push notifications to mobile devices. For more information about the Amazon Android AWS SDK, see the [Amazon mobile SDK documentation](https://aws.amazon.com/mobile/sdk/).   Add the following to your gradle file in the dependencies section to incorporate the Amazon AWS SDK.

```gradle
dependencies {
    ...
    compile 'com.amazonaws:aws-android-sdk-core:2.2.+'
    compile 'com.amazonaws:aws-android-sdk-sns:2.2.+'
    ...
}
```

## Permissions

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

### Record Audio
Being that Shout to Me is an audio-based platform, this permission is considered required. Launching the recording
overlay without the permission will result in a failure response indicating that the record audio permission is denied.

```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
    // User has not granted access to record audio.  Ask the user for permission now.
    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO }, 0);
}
```

### Location
Use of location functionality is optional in the Shout to Me platform. However, if location permission is enabled, the coordinates (lat/lon) of the person shouting are included with the Shout creation request and broadcasters will be able to see the location of the user.

```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION }, 0);
}
```

If the user rejects the permission request, there is a way to explain the rationale for the request. This is beyond the scope of this document. More information can be found in the [Android - Request Permissions documentation](https://developer.android.com/training/permissions/requesting.html#perm-request).