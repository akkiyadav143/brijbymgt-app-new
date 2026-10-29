# Keep all app classes (Fragments, Activities, ViewModels, Adapters, Entities, DAOs, Models)
-keep class com.brijbymgt.** { *; }
-keepclassmembers class com.brijbymgt.** { *; }

# Firebase & Play Services
-keepattributes *Annotation*, Signature, InnerClasses
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Room Database
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
