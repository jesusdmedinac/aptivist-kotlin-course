
package com.aptivist.kotlin.mcp.handler

import com.aptivist.kotlin.mcp.protocol.*
import com.aptivist.kotlin.mcp.json.*
import org.slf4j.LoggerFactory

/**
 * 🧑‍🏫: Este archivo demuestra IMPLEMENTATION de McpMessageHandler interface
 * 
 * Conceptos clave:
 * - Interface implementation
 * - Sealed class pattern matching
 * - Functional composition
 * - Error handling strategies
 * - Builder pattern para configuración
 */

/**
 * 🧑‍🏫: DEFAULT IMPLEMENTATION del message handler
 * 
 * Esta clase implementa McpMessageHandler interface y proporciona
 * funcionalidad básica que puede ser extendida o customizada
 */
class DefaultMcpMessageHandler(
    private val serverInfo: ServerInfo,
    private val capabilities: ServerCapabilities
) : McpMessageHandler {
    
    companion object {
        private val logger = LoggerFactory.getLogger(DefaultMcpMessageHandler::class.java)
    }
    
    /**
     * 🧑‍🏫: OVERRIDE de interface method con SUSPEND function
     * 
     * Esta función demuestra:
     * - Pattern matching exhaustivo con when
     * - Async/await pattern
     * - Nullable return types
     */
    override suspend fun handleMessage(message: McpMessage): McpMessage? {
        logger.debug("🔄 Handling message: ${message.getMethodName()}")
        
        /**
         * 🧑‍🏫: WHEN EXPRESSION exhaustivo - cubre todos los casos de sealed class
         * El compilador verifica que no falte ningún caso
         */
        return when (message) {
            is McpMessage.Initialize -> {
                handleInitialize(message)
            }
            is McpMessage.ListResources -> {
                handleListResources(message)
            }
            is McpMessage.ReadResource -> {
                handleReadResource(message)
            }
            // 🧑‍🏫: Result types no necesitan handling aquí - son responses
            is McpMessage.InitializeResult,
            is McpMessage.ListResourcesResult,
            is McpMessage.ReadResourceResult -> {
                logger.debug("📨 Received result message, no response needed")
                null
            }
        }
    }
    
    /**
     * 🧑‍🏫: OVERRIDE de default method implementation
     * Aunque el interface tiene implementación por defecto,
     * podemos customizarla según nuestras necesidades
     */
    override fun canHandle(message: McpMessage): Boolean {
        /**
         * 🧑‍🏫: WHEN EXPRESSION que retorna Boolean
         * Demuestra diferentes patterns de matching
         */
        return when (message) {
            is McpMessage.Initialize -> true
            is McpMessage.ListResources -> true
            is McpMessage.ReadResource -> true
            else -> false // Result messages are handled but don't need processing
        }
    }
    
    /**
     * 🧑‍🏫: PRIVATE SUSPEND FUNCTIONS para cada tipo de mensaje específico
     * Demuestra single responsibility principle y código mantenible
     */
    
    private suspend fun handleInitialize(message: McpMessage.Initialize): McpMessage.InitializeResult {
        logger.info("🚀 Handling MCP initialization from client: ${message.clientInfo.name}")
        
        // TODO Phase 2.5: Validate client capabilities and negotiate features
        logger.debug("🔍 TODO: Validate client capabilities: ${message.capabilities}")
        
        /**
         * 🧑‍🏫: RETURN de data class instance
         * Data classes proporcionan constructor limpio y readable
         */
        return McpMessage.InitializeResult(
            serverInfo = serverInfo,
            capabilities = capabilities
        )
    }
    
    private suspend fun handleListResources(message: McpMessage.ListResources): McpMessage.ListResourcesResult {
        logger.info("📋 Listing available resources")
        
        // TODO Phase 3.1: Implement actual resource discovery
        logger.debug("🔍 TODO: Implement real resource discovery and pagination")
        
        /**
         * 🧑‍🏫: MOCK DATA para demostrar structure
         * En Phase 3.1 esto se reemplazará con discovery real
         */
        val mockResources = listOf(
            Resource(
                uri = "file:///tmp/example.txt",
                name = "Example Text File",
                description = "A sample text file for demonstration",
                mimeType = "text/plain"
            ),
            Resource(
                uri = "file:///tmp/data.json",
                name = "Sample JSON Data", 
                description = "JSON data file with sample information",
                mimeType = "application/json"
            )
        )
        
        return McpMessage.ListResourcesResult(
            resources = mockResources
        )
    }
    
    private suspend fun handleReadResource(message: McpMessage.ReadResource): McpMessage.ReadResourceResult {
        logger.info("📖 Reading resource: ${message.uri}")
        
        // TODO Phase 3.2: Implement actual file/resource reading
        logger.debug("🔍 TODO: Implement secure file reading with permission checks")
        
        /**
         * 🧑‍🏫: MOCK CONTENT basado en URI
         * Demuestra conditional logic y string manipulation
         */
        val mockContent = when {
            message.uri.endsWith(".txt") -> {
                ResourceContent(
                    uri = message.uri,
                    mimeType = "text/plain",
                    text = "This is mock text content for ${message.uri}"
                )
            }
            message.uri.endsWith(".json") -> {
                ResourceContent(
                    uri = message.uri,
                    mimeType = "application/json", 
                    text = """{"message": "Mock JSON content", "uri": "${message.uri}"}"""
                )
            }
            else -> {
                ResourceContent(
                    uri = message.uri,
                    mimeType = "application/octet-stream",
                    text = "Binary content not supported in mock implementation"
                )
            }
        }
        
        return McpMessage.ReadResourceResult(
            contents = listOf(mockContent)
        )
    }
}

