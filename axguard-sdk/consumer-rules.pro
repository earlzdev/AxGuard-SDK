# JNI registration resolves native methods by the fully-qualified Java name
# (Java_com_axguard_sdk_internal_checks_<Class>_<method>). Keep every class and
# native member name that the SDK binds to a native symbol; otherwise a
# consumer's R8 config that drops the AGP default keeps will rename the class,
# JNI resolution silently fails with UnsatisfiedLinkError, and the check
# degrades to Unavailable at runtime.
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
