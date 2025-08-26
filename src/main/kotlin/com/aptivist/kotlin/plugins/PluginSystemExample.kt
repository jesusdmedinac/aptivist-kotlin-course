
package com.aptivist.kotlin.plugins

import com.aptivist.kotlin.plugins.commands.*
import com.aptivist.kotlin.plugins.examples.*
import com.aptivist.kotlin.times
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory

/**
 * 🎯 PLUGIN SYSTEM EXAMPLE - DEMOSTRACIÓN COMPLETA DEL SISTEMA (Phase 2.2)
 * 
 * Esta clase demuestra la integración completa del sistema de plugins,
 * mostrando cómo todos los componentes trabajan juntos en una aplicación real.
 * 
 * CONCEPTOS KOTLIN DEMOSTRADOS:
 * • System integration con múltiples componentes
 * • Coroutine orchestration con múltiples scopes
 * • Flow collection y event handling
 * • Exception handling y error recovery
 * • Resource management y cleanup
 * • Interactive console application
 * • Extension functions para APIs fluidas
 * • Higher-order functions para configuration
 */

/**
 * PED: Clase principal que demuestra el sistema completo de plugins.
 */
class PluginSystemExample {
    
    companion object {
        private val logger = LoggerFactory.getLogger(PluginSystemExample::class.java)
    }
    
    // PED: Componentes principales del sistema
    private val pluginManager = pluginManager {
        pluginDirectory = "plugins"
        maxConcurrentLoads = 3
        loadTimeoutMs = 10_000
        enableHotReload = false
        isolatePlugins = true
    }
    
    val commandRegistry = CommandRegistry()
    
