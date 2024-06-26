#!/usr/bin/env bash

set -e

rm -rf device/lib
mkdir device/lib
adb pull \
	/usr/lib/libGLESv2.so \
	/usr/lib/libEGL.so \
	/usr/lib/libdrm.so \
	/usr/lib/libinput.so \
	/usr/lib/libmali.so \
	/usr/lib/libfontconfig.so \
	device/lib/
