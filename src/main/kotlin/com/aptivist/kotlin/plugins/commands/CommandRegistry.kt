
package com.aptivist.kotlin.plugins.commands

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 📋 COMMAND REGISTRY - SISTEMA DE REGISTRO DE COMANDOS (Phase 2.2)
 * 
 * Esta clase gestiona el registro y ejecución de comandos del sistema,
 * demostrando concurrent programming, thread safety, y command pattern.
 * 
 * CONCEPTOS KOTLIN DEMOSTRADOS:
 * • ConcurrentHashMap para thread-safe collections
 * • Mutex para synchronization de operaciones críticas
 * • Extension functions para APIs fluidas
 * • Higher-order functions para filtering y mapping
 * • Sealed classes para type-safe results
 * • Data classes para statistics y reporting
 * • Suspend functions para operaciones asíncronas
 */

/**
 * PED: Data class para estadísticas del registry.
 */
data class CommandRegistryStatistics(
    val totalCommands: Int,
    val commandsByCategory: Map<CommandCategory, Int>,
    val commandsByPlugin: Map<String, Int>,
    val mostUsedCommands: List<Pair<String, Int>>
)

/**
 * PED: Sealed class para eventos del registry.
 */
sealed class CommandRegistryEvent {
    data class CommandRegistered(val command: Command, val pluginId: String?) : CommandRegistryEvent()
    data class CommandUnregistered(val commandName: String, val pluginId: String?) : CommandRegistryEvent()
    data class CommandExecuted(val commandName: String, val success: Boolean, val executionTimeMs: Long) : CommandRegistryEvent()
}

/**
 * PED: Clase principal para gestión de comandos con thread safety.
 */
class CommandRegistry {
    
    companion object {
        private val logger = LoggerFactory.getLogger(CommandRegistry::class.java)
    }
    
    // PED: ConcurrentHashMap para thread-safe storage
    private val commands = ConcurrentHashMap<String, Command>()
    private val commandsByPlugin = ConcurrentHashMap<String, MutableSet<String>>()
    private val commandUsageStats = ConcurrentHashMap<String, Int>()
    
    // PED: Mutex para operaciones que requieren atomicidad
    private val registrationMutex = Mutex()
    
