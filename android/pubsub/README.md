# Android example showing basic use of the Diffusion API

This is an Android application that demonstrates how
to create, update, and subscribe to a Diffusion topic.



## Building the example


You can build the example from the command line using gradle, but we recommend importing
the example into Android Studio as a project. The following has been tested with Android
Studio 3.2.1.

1. Copy the `diffusion-android.jar` to the `app/libs` directory.

2. Start Android Studio. Use the
`File/New/Import Project...` menu option, and select the `examples\android\pubsub` directory.

3. Use the `Build/Make Project` menu option.

## Running the example


1. Start the Diffusion server.

2. Use Android Studio (`Run/Run "app"`) to deploy the example to the Android emulator. Create a new virtual device if one doesn't already exist, using API level 19 or higher.

3. The example doesn't use Android UI features. To see the output, use Android Studio to examine the log.
