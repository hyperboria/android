cjdns for Android
=================

[![Build Status](https://travis-ci.org/hyperboria/android.svg?branch=master)](https://travis-ci.org/hyperboria/android) [![tip for next commit](https://tip4commit.com/projects/1049.svg)](https://tip4commit.com/github/BerlinMeshnet/cjdns-android)

Meshnet is an Android app that lets you connect to cjdns networks, without the need for a rooted phone -- thanks to the android.net.VpnService API introduced with Android 4.0 (Ice Cream Sandwich). Older versions still require root.

*Current state:*
- app starts cjdroute and stops it properly
- app crashes on real devices because of a threading issue
- shows cjdroute's pid on the emulator
- shows cjdroute's output in adb logcat -- with weird buffer issues though
- doesn't support ETHInterface and TUNInterface yet

TODO
----

- [x] Hello world
- [x] Build cjdroute
- [x] Start cjdroute from app
- [x] Properly kill cjdroute (via Admin API)
- [x] Extract Admin API config from cjdroute.conf
- [ ] Run as background service
- [ ] Display cjdroute logs
- [ ] Pass VpnService descriptor to cjdroute
- [ ] Display peer stats
- [ ] Allow peering info exchange via qr codes
- [ ] Release on fdroid (finn's repo: http://h.finn.io/fdroid/)
- [ ] Store cjdroute.conf on SD card
- [ ] Store cjdroute.conf somewhere safe
- [ ] Add IPv6 to contacts
- [ ] Use root for Android < 4.0
- [ ] tbc

Installation
------------

1. Android SDK and NDK
2. x86 or armeabi toolchain
  - `android-ndk/build/tools/make-standalone-toolchain.sh --platform=android-21 --toolchain=x86-4.9 --install-dir=i686-linux-android/ --system=linux-x86_64`
  - `android-ndk/build/tools/make-standalone-toolchain.sh --platform=android-21 --toolchain=arm-linux-androideabi-4.9 --install-dir=arm-linux-androideabi/ --system=linux-x86_64`
3. build cjdns
  - `git clone https://github.com/lgierth/cjdns.git && cd cjdns && git checkout android-wip`
  - for the x86 emulator:
    - `PATH=$PATH:/path/to/i686-linux-android/bin CROSS_COMPILE=i686-linux-android- TARGET_ARCH=x64 Seccomp_NO=1 ./cross-do`
    - `cp cjdroute /path/to/cjdns-android/src/main/assets/x86/`
  - for armeabi devices:
    - `PATH=$PATH:/path/to/arm-linux-androideabi/bin CROSS_COMPILE=arm-linux-androideabi- TARGET_ARCH=arm Seccomp_NO=1 ./cross-do`
    - `cp cjdroute /path/to/cjdns-android/src/main/assets/armeabi-v7a/`
  - for android-5.0 on x86 (and possibly other archs), you need to cross-do with `Seccomp_NO=1`
  - `cjdroute --genconf > /path/to/cjdns-android/src/main/assets/cjdroute.conf`
4. Create emulator
  - `android create avd -n cjdns -t 1 --abi x86 --force`
5. Start emulator
  - `emulator -avd cjdns -qemu -m 256 -enable-kvm`

Running
-------

6. build cjdns-android (and clear app cache)
  - `./gradlew installDebug`
7. tail app log
  - `adb logcat | grep cjdns`
8. tail cjdroute crashdumps (x86)
  - `adb logcat | ndk-trace -sym src/main/assets/x86/`
8. tail cjdroute crashdumps (armeabi)
  - `adb logcat | ndk-trace -sym src/main/assets/armeabi-v7a/`

Contact
-------

- Issue tracker: [github.com/BerlinMeshnet/cjdns-android/issues](https://github.com/BerlinMeshnet/cjdns-android/issues)
- IRC: #android on [HypeIRC](https://wiki.projectmeshnet.org/HypeIRC)
- Development updates: [www.lars.meshnet.berlin](http://www.lars.meshnet.berlin)

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
