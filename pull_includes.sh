#!/usr/bin/env bash

set -e

rm -rf usr/downloads usr/include
mkdir usr/downloads usr/include

pushd usr/downloads > /dev/null

echo "Downloading weston…"

weston_version="8.0.0"
curl -L --no-progress-meter "https://wayland.freedesktop.org/releases/weston-$weston_version.tar.xz" > "weston-$weston_version.tar.xz"
tar -xzf "weston-$weston_version.tar.xz"
rm "weston-$weston_version.tar.xz"
rsync -a \
  --include '*/' \
  --include '*.h' \
  --exclude '*' \
  "weston-$weston_version/include/libweston" ../include
rm -r "weston-$weston_version"

echo "Downloading pixman…"

pixman_version="0.34.0"
curl -L --no-progress-meter "https://cairographics.org/releases/pixman-$pixman_version.tar.gz" > "pixman-$pixman_version.tar.gz"
tar -xf "pixman-$pixman_version.tar.gz"
rm "pixman-$pixman_version.tar.gz"
cp "pixman-$pixman_version/pixman/pixman.h" ../include
cp "pixman-$pixman_version/pixman/pixman-version.h" ../include
rm -r "pixman-$pixman_version"

echo "Downloading xkbcommon…"

xkbcommon_version="0.7.0"
curl -L --no-progress-meter "http://xkbcommon.org/download/libxkbcommon-$xkbcommon_version.tar.xz" > "libxkbcommon-$xkbcommon_version.tar.xz"
tar -xzf "libxkbcommon-$xkbcommon_version.tar.xz"
rm "libxkbcommon-$xkbcommon_version.tar.xz"
cp -r "libxkbcommon-$xkbcommon_version/xkbcommon" ../include
rm -r "libxkbcommon-$xkbcommon_version"

popd > /dev/null
rmdir usr/downloads

echo "Downloading wayland…"

wayland_image=$(podman build -qf usr/Dockerfile.wayland)
wayland_container=$(podman run -d $wayland_image)
podman cp $wayland_container:/usr/local/include usr
podman rm $wayland_container > /dev/null

echo "Done"
