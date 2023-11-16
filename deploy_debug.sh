#!/usr/bin/env bash

set -e

# The adb server on the device does not set exit codes properly, so output from target.
check_for_original="$(adb shell 'ls /oem/app/flutter-gui/mixpad_gui.original > /dev/null 2>&1; echo $?' | tr -d '[:space:]')"

if [[ "$check_for_original" -ne 0 ]]; then
	echo "No mixpad_gui original found!"
	echo
	echo "Copying the original mixpad_gui binary"
	echo "  from: /oem/app/flutter-gui/mixpad_gui"
	echo "    to: /oem/app/flutter-gui/mixpad_gui.original"
	echo
	echo "None of these scripts will touch this file ever again."
	echo "Use it to manually restore the original if necessary."
	echo
	adb shell cp -n /oem/app/flutter-gui/mixpad_gui /oem/app/flutter-gui/mixpad_gui.original
fi

./gradlew -q linkDebugExecutableLinuxArm64
adb push build/bin/linuxArm64/debugExecutable/composeui-lightswitch.kexe /oem/app/flutter-gui
adb shell killall mixpad_gui
adb shell mv /oem/app/flutter-gui/mixpad_gui /oem/app/flutter-gui/mixpad_gui.backup
adb shell mv /oem/app/flutter-gui/composeui-lightswitch.kexe /oem/app/flutter-gui/mixpad_gui

echo "Done! GUI should restart within 15 seconds. Tailing logs…"
echo
adb shell 'cat /dev/null > /tmp/mixpad_gui.log && tail -f /tmp/mixpad_gui.log'
