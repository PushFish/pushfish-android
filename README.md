The Pushfish Android Client [![License](http://img.shields.io/badge/license-BSD-blue.svg?style=flat)](/LICENSE)
==========================
This is the pushfish android client. It's using MQTT to communicate with the server. The client is licensed 
under [BSD 2 clause][1] just like the rest of the project.

This is based on ![Pushjet](https://github.com/Pushjet/Pushjet-Android)

![Download the latest apk](https://gitlab.com/PushFish/PushFish-Android/-/jobs/artifacts/master/raw/app/build/outputs/apk/app-debug.apk?job=build)
## Permissions explained
The [permissions][4] used by PushFish might seem a bit broad but they all have a reason:

 - Read phone status and identity: This is needed [to generate the device UUID ][5] that authenticates the device with the server.
 - Take pictures and videos: This is needed to make sure we can scan QR codes to register new services.
 - Control flashlight/vibration and prevent phone from sleeping: This makes sure we can receive notifications.

## Screenshots
![Pushfish push listing material][6] ![Pushfish push listing][2] ![Subscriptions][3]


[1]: https://tldrlegal.com/license/bsd-2-clause-license-%28freebsd%29
[2]: http://pushjet.io/images/android/screenshot_1.png?1432482002
[3]: http://pushjet.io/images/android/screenshot_2.png?1432482002
[6]: http://pushjet.io/images/android/screenshot_3.png?1432482002
[4]: /app/src/main/AndroidManifest.xml
[5]: https://github.com/Pushjet/Pushjet-Android/blob/master/app/src/main/java/io/Pushjet/api/PushjetApi/DeviceUuidFactory.java
