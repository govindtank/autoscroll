# AutoScroll ProGuard Rules
# Keep Accessibility Service
-keep class com.droidtank.autoscroll.AutoScrollAccessibilityService { *; }

# Keep Boot Receiver
-keep class com.droidtank.autoscroll.BootReceiver { *; }

# Keep Room entities
-keep class com.droidtank.autoscroll.database.** { *; }

# Keep Compose
-dontwarn androidx.compose.**

# Keep Serialization
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
