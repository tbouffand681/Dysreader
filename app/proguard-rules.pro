# ── iText7 ──────────────────────────────────────────────────────────────────
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# ── Apache POI / OOXML ───────────────────────────────────────────────────────
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**

# XMLBeans (dépendance POI)
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**

# ── ML Kit ───────────────────────────────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.** { *; }

# ── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── Navigation Component ─────────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }

# ── ViewBinding / DataBinding ────────────────────────────────────────────────
-keep class * implements androidx.viewbinding.ViewBinding {
    public static * inflate(android.view.LayoutInflater);
    public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static * bind(android.view.View);
}

# ── Modèles de données ───────────────────────────────────────────────────────
-keep class com.dyslexiread.models.** { *; }

# ── FileProvider ─────────────────────────────────────────────────────────────
-keep class androidx.core.content.FileProvider { *; }
