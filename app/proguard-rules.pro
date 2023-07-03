# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Jan\Documents\Android SDK/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html


#####################################
# Car Report
#####################################

# Reports
-keep class org.juanro.autumandu.data.report.* { *; }

# Preferences
-keep class org.juanro.autumandu.gui.Preferences*
-keep class org.juanro.autumandu.gui.HelpActivity$*

# Sync Providers
-keep class org.juanro.autumandu.util.sync.provider.*

#####################################
# Libraries
#####################################

# Dropbox
-dontwarn com.dropbox.core.http.OkHttpRequestor
-dontwarn com.dropbox.core.http.OkHttpRequestor$*
-dontwarn javax.servlet.**

# Misc
-dontwarn org.slf4j.*

# OkHttp
-keepattributes Signature,*Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.*

# Google API Client, Google API Drive Service
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
-keep class com.google.api.client.** { *;}
-keep interface com.google.api.client.** { *;}
-keep class com.google.api.services.drive.** { *; }
-keep class * extends com.google.api.client.json.GenericJson { *; }
-dontwarn com.google.**

# RecyclerView
-keep class androidx.appcompat.widget.RebindReportingHolder { *; }
