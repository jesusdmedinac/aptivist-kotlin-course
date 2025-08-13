
package com.aptivist.kotlin.mcp.examples

import com.aptivist.kotlin.mcp.server.*
import com.aptivist.kotlin.mcp.handler.*
import com.aptivist.kotlin.mcp.protocol.*
import com.aptivist.kotlin.mcp.json.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * 🧑‍🏫: Este archivo demuestra USAGE EXAMPLES del servidor MCP
 * 
 * Conceptos pedagógicos:
 * - Main function y application entry point
 * - Coroutines para async execution
 * - Object composition y dependency injection
 * - Real-world usage patterns
 * - Flow processing y reactive streams
 */

/**
 * 🧑‍🏫: MAIN FUNCTION - entry point de la aplicación Kotlin
 * 
 * suspend fun main() permite usar coroutines directamente en main
 * Sin suspend necesitaríamos runBlocking {}
 */
suspend fun main() {
    val logger = LoggerFactory.getLogger("McpServerExample")
    
    logger.info("🚀 Starting MCP Server Example")
    
    /**
     * 🧑‍🏫: OBJECT COMPOSITION - construyendo dependencies
     * Demuestra dependency injection manual y builder pattern
     */
    val config = DefaultMcpServerConfig(
        serverName = "Kotlin Learning MCP Server",
        serverVersion = "1.0.0-phase-1.3",
        maxConnections = 5,
        timeoutMillis = 30_000L
    )
    
    /**
     * 🧑‍🏫: BUILDER PATTERN usage con DSL-style configuration
     * Demuestra functional configuration y fluent API
     */
    val messageHandler = mcpMessageHandler {
        serverName("Kotlin Learning MCP Server")
        serverVersion("1.0.0-phase-1.3")
        enableResources(true)
        enableTools(false) // TODO Phase 4: Enable tools
    }
    
    /**
     * 🧑‍🏫: INSTANCE CREATION con constructor injection
     * Demuestra composition over inheritance
     */
    val server = McpServerImpl(config, messageHandler)
    
    /**
     * 🧑‍🏫: COROUTINE SCOPE para manejar lifecycle de la aplicación
     * runBlocking bloquea hasta que todas las coroutines terminen
     */
    runBlocking {
        // Start server
        server.start(port = 8080)
        
        /**
         * 🧑‍🏫: PARALLEL COROUTINES con launch
         * Lanzamos múltiples coroutines que corren concurrentemente
         */
        
        // Launch message monitoring coroutine
        launch {
            monitorServerMessages(server)
        }
        
        // Launch example message sender
        launch {
            sendExampleMessages(server)
        }
        
        // Wait for user input to stop
        launch {
            println("💡 Press Enter to stop the server...")
            readln()
            server.stop()
            logger.info("👋 Server stopped by user")
        }
    }
    
    logger.info("✅ MCP Server Example completed")
}

/**
 * 🧑‍🏫: SUSPEND FUNCTION que demuestra FLOW processing
 * 
 * Flow.collect procesa mensajes de manera reactiva
 * try-catch maneja exceptions en coroutines
 */
private suspend fun monitorServerMessages(server: McpServer) {
    val logger = LoggerFactory.getLogger("MessageMonitor")
    
    try {
        /**
         * 🧑‍🏫: FLOW.COLLECT - terminal operation para procesar stream
         * Cada mensaje que llega se procesa asincrónamente
         */
        server.messageFlow().collect { message ->
            /**
             * 🧑‍🏫: WHEN EXPRESSION con sealed class
             * Smart cast permite acceso type-safe a propiedades específicas
             */
            when (message) {
                is JsonRpcMessage.Request -> {
                    logger.info("📨 Request: ${message.method} ${if (message.isNotification()) "(notification)" else "(id: ${message.id})"}")
                }
                is JsonRpcMessage.Response -> {
                    val status = if (message.error == null) "✅ success" else "❌ error: ${message.error.message}"
                    logger.info("📬 Response [${message.id}]: $status")
                }
            }
        }
    } catch (e: Exception) {
        logger.error("❌ Error monitoring messages: ${e.message}")
    }
}

/**
 * 🧑‍🏫: SUSPEND FUNCTION que demuestra message creation y sending
 * 
 * Demuestra:
 * - Data class instantiation
 * - Delay para simulation
 * - Error handling con try-catch
 */
private suspend fun sendExampleMessages(server: McpServer) {
    val logger = LoggerFactory.getLogger("ExampleSender")
    
    /**
     * 🧑‍🏫: DELAY - suspend function para simular tiempo real
     * No bloquea thread, permite que otras coroutines corran
     */
    delay(2000) // Wait for server to be ready
    
    logger.info("📤 Sending example JSON-RPC messages...")
    
    try {
        /**
         * 🧑‍🏫: DATA CLASS INSTANTIATION con named parameters
         * Named parameters hacen el código más readable y maintainable
         */
        val initRequest = JsonRpcMessage.Request(
            method = "initialize",
            params = null, // TODO Phase 2.5: Add actual initialization params  
            id = "init-001"
        )
        
        server.sendMessage(initRequest)
        delay(1000)
        
        val listResourcesRequest = JsonRpcMessage.Request(
            method = "resources/list",
            params = null,
            id = "list-001"
        )
        
        server.sendMessage(listResourcesRequest)
        delay(1000)
        
        val readResourceRequest = JsonRpcMessage.Request(
            method = "resources/read", 
            params = null, // TODO Phase 3.2: Add URI parameter
            id = "read-001"
        )
        
        server.sendMessage(readResourceRequest)
        
    } catch (e: Exception) {
        logger.error("❌ Error sending example messages: ${e.message}")
    }
}

/**
 * 🧑‍🏫: DEMONSTRATION function para JSON serialization
 * 
 * Esta función muestra:
 * - Extension function usage
 * - Result handling con fold()
 * - Practical JSON serialization examples
 */
fun demonstrateJsonSerialization() {
    val logger = LoggerFactory.getLogger("JsonDemo")
    
    logger.info("🔧 Demonstrating JSON Serialization...")
    
    /**
     * 🧑‍🏫: DATA CLASS creation con constructor conciso
     */
    val request = JsonRpcMessage.Request(
        method = "initialize",
        id = "demo-001"
    )
    
    /**
     * 🧑‍🏫: EXTENSION FUNCTION usage - toJson() se ve como método
     * Result<T> handling con fold() función higher-order
     */
    request.toJson().fold(
        onSuccess = { json -> logger.info("✅ Serialized JSON: $json") },
        onFailure = { error -> logger.error("❌ Serialization failed: ${error.message}") }
    )
    
    /**
     * 🧑‍🏫: String extension function para parsing
     */
    val jsonString = """{"jsonrpc":"2.0","method":"test","id":"demo"}"""
    
    jsonString.parseJsonRpcRequest().fold(
        onSuccess = { parsed -> logger.info("✅ Parsed request: ${parsed.method}") },
        onFailure = { error -> logger.error("❌ Parsing failed: ${error.message}") }
    )
}
