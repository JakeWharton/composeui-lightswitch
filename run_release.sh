#!/usr/bin/env bash

set -e

./gradlew -q linkReleaseExecutableLinuxArm64
adb push build/bin/linuxArm64/releaseExecutable/composeui-lightswitch.kexe /userdata
adb shell /userdata/composeui-lightswitch.kexe
#adb shell 'killall mixpad_gui && /userdata/composeui-lightswitch.kexe'