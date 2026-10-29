# Keep all app classes
-keep class com.brijbymgt.** { *; }
-keepclassmembers class com.brijbymgt.** { *; }

# Room Database Keep Rules
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# Firebase & Play Services
-keepattributes *Annotation*, Signature, InnerClasses
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
