# Compose UI for… a light switch!

Specifically, the **Orvibo MixPad D1**

 * OEM website: https://www.orvibo.com/en/product/mix_dimmer.html
 * Amazon: https://www.amazon.com/dp/B0B6PDKC64
 * More info: https://community.home-assistant.io/t/500842


## Disable automatic updates

By default, the device ships with automatic updates enabled. This is very problematic,
as the OEM has since closed the `adb` backdoor.

The very first thing you should do when powering on the device for the first time
is to disable automatic updates:

- Swipe left to the list of settings buttons
- Scroll down to "System Update" and click on it
- Click the "Auto update" checkbox to disable it

If the "About" screen within settings shows version 4.0.0 you should be good.

If your device updated past version 4.0.0, there are steps (which are not for the faint of heart)
to attach a keyboard to re-obtain root. See https://community.home-assistant.io/t/500842/19.


## Native libraries

The device ships with the following native libraries:

* `libweston-8` version unknown, probably 8.0.0

  Weston is not ABI stable, so they change the `.so` name with each release. Since this is "weston-8"
  we safely can assume it's 8.0.0. There were no other 8.x.x releases.

* `libpixman` version 0.34.0

* `libxkbcommon` version unknown, probably 0.7.0

  Weston 8 requires at least xkbcommon 0.3.0. Running `strings` on the `.so` shows a "0.7.0" string.
  New APIs in 0.8.0 are not present via `strings`. 0.7.2 introduced new keys which are not present in
  `strings`. So it's either 0.7.1 or 0.7.0, but we have no way of differentiating without testing for
  0.7.1's bugfixes which I'm too lazy to do.

* `libwayland-client` version unknown, probably 1.18.0

  Weston 8 requires at least wayland 1.17.0. In 1.18.0 Wayland added a `wl_global_remove` function
  which appears in `strings`.

The headers for these versions are checked in under `usr/include/`.
These were obtained by running the `pull_includes.sh` script (which you should not need to do).

The shared libraries for dynamic linking are checked in under `usr/lib/`.
These were obtained by running the `pull_libs.sh` script (which you should not need to do).


## Connection

The device is running an ADB server as root. Locate its IP address after connecting it to
your Wi-Fi network, and connect via ADB client on your computer.

```
$ adb connect 192.168.1.123
connected to 192.168.1.123:5555
```

Confirm the connection in the device list.

```
$ adb devices
List of devices attached
192.168.1.123:5555	device
```

Now you can use regular ADB commands to interact with the device.
`adb shell` will give you a Bash shell as root.
`adb pull` and `adb push` will allow file transfer.

However, in order to send over files, you must first remount the `/oem` filesystem as read-write.
In an `adb shell` to the device, run: `mount -o remount,rw /oem`. Later, if you want, you can revert
it to read-only: `mount -o remount,ro /oem`.


## Development

Builds can be created on a Linux or Mac machine running on X86 or ARM.

Once connected to the device (see above), there are three handy scripts for testing:

 * `push_debug.sh` builds a debug executable, pushes it to the device, kills the built-in GUI,
   backs up the built-in GUI, replaces the GUI with our executable, and tails the logs.
 * `push_release.sh` builds a release executable, pushes it to the device, kills the built-in GUI,
   backs up the built-in GUI, replaces the GUI with our executable, and tails the log.
 * `push_undo.sh` kills our GUI and restores the backup of the built-in GUI.

Additionally, the `push_debug.sh` and `push_release.sh` scripts will create an additional "original"
copy of the built-in GUI that is never again touched should you need to restore manually.


## License

    Copyright 2023 Jake Wharton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