/**
 * 🧑‍🏫: BUILDER CLASS para crear DefaultMcpMessageHandler fácilmente
 * 
 * Builder pattern demuestra:
 * - Fluent API design
 * - Method chaining
 * - Default values con customización opcional
 * - Immutable object construction
 */
class McpMessageHandlerBuilder {
    
    /**
     * 🧑‍🏫: MUTABLE PROPERTIES con default values
     * Builder pattern permite construcción step-by-step
     */
    private var serverName: String = "Kotlin MCP Server"
    private var serverVersion: String = "1.0.0"
    private var resourcesEnabled: Boolean = true
    private var toolsEnabled: Boolean = false
    
    /**
     * 🧑‍🏫: FLUENT API methods que retornan 'this'
     * Permite method chaining: builder.serverName("...").serverVersion("...").build()
     */
    fun serverName(name: String) = apply { this.serverName = name }
    fun serverVersion(version: String) = apply { this.serverVersion = version }
    fun enableResources(enabled: Boolean = true) = apply { this.resourcesEnabled = enabled }
    fun enableTools(enabled: Boolean = true) = apply { this.toolsEnabled = enabled }
    
    /**
     * 🧑‍🏫: BUILD method que construye el object final
     * Convierte mutable builder state en immutable result
     */
    fun build(): DefaultMcpMessageHandler {
        val serverInfo = ServerInfo(
            name = serverName,
            version = serverVersion
        )
        
        val capabilities = ServerCapabilities(
            resources = if (resourcesEnabled) ResourcesCapability(subscribe = false, listChanged = false) else null,
            tools = if (toolsEnabled) ToolsCapability(enabled = true) else null
        )
        
        return DefaultMcpMessageHandler(serverInfo, capabilities)
    }
}

/**
 * 🧑‍🏫: CONVENIENCE FUNCTION para crear builder
 * Top-level function que hace la API más accesible
 */
fun mcpMessageHandler(config: McpMessageHandlerBuilder.() -> Unit = {}): DefaultMcpMessageHandler {
    return McpMessageHandlerBuilder().apply(config).build()
}
