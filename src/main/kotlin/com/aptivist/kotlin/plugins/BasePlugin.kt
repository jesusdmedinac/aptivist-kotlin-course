
package com.aptivist.kotlin.plugins

import com.aptivist.kotlin.mcp.protocol.McpMessage
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory

/**
 * 🏗️ BASE PLUGIN - IMPLEMENTACIÓN ABSTRACTA REUTILIZABLE (Phase 2.2)
 * 
 * Esta clase abstracta proporciona una implementación base para plugins,
 * demostrando el patrón Template Method y programación orientada a objetos avanzada.
 * 
 * CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Abstract classes con implementación parcial
 * • Template Method Pattern para reutilización de código
 * • MutableStateFlow para state management reactivo
 * • Lazy initialization con thread safety
 * • Protected members para herencia controlada
 * • Override de propiedades abstractas
 * • Exception handling con logging estructurado
 */

/**
 * PED: Abstract class que implementa la interfaz Plugin con funcionalidad común.
 * Las abstract classes permiten combinar implementación concreta con métodos abstractos.
 */
abstract class BasePlugin(
    override val metadata: PluginMetadata
) : Plugin {
    
    // PED: Companion object para logger compartido entre todas las instancias
    companion object {
        private val logger = LoggerFactory.getLogger(BasePlugin::class.java)
    }
    
    // PED: MutableStateFlow para state management reactivo y thread-safe
    private val _currentState = MutableStateFlow<PluginState>(PluginState.Unloaded)
    override val currentState: PluginState get() = _currentState.value
    
    // PED: MutableSharedFlow para eventos del plugin
    private val _events = MutableSharedFlow<PluginEvent>(
        replay = 5, // PED: Mantener los últimos 5 eventos
        extraBufferCapacity = 50
    )
    override val events: Flow<PluginEvent> = _events.asSharedFlow()
    
    // PED: Lazy initialization para recursos costosos
    protected val pluginLogger by lazy { 
        LoggerFactory.getLogger("Plugin.${metadata.id}")
    }
    
    // PED: Protected property para que las subclases puedan acceder
    protected var isInitialized: Boolean = false
        private set // PED: Setter privado para encapsulación
    
    /**
     * PED: Template Method Pattern - método final que define el flujo de inicialización.
     * Las subclases implementan onInitialize() pero no pueden cambiar el flujo general.
     */
    final override suspend fun initialize(): Result<Unit> {
        return try {
            if (isInitialized) {
                return Result.success(Unit)
            }
            
            updateState(PluginState.Loading)
            pluginLogger.info("🔄 Inicializando plugin: ${metadata.name}")
            
            // PED: Llamar al método abstracto que implementan las subclases
            onInitialize().getOrThrow()
            
            isInitialized = true
            updateState(PluginState.Loaded)
            pluginLogger.info("✅ Plugin inicializado exitosamente: ${metadata.name}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            val errorState = PluginState.Failed("Error durante inicialización", e)
            updateState(errorState)
            emitEvent(PluginEvent.ErrorOccurred(metadata.id, e))
            pluginLogger.error("❌ Error inicializando plugin ${metadata.name}", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Template Method Pattern para activación.
     */
    final override suspend fun activate(): Result<Unit> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("Plugin debe ser inicializado antes de activar"))
            }
            
            if (currentState == PluginState.Active) {
                return Result.success(Unit)
            }
            
            pluginLogger.info("🚀 Activando plugin: ${metadata.name}")
            
            // PED: Llamar método abstracto de la subclase
            onActivate().getOrThrow()
            
            updateState(PluginState.Active)
            pluginLogger.info("✅ Plugin activado: ${metadata.name}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            val errorState = PluginState.Failed("Error durante activación", e)
            updateState(errorState)
            emitEvent(PluginEvent.ErrorOccurred(metadata.id, e))
            pluginLogger.error("❌ Error activando plugin ${metadata.name}", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Template Method Pattern para desactivación.
     */
    final override suspend fun deactivate(): Result<Unit> {
        return try {
            if (currentState != PluginState.Active) {
                return Result.success(Unit)
            }
            
            pluginLogger.info("⏸️ Desactivando plugin: ${metadata.name}")
            
            onDeactivate().getOrThrow()
            
            updateState(PluginState.Loaded)
            pluginLogger.info("✅ Plugin desactivado: ${metadata.name}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            emitEvent(PluginEvent.ErrorOccurred(metadata.id, e))
            pluginLogger.error("❌ Error desactivando plugin ${metadata.name}", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Template Method Pattern para shutdown.
     */
    final override suspend fun shutdown(): Result<Unit> {
        return try {
            pluginLogger.info("🛑 Cerrando plugin: ${metadata.name}")
            
            // PED: Desactivar primero si está activo
            if (currentState == PluginState.Active) {
                deactivate().getOrThrow()
            }
            
            onShutdown().getOrThrow()
            
            isInitialized = false
            updateState(PluginState.Unloaded)
            pluginLogger.info("✅ Plugin cerrado: ${metadata.name}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            emitEvent(PluginEvent.ErrorOccurred(metadata.id, e))
            pluginLogger.error("❌ Error cerrando plugin ${metadata.name}", e)
            Result.failure(e)
        }
    }
    
    /**
     * PED: Implementación default del manejo de mensajes.
     * Las subclases pueden override este método para funcionalidad específica.
     */
    override suspend fun handleMessage(message: McpMessage): Result<McpMessage?> {
        return try {
            if (currentState != PluginState.Active) {
                return Result.failure(IllegalStateException("Plugin no está activo"))
            }
            
            if (!canHandle(message::class.java)) {
                return Result.success(null)
            }
            
            pluginLogger.debug("📨 Procesando mensaje: ${message::class.simpleName}")
            
            val result = onHandleMessage(message)
            
            if (result.isSuccess) {
                emitEvent(PluginEvent.MessageReceived(metadata.id, message))
            }
            
            result
            
        } catch (e: Exception) {
            emitEvent(PluginEvent.ErrorOccurred(metadata.id, e))
            pluginLogger.error("❌ Error procesando mensaje", e)
            Result.failure(e)
        }
    }
    
    // PED: REGION - Métodos abstractos que deben implementar las subclases
    
    /**
     * PED: Método abstracto para inicialización específica del plugin.
     */
    protected abstract suspend fun onInitialize(): Result<Unit>
    
    /**
     * PED: Método abstracto para activación específica del plugin.
     */
    protected abstract suspend fun onActivate(): Result<Unit>
    
    /**
     * PED: Método abstracto para desactivación específica del plugin.
     */
    protected abstract suspend fun onDeactivate(): Result<Unit>
    
    /**
     * PED: Método abstracto para shutdown específico del plugin.
     */
    protected abstract suspend fun onShutdown(): Result<Unit>
    
    /**
     * PED: Método abstracto para manejo de mensajes específico.
     */
    protected abstract suspend fun onHandleMessage(message: McpMessage): Result<McpMessage?>
    
    // PED: REGION - Métodos auxiliares protegidos
    
    /**
     * PED: Protected method para actualizar estado y emitir evento.
     */
    protected fun updateState(newState: PluginState) {
        val oldState = _currentState.value
        _currentState.value = newState
        emitEvent(PluginEvent.StateChanged(metadata.id, oldState, newState))
    }
    
    /**
     * PED: Protected method para emitir eventos.
     */
    protected fun emitEvent(event: PluginEvent) {
        _events.tryEmit(event)
    }

    fun logWithContext(level: String, message: String, throwable: Throwable? = null) {
        val contextMessage = "[${metadata.id}] $message"
        when (level.uppercase()) {
            "DEBUG" -> pluginLogger.debug(contextMessage, throwable)
            "INFO" -> pluginLogger.info(contextMessage, throwable)
            "WARN" -> pluginLogger.warn(contextMessage, throwable)
            "ERROR" -> pluginLogger.error(contextMessage, throwable)
        }
    }
}

/**
 * PED: Extension functions para BasePlugin que proporcionan APIs fluidas.
 */

/**
 * PED: Extension function para logging más fluido.
 */
fun BasePlugin.logInfo(message: String) = logWithContext("INFO", message)
fun BasePlugin.logWarn(message: String, throwable: Throwable? = null) = logWithContext("WARN", message, throwable)
fun BasePlugin.logError(message: String, throwable: Throwable? = null) = logWithContext("ERROR", message, throwable)
fun BasePlugin.logDebug(message: String) = logWithContext("DEBUG", message)

/**
 * PED: Extension function para verificar si el plugin está en un estado específico.
 */
fun BasePlugin.isInState(state: PluginState): Boolean = currentState == state
fun BasePlugin.isActive(): Boolean = isInState(PluginState.Active)
fun BasePlugin.isLoaded(): Boolean = isInState(PluginState.Loaded)
fun BasePlugin.isFailed(): Boolean = currentState is PluginState.Failed
