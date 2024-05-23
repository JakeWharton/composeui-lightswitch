#!/usr/bin/env bash

set -e

./gradlew -q linkDebugExecutableLinuxArm64
adb shell 'killall mixpad_gui'
adb shell 'rm -rf /oem/app/flutter-gui'
adb shell 'mkdir /oem/app/flutter-gui'
adb push build/bin/linuxArm64/debugExecutable/composeui-lightswitch.kexe /oem/app/flutter-gui
adb shell 'mv /oem/app/flutter-gui/composeui-lightswitch.kexe /oem/app/flutter-gui/mixpad_gui'

echo "Done! GUI should restart within 15 seconds. Tailing logs…"
echo
adb shell 'cat /dev/null > /tmp/mixpad_gui.log && tail -f /tmp/mixpad_gui.log'
