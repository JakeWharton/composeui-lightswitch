#!/usr/bin/env bash

set -e

./gradlew -q linkReleaseExecutableLinuxArm64

local_md5="$(md5sum build/bin/linuxArm64/releaseExecutable/composeui-lightswitch.kexe | cut -d" " -f1 | tr -d '[:space:]')"
remote_md5="$(adb shell 'md5sum /userdata/composeui-lightswitch.kexe | cut -d" " -f1' | tr -d '[:space:]')"

echo "Local: '$local_md5'"
echo "Remote: '$remote_md5'"

if [[ "$local_md5" != "$remote_md5" ]]; then
	adb push build/bin/linuxArm64/releaseExecutable/composeui-lightswitch.kexe /userdata
fi

# It might not be running if it crashed, so suppress output.
adb shell 'killall mixpad_gui > /dev/null 2>&1'

adb shell '/userdata/composeui-lightswitch.kexe'
