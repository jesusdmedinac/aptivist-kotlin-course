
package com.aptivist.kotlin.plugins.commands

import kotlinx.serialization.Serializable

/**
 * 🎯 SISTEMA DE COMANDOS - ARQUITECTURA EXTENSIBLE (Phase 2.2)
 * 
 * Este sistema define la arquitectura para comandos ejecutables por plugins,
 * demostrando sealed classes, data classes, y programación funcional.
 * 
 * CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Sealed classes para type-safe command hierarchy
 * • Data classes con serialización automática
 * • Enum classes para categorización
 * • Extension functions para APIs fluidas
 * • Higher-order functions para validation y execution
 * • Result<T> para error handling funcional
 * • Vararg parameters para argumentos flexibles
 */

/**
 * PED: Enum class para categorizar comandos.
 */
enum class CommandCategory {
    SYSTEM,      // Comandos del sistema (help, version, etc.)
    PLUGIN,      // Comandos específicos de plugins
    UTILITY,     // Comandos utilitarios
    DEVELOPMENT, // Comandos para desarrollo
    CUSTOM       // Comandos personalizados
}

/**
 * PED: Data class para metadatos del comando con serialización.
 */
@Serializable
data class CommandMetadata(
    val name: String,
    val description: String,
    val category: CommandCategory,
    val usage: String,
    val examples: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val requiresPlugin: String? = null // PED: ID del plugin que proporciona el comando
)

/**
 * PED: Data class para argumentos del comando.
 */
@Serializable
data class CommandArgument(
    val name: String,
    val description: String,
    val required: Boolean = true,
    val defaultValue: String? = null,
    val validValues: List<String>? = null // PED: Valores válidos para validación
)

/**
 * PED: Sealed class para resultados de ejecución de comandos.
 */
sealed class CommandResult {
    data class Success(val output: String, val data: Map<String, Any> = emptyMap()) : CommandResult()
    data class Error(val message: String, val cause: Throwable? = null) : CommandResult()
    data class Help(val helpText: String) : CommandResult()
}

/**
 * PED: Data class para contexto de ejecución del comando.
 */
data class CommandContext(
    val arguments: List<String>,
    val options: Map<String, String> = emptyMap(),
    val environment: Map<String, String> = emptyMap(),
    val workingDirectory: String = System.getProperty("user.dir")
)

/**
 * PED: Interfaz principal para comandos ejecutables.
 */
interface Command {
    val metadata: CommandMetadata
    val arguments: List<CommandArgument>
    
    /**
     * PED: Suspend function para ejecución asíncrona del comando.
     */
    suspend fun execute(context: CommandContext): CommandResult
    
    /**
     * PED: Method para validar argumentos antes de la ejecución.
     */
    fun validateArguments(context: CommandContext): Result<Unit> {
        // PED: Implementación default que verifica argumentos requeridos
        val requiredArgs = arguments.filter { it.required }
        val providedArgs = context.arguments
        
        if (providedArgs.size < requiredArgs.size) {
            val missing = requiredArgs.drop(providedArgs.size).map { it.name }
            return Result.failure(
                IllegalArgumentException("Argumentos requeridos faltantes: ${missing.joinToString()}")
            )
        }
        
        return Result.success(Unit)
    }
    
    /**
     * PED: Method default para generar texto de ayuda.
     */
    fun generateHelpText(): String {
        return buildString {
            appendLine("Comando: ${metadata.name}")
            appendLine("Descripción: ${metadata.description}")
            appendLine("Uso: ${metadata.usage}")
            
            if (arguments.isNotEmpty()) {
                appendLine("\nArgumentos:")
                arguments.forEach { arg ->
                    val required = if (arg.required) "[requerido]" else "[opcional]"
                    appendLine("  ${arg.name} $required - ${arg.description}")
                    arg.defaultValue?.let { appendLine("    Valor por defecto: $it") }
                    arg.validValues?.let { appendLine("    Valores válidos: ${it.joinToString()}") }
                }
            }
            
            if (metadata.aliases.isNotEmpty()) {
                appendLine("\nAlias: ${metadata.aliases.joinToString()}")
            }
            
            if (metadata.examples.isNotEmpty()) {
                appendLine("\nEjemplos:")
                metadata.examples.forEach { example ->
                    appendLine("  $example")
                }
            }
        }
    }
}

/**
 * PED: Abstract class que implementa Command con funcionalidad común.
 */
