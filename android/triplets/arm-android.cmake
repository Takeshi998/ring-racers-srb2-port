# arm-android with NEON forced ON.
# vcpkg's community triplet disables NEON (-DANDROID_ARM_NEON=OFF),
# which NDK r23+ rejects ("Disabling Neon is no longer supported").
# Used via VCPKG_OVERLAY_TRIPLETS=<repo>/android/triplets (arm32 builds only).
set(VCPKG_TARGET_ARCHITECTURE arm)
set(VCPKG_CRT_LINKAGE dynamic)
set(VCPKG_LIBRARY_LINKAGE static)
set(VCPKG_CMAKE_SYSTEM_NAME Android)
set(VCPKG_CMAKE_SYSTEM_VERSION 28)
set(VCPKG_MAKE_BUILD_TRIPLET "--host=armv7a-linux-androideabi")
set(VCPKG_CMAKE_CONFIGURE_OPTIONS -DANDROID_ABI=armeabi-v7a -DANDROID_ARM_NEON=ON)
