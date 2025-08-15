
/**
 * PED: Este objeto demuestra el uso de 'object' como singleton en Kotlin.
 * Los objects son lazy por defecto y thread-safe, ideales para configuración.
 */
object BuildConfig {
    // PED: const val crea una constante de tiempo de compilación
    const val kotlinVersion = "1.9.22"
    const val jvmTarget = "17"
    
    // PED: val normal se evalúa en runtime (vs const val en compile-time)
    val buildTimestamp: Long = System.currentTimeMillis()
    
    // PED: Propiedades computadas usando custom getters
    val isCI: Boolean
        get() = System.getenv("CI") == "true"
    
    val buildEnvironment: String
        get() = if (isCI) "CI" else "local"
}
