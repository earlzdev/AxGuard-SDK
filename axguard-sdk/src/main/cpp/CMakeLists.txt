cmake_minimum_required(VERSION 3.22.1)
project(axguard_integrity)

# C++17 for the OBFS constexpr string template in obfs.h.
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Keystream seed shared with Obfs.kt. Fallback for standalone CMake builds only;
# the Gradle build passes AxGuardBuild.OBFS_KEY. Keep in sync.
if(NOT DEFINED AXGUARD_OBFS_KEY)
    set(AXGUARD_OBFS_KEY 0x6D7A3F15)
endif()

add_library(axguard-integrity SHARED
    jni_onload.cpp
    integrity_check.cpp
    common/syscall_io.cpp
    common/task_iter.cpp
    checks/native_props.cpp
    checks/debugger_check.cpp
    checks/hook_check.cpp
    checks/root_check.cpp
    checks/selinux_check.cpp
)

target_compile_definitions(axguard-integrity PRIVATE
    AXGUARD_OBFS_KEY=${AXGUARD_OBFS_KEY}
)

# Hardening flags.
# -fvisibility=hidden + -fvisibility-inlines-hidden: only JNIEXPORT entry points
#   are exported. Probes are bound via RegisterNatives (jni_onload.cpp), so the
#   .so exports only JNI_OnLoad — no Java_… symbols for an attacker to locate.
# -fstack-protector-strong: canary-based buffer overflow detection.
# -Wall/-Wextra: turn on warnings, address them incrementally. -Werror is
#   deliberately NOT enabled yet; warnings should be triaged first.
target_compile_options(axguard-integrity PRIVATE
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -fstack-protector-strong
    -Wall
    -Wextra
    # Release-only: _FORTIFY_SOURCE=2 requires -O1+, and dead-code sectioning
    # only helps once we ask the linker to garbage-collect.
    $<$<CONFIG:Release>:-D_FORTIFY_SOURCE=2>
    $<$<CONFIG:Release>:-ffunction-sections>
    $<$<CONFIG:Release>:-fdata-sections>
)

# -Wl,-z,relro + -Wl,-z,now: full RELRO — GOT is read-only after linking,
#   closes the "overwrite GOT to hook libc" attack class.
# -Wl,--as-needed: drop unused DT_NEEDED entries.
# --gc-sections + --strip-all (release-only): drop dead sections and strip
#   symbol/debug info. With visibility hiding, the release .so exports only JNI_OnLoad.
target_link_options(axguard-integrity PRIVATE
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,--as-needed
    $<$<CONFIG:Release>:-Wl,--gc-sections>
    $<$<CONFIG:Release>:-Wl,--strip-all>
)

# AXGUARD_DEBUG gates every __android_log_print in native. Release builds
# never emit logcat output from this library.
target_compile_definitions(axguard-integrity PRIVATE
    $<$<CONFIG:Debug>:AXGUARD_DEBUG>
)

find_library(log-lib log)
target_link_libraries(axguard-integrity ${log-lib})
