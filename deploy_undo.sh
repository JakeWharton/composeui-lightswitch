#!/usr/bin/env bash

set -e

# It might not be running if it crashed, so suppress output.
adb shell 'killall mixpad_gui > /dev/null 2>&1'

adb shell 'cp /oem/app/flutter-gui/mixpad_gui.backup /oem/app/flutter-gui/mixpad_gui && rm /oem/app/flutter-gui/mixpad_gui.backup'

echo "Backup restored! GUI should restart within 15 seconds."
