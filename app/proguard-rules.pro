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
# Keep rules for Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep rules for Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep rules for Apache POI
-keep class org.apache.poi.** { *; }

# Keep rules for coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep view binding
-keep class * extends androidx.viewbinding.ViewBinding { *; }

# Keep data classes
-keepclassmembers class * {
    public <init>();
}