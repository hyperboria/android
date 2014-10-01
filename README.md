cjdns for Android
=================

Meshnet is an Android app that lets you connect to cjdns networks, without the need for a rooted phone -- thanks to the android.net.VpnService API introduced with Android 4.0 (Ice Cream Sandwich). Older versions still require root.

*Current state:*
  - the app starts and shows the output of cjdroute
  - doesn't kill cjdroute yet, ouch
  - doesn't support ETHInterface and TUNInterface yet

TODO
----

- [x] Hello world
- [x] Build cjdroute
- [x] Start cjdroute from app
- [x] Display cjdroute logs
- [ ] Properly kill cjdroute (via Admin API)
- [ ] Pass VpnService descriptor to cjdroute
- [ ] Display peer stats
- [ ] Run as background service
- [ ] Release on fdroid
- [ ] Store cjdroute.conf on SD card
- [ ] Store cjdroute.conf somewhere safe
- [ ] Add IPv6 to contacts
- [ ] Use root for Android < 4.0
- [ ] tbc

Installation
------------

1. Android SDK and NDK
2. x86 toolchain
3. build cjdns
  - set `PREFIX` to `/data/data/berlin.meshnet.cjdns/cache/cjdns_pipe_` around line 340 in util/events/libuv/Pipe.c
  - `PATH=$PATH:/path/to/i686-linux-android/bin CROSS_COMPILE=i686-linux-android- TARGET_ARCH=x64 ./cross-do`
  - `cp cjdroute cjdroute.conf /path/to/cjdns-android/src/main/assets/`
4. Create emulator
  - `android create avd -n cjdns -t 1 --abi x86 --force`
5. Start emulator
  - `emulator -avd cjdns -qemu -m 512 -enable-kvm`

Running
-------

6. build cjdns-android (and clear app cache)
  - `./gradlew uninstallAll installDebug`
7. tail app log
  - `adb logcat | grep cjdns`
8. tail cjdroute crashdumps
  - `adb logcat | ndk-trace -sym src/main/assets/`

Contact
-------

- Issue tracker: [github.com/berlinmeshnet/cjdns-android/issues](https://github.com/berlinmeshnet/cjdns-android/issues)
- IRC: #android on [HypeIRC](https://wiki.projectmeshnet.org/HypeIRC)
- Development updates: [www.lars.meshnet.berlin](http://www.lars.meshnet.berlin)