    // PED: Scope para la aplicación con SupervisorJob para error isolation
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("PluginSystemExample")
    )
    
    // PED: Plugins de ejemplo
    val examplePlugins = listOf(
        EchoPlugin(),
        UtilityPlugin()
    )
    
    /**
     * PED: Suspend function para inicializar el sistema completo.
     */
    suspend fun initialize(): Result<Unit> = withContext(applicationScope.coroutineContext) {
        try {
            logger.info("🚀 Inicializando sistema de plugins...")
            
            // PED: Registrar comandos del sistema
            SystemCommands.registerSystemCommands(commandRegistry).fold(
                onSuccess = { count -> 
                    logger.info("✅ Registrados $count comandos del sistema") 
                },
                onFailure = { error -> 
                    logger.error("❌ Error registrando comandos del sistema", error)
                    return@withContext Result.failure(error)
                }
            )
            
            // PED: Cargar y activar plugins de ejemplo
            loadExamplePlugins()
            
            // PED: Configurar monitoring de eventos
            setupEventMonitoring()
            
            logger.info("✅ Sistema de plugins inicializado exitosamente")
            Result.success(Unit)
            
        } catch (e: Exception) {
            logger.error("❌ Error inicializando sistema de plugins", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Suspend function para ejecutar el sistema interactivo.
     */
    suspend fun runInteractiveMode(): Unit = withContext(applicationScope.coroutineContext) {
        logger.info("🎮 Iniciando modo interactivo...")
        
        printWelcomeMessage()
        
        // PED: Loop principal de la aplicación
        while (true) {
            try {
                print("\n🎯 plugin-system> ")
                val input = readlnOrNull()?.trim() ?: break
                
                if (input.isEmpty()) continue
                
                // PED: Comandos especiales del sistema
                when (input.lowercase()) {
                    "exit", "quit", "q" -> {
                        println("👋 Cerrando sistema de plugins...")
                        break
                    }
                    "clear", "cls" -> {
                        // PED: Limpiar pantalla (simulado)
                        repeat(50) { println() }
                        printWelcomeMessage()
                        continue
                    }
                }
                
                // PED: Procesar comando
                processCommand(input)
                
            } catch (e: Exception) {
                logger.error("❌ Error en modo interactivo", e)
                println("❌ Error: ${e.message}")
            }
        }
    }
    
    /**
     * PED: Suspend function para shutdown del sistema.
     */
    suspend fun shutdown(): Unit = withContext(applicationScope.coroutineContext) {
        try {
            logger.info("🛑 Cerrando sistema de plugins...")
            
            // PED: Shutdown plugin manager
            pluginManager.shutdown()
            
            // PED: Limpiar command registry
            commandRegistry.clear()
            
            // PED: Cancelar application scope
            applicationScope.cancel()
            
            logger.info("✅ Sistema de plugins cerrado exitosamente")
            
        } catch (e: Exception) {
            logger.error("❌ Error cerrando sistema", e)
        }
    }
    
    // PED: REGION - Métodos privados auxiliares
    
    /**
     * PED: Private suspend function para cargar plugins de ejemplo.
     */
    private suspend fun loadExamplePlugins() {
        logger.info("📦 Cargando plugins de ejemplo...")
        
        examplePlugins.forEach { plugin ->
            try {
                // PED: Simular carga de plugin (normalmente sería desde JAR)
                logger.info("🔄 Cargando plugin: ${plugin.metadata.name}")
                
                // PED: Inicializar y activar plugin
                plugin.initialize().getOrThrow()
                plugin.activate().getOrThrow()
                
                // PED: Registrar comandos del plugin
                when (plugin) {
                    is EchoPlugin -> {
                        commandRegistry.registerCommands(plugin.getPluginCommands(), plugin.metadata.id)
                    }
                    is UtilityPlugin -> {
                        commandRegistry.registerCommands(plugin.getPluginCommands(), plugin.metadata.id)
                    }
                }
                
                logger.info("✅ Plugin cargado: ${plugin.metadata.name}")
                
            } catch (e: Exception) {
                logger.error("❌ Error cargando plugin ${plugin.metadata.name}", e)
            }
        }
    }
    
    /**
     * PED: Private function para configurar monitoring de eventos.
     */
    private fun setupEventMonitoring() {
        // PED: Monitorear eventos del plugin manager
        applicationScope.launch {
            pluginManager.events.collect { event ->
                when (event) {
                    is PluginManagerEvent.PluginLoaded -> {
                        logger.info("📦 Plugin cargado: ${event.plugin.metadata.name}")
                    }
                    is PluginManagerEvent.PluginUnloaded -> {
                        logger.info("🗑️ Plugin descargado: ${event.pluginId}")
                    }
                    is PluginManagerEvent.PluginError -> {
                        logger.error("❌ Error en plugin ${event.pluginId}", event.error)
                    }
                    is PluginManagerEvent.DependencyResolved -> {
                        logger.debug("🔗 Dependencia resuelta: ${event.pluginId} -> ${event.dependencyId}")
                    }
                }
            }
        }
    }
    
    /**
     * PED: Private suspend function para procesar comandos.
     */
    private suspend fun processCommand(input: String) {
        val parts = input.split(" ")
        val commandName = parts.first()
        val arguments = parts.drop(1)
        
        val context = arguments.toCommandContext()
        
        // PED: Ejecutar comando y mostrar resultado
        val result = commandRegistry.executeCommand(commandName, context)
        
        when (result) {
            is CommandResult.Success -> {
                println(result.output)
                if (result.data.isNotEmpty()) {
                    println("📊 Datos adicionales: ${result.data}")
                }
            }
            is CommandResult.Error -> {
                println("❌ Error: ${result.message}")
                result.cause?.let { 
                    logger.debug("Causa del error", it)
                }
            }
            is CommandResult.Help -> {
                println(result.helpText)
            }
        }
    }
    
    /**
     * PED: Private function para mostrar mensaje de bienvenida.
     */
    private fun printWelcomeMessage() {
        println("""
            🎯 SISTEMA DE PLUGINS MCP KOTLIN
            ================================
            
            ✨ Conceptos Kotlin demostrados:
            • Plugin architecture con interfaces y abstract classes
            • Command pattern con sealed classes y DSL
            • Concurrent programming con coroutines y flows
            • Thread-safe collections y synchronization
            • Extension functions y higher-order functions
            • Error handling funcional con Result<T>
            • Resource management y lifecycle handling
            
            📋 Comandos disponibles:
            • help                 - Muestra ayuda general
            • version              - Información de versión
            • status               - Estado del sistema
            • config               - Configuración del sistema
            • echo <mensaje>       - Hace echo de un mensaje
            • datetime [formato]   - Fecha y hora actual
            • random <tipo> [args] - Generación aleatoria
            • ls [directorio]      - Lista archivos
            • text <op> <texto>    - Procesamiento de texto
            • calc <n1> <op> <n2>  - Calculadora simple
            
            💡 Escribe 'help <comando>' para ayuda específica
            💡 Escribe 'exit' para salir
        """.trimIndent())
    }
}

/**
 * PED: Extension functions para PluginSystemExample.
 */

/**
 * PED: Extension function para obtener estadísticas del sistema.
 */
fun PluginSystemExample.getSystemStatistics(): Map<String, Any> {
    return mapOf(
        "plugins_loaded" to examplePlugins.size,
        "commands_registered" to commandRegistry.getAllCommands().size,
        "system_uptime" to System.currentTimeMillis(),
        "memory_used" to (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
    )
}

/**
 * PED: Main function para ejecutar el ejemplo.
 */
suspend fun main() {
    val system = PluginSystemExample()
    
    try {
        // PED: Inicializar sistema
        system.initialize().fold(
            onSuccess = {
                println("✅ Sistema inicializado exitosamente")
                
                // PED: Ejecutar modo interactivo
                system.runInteractiveMode()
            },
            onFailure = { error ->
                println("❌ Error inicializando sistema: ${error.message}")
                error.printStackTrace()
            }
        )
        
    } finally {
        // PED: Cleanup garantizado
        system.shutdown()
    }
}

/**
 * PED: Function para ejecutar ejemplo programático (sin interacción).
 */
suspend fun runProgrammaticExample() {
    val system = PluginSystemExample()
    
    try {
        system.initialize().getOrThrow()
        
        // PED: Ejecutar algunos comandos de ejemplo
        val registry = system.commandRegistry
        
        println("🧪 EJECUTANDO EJEMPLOS PROGRAMÁTICOS")
        println("=" * 40)
        
        // PED: Comando version
        val versionResult = registry.executeCommand("version", emptyList<String>().toCommandContext())
        println("Version: $versionResult")
        
        // PED: Comando echo
        val echoResult = registry.executeCommand("echo", listOf("Hola", "desde", "Kotlin!").toCommandContext())
        println("Echo: $echoResult")
        
        // PED: Comando datetime
        val dateResult = registry.executeCommand("datetime", listOf("iso").toCommandContext())
        println("DateTime: $dateResult")
        
        // PED: Comando random
        val randomResult = registry.executeCommand("random", listOf("int", "1", "100").toCommandContext())
        println("Random: $randomResult")
        
        // PED: Comando calc
        val calcResult = registry.executeCommand("calc", listOf("15", "*", "7").toCommandContext())
        println("Calc: $calcResult")
        
        println("\n✅ Ejemplos programáticos completados")
        
    } catch (e: Exception) {
        println("❌ Error en ejemplo programático: ${e.message}")
        e.printStackTrace()
    } finally {
        system.shutdown()
    }
}
