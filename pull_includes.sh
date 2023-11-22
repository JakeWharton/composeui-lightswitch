#!/usr/bin/env bash

set -e

rm -rf usr/downloads usr/include
mkdir usr/downloads usr/include

pushd usr/downloads > /dev/null

echo "Downloading GBM / GLES2 via Mesa…"

mesa_version="18.0.0"
curl -L --no-progress-meter "https://archive.mesa3d.org/mesa-$mesa_version.tar.gz" > "mesa-$mesa_version.tar.xz"
tar -xf "mesa-$mesa_version.tar.xz"
rm "mesa-$mesa_version.tar.xz"
cp "mesa-$mesa_version/src/gbm/main/gbm.h" ../include
cp -r "mesa-$mesa_version/include/GLES2" ../include
rm -r "mesa-$mesa_version"

echo "Downloading EGL…"

mkdir ../include/EGL ../include/KHR
curl -L --no-progress-meter https://registry.khronos.org/EGL/api/EGL/egl.h > ../include/EGL/egl.h
curl -L --no-progress-meter https://registry.khronos.org/EGL/api/EGL/eglplatform.h > ../include/EGL/eglplatform.h
curl -L --no-progress-meter https://registry.khronos.org/EGL/api/EGL/eglext.h > ../include/EGL/eglext.h
curl -L --no-progress-meter https://registry.khronos.org/EGL/api/KHR/khrplatform.h > ../include/KHR/khrplatform.h

popd > /dev/null
rmdir usr/downloads

echo "Downloading libdrm…"

libdrm_image=$(podman build -qf usr/Dockerfile.libdrm)
libdrm_container=$(podman run -d $libdrm_image)
podman cp $libdrm_container:/usr/local/include usr
podman rm $libdrm_container > /dev/null

echo "Done"
