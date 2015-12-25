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
-keep class me.kuehle.carreport.data.report.* { *; }

# Preferences
-keep class me.kuehle.carreport.gui.Preferences*
-keep class me.kuehle.carreport.gui.HelpActivity$*

# Sync Providers
-keep class me.kuehle.carreport.util.sync.provider.*

#####################################
# Libraries
#####################################

# Dropbox
-dontwarn com.dropbox.core.http.OkHttpRequestor
-dontwarn com.dropbox.core.http.OkHttpRequestor$*
-dontwarn com.dropbox.core.DbxStandardSessionStore
-keepnames class com.fasterxml.jackson.** { *; }
-keeppackagenames com.dropbox.core.http

# Jackrabbit
-dontwarn org.apache.**

# Misc
-dontwarn org.slf4j.*

# Google API Client, Google API Drive Service
-keep class com.google.** { *;}
-keep interface com.google.** { *;}
-dontwarn com.google.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue
-keepattributes *Annotation*,Signature
-keep class * extends com.google.api.client.json.GenericJson { *; }
-keep class com.google.api.services.drive.** { *; }
