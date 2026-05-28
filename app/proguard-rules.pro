# Preserve line numbers and source file names for easier debugging of release crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep GSON DTOs
-keepclassmembers class com.nary.gym.data.remote.dto.** { *; }
-keep class com.nary.gym.data.remote.dto.** { *; }

# Keep Room Entities and related classes
-keep class com.nary.gym.data.room_db.** { *; }
-keepclassmembers class com.nary.gym.data.room_db.** { *; }

# Keep Hilt/Dagger generated classes (usually handled by rules but safe to have)
-keep class **_HiltModules* { *; }

# Keep Retrofit Service interfaces
-keep interface com.nary.gym.data.remote.ApiService { *; }

# Keep Enum types used in models
-keepclassmembers enum com.nary.gym.core.WorkOutType { *; }
