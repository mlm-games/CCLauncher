# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

-keepattributes Signature
-keepattributes *Annotation*
-dontobfuscate

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep class app.cclauncher.data.settings.AppSettings { *; }
-keepclassmembers class app.cclauncher.data.settings.AppSettings { *; }
-keep @app.cclauncher.data.settings.Setting class * { *; }
-keepclasseswithmembers class * {
    @app.cclauncher.data.settings.Setting <fields>;
}

-keepattributes *Annotation*,EnclosingMethod,Signature,KotlinMetadata

-keep class kotlin.Metadata { *; }

-keep class kotlin.reflect.** { *; }


-keepattributes RuntimeVisibleAnnotations
-keepclassmembers class app.cclauncher.data.settings.AppSettings {
    @app.cclauncher.data.settings.Setting *;
}

# Keep all Setting annotations
-keep @interface app.cclauncher.data.settings.Setting
-keepattributes *Annotation*

# Keep all enum classes used in annotations
-keepclassmembers enum app.cclauncher.data.settings.SettingCategory { *; }
-keepclassmembers enum app.cclauncher.data.settings.SettingType { *; }

# Keep all reflection metadata
-keepattributes Signature, InnerClasses
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlin.jvm.internal.** { *; }

-keepclassmembers class app.cclauncher.data.settings.AppSettings {
    <fields>;
    <methods>;
}

-keep class app.cclauncher.data.settings.SettingsManager { *; }

-keep class app.cclauncher.ui.screens.SettingsScreenKt { *; }
