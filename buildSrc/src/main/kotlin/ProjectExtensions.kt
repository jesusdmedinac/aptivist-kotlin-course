
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.*

/**
 * PED: Extension functions permiten añadir métodos a tipos existentes sin herencia.
 * Estas funciones demuestran cómo extender la funcionalidad de Project y TaskContainer.
 */

// PED: Extension function en Project que demuestra higher-order functions
fun Project.configureKotlinOptions(configure: org.jetbrains.kotlin.gradle.tasks.KotlinCompile.() -> Unit) {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach(configure)
}

// PED: Extension function que usa lambdas para configuración condicional
fun Project.onlyIf(condition: Boolean, action: Project.() -> Unit) {
    if (condition) {
        action()
    }
}

// PED: Extension property (computed property) para Project
val Project.isRootProject: Boolean
    get() = this == rootProject

// PED: Extension function que demuestra el uso de reified generics
inline fun <reified T : Task> TaskContainer.configureAll(noinline configuration: T.() -> Unit) {
    withType<T>().configureEach(configuration)
}

// PED: Extension function para crear tasks de manera más fluida
fun Project.createReportTask(taskName: String, reportContent: () -> String) {
    tasks.register(taskName) {
        group = "reporting"
        description = "Auto-generated report task: $taskName"
        
        doLast {
            val content = reportContent()
            println("📋 REPORTE: $taskName")
            println("=" * 50)
            println(content)
        }
    }
}

// PED: Extension function que demuestra scope functions (apply, let, run, etc.)
fun Project.setupProjectDefaults() {
    // PED: apply ejecuta un bloque y retorna el receptor (this)
    apply {
        // PED: with permite ejecutar código en el contexto del objeto
        with(extensions) {
            // Configuraciones por defecto para el proyecto
        }
    }
    
    // PED: also es útil para side effects manteniendo el objeto original
    version.also { v ->
        logger.info("Configurando proyecto ${name} versión $v")
    }
    
    // PED: takeIf retorna el objeto si cumple la condición, null si no
    version.toString().takeIf { it.contains("SNAPSHOT") }?.let {
        logger.warn("⚠️  Versión SNAPSHOT detectada: $it")
    }
}

// PED: Extension function que usa when (equivalente a switch pero más poderoso)
fun Project.getEnvironmentConfig(): Map<String, String> {
    return when (BuildConfig.buildEnvironment) {
        "CI" -> mapOf(
            "logging.level" to "INFO",
            "performance.monitoring" to "true"
        )
        "local" -> mapOf(
            "logging.level" to "DEBUG", 
            "performance.monitoring" to "false"
        )
        else -> mapOf(
            "logging.level" to "WARN",
            "performance.monitoring" to "false"
        )
    }
}

// PED: Operator overloading - permite usar * como operador personalizado
operator fun String.times(count: Int): String = repeat(count)
