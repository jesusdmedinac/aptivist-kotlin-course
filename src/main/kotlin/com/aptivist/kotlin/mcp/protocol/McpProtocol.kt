
package com.aptivist.kotlin.mcp.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 🧑‍🏫: Este archivo demuestra INTERFACES en Kotlin y MCP-specific message types
 * 
 * INTERFACES en Kotlin:
 * - Pueden contener declaraciones de métodos abstractos
 * - Pueden tener implementaciones por defecto (default methods)
 * - Pueden tener propiedades (pero no backing fields)
 * - Una clase puede implementar múltiples interfaces
 */

/**
 * 🧑‍🏫: SEALED CLASS específica para mensajes del protocolo MCP
 * Extiende la funcionalidad JSON-RPC básica con tipos específicos de MCP
 */
@Serializable
sealed class McpMessage {
    
    // 🧑‍🏫: Data classes para diferentes tipos de mensajes MCP
    @Serializable
    data class Initialize(
        val clientInfo: ClientInfo,
        val capabilities: ClientCapabilities? = null
    ) : McpMessage()

    @Serializable
    data class InitializeResult(
        val serverInfo: ServerInfo,
        val capabilities: ServerCapabilities
    ) : McpMessage()

    @Serializable
    data class ListResources(
        val cursor: String? = null
    ) : McpMessage()

    @Serializable
    data class ListResourcesResult(
        val resources: List<Resource>
    ) : McpMessage()

    @Serializable
    data class ReadResource(
        val uri: String
    ) : McpMessage()

    @Serializable
    data class ReadResourceResult(
        val contents: List<ResourceContent>
    ) : McpMessage()
}

/**
 * 🧑‍🏫: DATA CLASSES para estructuras de datos del protocolo MCP
 * Estas clases representan la información intercambiada entre cliente y servidor
 */
@Serializable
data class ClientInfo(
    val name: String,
    val version: String
)

@Serializable  
data class ServerInfo(
    val name: String,
    val version: String
)

@Serializable
data class ClientCapabilities(
    val sampling: SamplingCapability? = null,
    val resources: ResourcesCapability? = null
)

@Serializable
data class ServerCapabilities(
    val resources: ResourcesCapability? = null,
    val tools: ToolsCapability? = null
)

@Serializable
data class SamplingCapability(
    val enabled: Boolean = false
)

@Serializable
data class ResourcesCapability(
    val subscribe: Boolean = false,
    val listChanged: Boolean = false
)

@Serializable
data class ToolsCapability(
    val enabled: Boolean = false
)

@Serializable
data class Resource(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String? = null
)

@Serializable
data class ResourceContent(
    val uri: String,
    val mimeType: String,
    val text: String? = null,
    val blob: String? = null  // Base64 encoded binary data
)

/**
 * 🧑‍🏫: INTERFACE que define el contrato para manejar mensajes MCP
 * Esto demuestra cómo las interfaces proporcionan:
 * - Abstracción: definimos QUÉ se debe hacer, no CÓMO
 * - Polimorfismo: diferentes implementaciones pueden manejar mensajes de diferentes maneras
 * - Testabilidad: podemos crear mocks fácilmente
 */
interface McpMessageHandler {
    
    /**
     * 🧑‍🏫: SUSPEND FUNCTION - función que puede ser pausada y resumida
     * Fundamental para programación asíncrona en Kotlin
     */
    suspend fun handleMessage(message: McpMessage): McpMessage?
    
    /**
     * 🧑‍🏫: Método con implementación por defecto en interface
     * Las implementaciones pueden override este comportamiento
     */
    fun canHandle(message: McpMessage): Boolean = true
}

/**
 * 🧑‍🏫: Extension function que demuestra WHEN EXPRESSION exhaustivo
 * El compilador de Kotlin garantiza que cubramos todos los casos posibles
 * con sealed classes
 */
fun McpMessage.getMethodName(): String = when (this) {
    is McpMessage.Initialize -> "initialize"
    is McpMessage.InitializeResult -> "initialize/result"
    is McpMessage.ListResources -> "resources/list"
    is McpMessage.ListResourcesResult -> "resources/list/result"
    is McpMessage.ReadResource -> "resources/read"
    is McpMessage.ReadResourceResult -> "resources/read/result"
    // 🧑‍🏫: No necesitamos `else` porque sealed class garantiza exhaustividad
}
