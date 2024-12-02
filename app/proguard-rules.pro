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

# Keep Apache POI and related classes
-keep class org.apache.poi.** { *; }
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.hssf.** { *; }

# Keep java.awt classes (if necessary for POI functionality)
-keep class java.awt.** { *; }

# Keep all XML stream reader and SAX related classes
-keep class javax.xml.stream.** { *; }
-keep class net.sf.saxon.** { *; }

# Ignore warnings for libraries that are not needed in Android
-dontwarn org.apache.batik.**
-dontwarn org.osgi.framework.**

