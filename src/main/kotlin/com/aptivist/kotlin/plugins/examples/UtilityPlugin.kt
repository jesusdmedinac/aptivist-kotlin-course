
package com.aptivist.kotlin.plugins.examples

import com.aptivist.kotlin.mcp.protocol.McpMessage
import com.aptivist.kotlin.plugins.*
import com.aptivist.kotlin.plugins.commands.*
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 🛠️ UTILITY PLUGIN - PLUGIN CON COMANDOS UTILITARIOS (Phase 2.2)
 * 
 * Este plugin demuestra implementación más compleja con múltiples comandos utilitarios,
 * mostrando conceptos avanzados como file I/O, date/time handling, y random generation.
 * 
 * CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Complex plugin implementation con múltiples capabilities
 • File I/O operations con extension functions
 • Date/time handling con LocalDateTime y formatters
 • Random number generation con kotlin.random
 • String manipulation y validation
 • Exception handling con try-catch y Result<T>
 • Collection operations (map, filter, take, etc.)
 • Regex patterns para validation
 */

/**
 * PED: Plugin utilitario que demuestra implementación compleja.
 */
class UtilityPlugin : BasePlugin(
    metadata = PluginMetadata(
        id = "utility-plugin",
        name = "Utility Plugin",
        version = "1.2.0",
        description = "Plugin con comandos utilitarios para tareas comunes del sistema",
        author = "Aptivist Kotlin Course",
        capabilities = setOf(
            PluginCapability.ToolExecution,
            PluginCapability.CustomCapability("file-operations"),
            PluginCapability.CustomCapability("text-processing"),
            PluginCapability.CustomCapability("random-generation")
        )
    )
) {
    
    // PED: Private properties para configuración del plugin
    private val pluginCommands = mutableListOf<Command>()
    private val workingDirectory = File(System.getProperty("user.dir"))
    private val random = Random.Default
    
    override suspend fun onInitialize(): Result<Unit> {
        return try {
            logInfo("Inicializando Utility Plugin...")
            
            // PED: Verificar permisos de directorio de trabajo
            if (!workingDirectory.canRead()) {
                return Result.failure(IllegalStateException("No se puede leer el directorio de trabajo"))
            }
            
            createUtilityCommands()
            delay(300) // PED: Simular inicialización
            
            logInfo("Utility Plugin inicializado con ${pluginCommands.size} comandos")
            Result.success(Unit)
            
        } catch (e: Exception) {
            logError("Error inicializando Utility Plugin", e)
            Result.failure(e)
        }
    }
    
    override suspend fun onActivate(): Result<Unit> {
        return try {
            logInfo("Activando Utility Plugin...")
            delay(150)
            logInfo("Utility Plugin activado - Comandos utilitarios disponibles")
            Result.success(Unit)
        } catch (e: Exception) {
            logError("Error activando Utility Plugin", e)
            Result.failure(e)
        }
    }
    
    override suspend fun onDeactivate(): Result<Unit> {
        return try {
            logInfo("Desactivando Utility Plugin...")
            delay(100)
            logInfo("Utility Plugin desactivado")
            Result.success(Unit)
        } catch (e: Exception) {
            logError("Error desactivando Utility Plugin", e)
            Result.failure(e)
        }
    }
    
    override suspend fun onShutdown(): Result<Unit> {
        return try {
            logInfo("Cerrando Utility Plugin...")
            pluginCommands.clear()
            logInfo("Utility Plugin cerrado")
            Result.success(Unit)
        } catch (e: Exception) {
            logError("Error cerrando Utility Plugin", e)
            Result.failure(e)
        }
    }
    
    override fun <T : McpMessage> canHandle(messageType: Class<T>): Boolean {
        // PED: Este plugin maneja mensajes relacionados con herramientas
        return messageType.simpleName.contains("Tool") || 
               messageType.simpleName.contains("Resource")
    }
    
    override suspend fun onHandleMessage(message: McpMessage): Result<McpMessage?> {
        return try {
            logDebug("Procesando mensaje utilitario: ${message::class.simpleName}")
            delay(25) // PED: Simular procesamiento
            
            // PED: En un plugin real, se procesarían mensajes específicos
            logInfo("Mensaje utilitario procesado")
            Result.success(null) // PED: No modificamos el mensaje
            
        } catch (e: Exception) {
            logError("Error procesando mensaje utilitario", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Function para obtener comandos del plugin.
     */
    fun getPluginCommands(): List<Command> = pluginCommands.toList()
    
    // PED: REGION - Creación de comandos utilitarios
    
    /**
     * PED: Private function para crear todos los comandos utilitarios.
     */
    private fun createUtilityCommands() {
        pluginCommands.addAll(listOf(
            createDateTimeCommand(),
            createRandomCommand(),
            createFileListCommand(),
            createTextProcessCommand(),
            createCalculatorCommand()
        ))
    }
    
    /**
     * PED: Comando para mostrar fecha y hora actual.
     */
    private fun createDateTimeCommand(): Command = buildCommand {
        metadata(
            name = "datetime",
            description = "Muestra fecha y hora actual en diferentes formatos",
            category = CommandCategory.UTILITY,
            usage = "datetime [formato]",
            examples = listOf(
                "datetime",
                "datetime iso",
                "datetime custom"
            ),
            aliases = listOf("date", "time", "dt"),
            requiresPlugin = metadata.id
        )
        
        argument(
            name = "formato",
            description = "Formato de fecha/hora",
            required = false,
            defaultValue = "default",
            validValues = listOf("default", "iso", "custom", "timestamp")
        )
        
        execute { context ->
            val format = context.getArgumentOrDefault(0, this@buildCommand) ?: "default"
            val now = LocalDateTime.now()
            
            val result = when (format.lowercase()) {
                "iso" -> now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                "custom" -> now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                "timestamp" -> System.currentTimeMillis().toString()
                else -> now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            }
            
            CommandResult.Success("🕐 Fecha/Hora ($format): $result")
        }
    }
    
    /**
     * PED: Comando para generar números aleatorios.
     */
    private fun createRandomCommand(): Command = buildCommand {
        metadata(
            name = "random",
            description = "Genera números aleatorios o selecciona elementos al azar",
            category = CommandCategory.UTILITY,
            usage = "random <tipo> [parámetros...]",
            examples = listOf(
                "random int 1 100",
                "random float 0.0 1.0",
                "random choice apple banana orange",
                "random uuid"
            ),
            aliases = listOf("rand", "rnd"),
            requiresPlugin = metadata.id
        )
        
        argument(
            name = "tipo",
            description = "Tipo de generación aleatoria",
            required = true,
            validValues = listOf("int", "float", "choice", "uuid", "string")
        )
        
        execute { context ->
            if (context.arguments.isEmpty()) {
                return@execute CommandResult.Error("Tipo de random requerido")
            }
            
            val type = context.arguments[0].lowercase()
            val args = context.arguments.drop(1)
            
            val result = when (type) {
                "int" -> {
                    val min = args.getOrNull(0)?.toIntOrNull() ?: 0
                    val max = args.getOrNull(1)?.toIntOrNull() ?: 100
                    "🎲 Número aleatorio: ${random.nextInt(min, max + 1)}"
                }
                "float" -> {
                    val min = args.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    val max = args.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                    val value = min + random.nextDouble() * (max - min)
                    "🎲 Número aleatorio: ${"%.4f".format(value)}"
                }
                "choice" -> {
                    if (args.isEmpty()) {
                        "❌ Se requieren opciones para elegir"
                    } else {
                        val choice = args.random(random)
                        "🎲 Elección aleatoria: $choice"
                    }
                }
                "uuid" -> {
                    val uuid = java.util.UUID.randomUUID().toString()
                    "🎲 UUID aleatorio: $uuid"
                }
                "string" -> {
                    val length = args.getOrNull(0)?.toIntOrNull() ?: 8
                    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                    val randomString = (1..length).map { chars.random(random) }.joinToString("")
                    "🎲 String aleatorio: $randomString"
                }
                else -> "❌ Tipo de random no soportado: $type"
            }
            
            CommandResult.Success(result)
        }
    }
    
    /**
     * PED: Comando para listar archivos del directorio.
     */
    private fun createFileListCommand(): Command = buildCommand {
        metadata(
            name = "ls",
            description = "Lista archivos y directorios",
            category = CommandCategory.UTILITY,
            usage = "ls [directorio] [--detailed]",
            examples = listOf(
                "ls",
                "ls /home/user",
                "ls --detailed",
                "ls . --detailed"
            ),
            aliases = listOf("list", "dir"),
            requiresPlugin = metadata.id
        )
        
        argument(
            name = "directorio",
            description = "Directorio a listar",
            required = false,
            defaultValue = "."
        )
        
        execute { context ->
            val dirPath = context.getArgumentOrDefault(0, this@buildCommand) ?: "."
            val detailed = "--detailed" in context.options || "-l" in context.options
            
            try {
                val directory = File(dirPath)
                if (!directory.exists()) {
                    return@execute CommandResult.Error("Directorio no existe: $dirPath")
                }
                
                if (!directory.isDirectory) {
                    return@execute CommandResult.Error("No es un directorio: $dirPath")
                }
                
                val files = directory.listFiles()?.sortedBy { it.name } ?: emptyList()
                
                val result = buildString {
                    appendLine("📁 CONTENIDO DE: ${directory.absolutePath}")
                    appendLine("=" * 50)
                    
                    if (files.isEmpty()) {
                        appendLine("(Directorio vacío)")
                    } else {
                        files.forEach { file ->
                            val icon = if (file.isDirectory) "📁" else "📄"
                            val name = file.name
                            
                            if (detailed) {
                                val size = if (file.isFile) "${file.length()} bytes" else "<DIR>"
                                val modified = java.time.Instant.ofEpochMilli(file.lastModified())
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                appendLine("$icon $name ($size) - $modified")
                            } else {
                                appendLine("$icon $name")
                            }
                        }
                    }
                    
                    appendLine("\nTotal: ${files.size} elementos")
                }
                
                CommandResult.Success(result)
                
            } catch (e: Exception) {
                CommandResult.Error("Error listando directorio: ${e.message}", e)
            }
        }
    }
    
    /**
     * PED: Comando para procesamiento de texto.
     */
    private fun createTextProcessCommand(): Command = buildCommand {
        metadata(
            name = "text",
            description = "Procesa texto con diferentes operaciones",
            category = CommandCategory.UTILITY,
            usage = "text <operación> <texto>",
            examples = listOf(
                "text upper 'hola mundo'",
                "text lower 'HOLA MUNDO'",
                "text reverse 'abcdef'",
                "text count 'hola mundo hola'",
                "text words 'esto es una prueba'"
            ),
            requiresPlugin = metadata.id
        )
        
        argument(
            name = "operación",
            description = "Operación a realizar",
            required = true,
            validValues = listOf("upper", "lower", "reverse", "count", "words", "chars")
        )
        
        argument(
            name = "texto",
            description = "Texto a procesar",
            required = true
        )
        
        execute { context ->
            if (context.arguments.size < 2) {
                return@execute CommandResult.Error("Se requiere operación y texto")
            }
            
            val operation = context.arguments[0].lowercase()
            val text = context.arguments.drop(1).joinToString(" ")
            
            val result = when (operation) {
                "upper" -> "🔤 Mayúsculas: ${text.uppercase()}"
                "lower" -> "🔤 Minúsculas: ${text.lowercase()}"
                "reverse" -> "🔄 Reverso: ${text.reversed()}"
                "count" -> "📊 Caracteres: ${text.length}, Palabras: ${text.split("\\s+".toRegex()).size}"
                "words" -> {
                    val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    "📝 Palabras (${words.size}): ${words.joinToString(", ")}"
                }
                "chars" -> {
                    val charCount = text.groupingBy { it }.eachCount()
                        .toList().sortedByDescending { it.second }.take(5)
                    "🔤 Caracteres más frecuentes: ${charCount.joinToString { "${it.first}:${it.second}" }}"
                }
                else -> "❌ Operación no soportada: $operation"
            }
            
            CommandResult.Success(result)
        }
    }
    
    /**
     * PED: Comando calculadora simple.
     */
    private fun createCalculatorCommand(): Command = buildCommand {
        metadata(
            name = "calc",
            description = "Calculadora simple para operaciones básicas",
            category = CommandCategory.UTILITY,
            usage = "calc <número1> <operación> <número2>",
            examples = listOf(
                "calc 10 + 5",
                "calc 20 - 8",
                "calc 6 * 7",
                "calc 15 / 3",
                "calc 2 ^ 8"
            ),
            aliases = listOf("calculate", "math"),
            requiresPlugin = metadata.id
        )
        
        execute { context ->
            if (context.arguments.size != 3) {
                return@execute CommandResult.Error("Formato: calc <número1> <operación> <número2>")
            }
            
            val num1Str = context.arguments[0]
            val operation = context.arguments[1]
            val num2Str = context.arguments[2]
            
            try {
                val num1 = num1Str.toDouble()
                val num2 = num2Str.toDouble()
                
                val result = when (operation) {
                    "+" -> num1 + num2
                    "-" -> num1 - num2
                    "*", "x" -> num1 * num2
                    "/" -> {
                        if (num2 == 0.0) {
                            return@execute CommandResult.Error("División por cero no permitida")
                        }
                        num1 / num2
                    }
                    "^", "**" -> kotlin.math.pow(num1, num2)
                    "%" -> num1 % num2
                    else -> return@execute CommandResult.Error("Operación no soportada: $operation")
                }
                
                val formattedResult = if (result == result.toLong().toDouble()) {
                    result.toLong().toString()
                } else {
                    "%.6f".format(result).trimEnd('0').trimEnd('.')
                }
                
                CommandResult.Success("🧮 Resultado: $num1 $operation $num2 = $formattedResult")
                
            } catch (e: NumberFormatException) {
                CommandResult.Error("Números inválidos: $num1Str, $num2Str")
            } catch (e: Exception) {
                CommandResult.Error("Error en cálculo: ${e.message}", e)
            }
        }
    }
}
