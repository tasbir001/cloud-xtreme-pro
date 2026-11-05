# Cloud Xtreme Pro - Proguard & R8 Rules

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Virtualization models & JSON serialization
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class com.cloudxtreme.pro.core.model.** { *; }

# Keep reflection hooks for VirtualContext & VirtualPackageManager
-keep class com.cloudxtreme.pro.virtualization.** { *; }

# Keep Firebase and Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
