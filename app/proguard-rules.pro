# Room Database & Models
-keep class com.brijbymgt.data.entity.** { *; }
-keep class com.brijbymgt.data.model.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# Firebase & Play Services
-keepattributes *Annotation*, Signature, InnerClasses
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
