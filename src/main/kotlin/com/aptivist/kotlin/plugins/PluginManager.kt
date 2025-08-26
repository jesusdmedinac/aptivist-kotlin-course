
package com.aptivist.kotlin.plugins

import com.aptivist.kotlin.mcp.protocol.McpMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 🎛️ PLUGIN MANAGER - SISTEMA DE GESTIÓN DE PLUGINS (Phase 2.2)
 * 
 * Esta clase implementa el patrón Manager para gestionar el ciclo de vida de plugins,
 * demostrando conceptos avanzados de Kotlin como coroutines, concurrent programming,
 * y dynamic class loading.
 * 
 * CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Class con constructor primario y propiedades
 * • Concurrent programming con ConcurrentHashMap y AtomicLong
 * • Coroutines con CoroutineScope y SupervisorJob
 * • Flow para streams reactivos y hot streams
 * • Extension functions para APIs fluidas
 * • Sealed classes para type-safe state management
 * • Higher-order functions para callbacks y configuración
 * • Dynamic class loading con URLClassLoader
 * • Exception handling con Result<T>
 */

/**
 * PED: Data class para configuración del PluginManager.
 * Demuestra el uso de default parameters y validation.
 */
data class PluginManagerConfig(
    var pluginDirectory: String = "plugins",
    var maxConcurrentLoads: Int = 5,
    var loadTimeoutMs: Long = 30_000,
    var enableHotReload: Boolean = false,
    var isolatePlugins: Boolean = true
) {
    init {
        // PED: Validation en el init block
        require(maxConcurrentLoads > 0) { "maxConcurrentLoads debe ser mayor que 0" }
        require(loadTimeoutMs > 0) { "loadTimeoutMs debe ser mayor que 0" }
    }
}

/**
 * PED: Sealed class para eventos del PluginManager.
 */
sealed class PluginManagerEvent {
    data class PluginLoaded(val plugin: Plugin) : PluginManagerEvent()
    data class PluginUnloaded(val pluginId: String) : PluginManagerEvent()
    data class PluginError(val pluginId: String, val error: Throwable) : PluginManagerEvent()
    data class DependencyResolved(val pluginId: String, val dependencyId: String) : PluginManagerEvent()
}

/**
 * PED: Clase principal que gestiona plugins usando concurrent programming y coroutines.
 */
