
package com.aptivist.kotlin.plugins

import com.aptivist.kotlin.mcp.protocol.McpMessage
import kotlinx.coroutines.flow.Flow

/**
 * 🔌 INTERFAZ PLUGIN - SISTEMA DE EXTENSIBILIDAD (Phase 2.2)
 * 
 * Esta interfaz define el contrato que deben cumplir todos los plugins del sistema MCP.
 * Demuestra conceptos avanzados de Kotlin como interfaces, sealed classes, y Flow.
 * 
 * CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Interfaces con propiedades y métodos abstractos
 * • Data classes para metadatos inmutables
 * • Sealed classes para representar estados type-safe
 * • Flow para streams reactivos de eventos
 * • Suspend functions para operaciones asíncronas
 * • Result<T> para manejo funcional de errores
 */

/**
 * PED: Data class que encapsula metadatos inmutables del plugin.
 * Las data classes proporcionan automáticamente equals(), hashCode(), toString() y copy().
 */
data class PluginMetadata(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val dependencies: List<String> = emptyList(), // PED: Default parameter para lista vacía
    val capabilities: Set<PluginCapability> = emptySet() // PED: Set para evitar duplicados
)

/**
 * PED: Sealed class que representa las capacidades que puede tener un plugin.
 * Las sealed classes permiten pattern matching exhaustivo y type safety.
 */
sealed class PluginCapability {
    object MessageHandling : PluginCapability()
    object ResourceProviding : PluginCapability()
    object ToolExecution : PluginCapability()
    data class CustomCapability(val name: String) : PluginCapability()
}

/**
 * PED: Sealed class para representar el estado del plugin de manera type-safe.
 * Esto es mejor que usar enums porque permite asociar datos específicos a cada estado.
 */
sealed class PluginState {
    object Unloaded : PluginState()
    object Loading : PluginState()
    object Loaded : PluginState()
    object Active : PluginState()
    object Error : PluginState()
    data class Failed(val reason: String, val exception: Throwable? = null) : PluginState()
}

/**
 * PED: Data class para eventos del plugin usando sealed class para type safety.
 */
sealed class PluginEvent {
    data class StateChanged(val pluginId: String, val oldState: PluginState, val newState: PluginState) : PluginEvent()
    data class MessageReceived(val pluginId: String, val message: McpMessage) : PluginEvent()
    data class ErrorOccurred(val pluginId: String, val error: Throwable) : PluginEvent()
}

/**
 * PED: Interfaz principal que define el contrato para todos los plugins.
 * Las interfaces en Kotlin pueden tener propiedades abstractas y métodos con implementación default.
 */
interface Plugin {
    /**
     * PED: Propiedad abstracta que debe ser implementada por cada plugin.
     * Las interfaces pueden declarar propiedades que deben ser proporcionadas por las implementaciones.
     */
    val metadata: PluginMetadata
    
    /**
     * PED: Propiedad abstracta para el estado actual del plugin.
     * Usar sealed classes permite type-safe state management.
     */
    val currentState: PluginState
    
    /**
     * PED: Flow para emitir eventos del plugin de manera reactiva.
     * Flow es la API de Kotlin para streams asíncronos y reactivos.
     */
    val events: Flow<PluginEvent>
    
    /**
     * PED: Suspend function para inicializar el plugin de manera asíncrona.
     * Las suspend functions pueden ser pausadas y reanudadas sin bloquear threads.
     * 
     * @return Result<Unit> para manejo funcional de errores sin exceptions
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * PED: Suspend function para activar el plugin después de la inicialización.
     */
    suspend fun activate(): Result<Unit>
    
    /**
     * PED: Suspend function para desactivar el plugin temporalmente.
     */
    suspend fun deactivate(): Result<Unit>
    
    /**
     * PED: Suspend function para limpiar recursos y detener el plugin.
     */
    suspend fun shutdown(): Result<Unit>
    
    /**
     * PED: Método para verificar si el plugin puede manejar un tipo específico de mensaje.
     * Usa reified generics para type checking en runtime.
     */
    fun <T : McpMessage> canHandle(messageType: Class<T>): Boolean
    
    /**
     * PED: Suspend function para procesar mensajes MCP.
     * Retorna Result<McpMessage?> donde null indica que el mensaje no fue procesado.
     */
    suspend fun handleMessage(message: McpMessage): Result<McpMessage?>
    
    /**
     * PED: Método default con implementación en la interfaz.
     * Las interfaces en Kotlin pueden tener métodos con implementación default.
     */
    fun isCompatibleWith(otherPlugin: Plugin): Boolean {
        // PED: Implementación default que verifica compatibilidad básica
        return !metadata.dependencies.contains(otherPlugin.metadata.id) ||
               metadata.version.isCompatibleWith(otherPlugin.metadata.version)
    }
}

/**
 * PED: Extension function para String que verifica compatibilidad de versiones.
 * Las extension functions permiten añadir funcionalidad a tipos existentes.
 */
private fun String.isCompatibleWith(otherVersion: String): Boolean {
    // PED: Implementación simple de compatibilidad semántica
    val thisParts = this.split(".").map { it.toIntOrNull() ?: 0 }
    val otherParts = otherVersion.split(".").map { it.toIntOrNull() ?: 0 }
    
    // PED: Usar zip para combinar las listas y comparar elemento por elemento
    return thisParts.zip(otherParts).all { (thisVer, otherVer) ->
        thisVer >= otherVer
    }
}
