# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Jan\Documents\Android SDK/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Database
-keepattributes *Annotation*
-keep class com.activeandroid.** { *; }
-keep class me.kuehle.carreport.db.** { *; }

# Reports
-keep class me.kuehle.chartlib.ChartView
-keep class me.kuehle.carreport.data.report.* { *; }

# Preferences
-keep class me.kuehle.carreport.gui.Preferences*
-keep class me.kuehle.carreport.gui.HelpActivity$*

# Dropbox
-dontwarn org.apache.**

# Joda Time
-keep class org.joda.time.DateTimeZone.Provider
-keep class org.joda.time.tz.UTCProvider
-dontwarn org.joda.convert.**

# Misc
-dontwarn org.bouncycastle.**

# Google Play Services
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
