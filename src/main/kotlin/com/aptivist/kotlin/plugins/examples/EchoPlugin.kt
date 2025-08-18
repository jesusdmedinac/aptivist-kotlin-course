
package com.aptivist.kotlin.plugins.examples

import com.aptivist.kotlin.mcp.protocol.McpMessage
import com.aptivist.kotlin.plugins.*
import com.aptivist.kotlin.plugins.commands.*
import kotlinx.coroutines.delay

/**
 * 🔊 ECHO PLUGIN - PLUGIN DE EJEMPLO SIMPLE (Phase 2.2)
 * 
 * Este plugin demuestra una implementación básica que extiende BasePlugin,
 * mostrando conceptos de herencia, override de métodos, y command registration.
 * 
 * CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Class inheritance con constructor primario
 * • Override de métodos abstractos y propiedades
 * • Object expressions para implementaciones inline
 * • Extension functions para logging fluido
 * • Suspend functions con delay para simulación
 * • Pattern matching con when expressions
 * • String templates y interpolation
 */

/**
 * PED: Clase que extiende BasePlugin demostrando herencia e implementación.
 */
class EchoPlugin : BasePlugin(
    // PED: Constructor primario que pasa metadata al constructor padre
    metadata = PluginMetadata(
        id = "echo-plugin",
        name = "Echo Plugin",
        version = "1.0.0",
        description = "Plugin de ejemplo que hace echo de mensajes y proporciona comandos básicos",
        author = "Aptivist Kotlin Course",
        capabilities = setOf(
            PluginCapability.MessageHandling,
            PluginCapability.CustomCapability("echo-processing")
        )
    )
) {
    
    // PED: Private property para almacenar comandos del plugin
    private val pluginCommands = mutableListOf<Command>()
    
    /**
     * PED: Override del método abstracto onInitialize.
     * Demuestra inicialización específica del plugin.
     */
    override suspend fun onInitialize(): Result<Unit> {
        return try {
            logInfo("Inicializando Echo Plugin...")
            
            // PED: Simular inicialización con delay
            delay(500)
            
            // PED: Crear comandos del plugin
            createPluginCommands()
            
            logInfo("Echo Plugin inicializado exitosamente")
            Result.success(Unit)
            
        } catch (e: Exception) {
            logError("Error inicializando Echo Plugin", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Override del método abstracto onActivate.
     */
    override suspend fun onActivate(): Result<Unit> {
        return try {
            logInfo("Activando Echo Plugin...")
            
            // PED: Simular activación
            delay(200)
            
            logInfo("Echo Plugin activado - Listo para procesar mensajes")
            Result.success(Unit)
            
        } catch (e: Exception) {
            logError("Error activando Echo Plugin", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Override del método abstracto onDeactivate.
     */
    override suspend fun onDeactivate(): Result<Unit> {
        return try {
            logInfo("Desactivando Echo Plugin...")
            delay(100)
            logInfo("Echo Plugin desactivado")
            Result.success(Unit)
            
        } catch (e: Exception) {
            logError("Error desactivando Echo Plugin", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Override del método abstracto onShutdown.
     */
    override suspend fun onShutdown(): Result<Unit> {
        return try {
            logInfo("Cerrando Echo Plugin...")
            
            // PED: Limpiar recursos
            pluginCommands.clear()
            
            logInfo("Echo Plugin cerrado exitosamente")
            Result.success(Unit)
            
        } catch (e: Exception) {
            logError("Error cerrando Echo Plugin", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Override del método canHandle para especificar qué mensajes puede procesar.
     */
    override fun <T : McpMessage> canHandle(messageType: Class<T>): Boolean {
        // PED: Este plugin puede manejar cualquier tipo de mensaje para hacer echo
        return true
    }
    
    /**
     * PED: Override del método abstracto onHandleMessage.
     */
    override suspend fun onHandleMessage(message: McpMessage): Result<McpMessage?> {
        return try {
            logDebug("Procesando mensaje: ${message::class.simpleName}")
            
            // PED: Simular procesamiento
            delay(50)
            
            // PED: Por simplicidad, retornamos el mismo mensaje (echo)
            // En un plugin real, se procesaría y transformaría el mensaje
            logInfo("Echo procesado para mensaje: ${message::class.simpleName}")
            
            Result.success(message) // PED: Echo del mensaje original
            
        } catch (e: Exception) {
            logError("Error procesando mensaje", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Function para obtener los comandos del plugin.
     */
    fun getPluginCommands(): List<Command> = pluginCommands.toList()
    
    // PED: REGION - Métodos privados auxiliares
    
    /**
     * PED: Private function para crear comandos específicos del plugin.
     */
    private fun createPluginCommands() {
        // PED: Comando echo usando buildCommand DSL
        val echoCommand = buildCommand {
            metadata(
                name = "echo",
                description = "Hace echo de un mensaje",
                category = CommandCategory.PLUGIN,
                usage = "echo <mensaje>",
                examples = listOf(
                    "echo Hola mundo",
                    "echo 'Mensaje con espacios'"
                ),
                requiresPlugin = metadata.id
            )
            
            argument(
                name = "mensaje",
                description = "Mensaje para hacer echo",
                required = true
            )
            
            execute { context ->
                val message = context.arguments.joinToString(" ")
                if (message.isBlank()) {
                    CommandResult.Error("Mensaje no puede estar vacío")
                } else {
                    CommandResult.Success("🔊 Echo: $message")
                }
            }
        }
        
        // PED: Comando plugin-info usando object expression
        val pluginInfoCommand = object : BaseCommand(
            CommandMetadata(
                name = "plugin-info",
                description = "Muestra información del Echo Plugin",
                category = CommandCategory.PLUGIN,
                usage = "plugin-info",
                requiresPlugin = metadata.id
            )
        ) {
            override suspend fun executeCommand(context: CommandContext): CommandResult {
                val info = buildString {
                    appendLine("🔊 ECHO PLUGIN INFORMACIÓN")
                    appendLine("=" * 30)
                    appendLine("ID: ${metadata.id}")
                    appendLine("Nombre: ${metadata.name}")
                    appendLine("Versión: ${metadata.version}")
                    appendLine("Autor: ${metadata.author}")
                    appendLine("Descripción: ${metadata.description}")
                    appendLine("Estado: ${currentState::class.simpleName}")
                    appendLine("Capacidades: ${metadata.capabilities.joinToString()}")
                    appendLine("Comandos disponibles: ${pluginCommands.size}")
                }
                
                return CommandResult.Success(info)
            }
        }
        
        // PED: Comando test-delay que demuestra suspend functions
        val testDelayCommand = buildCommand {
            metadata(
                name = "test-delay",
                description = "Comando de prueba con delay configurable",
                category = CommandCategory.DEVELOPMENT,
                usage = "test-delay [milisegundos]",
                examples = listOf(
                    "test-delay",
                    "test-delay 1000",
                    "test-delay 5000"
                ),
                requiresPlugin = metadata.id
            )
            
            argument(
                name = "milisegundos",
                description = "Tiempo de delay en milisegundos",
                required = false,
                defaultValue = "1000",
                validValues = listOf("500", "1000", "2000", "5000")
            )
            
            execute { context ->
                val delayMs = context.getArgumentOrDefault(0, this@buildCommand)?.toLongOrNull() ?: 1000L
                
                if (delayMs < 0 || delayMs > 10000) {
                    return@execute CommandResult.Error("Delay debe estar entre 0 y 10000 ms")
                }
                
                val startTime = System.currentTimeMillis()
                delay(delayMs)
                val actualDelay = System.currentTimeMillis() - startTime
                
                CommandResult.Success("⏱️ Delay completado: ${actualDelay}ms (solicitado: ${delayMs}ms)")
            }
        }
        
        // PED: Agregar comandos a la lista
        pluginCommands.addAll(listOf(echoCommand, pluginInfoCommand, testDelayCommand))
    }
}

/**
 * PED: Extension functions específicas para EchoPlugin.
 */

/**
 * PED: Extension function para verificar si el plugin puede hacer echo.
 */
fun EchoPlugin.canEcho(): Boolean = isActive()

/**
 * PED: Extension function para obtener estadísticas del plugin.
 */
fun EchoPlugin.getStatistics(): Map<String, Any> = mapOf(
    "id" to metadata.id,
    "name" to metadata.name,
    "version" to metadata.version,
    "state" to currentState::class.simpleName,
    "commands" to getPluginCommands().size,
    "capabilities" to metadata.capabilities.size,
    "isActive" to isActive()
)
