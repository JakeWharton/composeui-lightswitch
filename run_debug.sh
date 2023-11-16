#!/usr/bin/env bash

set -e

./gradlew -q linkDebugExecutableLinuxArm64
adb push build/bin/linuxArm64/debugExecutable/composeui-lightswitch.kexe /oem/app/flutter-gui
adb shell /oem/app/flutter-gui/composeui-lightswitch.kexe
