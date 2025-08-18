
package com.aptivist.kotlin.plugins.commands

import com.aptivist.kotlin.Config
import kotlinx.coroutines.delay

/**
 * 🛠️ SYSTEM COMMANDS - COMANDOS BÁSICOS DEL SISTEMA (Phase 2.2)
 * 
 * Esta clase proporciona comandos básicos del sistema como help, version, status,
 * demostrando implementación concreta de comandos y DSL builders.
 * 
 * CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Object singleton para comandos del sistema
 * • Extension functions para crear comandos con DSL
 * • String templates y multiline strings
 * • System properties y environment access
 * • Suspend functions con delay para simulación
 * • Higher-order functions para command building
 */

/**
 * PED: Object singleton que contiene todos los comandos del sistema.
 * Los objects son thread-safe por defecto y se inicializan lazy.
 */
object SystemCommands {
    
    /**
     * PED: Comando help usando buildCommand DSL.
     */
    val helpCommand = buildCommand {
        metadata(
            name = "help",
            description = "Muestra ayuda general o específica de un comando",
            category = CommandCategory.SYSTEM,
            usage = "help [comando]",
            examples = listOf(
                "help",
                "help version",
                "help plugin list"
            ),
            aliases = listOf("h", "?")
        )
        
        argument(
            name = "comando",
            description = "Comando específico para mostrar ayuda",
            required = false
        )
        
        execute { context ->
            val commandName = context.getArgumentOrDefault(0, this@buildCommand)
            
            if (commandName != null) {
                // PED: Ayuda específica de comando (se implementaría con acceso al registry)
                CommandResult.Help("Ayuda específica para '$commandName' no implementada aún")
            } else {
                // PED: Ayuda general usando multiline string
                val helpText = """
                    🎯 SISTEMA MCP KOTLIN - AYUDA GENERAL
                    =====================================
                    
                    Este es un sistema de plugins MCP construido en Kotlin que demuestra
                    conceptos avanzados del lenguaje a través de un proyecto práctico.
                    
                    COMANDOS DISPONIBLES:
                    • help [comando]     - Muestra esta ayuda o ayuda específica
                    • version           - Muestra información de versión
                    • status            - Muestra estado del sistema
                    • config [clave]    - Muestra configuración del sistema
                    • plugin <acción>   - Gestiona plugins del sistema
                    
                    EJEMPLOS:
                    help version        - Ayuda del comando version
                    status              - Estado actual del sistema
                    config server.port  - Valor de configuración específica
                    
                    💡 Usa 'help <comando>' para obtener ayuda detallada de cualquier comando.
                """.trimIndent()
                
                CommandResult.Help(helpText)
            }
        }
    }
    
    /**
     * PED: Comando version con información detallada del sistema.
     */
    val versionCommand = buildCommand {
        metadata(
            name = "version",
            description = "Muestra información de versión del sistema",
            category = CommandCategory.SYSTEM,
            usage = "version [--detailed]",
            examples = listOf("version", "version --detailed"),
            aliases = listOf("v", "ver")
        )
        
        execute { context ->
            val detailed = "--detailed" in context.options || "-d" in context.options
            
            val versionInfo = if (detailed) {
                buildString {
                    appendLine("🎯 SISTEMA MCP KOTLIN")
                    appendLine("=" * 40)
                    appendLine("Versión: 1.0-SNAPSHOT")
                    appendLine("Kotlin: ${KotlinVersion.CURRENT}")
                    appendLine("JVM: ${System.getProperty("java.version")}")
                    appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
                    appendLine("Arquitectura: ${System.getProperty("os.arch")}")
                    appendLine("Usuario: ${System.getProperty("user.name")}")
                    appendLine("Directorio: ${System.getProperty("user.dir")}")
                    appendLine("Memoria libre: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB")
                    appendLine("Memoria total: ${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB")
                    appendLine("Procesadores: ${Runtime.getRuntime().availableProcessors()}")
                }
            } else {
                "🎯 Sistema MCP Kotlin v1.0-SNAPSHOT (Kotlin ${KotlinVersion.CURRENT})"
            }
            
            CommandResult.Success(versionInfo)
        }
    }
    
