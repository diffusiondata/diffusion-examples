# Android example showing basic use of the Diffusion API

This is an Android application that demonstrates how
to create, update, and subscribe to a Diffusion topic.


## Building the example

You can build the example from the command line using gradle, but we recommend importing
the example folder into Android Studio as a project. The following has been tested with
Android Studio Quail 1 | 2026.1.1 Patch 2

1. Copy the project folder to a location of your choice.

2. Start Android Studio. Use the `File/New/Import Project...` menu option,
   and select the project folder, e.g. `~/AndroidStudioProjects/Diffusion/pubsub` directory.

3. Run the `File/Sync Project with Gradle Files` menu option.

4. (Optionally) update the Diffusion Server credentials within `app/src/main/java/com/diffusiondata/android/example/PubSubExample.java`. The default config assumes a Diffusion Server instance running at localhost.

5. Run the `Build/Assemble Project` menu option.


## Running the example

1. Start your Diffusion server (either locally, or in Diffusion Cloud).

2. Create and/or start a virtual device within Android Studio. The demo project is configured for a minimum Android API version of 24 (Android 7 - Nougat), or higher. A Pixel 8 device is a good, current candidate.

3. Within the Android Studio project explorer, right/context click on the `PubSubExample.java` and select `Run` to deploy and run the example on the virtual device.

4. The example app will create, publish and subscribe to a Diffusion topic at the path 'counter'. Updates to the 'counter' topic will be displayed on the virtual device UI. 

```declarative
Connected to Diffusion server.
Subscribed to topic path: counter
Created topic 'counter' with result: EXISTS
Received value on counter: 0
Received value on counter: 1
Received value on counter: 2
...
```

5. You can also view equivalent Android log output - within Android Studio select `View/Tool Windows/Logcat` 
