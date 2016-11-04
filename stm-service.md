---
layout: home
---

# StmService

[StmService](http://localhost:4000/stm-sdk-android/reference/me/shoutto/sdk/StmService.html) is the primary class in the
Shout to Me SDK for Android. It inherits from the Android
[Service](https://developer.android.com/reference/android/app/Service.html) class.  When you start or bind to the
service, it reads the access token and channel ID values from AndroidManifest.xml (as described in [Setup](setup)) and
initializes itself with the Shout To Me service.