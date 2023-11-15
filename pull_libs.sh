#!/usr/bin/env bash

set -e

rm -rf usr/lib
mkdir usr/lib
adb pull \
	/usr/lib/libwayland-client.so \
	usr/lib/