abstract class BaseCommand(
    override val metadata: CommandMetadata,
    override val arguments: List<CommandArgument> = emptyList()
) : Command {
    
    /**
     * PED: Template method que maneja validación y ejecución.
     */
    final override suspend fun execute(context: CommandContext): CommandResult {
        return try {
            // PED: Validar argumentos primero
            validateArguments(context).getOrThrow()
            
            // PED: Ejecutar implementación específica
            executeCommand(context)
            
        } catch (e: IllegalArgumentException) {
            CommandResult.Error("Error de argumentos: ${e.message}", e)
        } catch (e: Exception) {
            CommandResult.Error("Error ejecutando comando: ${e.message}", e)
        }
    }
    
    /**
     * PED: Método abstracto que implementan las subclases.
     */
    protected abstract suspend fun executeCommand(context: CommandContext): CommandResult
}

/**
 * PED: Extension functions para trabajar con comandos de manera fluida.
 */

/**
 * PED: Extension function para crear CommandContext de manera fluida.
 */
fun List<String>.toCommandContext(
    options: Map<String, String> = emptyMap(),
    environment: Map<String, String> = System.getenv()
): CommandContext = CommandContext(
    arguments = this,
    options = options,
    environment = environment
)

/**
 * PED: Extension function para verificar si un comando coincide con un nombre o alias.
 */
fun Command.matches(name: String): Boolean {
    return metadata.name.equals(name, ignoreCase = true) ||
           metadata.aliases.any { it.equals(name, ignoreCase = true) }
}

/**
 * PED: Extension function para obtener argumentos con valores por defecto.
 */
fun CommandContext.getArgumentOrDefault(index: Int, command: Command): String? {
    return arguments.getOrNull(index) ?: command.arguments.getOrNull(index)?.defaultValue
}

/**
 * PED: Extension function para validar valores de argumentos.
 */
fun CommandContext.validateArgumentValues(command: Command): Result<Unit> {
    command.arguments.forEachIndexed { index, arg ->
        val value = arguments.getOrNull(index)
        if (value != null && arg.validValues != null) {
            if (value !in arg.validValues) {
                return Result.failure(
                    IllegalArgumentException(
                        "Valor inválido para ${arg.name}: '$value'. Valores válidos: ${arg.validValues.joinToString()}"
                    )
                )
            }
        }
    }
    return Result.success(Unit)
}

/**
 * PED: Higher-order function para crear comandos con DSL.
 */
inline fun command(
    name: String,
    description: String,
    category: CommandCategory = CommandCategory.CUSTOM,
    crossinline execution: suspend (CommandContext) -> CommandResult
): Command = object : BaseCommand(
    CommandMetadata(
        name = name,
        description = description,
        category = category,
        usage = name
    )
) {
    override suspend fun executeCommand(context: CommandContext): CommandResult {
        return execution(context)
    }
}

/**
 * PED: Builder class para crear comandos complejos con DSL.
 */
class CommandBuilder {
    private lateinit var commandMetadata: CommandMetadata
    private val arguments = mutableListOf<CommandArgument>()
    private var execution: (suspend (CommandContext) -> CommandResult)? = null
    
    fun metadata(
        name: String,
        description: String,
        category: CommandCategory = CommandCategory.CUSTOM,
        usage: String = name,
        examples: List<String> = emptyList(),
        aliases: List<String> = emptyList(),
        requiresPlugin: String? = null
    ) {
        commandMetadata = CommandMetadata(name, description, category, usage, examples, aliases, requiresPlugin)
    }
    
    fun argument(
        name: String,
        description: String,
        required: Boolean = true,
        defaultValue: String? = null,
        validValues: List<String>? = null
    ) {
        arguments.add(CommandArgument(name, description, required, defaultValue, validValues))
    }
    
    fun execute(block: suspend (CommandContext) -> CommandResult) {
        execution = block
    }
    
    fun build(): Command {
        require(::commandMetadata.isInitialized) { "Metadata es requerida" }
        requireNotNull(execution) { "Execution block es requerido" }
        
        return object : BaseCommand(commandMetadata, arguments) {
            override suspend fun executeCommand(context: CommandContext): CommandResult {
                return execution!!(context)
            }
        }
    }
}

/**
 * PED: DSL function para crear comandos con builder pattern.
 */
fun buildCommand(configure: CommandBuilder.() -> Unit): Command {
    return CommandBuilder().apply(configure).build()
}
