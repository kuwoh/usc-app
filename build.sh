echo "
set(VCPKG_TARGET_ARCHITECTURE arm64)
set(VCPKG_CRT_LINKAGE static)
set(VCPKG_LIBRARY_LINKAGE static)
set(VCPKG_CMAKE_SYSTEM_NAME Android)
set(VCPKG_LIBRARY_LINKAGE static)
" > ./vcpkg/triplets/community/arm64-android.cmake
//line-default 3-dynamic 4-dynamic
./gradlew assembleDebug
