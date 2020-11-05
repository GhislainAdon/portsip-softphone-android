# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\huacai\AppData\Local\Android\android-studio\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
-keep class androidx.appcompat.widget.** { *; }
-keep class androidx.appcompat.widget.SearchView { *; }
-keep class com.portgo.view.IconTipsMenu { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.gms.** { *; }
-keep class org.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class javax.** { *; }
-keep class com.googlecode.** { *; }

-keep public interface com.android.vending.billing.IInAppBillingService
-keep public interface com.android.vending.licensing.ILicensingService

-dontwarn com.googlecode.**
-dontwarn okio.**
-dontwarn android.support.**
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.gms.**
#-dontwarn com.google.firebase.messaging.**

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.portsip.*{
    public private protected  *;
}
-keep class org.webrtc.**{
    *;
}
