cjdns for Android
=================

[![Build Status](https://travis-ci.org/hyperboria/android.svg?branch=master)](https://travis-ci.org/hyperboria/android) [![tip for next commit](https://tip4commit.com/projects/1049.svg)](https://tip4commit.com/github/hyperboria/android)

Meshnet is an Android app that lets you connect to cjdns networks, without the need for a rooted phone -- thanks to the android.net.VpnService API introduced with Android 4.0 (Ice Cream Sandwich). Older versions still require root.

*Current state: App starts and stops cjdroute for rooted devices with Android 4.4 (KitKat) and below. A public peer is added by default, so you should be able browse websites and reach services on Hyperboria just by starting the cjdns service with the toggle. All other menus are only populated with mock data at the moment and you cannot add additonal peers.* 

Installation
------------

1. Install the [Android SDK](http://developer.android.com/sdk/index.html) 
2. Clone this application repo
3. Clone [cjdns](https://github.com/hyperboria/cjdns) and run `./android_do`
4. Copy built artifacts from `./build_android/` into the application repo such that corresponding `cjdroute` binaries are located as such:

    ```
    ./src/main/assets/armeabi-v7a/cjdroute
    ./src/main/assets/x86/cjdroute
    ```

5. Build application and install:

    ```
    ./gradlew installDebug
    ```

Contact
-------

- Issue tracker: [github.com/hyperboria/android/issues](https://github.com/hyperboria/android/issues)
- IRC: #android on [HypeIRC](https://wiki.projectmeshnet.org/HypeIRC)

Notes
-----

- Re: Text Relocations warning
  - https://android-review.googlesource.com/#/c/91485/2/linker/linker.cpp
- Re: SecComp
  - https://code.google.com/p/chromium/issues/detail?id=308763
  - https://wiki.mozilla.org/Security/Sandbox/Seccomp
  - https://github.com/sigma-1/raw-android-patches
- Setup
  - https://gist.github.com/venkateshshukla/9736261
