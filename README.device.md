# Orvibo MixPad D1

Details from the OEM binary that might be useful.

## GUI

Flutter app compiled to a native app with https://github.com/sony/flutter-embedded-linux.

### Startup sequence

 * `FlutterViewController`

   https://github.com/sony/flutter-embedded-linux/blob/bd5e4b67aad84d42c42aa7222fde33274baa08bf/src/client_wrapper/flutter_view_controller.cc#L12

 * `FlutterDesktopViewControllerCreate`

   https://github.com/sony/flutter-embedded-linux/blob/bd5e4b67aad84d42c42aa7222fde33274baa08bf/src/flutter/shell/platform/linux_embedded/flutter_elinux.cc#L80-L81

 * `ELinuxWindowDrm`

   https://github.com/sony/flutter-embedded-linux/blob/bd5e4b67aad84d42c42aa7222fde33274baa08bf/src/flutter/shell/platform/linux_embedded/window/elinux_window_drm.h#L37

 * `NativeWindowDrmGbm`

   https://github.com/sony/flutter-embedded-linux/blob/bd5e4b67aad84d42c42aa7222fde33274baa08bf/src/flutter/shell/platform/linux_embedded/window/native_window_drm_gbm.cc#L22

 * `NativeWindowDrm`

   https://github.com/sony/flutter-embedded-linux/blob/bd5e4b67aad84d42c42aa7222fde33274baa08bf/src/flutter/shell/platform/linux_embedded/window/native_window_drm.cc#L18


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

* `libinput` version 1.8.2

  The `.so` suffix is `10.13.0` which is seemingly nonsensical. Thankfully `strings` shows that each
  version embeds a link to its documentation whose URL contains the version of 1.8.2.

* `libdrm` version unknown, 2.4.something

  The 2.4.x series has been going on for 15 years so that's a safe bet, but what patch version?
  I started bisecting the public symbols, but it wasn't yielding good results. I guessed .87 for now.
  In .98 they added a `drmIsMaster` API which is not available, so it's definitely older than that.

The headers for these versions are checked in under `device/include/`.
These were obtained by running the `pull_includes.sh` script (which you should not need to do).

The shared libraries for dynamic linking are checked in under `device/lib/`.
These were obtained by running the `pull_libs.sh` script (which you should not need to do).
