#!/usr/bin/env bash

set -e

rm -rf usr/lib
mkdir usr/lib
adb pull \
	/usr/lib/libGLESv2.so \
	/usr/lib/libEGL.so \
	/usr/lib/libdrm.so \
	/usr/lib/libmali.so \
	usr/lib/
