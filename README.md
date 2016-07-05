cjdns for Android
=================

[![Build Status](https://travis-ci.org/hyperboria/android.svg?branch=master)](https://travis-ci.org/hyperboria/android) [![tip for next commit](https://tip4commit.com/projects/1049.svg)](https://tip4commit.com/github/hyperboria/android)

Meshnet is an Android app that lets you connect to cjdns networks, without the need for a rooted phone—thanks to the android.net.VpnService API introduced with Android 4.0 (Ice Cream Sandwich). Older versions still require root and routes through a TUN device.

**Current state:** App starts and stops cjdroute for rooted devices with Android 4.4 (KitKat) and below. A public peer is added by default, so you should be able to browse websites and reach services on Hyperboria just by starting the cjdns service with the toggle. All other menus are only populated with mock data at the moment and you cannot add additional peers.

Installation
------------
 
1. Clone this application repo
2. Build application and install on device:

    ```
    ./gradlew installDebug
    ```

3. (Optional) Install the [Android SDK](http://developer.android.com/sdk/index.html) and open project using it

Contact
-------

- Find out how to help by visiting our [issue tracker](https://github.com/hyperboria/android/issues)
- IRC channel for this project: **#android on [HypeIRC](irc://irc.hypeirc.net)**

    ```
    fc13:6176:aaca:8c7f:9f55:924f:26b3:4b14
    fcbf:7bbc:32e4:0716:bd00:e936:c927:fc14
    ```

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
