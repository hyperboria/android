cjdns for Android
=================

[![Build Status](https://travis-ci.org/hyperboria/android.svg?branch=master)](https://travis-ci.org/hyperboria/android) [![tip for next commit](https://tip4commit.com/projects/1049.svg)](https://tip4commit.com/github/hyperboria/android)

Meshnet is an Android app that lets you connect to cjdns networks, without the need for a rooted phone—thanks to the android.net.VpnService API introduced with Android 4.0 (Ice Cream Sandwich). Older versions still require root and routes through a TUN device.

**Current state:** App starts and stops cjdroute and sets up a VPN to access Hyperboria for non-rooted devices. Two public peers are added by default, so you should be able to browse websites and reach services on Hyperboria just by starting the cjdns service with the toggle. All other menus are only populated with mock data at the moment and you cannot add additional peers.

Installation
------------

1. Install the [Android SDK](http://developer.android.com/sdk/index.html)
 
1. Clone this application repo

1. Optionally download the [Android NDK](https://developer.android.com/ndk/index.html) version r11c and set `ANDROID_NDK_HOME` to its path

1. Build application and install on device by running `./install_debug`. This will also clone the [cjdns repo](https://github.com/cjdelisle/cjdns) and build the native artifacts for Android. If `ANDROID_NDK_HOME` is not set or the version is incorrect, the Android NDK will also be downloaded.

**Note:** The cjdns repo is currently cloned from a fork until patches are merged into **cjdelisle/cjdns**.

Contact
-------

- Find out how to help by visiting our [issue tracker](https://github.com/hyperboria/android/issues)
- [Matrix](https://matrix.org) chat room for this project: [#android:tomesh.net](https://chat.tomesh.net/#/room/#android:tomesh.net)

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
