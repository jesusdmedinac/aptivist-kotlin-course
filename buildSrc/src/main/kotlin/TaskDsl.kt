
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

/**
 * PED: Este archivo demuestra cómo crear un DSL (Domain Specific Language) 
 * personalizado usando Kotlin. Los DSL son especialmente útiles para configuración.
 */

// PED: Clase que representa una configuración usando el patrón Builder
class CustomTaskConfig {
    var taskName: String = ""
    var taskGroup: String = "custom"
    var taskDescription: String = ""
    var executeWhen: () -> Boolean = { true }
    var actions: MutableList<() -> Unit> = mutableListOf()
    
    // PED: Función infix permite sintaxis más natural: action doThis { ... }
    infix fun action(block: () -> Unit) {
        actions.add(block)
    }
    
    // PED: Función que demuestra el uso de lambdas con receiver
    fun whenCondition(condition: () -> Boolean) {
        executeWhen = condition
    }
}

// PED: Extension function que crea un DSL para configurar tasks personalizadas
fun Project.customTask(name: String, configure: CustomTaskConfig.() -> Unit): TaskProvider<Task> {
    // PED: apply es un scope function que ejecuta el bloque en el contexto del objeto
    val config = CustomTaskConfig().apply {
        taskName = name
        configure() // PED: Ejecuta la lambda de configuración
    }
    
    return tasks.register(config.taskName) {
        group = config.taskGroup
        description = config.taskDescription
        
        // PED: onlyIf toma un lambda que determina si ejecutar la task
        onlyIf { config.executeWhen() }
        
        // PED: doLast ejecuta las acciones al final de la task
        doLast {
            config.actions.forEach { action ->
                action() // PED: Ejecuta cada lambda de acción
            }
        }
    }
}

// PED: DSL para configuración de dependencias personalizada
class DependencyConfiguration {
    private val implementations = mutableListOf<String>()
    private val testImplementations = mutableListOf<String>()
    
    // PED: Función infix para sintaxis más fluida
    infix fun impl(dependency: String) {
        implementations.add(dependency)
    }
    
    infix fun testImpl(dependency: String) {
        testImplementations.add(dependency)
    }
    
    // PED: Function que aplica las configuraciones al proyecto
    fun applyTo(project: Project) {
        project.dependencies.apply {
            implementations.forEach { dep ->
                add("implementation", dep)
            }
            testImplementations.forEach { dep ->
                add("testImplementation", dep)
            }
        }
    }
}

// PED: Extension function que demuestra DSL para dependencias
fun Project.customDependencies(configure: DependencyConfiguration.() -> Unit) {
    val config = DependencyConfiguration()
    config.configure()
    config.applyTo(this)
}

// PED: Sealed class que demuestra pattern matching avanzado con when
sealed class BuildPhase(val description: String) {
    object Compile : BuildPhase("Compilación de código fuente")
    object Test : BuildPhase("Ejecución de pruebas")
    object Package : BuildPhase("Empaquetado de artefactos")
    object Deploy : BuildPhase("Despliegue a producción")
}

// PED: Extension function que usa sealed classes para configuración por fases
fun Project.configureForPhase(phase: BuildPhase, action: Project.() -> Unit) {
    // PED: when con sealed classes es exhaustivo - no necesita else
    val shouldExecute = when (phase) {
        is BuildPhase.Compile -> true
        is BuildPhase.Test -> !project.hasProperty("skip.tests")
        is BuildPhase.Package -> project.hasProperty("enable.packaging")
        is BuildPhase.Deploy -> BuildConfig.buildEnvironment == "CI"
    }
    
    if (shouldExecute) {
        logger.info("🏗️  Configurando fase: ${phase.description}")
        action()
    }
}