class PluginManager(
    private val config: PluginManagerConfig = PluginManagerConfig(),
    private val parentScope: CoroutineScope = GlobalScope
) {
    
    // PED: Companion object para logger estático y constantes
    companion object {
        private val logger = LoggerFactory.getLogger(PluginManager::class.java)
        private const val PLUGIN_INTERFACE_NAME = "com.aptivist.kotlin.plugins.Plugin"
    }
    
    // PED: ConcurrentHashMap para thread-safe storage de plugins
    private val plugins = ConcurrentHashMap<String, Plugin>()
    private val pluginStates = ConcurrentHashMap<String, PluginState>()
    private val pluginClassLoaders = ConcurrentHashMap<String, URLClassLoader>()
    
    // PED: AtomicLong para thread-safe counters
    private val loadedPluginsCount = AtomicLong(0)
    private val activePluginsCount = AtomicLong(0)
    
    // PED: SupervisorJob para error isolation entre plugins
    private val supervisorJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val managerScope = CoroutineScope(parentScope.coroutineContext + supervisorJob + CoroutineName("PluginManager"))
    
    // PED: MutableSharedFlow para hot stream de eventos
    private val _events = MutableSharedFlow<PluginManagerEvent>(
        replay = 10, // PED: Mantener los últimos 10 eventos para nuevos suscriptores
        extraBufferCapacity = 100 // PED: Buffer adicional para evitar suspensión
    )
    
    /**
     * PED: Flow público para suscribirse a eventos del manager.
     * asSharedFlow() convierte el MutableSharedFlow en un SharedFlow inmutable.
     */
    val events: SharedFlow<PluginManagerEvent> = _events.asSharedFlow()
    
    /**
     * PED: Propiedad computada que retorna una vista inmutable de los plugins.
     * Map.toMap() crea una copia inmutable del ConcurrentHashMap.
     */
    val loadedPlugins: Map<String, Plugin>
        get() = plugins.toMap()
    
    /**
     * PED: Extension property usando custom getter para estadísticas.
     */
    val statistics: PluginManagerStatistics
        get() = PluginManagerStatistics(
            totalPlugins = plugins.size,
            activePlugins = activePluginsCount.get().toInt(),
            loadedPlugins = loadedPluginsCount.get().toInt(),
            failedPlugins = pluginStates.values.count { it is PluginState.Failed }
        )
    
    /**
     * PED: Suspend function para cargar un plugin desde archivo JAR.
     * Demuestra dynamic class loading y error handling con Result<T>.
     */
    suspend fun loadPlugin(jarFile: File): Result<Plugin> = withContext(Dispatchers.IO) {
        try {
            logger.info("🔄 Cargando plugin desde: ${jarFile.absolutePath}")
            
            // PED: Validación de archivo
            if (!jarFile.exists() || !jarFile.canRead()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Archivo JAR no existe o no es legible: ${jarFile.absolutePath}")
                )
            }
            
            // PED: Crear ClassLoader aislado si está habilitado
            val classLoader = if (config.isolatePlugins) {
                URLClassLoader(arrayOf(jarFile.toURI().toURL()), this::class.java.classLoader)
            } else {
                this::class.java.classLoader
            }
            
            // PED: Buscar clases que implementen Plugin usando reflection
            val pluginClass = findPluginClass(classLoader, jarFile)
                ?: return@withContext Result.failure(
                    ClassNotFoundException("No se encontró implementación de Plugin en ${jarFile.name}")
                )
            
            // PED: Instanciar plugin usando reflection
            val plugin = pluginClass.getDeclaredConstructor().newInstance() as Plugin
            
            // PED: Verificar dependencias antes de registrar
            val dependencyCheck = verifyDependencies(plugin)
            if (dependencyCheck.isFailure) {
                return@withContext dependencyCheck
            }
            
            // PED: Registrar plugin y actualizar estado
            plugins[plugin.metadata.id] = plugin
            pluginStates[plugin.metadata.id] = PluginState.Loaded
            
            if (config.isolatePlugins) {
                pluginClassLoaders[plugin.metadata.id] = classLoader as URLClassLoader
            }
            
            loadedPluginsCount.incrementAndGet()
            
            // PED: Emitir evento usando trySend para no suspender
            _events.tryEmit(PluginManagerEvent.PluginLoaded(plugin))
            
            logger.info("✅ Plugin cargado exitosamente: ${plugin.metadata.name} v${plugin.metadata.version}")
            Result.success(plugin)
            
        } catch (e: Exception) {
            logger.error("❌ Error cargando plugin desde ${jarFile.absolutePath}", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Suspend function para cargar todos los plugins de un directorio.
     * Demuestra parallel processing con async y awaitAll.
     */
    suspend fun loadPluginsFromDirectory(directory: File = File(config.pluginDirectory)): Result<List<Plugin>> {
        return try {
            if (!directory.exists() || !directory.isDirectory) {
                logger.warn("⚠️ Directorio de plugins no existe: ${directory.absolutePath}")
                return Result.success(emptyList())
            }
            
            val jarFiles = directory.listFiles { _, name -> name.endsWith(".jar") } ?: emptyArray()
            logger.info("🔍 Encontrados ${jarFiles.size} archivos JAR en ${directory.absolutePath}")
            
            // PED: Usar async para carga paralela con límite de concurrencia
            val semaphore = kotlinx.coroutines.sync.Semaphore(config.maxConcurrentLoads)
            
            val loadResults = jarFiles.map { jarFile ->
                managerScope.async {
                    semaphore.withPermit {
                        withTimeout(config.loadTimeoutMs) {
                            loadPlugin(jarFile)
                        }
                    }
                }
            }.awaitAll()
            
            // PED: Filtrar resultados exitosos usando mapNotNull y getOrNull
            val successfulPlugins = loadResults.mapNotNull { it.getOrNull() }
            
            logger.info("📦 Cargados ${successfulPlugins.size} de ${jarFiles.size} plugins")
            Result.success(successfulPlugins)
            
        } catch (e: Exception) {
            logger.error("❌ Error cargando plugins del directorio", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Suspend function para activar un plugin específico.
     */
    suspend fun activatePlugin(pluginId: String): Result<Unit> {
        val plugin = plugins[pluginId] 
            ?: return Result.failure(IllegalArgumentException("Plugin no encontrado: $pluginId"))
        
        return try {
            pluginStates[pluginId] = PluginState.Loading
            
            // PED: Inicializar y activar plugin
            plugin.initialize().getOrThrow()
            plugin.activate().getOrThrow()
            
            pluginStates[pluginId] = PluginState.Active
            activePluginsCount.incrementAndGet()
            
            logger.info("🚀 Plugin activado: ${plugin.metadata.name}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            pluginStates[pluginId] = PluginState.Failed(e.message ?: "Error desconocido", e)
            _events.tryEmit(PluginManagerEvent.PluginError(pluginId, e))
            logger.error("❌ Error activando plugin $pluginId", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Suspend function para descargar un plugin y limpiar recursos.
     */
    suspend fun unloadPlugin(pluginId: String): Result<Unit> {
        val plugin = plugins[pluginId] 
            ?: return Result.failure(IllegalArgumentException("Plugin no encontrado: $pluginId"))
        
        return try {
            // PED: Shutdown plugin si está activo
            if (pluginStates[pluginId] == PluginState.Active) {
                plugin.deactivate().getOrThrow()
                plugin.shutdown().getOrThrow()
                activePluginsCount.decrementAndGet()
            }
            
            // PED: Limpiar recursos
            plugins.remove(pluginId)
            pluginStates.remove(pluginId)
            
            // PED: Cerrar ClassLoader si existe
            pluginClassLoaders.remove(pluginId)?.close()
            
            loadedPluginsCount.decrementAndGet()
            _events.tryEmit(PluginManagerEvent.PluginUnloaded(pluginId))
            
            logger.info("🗑️ Plugin descargado: ${plugin.metadata.name}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            logger.error("❌ Error descargando plugin $pluginId", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Suspend function para procesar un mensaje a través de todos los plugins activos.
     * Demuestra processing pipeline con filter, map y firstNotNullOfOrNull.
     */
    suspend fun processMessage(message: McpMessage): Result<McpMessage?> {
        return try {
            // PED: Filtrar plugins activos que pueden manejar el mensaje
            val capablePlugins = plugins.values
                .filter { pluginStates[it.metadata.id] == PluginState.Active }
                .filter { it.canHandle(message::class.java) }
            
            if (capablePlugins.isEmpty()) {
                return Result.success(null) // PED: Ningún plugin puede manejar el mensaje
            }
            
            // PED: Procesar mensaje con el primer plugin que lo maneje exitosamente
            val result = capablePlugins.firstNotNullOfOrNull { plugin ->
                try {
                    plugin.handleMessage(message).getOrNull()
                } catch (e: Exception) {
                    logger.warn("⚠️ Plugin ${plugin.metadata.id} falló procesando mensaje", e)
                    null
                }
            }
            
            Result.success(result)
            
        } catch (e: Exception) {
            logger.error("❌ Error procesando mensaje", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Function para shutdown del manager y cleanup de recursos.
     */
    suspend fun shutdown() {
        logger.info("🛑 Iniciando shutdown del PluginManager...")
        
        // PED: Descargar todos los plugins
        plugins.keys.toList().forEach { pluginId ->
            unloadPlugin(pluginId).onFailure { error ->
                logger.warn("⚠️ Error descargando plugin $pluginId durante shutdown", error)
            }
        }
        
        // PED: Cancelar scope y cerrar recursos
        supervisorJob.cancel()
        pluginClassLoaders.values.forEach { it.close() }
        
        logger.info("✅ PluginManager shutdown completado")
    }
    
    // PED: REGION - Métodos privados auxiliares
    
    /**
     * PED: Private function para encontrar clase Plugin usando reflection.
     */
    private fun findPluginClass(classLoader: ClassLoader, jarFile: File): Class<*>? {
        return try {
            // PED: Esta es una implementación simplificada
            // En un sistema real, se usaría ASM o similar para escanear el JAR
            val jarUrl = jarFile.toURI().toURL()
            val tempClassLoader = URLClassLoader(arrayOf(jarUrl), classLoader)
            
            // PED: Por simplicidad, asumimos que hay una clase conocida
            // En la implementación real, se escanearían todas las clases del JAR
            null // PED: Placeholder para implementación completa
            
        } catch (e: Exception) {
            logger.debug("Error escaneando JAR para clases Plugin", e)
            null
        }
    }
    
    /**
     * PED: Private function para verificar dependencias del plugin.
     */
    private fun verifyDependencies(plugin: Plugin): Result<Plugin> {
        val missingDependencies = plugin.metadata.dependencies.filter { depId ->
            !plugins.containsKey(depId)
        }
        
        return if (missingDependencies.isEmpty()) {
            Result.success(plugin)
        } else {
            Result.failure(
                IllegalStateException("Dependencias faltantes: ${missingDependencies.joinToString()}")
            )
        }
    }
}

/**
 * PED: Data class para estadísticas del PluginManager.
 */
data class PluginManagerStatistics(
    val totalPlugins: Int,
    val activePlugins: Int,
    val loadedPlugins: Int,
    val failedPlugins: Int
) {
    val successRate: Double
        get() = if (totalPlugins > 0) (activePlugins.toDouble() / totalPlugins) * 100 else 0.0
}

/**
 * PED: Extension function para crear PluginManager con DSL.
 */
fun pluginManager(
    scope: CoroutineScope = GlobalScope,
    configure: PluginManagerConfig.() -> Unit = {}
): PluginManager {
    val config = PluginManagerConfig().apply(configure)
    return PluginManager(config, scope)
}