    /**
     * PED: Suspend function para registrar un comando de manera thread-safe.
     */
    suspend fun registerCommand(command: Command, pluginId: String? = null): Result<Unit> {
        return registrationMutex.withLock {
            try {
                val commandName = command.metadata.name
                
                // PED: Verificar si el comando ya existe
                if (commands.containsKey(commandName)) {
                    return Result.failure(
                        IllegalArgumentException("Comando '$commandName' ya está registrado")
                    )
                }
                
                // PED: Verificar aliases para evitar conflictos
                val conflictingAlias = command.metadata.aliases.find { alias ->
                    commands.keys.any { existingName ->
                        existingName.equals(alias, ignoreCase = true) ||
                        commands[existingName]?.metadata?.aliases?.any { 
                            it.equals(alias, ignoreCase = true) 
                        } == true
                    }
                }
                
                if (conflictingAlias != null) {
                    return Result.failure(
                        IllegalArgumentException("Alias '$conflictingAlias' ya está en uso")
                    )
                }
                
                // PED: Registrar comando
                commands[commandName] = command
                commandUsageStats[commandName] = 0
                
                // PED: Asociar con plugin si se proporciona
                pluginId?.let { id ->
                    commandsByPlugin.computeIfAbsent(id) { mutableSetOf() }.add(commandName)
                }
                
                logger.info("✅ Comando registrado: $commandName" + 
                           if (pluginId != null) " (plugin: $pluginId)" else "")
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                logger.error("❌ Error registrando comando ${command.metadata.name}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * PED: Suspend function para desregistrar un comando.
     */
    suspend fun unregisterCommand(commandName: String): Result<Unit> {
        return registrationMutex.withLock {
            try {
                val command = commands.remove(commandName)
                    ?: return Result.failure(IllegalArgumentException("Comando '$commandName' no encontrado"))
                
                commandUsageStats.remove(commandName)
                
                // PED: Remover de asociaciones de plugins
                commandsByPlugin.values.forEach { commandSet ->
                    commandSet.remove(commandName)
                }
                
                logger.info("🗑️ Comando desregistrado: $commandName")
                Result.success(Unit)
                
            } catch (e: Exception) {
                logger.error("❌ Error desregistrando comando $commandName", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * PED: Suspend function para desregistrar todos los comandos de un plugin.
     */
    suspend fun unregisterPluginCommands(pluginId: String): Result<Int> {
        return registrationMutex.withLock {
            try {
                val pluginCommands = commandsByPlugin[pluginId]?.toList() ?: emptyList()
                var unregisteredCount = 0
                
                pluginCommands.forEach { commandName ->
                    unregisterCommand(commandName).onSuccess {
                        unregisteredCount++
                    }
                }
                
                commandsByPlugin.remove(pluginId)
                
                logger.info("🧹 Desregistrados $unregisteredCount comandos del plugin: $pluginId")
                Result.success(unregisteredCount)
                
            } catch (e: Exception) {
                logger.error("❌ Error desregistrando comandos del plugin $pluginId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * PED: Function para buscar comando por nombre o alias.
     */
    fun findCommand(name: String): Command? {
        // PED: Buscar por nombre exacto primero
        commands[name]?.let { return it }
        
        // PED: Buscar por alias usando extension function
        return commands.values.find { it.matches(name) }
    }
    
    /**
     * PED: Suspend function para ejecutar un comando.
     */
    suspend fun executeCommand(commandName: String, context: CommandContext): CommandResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val command = findCommand(commandName)
                ?: return CommandResult.Error("Comando no encontrado: $commandName")
            
            logger.debug("🚀 Ejecutando comando: $commandName")
            
            val result = command.execute(context)
            val executionTime = System.currentTimeMillis() - startTime
            
            // PED: Actualizar estadísticas de uso
            commandUsageStats.compute(command.metadata.name) { _, count -> (count ?: 0) + 1 }
            
            logger.debug("✅ Comando ejecutado: $commandName (${executionTime}ms)")
            
            result
            
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            logger.error("❌ Error ejecutando comando $commandName (${executionTime}ms)", e)
            CommandResult.Error("Error ejecutando comando: ${e.message}", e)
        }
    }
    
    /**
     * PED: Function para obtener todos los comandos registrados.
     */
    fun getAllCommands(): Map<String, Command> = commands.toMap()
    
    /**
     * PED: Function para obtener comandos por categoría usando filtering.
     */
    fun getCommandsByCategory(category: CommandCategory): List<Command> {
        return commands.values.filter { it.metadata.category == category }
    }
    
    /**
     * PED: Function para obtener comandos de un plugin específico.
     */
    fun getPluginCommands(pluginId: String): List<Command> {
        val commandNames = commandsByPlugin[pluginId] ?: return emptyList()
        return commandNames.mapNotNull { commands[it] }
    }
    
    /**
     * PED: Function para generar estadísticas del registry.
     */
    fun getStatistics(): CommandRegistryStatistics {
        val commandsByCategory = commands.values
            .groupBy { it.metadata.category }
            .mapValues { it.value.size }
        
        val commandsByPlugin = commandsByPlugin
            .mapValues { it.value.size }
        
        val mostUsedCommands = commandUsageStats
            .toList()
            .sortedByDescending { it.second }
            .take(10)
        
        return CommandRegistryStatistics(
            totalCommands = commands.size,
            commandsByCategory = commandsByCategory,
            commandsByPlugin = commandsByPlugin,
            mostUsedCommands = mostUsedCommands
        )
    }
    
    /**
     * PED: Function para generar texto de ayuda general.
     */
    fun generateHelpText(category: CommandCategory? = null): String {
        val commandsToShow = if (category != null) {
            getCommandsByCategory(category)
        } else {
            commands.values.toList()
        }
        
        return buildString {
            appendLine("📋 COMANDOS DISPONIBLES")
            appendLine("=" * 50)
            
            if (category != null) {
                appendLine("Categoría: ${category.name}")
                appendLine()
            }
            
            // PED: Agrupar por categoría para mejor presentación
            commandsToShow
                .groupBy { it.metadata.category }
                .toSortedMap()
                .forEach { (cat, cmds) ->
                    appendLine("${cat.name}:")
                    cmds.sortedBy { it.metadata.name }.forEach { cmd ->
                        appendLine("  ${cmd.metadata.name.padEnd(20)} - ${cmd.metadata.description}")
                        if (cmd.metadata.aliases.isNotEmpty()) {
                            appendLine("    Alias: ${cmd.metadata.aliases.joinToString()}")
                        }
                    }
                    appendLine()
                }
            
            appendLine("💡 Usa 'help <comando>' para obtener ayuda específica de un comando")
        }
    }
    
    /**
     * PED: Function para limpiar el registry.
     */
    suspend fun clear(): Result<Unit> {
        return registrationMutex.withLock {
            try {
                val commandCount = commands.size
                commands.clear()
                commandsByPlugin.clear()
                commandUsageStats.clear()
                
                logger.info("🧹 Registry limpiado: $commandCount comandos removidos")
                Result.success(Unit)
                
            } catch (e: Exception) {
                logger.error("❌ Error limpiando registry", e)
                Result.failure(e)
            }
        }
    }
}

/**
 * PED: Extension functions para CommandRegistry que proporcionan APIs fluidas.
 */

/**
 * PED: Extension function para registrar múltiples comandos.
 */
suspend fun CommandRegistry.registerCommands(
    commands: List<Command>, 
    pluginId: String? = null
): Result<Int> {
    var successCount = 0
    var lastError: Throwable? = null
    
    commands.forEach { command ->
        registerCommand(command, pluginId).fold(
            onSuccess = { successCount++ },
            onFailure = { lastError = it }
        )
    }
    
    return if (lastError != null && successCount == 0) {
        Result.failure(lastError!!)
    } else {
        Result.success(successCount)
    }
}

/**
 * PED: Extension function para buscar comandos con pattern matching.
 */
fun CommandRegistry.searchCommands(pattern: String): List<Command> {
    val regex = pattern.replace("*", ".*").toRegex(RegexOption.IGNORE_CASE)
    return getAllCommands().values.filter { command ->
        regex.matches(command.metadata.name) ||
        command.metadata.description.contains(pattern, ignoreCase = true) ||
        command.metadata.aliases.any { regex.matches(it) }
    }
}

/**
 * PED: Extension function para verificar si un comando existe.
 */
fun CommandRegistry.hasCommand(name: String): Boolean = findCommand(name) != null

/**
 * PED: Infix function para sintaxis más fluida.
 */
infix fun CommandRegistry.execute(commandLine: String): suspend () -> CommandResult = {
    val parts = commandLine.split(" ")
    val commandName = parts.first()
    val arguments = parts.drop(1)
    executeCommand(commandName, arguments.toCommandContext())
}