    /**
     * PED: Comando status que simula verificación del sistema.
     */
    val statusCommand = buildCommand {
        metadata(
            name = "status",
            description = "Muestra el estado actual del sistema",
            category = CommandCategory.SYSTEM,
            usage = "status [--refresh]",
            examples = listOf("status", "status --refresh"),
            aliases = listOf("stat", "st")
        )
        
        execute { context ->
            val refresh = "--refresh" in context.options
            
            if (refresh) {
                // PED: Simular refresh con delay
                delay(1000)
            }
            
            val statusInfo = buildString {
                appendLine("📊 ESTADO DEL SISTEMA")
                appendLine("=" * 30)
                appendLine("Estado: ✅ Operacional")
                appendLine("Uptime: ${System.currentTimeMillis() / 1000} segundos")
                appendLine("Plugins cargados: 0") // PED: Se actualizaría con datos reales
                appendLine("Comandos registrados: 4") // PED: Se actualizaría con datos reales
                appendLine("Memoria usada: ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024} MB")
                appendLine("Threads activos: ${Thread.activeCount()}")
                
                if (refresh) {
                    appendLine("\n🔄 Estado actualizado")
                }
            }
            
            CommandResult.Success(statusInfo)
        }
    }
    
    /**
     * PED: Comando config para mostrar configuración del sistema.
     */
    val configCommand = buildCommand {
        metadata(
            name = "config",
            description = "Muestra configuración del sistema",
            category = CommandCategory.SYSTEM,
            usage = "config [clave]",
            examples = listOf(
                "config",
                "config server.port",
                "config app.name"
            ),
            aliases = listOf("cfg", "conf")
        )
        
        argument(
            name = "clave",
            description = "Clave específica de configuración a mostrar",
            required = false
        )
        
        execute { context ->
            val key = context.getArgumentOrDefault(0, this@buildCommand)
            
            if (key != null) {
                // PED: Mostrar configuración específica
                val value = Config.getProperty(key) ?: "No encontrada"
                CommandResult.Success("$key = $value")
            } else {
                // PED: Mostrar toda la configuración
                val configInfo = buildString {
                    appendLine("⚙️ CONFIGURACIÓN DEL SISTEMA")
                    appendLine("=" * 35)
                    
                    // PED: Mostrar propiedades del sistema más relevantes
                    val relevantProps = listOf(
                        "app.name",
                        "app.version", 
                        "server.port",
                        "logging.level"
                    )
                    
                    relevantProps.forEach { prop ->
                        val value = Config.getProperty(prop) ?: "No configurada"
                        appendLine("$prop = $value")
                    }
                    
                    appendLine("\n💡 Usa 'config <clave>' para ver una configuración específica")
                }
                
                CommandResult.Success(configInfo)
            }
        }
    }
    
    /**
     * PED: Function para obtener todos los comandos del sistema.
     */
    fun getAllSystemCommands(): List<Command> = listOf(
        helpCommand,
        versionCommand,
        statusCommand,
        configCommand
    )
    
    /**
     * PED: Suspend function para registrar todos los comandos del sistema.
     */
    suspend fun registerSystemCommands(registry: CommandRegistry): Result<Int> {
        return registry.registerCommands(getAllSystemCommands(), "system")
    }
}

/**
 * PED: Extension functions para crear comandos del sistema de manera fluida.
 */

/**
 * PED: Extension function para crear comando de información.
 */
fun createInfoCommand(
    name: String,
    description: String,
    infoProvider: suspend () -> String
): Command = buildCommand {
    metadata(
        name = name,
        description = description,
        category = CommandCategory.SYSTEM,
        usage = name
    )
    
    execute {
        try {
            val info = infoProvider()
            CommandResult.Success(info)
        } catch (e: Exception) {
            CommandResult.Error("Error obteniendo información: ${e.message}", e)
        }
    }
}

/**
 * PED: Extension function para crear comando con validación.
 */
fun createValidatedCommand(
    name: String,
    description: String,
    arguments: List<CommandArgument>,
    validator: (CommandContext) -> Result<Unit>,
    executor: suspend (CommandContext) -> CommandResult
): Command = object : BaseCommand(
    CommandMetadata(name, description, CommandCategory.SYSTEM, name),
    arguments
) {
    override fun validateArguments(context: CommandContext): Result<Unit> {
        return super.validateArguments(context).fold(
            onSuccess = { validator(context) },
            onFailure = { Result.failure(it) }
        )
    }
    
    override suspend fun executeCommand(context: CommandContext): CommandResult {
        return executor(context)
    }
}
