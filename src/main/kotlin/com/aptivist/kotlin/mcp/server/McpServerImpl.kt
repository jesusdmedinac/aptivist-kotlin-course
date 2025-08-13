
package com.aptivist.kotlin.mcp.server

import com.aptivist.kotlin.mcp.protocol.*
import com.aptivist.kotlin.mcp.json.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.LoggerFactory

/**
 * 🧑‍🏫: Este archivo demuestra IMPLEMENTACIÓN CONCRETA de interfaces abstractas
 * 
 * Conceptos demostrados:
 * - Implementación de interfaces y abstract classes
 * - Coroutines y Flow para programación asíncrona
 * - Channels para comunicación between coroutines
 * - Composition over inheritance
 * - Logging integration
 * - Error handling patterns
 */

/**
 * 🧑‍🏫: CONCRETE IMPLEMENTATION del servidor MCP
 * 
 * Hereda de BaseMcpServer (abstract class) e implementa métodos abstractos
 * Demuestra cómo las abstract classes proporcionan estructura común
 * mientras permiten customización específica
 */
class McpServerImpl(
    config: McpServerConfig,
    messageHandler: McpMessageHandler
) : BaseMcpServer(config, messageHandler) {
    
    /**
     * 🧑‍🏫: COMPANION OBJECT para logging - patrón estático en Kotlin
     * Similar a static en Java pero más poderoso
     */
    companion object {
        private val logger = LoggerFactory.getLogger(McpServerImpl::class.java)
    }
    
    /**
     * 🧑‍🏫: COROUTINE SCOPE para manejar todas las operaciones asíncronas
     * - SupervisorJob: si una coroutine falla, no cancela las demás
     * - Dispatchers.Default: pool de threads optimizado para CPU-intensive tasks
     */
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    /**
     * 🧑‍🏫: CHANNEL - comunicación type-safe between coroutines
     * - Channel.UNLIMITED: buffer ilimitado (cuidado con memory leaks)
     * - Alternative: Channel.BUFFERED, Channel.RENDEZVOUS, etc.
     */
    private val messageChannel = Channel<JsonRpcMessage>(Channel.UNLIMITED)
    
    /**
     * 🧑‍🏫: MUTABLE LIST para tracking de conexiones activas
     * En implementación real esto sería thread-safe (ConcurrentHashMap, etc.)
     */
    private val activeConnections = mutableListOf<McpConnection>()
    
    /**
     * 🧑‍🏫: Implementation de método abstracto de BaseMcpServer
     * Override keyword es obligatorio cuando implementamos método de parent class
     */
    override suspend fun startServer(port: Int) {
        logger.info("🚀 Starting MCP Server on port $port")
        logger.info("📋 Server config: ${config.serverName} v${config.serverVersion}")
        
        /**
         * 🧑‍🏫: COROUTINE LAUNCH - inicia nueva coroutine en serverScope
         * Launch vs async:
         * - launch: para fire-and-forget operations
         * - async: cuando necesitas return value
         */
        serverScope.launch {
            // TODO Phase 2.1: Implementar Ktor WebSocket server real
            logger.info("📡 Server listening on port $port (mock implementation)")
            
            // TODO Phase 2.2: Setup WebSocket endpoint para MCP protocol
            setupWebSocketEndpoint(port)
        }
        
        // Start message processing coroutine
        serverScope.launch {
            processMessages()
        }
    }
    
    override suspend fun stopServer() {
        logger.info("🛑 Stopping MCP Server")
        
        // Close all active connections
        activeConnections.forEach { connection ->
            try {
                connection.close()
            } catch (e: Exception) {
                logger.warn("⚠️ Error closing connection: ${e.message}")
            }
        }
        activeConnections.clear()
        
        // Close message channel
        messageChannel.close()
        
        logger.info("✅ MCP Server stopped")
    }
    
    /**
     * 🧑‍🏫: FLOW implementation - reactive stream of messages
     * 
     * receiveAsFlow() convierte Channel en Flow:
     * - Flow es cold stream (lazy)
     * - Channel es hot stream (eager)
     * - Flow provides backpressure handling automáticamente
     */
    override fun messageFlow(): Flow<JsonRpcMessage> {
        return messageChannel.receiveAsFlow()
    }
    
    /**
     * 🧑‍🏫: Implementation de mensaje sending
     * Demuestra uso de Channel.send() para comunicación asíncrona
     */
    override suspend fun sendMessage(message: JsonRpcMessage) {
        /**
         * 🧑‍🏫: TRY-CATCH for error handling en suspend functions
         * runCatching {} es alternativa más funcional
         */
        try {
            messageChannel.send(message)
            logger.debug("📤 Message sent: ${message.getMessageType()}")
        } catch (e: Exception) {
            logger.error("❌ Failed to send message: ${e.message}")
        }
    }
    
    /**
     * 🧑‍🏫: PRIVATE SUSPEND FUNCTION - método auxiliar asíncrono
     * 
     * TODO para Phase 2.1: Implementar WebSocket real con Ktor
     */
    private suspend fun setupWebSocketEndpoint(port: Int) {
        // TODO Phase 2.1: Replace with actual Ktor implementation
        logger.info("🔧 TODO: Setup actual WebSocket endpoint using Ktor")
        logger.info("📝 Current: Mock WebSocket server on port $port")
        
        // Mock connection simulation
        val mockConnection = MockMcpConnection()
        activeConnections.add(mockConnection)
        
        // TODO Phase 2.2: Handle real WebSocket connections
        // embeddedServer(Netty, port = port) {
        //     install(WebSockets)
        //     routing {
        //         webSocket("/mcp") {
        //             handleMcpConnection(this)
        //         }
        //     }
        // }.start(wait = false)
    }
    
    /**
     * 🧑‍🏫: SUSPEND FUNCTION que demuestra message processing loop
     * Esta función muestra cómo usar Flow.collect para procesar streams
     */
    private suspend fun processMessages() {
        logger.info("🔄 Starting message processing loop")
        
        /**
         * 🧑‍🏫: FLOW.COLLECT - consume messages from stream
         * collect es terminal operation que procesa cada elemento del flow
         */
        messageFlow().collect { message ->
            /**
             * 🧑‍🏫: WHEN EXPRESSION con sealed class - type-safe pattern matching
             * El compilador garantiza exhaustividad (cubrir todos los casos)
             */
            when (message) {
                is JsonRpcMessage.Request -> {
                    logger.debug("📨 Processing request: ${message.method}")
                    handleJsonRpcRequest(message)
                }
                is JsonRpcMessage.Response -> {
                    logger.debug("📬 Processing response for ID: ${message.id}")
                    handleJsonRpcResponse(message)
                }
            }
        }
    }
    
    /**
     * 🧑‍🏫: PRIVATE SUSPEND FUNCTIONS para diferentes tipos de mensajes
     * Demuestra separación de responsabilidades y single responsibility principle
     */
    private suspend fun handleJsonRpcRequest(request: JsonRpcMessage.Request) {
        // TODO Phase 2.3: Implement proper request routing based on method
        logger.debug("🎯 TODO: Route request method '${request.method}' to appropriate handler")
        
        /**
         * 🧑‍🏫: WHEN EXPRESSION para routing básico de métodos
         * En Phase 2.3 esto se expandirá con handlers específicos
         */
        when (request.method) {
            "initialize" -> {
                // TODO: Handle MCP initialization
                logger.info("🔗 TODO: Handle MCP initialize request")
            }
            "resources/list" -> {
                // TODO: Handle resource listing
                logger.info("📋 TODO: Handle resources/list request")
            }
            "resources/read" -> {
                // TODO: Handle resource reading
                logger.info("📖 TODO: Handle resources/read request")
            }
            else -> {
                logger.warn("❓ Unknown method: ${request.method}")
                // TODO Phase 2.3: Send method_not_found error response
            }
        }
    }
    
    private suspend fun handleJsonRpcResponse(response: JsonRpcMessage.Response) {
        // TODO Phase 2.4: Implement response correlation and callback handling
        logger.debug("✅ TODO: Correlate response ${response.id} with pending request")
        
        /**
         * 🧑‍🏫: NULL SAFETY demonstration con response properties
         */
        when {
            response.result != null -> {
                logger.info("✅ Success response for ${response.id}")
            }
            response.error != null -> {
                logger.error("❌ Error response for ${response.id}: ${response.error.message}")
            }
            else -> {
                logger.warn("⚠️ Invalid response format for ${response.id}")
            }
        }
    }
}

/**
 * 🧑‍🏫: MOCK IMPLEMENTATION para testing y development
 * 
 * Esta clase demuestra:
 * - Implementation de interface
 * - Mocking patterns para development
 * - Placeholder implementation antes de integración real
 */
class MockMcpConnection : McpConnection {
    
    private var connected = true
    
    override suspend fun send(message: String) {
        // TODO Phase 2.1: Replace with real WebSocket send
        println("📤 MOCK: Sending message: ${message.take(100)}...")
    }
    
    override suspend fun receive(): String? {
        // TODO Phase 2.1: Replace with real WebSocket receive
        return null // Mock: no messages for now
    }
    
    override fun close() {
        connected = false
        println("🔌 MOCK: Connection closed")
    }
    
    override val isConnected: Boolean
        get() = connected
}
