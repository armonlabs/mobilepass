# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


-printmapping out.map
-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,EnclosingMethod
# Preserve all annotations.
-keepattributes *Annotation*
# Preserve all public classes, and their public and protected fields and
# methods.
-keep public class * {
    public protected *;
}

# Preserve all .class method names.
-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# Preserve all native method names and the names of their classes.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve the special static methods that are required in all enumeration
# classes.
-keepclassmembers class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
# You can comment this out if your library doesn't use serialization.
# If your code contains serializable classes that have to be backward
# compatible, please refer to the manual.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# -keep class org.spongycastle.*.* { *; }
-keep class org.spongycastle.* {*;}
-keepclassmembers class org.spongycastle.*

# Google Play Services Location koruma
-keep class kotlin.Metadata { *; }
-keep class com.google.android.gms.internal.location.** { *; }

# Huawei HMS kütüphaneleri koruma
-keep class com.huawei.hms.** { *; }
-dontwarn com.huawei.hms.**
-dontwarn com.huawei.android.telephony.**

# Apache Compress ve Brotli koruma
-keep class org.apache.commons.compress.** { *; }
-keep class org.brotli.** { *; }

# Bouncy Castle koruma
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-dontwarn java.lang.management.**
-dontwarn javax.naming.**

# app/build/outputs/mapping/release/usage.txt
# Your library may contain more items that need to be preserved;
# typically classes that are dynamically created using Class.forName:
# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface