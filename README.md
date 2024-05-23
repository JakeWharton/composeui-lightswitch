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

However, in order to replace the built-in GUI, you must first remount the `/oem` filesystem as read-write.
In an `adb shell` to the device, run: `mount -o remount,rw /oem`. Later, if you want, you can revert
it to read-only: `mount -o remount,ro /oem`.


## Development

Builds can be created on a Linux or Mac machine running on X86 or ARM.

Once connected to the device (see above), there are scripts for quickly testing:

 * `run_debug.sh` build a debug executable, pushes it to the device, kills the built-in GUI,
   and starts our executable (the built-in GUI will come back within 15 seconds).
 * `release.sh` build a release executable, pushes it to the device, kills the built-in GUI,
   and starts our executable (the built-in GUI will come back within 15 seconds).

Or for longer deployments:

 * `deploy_debug.sh` builds a debug executable, pushes it to the device, kills the built-in GUI,
   replaces the GUI with our executable, and tails the logs.
 * `deploy_release.sh` builds a release executable, pushes it to the device, kills the built-in GUI,
   replaces the GUI with our executable, and tails the log.
 * `deploy_undo.sh` kills our GUI and restores the backup of the built-in GUI from this repo.
   Consider doing your own `adb pull /oem/app/flutter-gui` backup as well.

You must have remounted the `/oem` partition as read-write (as detailed above) before deploying.


## Thanks

I am not a low-level graphics programmer.
These snippets were instrumental in the setup of this project.

- https://gist.github.com/Miouyouyou/89e9fe56a2c59bce7d4a18a858f389ef
- https://github.com/dvdhrm/docs/blob/master/drm-howto/modeset-vsync.c


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
