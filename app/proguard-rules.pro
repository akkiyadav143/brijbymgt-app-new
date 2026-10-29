# Room Database Entities & DAOs
-keep class com.brijbymgt.data.entity.** { *; }
-keep class com.brijbymgt.data.dao.** { *; }
-keep class com.brijbymgt.data.database.AppDatabase { *; }

# Firebase & Play Services
-keepattributes *Annotation*, Signature, InnerClasses
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Data Models
-keep class com.brijbymgt.data.model.** { *; }
