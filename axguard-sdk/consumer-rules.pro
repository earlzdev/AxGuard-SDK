# RegisterNatives (cpp/jni_onload.cpp) binds each method by runtime class name +
# method name/signature. If R8 renames them, registration no-ops, the call throws
# UnsatisfiedLinkError, and the check degrades to Unavailable. Keep both.
-keepclasseswithmembernames,includedescriptorclasses class com.axguard.sdk.internal.checks.AppIntegrityCheck {
    native <methods>;
}
-keepclasseswithmembernames,includedescriptorclasses class com.axguard.sdk.internal.checks.DebuggerCheck {
    native <methods>;
}
-keepclasseswithmembernames,includedescriptorclasses class com.axguard.sdk.internal.checks.HookCheck {
    native <methods>;
}
-keepclasseswithmembernames,includedescriptorclasses class com.axguard.sdk.internal.checks.RootCheck {
    native <methods>;
}
-keepclasseswithmembernames,includedescriptorclasses class com.axguard.sdk.internal.checks.SelinuxCheck {
    native <methods>;
}
-keepclasseswithmembernames,includedescriptorclasses class com.axguard.sdk.internal.utils.NativeProps {
    native <methods>;
}
