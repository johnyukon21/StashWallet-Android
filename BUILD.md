# Build StashWallet-Android

## Build requirements

You need to have the following Android developer tools installed:

- "Android SDK Platform-tools" version 29.0.3 recommended
- "Android SDK Tools" version 26.1.1
- "Android SDK Build-tools" version 29.0.3

The above tools can be installed from the Android SDK manager.

## Clone the repo

`git clone https://github.com/johnyukon21/StashWallet-Android.git`

`cd StashWallet-Android`

## How to build

#### Use the released native libraries (recommended):

The pre-built native libraries are the same versions used in the builds
published by GreenAddress. Gradle/Android Studio will automatically use the latest.

To download and update manually native libraries to the latest supported run:

`cd app && ./fetch_gdk_binaries.sh && cd ..`


#### Build the Android app

Run:

`./gradlew build`

This will build both release and debug builds.

You can speed up builds by limiting the tasks which run. Use:

`./gradlew --tasks`

To see a list of available tasks.
