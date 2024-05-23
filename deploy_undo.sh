#!/usr/bin/env bash

set -e

# It might not be running if it crashed, so suppress output.
adb shell 'killall mixpad_gui > /dev/null 2>&1'

adb shell 'rm -rf /oem/app/flutter-gui'
adb push flutter-gui /oem/app/

echo "Backup restored! GUI should restart within 15 seconds."
