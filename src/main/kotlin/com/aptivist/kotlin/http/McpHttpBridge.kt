
package com.aptivist.kotlin.http

import com.aptivist.kotlin.mcp.server.McpServer
import com.aptivist.kotlin.mcp.server.McpServerImpl
import com.aptivist.kotlin.mcp.handler.DefaultMcpMessageHandler
import com.aptivist.kotlin.mcp.protocol.JsonRpcMessage
import com.aptivist.kotlin.mcp.protocol.McpMessage
import com.aptivist.kotlin.mcp.json.JsonSerializer
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory

/**
 * PED: BRIDGE ENTRE SERVIDOR HTTP/WEBSOCKET Y SERVIDOR MCP (Phase 2.1)
 * 
 * Esta clase demuestra patrones avanzados de integración en Kotlin:
 * - **Adapter Pattern**: Adapta la interfaz WebSocket a la interfaz MCP
 * - **Bridge Pattern**: Conecta dos subsistemas diferentes (HTTP y MCP)
 * - **Coroutines Integration**: Integra coroutines de diferentes contextos
 * - **Channel Communication**: Usa channels para comunicación entre componentes
 * - **Exception Handling**: Manejo robusto de errores en múltiples capas
 * - **Resource Management**: Proper lifecycle management de múltiples recursos
 * 
 * Este bridge permite que el servidor MCP (diseñado para stdio/pipes)
 * funcione sobre WebSockets, habilitando comunicación web-based.
 */
class McpHttpBridge(
    private val mcpServer: McpServer = McpServerImpl()
) {
    companion object {
        private val logger = LoggerFactory.getLogger(McpHttpBridge::class.java)
    }
    
    /**
     * PED: Data class para representar una conexión MCP sobre WebSocket
     * 
     * - WebSocketSession: Conexión WebSocket subyacente
     * - Channel: Canales para comunicación bidireccional con MCP server
     * - CoroutineScope: Scope para manejar coroutines de la conexión
     */
    data class McpWebSocketConnection(
        val session: WebSocketSession,
        val incomingChannel: Channel<JsonRpcMessage> = Channel(Channel.UNLIMITED),
        val outgoingChannel: Channel<JsonRpcMessage> = Channel(Channel.UNLIMITED),
        val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    )
    
    /**
     * PED: Función suspend para manejar una conexión MCP sobre WebSocket
     * 
     * Esta función demuestra:
     * - Integration patterns: Conecta WebSocket con MCP server
     * - Coroutine orchestration: Múltiples coroutines trabajando juntas
     * - Exception isolation: Errores en una conexión no afectan otras
     * - Resource cleanup: Proper disposal de recursos al cerrar conexión
     */
    suspend fun handleMcpConnection(session: WebSocketSession) {
        val connection = McpWebSocketConnection(session)
        logger.info("🔗 Nueva conexión MCP sobre WebSocket iniciada")
        
        try {
            // PED: Iniciar el servidor MCP para esta conexión
            mcpServer.start()
            
            // PED: Crear coroutines para manejar la comunicación bidireccional
            connection.scope.launch {
                // PED: Coroutine para manejar mensajes WebSocket -> MCP
                handleWebSocketToMcp(connection)
            }
            
            connection.scope.launch {
                // PED: Coroutine para manejar mensajes MCP -> WebSocket
                handleMcpToWebSocket(connection)
            }
            
            // PED: Coroutine principal que maneja el ciclo de vida de la conexión
            handleConnectionLifecycle(connection)
            
        } catch (e: Exception) {
            logger.error("❌ Error en conexión MCP: ${e.message}", e)
        } finally {
            // PED: Cleanup garantizado
            cleanupConnection(connection)
        }
    }
    
    /**
     * PED: Maneja mensajes desde WebSocket hacia el servidor MCP
     * 
     * - Frame processing: Convierte WebSocket frames a mensajes MCP
     * - JSON deserialization: Parsea JSON a objetos Kotlin type-safe
     * - Error handling: Maneja errores de parsing y comunicación
     */
    private suspend fun handleWebSocketToMcp(connection: McpWebSocketConnection) {
        try {
            // PED: Procesar frames entrantes del WebSocket
            for (frame in connection.session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("📨 WebSocket -> MCP: $text")
                        
                        try {
                            // PED: Deserializar mensaje JSON-RPC
                            val jsonRpcMessage = JsonSerializer.jsonConfig.decodeFromString<JsonRpcMessage>(text)
                            
                            // PED: Enviar al canal de entrada del MCP server
                            connection.incomingChannel.send(jsonRpcMessage)
                            
                        } catch (e: Exception) {
                            logger.warn("⚠️ Error parseando mensaje JSON-RPC: ${e.message}")
                            sendErrorResponse(connection, "Invalid JSON-RPC format: ${e.message}")
                        }
                    }
                    is Frame.Close -> {
                        logger.info("🔌 Cliente cerró conexión WebSocket MCP")
                        break
                    }
                    else -> {
                        logger.debug("🔍 Frame WebSocket no soportado: ${frame.frameType}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Error procesando WebSocket -> MCP: ${e.message}", e)
        }
    }
    
    /**
     * PED: Maneja mensajes desde el servidor MCP hacia WebSocket
     * 
     * - Channel consumption: Consume mensajes del canal de salida MCP
     * - JSON serialization: Convierte objetos Kotlin a JSON
     * - WebSocket sending: Envía mensajes al cliente WebSocket
     */
    private suspend fun handleMcpToWebSocket(connection: McpWebSocketConnection) {
        try {
            // PED: Consumir mensajes del canal de salida del MCP server
            connection.outgoingChannel.consumeEach { message ->
                try {
                    // PED: Serializar mensaje a JSON
                    val json = JsonSerializer.jsonConfig.encodeToString(message)
                    logger.debug("📤 MCP -> WebSocket: $json")
                    
                    // PED: Enviar al cliente WebSocket
                    connection.session.send(Frame.Text(json))
                    
                } catch (e: Exception) {
                    logger.error("❌ Error enviando MCP -> WebSocket: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Error procesando MCP -> WebSocket: ${e.message}", e)
        }
    }
    
    /**
     * PED: Maneja el ciclo de vida de la conexión MCP
     * 
     * Esta función actúa como el "main loop" de la conexión,
     * procesando mensajes MCP y coordinando la comunicación.
     */
    private suspend fun handleConnectionLifecycle(connection: McpWebSocketConnection) {
        try {
            // PED: Procesar mensajes entrantes del canal
            connection.incomingChannel.consumeEach { jsonRpcMessage ->
                try {
                    // PED: Procesar mensaje usando el servidor MCP
                    val response = processMessageWithMcp(jsonRpcMessage)
                    
                    // PED: Si hay respuesta, enviarla de vuelta
                    response?.let { 
                        connection.outgoingChannel.send(it)
                    }
                    
                } catch (e: Exception) {
                    logger.error("❌ Error procesando mensaje MCP: ${e.message}", e)
                    sendErrorResponse(connection, "MCP processing error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Error en ciclo de vida de conexión MCP: ${e.message}", e)
        }
    }
    
    /**
     * PED: Procesa un mensaje usando el servidor MCP
     * 
     * Esta función demuestra cómo integrar el servidor MCP existente
     * con el nuevo sistema de comunicación WebSocket.
     */
    private suspend fun processMessageWithMcp(message: JsonRpcMessage): JsonRpcMessage? {
        return try {
            // PED: Por ahora, crear una respuesta mock
            // En una implementación completa, esto delegaría al McpServer real
            when (message) {
                is JsonRpcMessage.Request -> {
                    logger.info("🔄 Procesando request MCP: ${message.method}")
                    
                    // PED: Crear respuesta basada en el método
                    val result = when (message.method) {
                        "initialize" -> mapOf(
                            "protocolVersion" to "2024-11-05",
                            "capabilities" to mapOf(
                                "logging" to emptyMap<String, Any>(),
                                "prompts" to mapOf("listChanged" to true),
                                "resources" to mapOf("subscribe" to true, "listChanged" to true),
                                "tools" to mapOf("listChanged" to true)
                            ),
                            "serverInfo" to mapOf(
                                "name" to "aptivist-kotlin-mcp-server",
                                "version" to "1.0.0"
                            )
                        )
                        "ping" -> mapOf("status" to "pong")
                        else -> mapOf("status" to "method_not_implemented", "method" to message.method)
                    }
                    
                    JsonRpcMessage.Response(
                        id = message.id,
                        result = result
                    )
                }
                is JsonRpcMessage.Notification -> {
                    logger.info("📢 Procesando notification MCP: ${message.method}")
                    // PED: Las notificaciones no requieren respuesta
                    null
                }
                is JsonRpcMessage.Response -> {
                    logger.info("📨 Recibida respuesta MCP: ${message.id}")
                    // PED: Las respuestas no generan nuevas respuestas
                    null
                }
                is JsonRpcMessage.Error -> {
                    logger.warn("⚠️ Recibido error MCP: ${message.error}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Error procesando mensaje MCP: ${e.message}", e)
            JsonRpcMessage.Error(
                id = when (message) {
                    is JsonRpcMessage.Request -> message.id
                    is JsonRpcMessage.Response -> message.id
                    is JsonRpcMessage.Error -> message.id
                    else -> null
                },
                error = mapOf(
                    "code" to -32603,
                    "message" to "Internal error: ${e.message}"
                )
            )
        }
    }
    
    /**
     * PED: Envía una respuesta de error al cliente
     */
    private suspend fun sendErrorResponse(connection: McpWebSocketConnection, errorMessage: String) {
        try {
            val errorResponse = JsonRpcMessage.Error(
                id = null,
                error = mapOf(
                    "code" to -32700,
                    "message" to errorMessage
                )
            )
            connection.outgoingChannel.send(errorResponse)
        } catch (e: Exception) {
            logger.error("❌ Error enviando respuesta de error: ${e.message}", e)
        }
    }
    
    /**
     * PED: Limpia recursos de la conexión
     * 
     * - Channel cleanup: Cierra canales para evitar memory leaks
     * - Coroutine cleanup: Cancela coroutines activas
     * - MCP server cleanup: Detiene el servidor MCP si es necesario
     */
    private suspend fun cleanupConnection(connection: McpWebSocketConnection) {
        try {
            logger.info("🧹 Limpiando conexión MCP...")
            
            // PED: Cerrar canales
            connection.incomingChannel.close()
            connection.outgoingChannel.close()
            
            // PED: Cancelar scope de coroutines
            connection.scope.cancel("Connection closed")
            
            // PED: Detener servidor MCP (en implementación real, manejar múltiples conexiones)
            mcpServer.stop()
            
            logger.info("✅ Conexión MCP limpiada exitosamente")
            
        } catch (e: Exception) {
            logger.error("❌ Error limpiando conexión MCP: ${e.message}", e)
        }
    }
}

/**
 * PED: FACTORY FUNCTION PARA CREAR BRIDGE CON CONFIGURACIÓN PERSONALIZADA
 * 
 * Esta función demuestra:
 * - Factory pattern: Creación de objetos con configuración
 * - Default parameters: Valores por defecto para parámetros opcionales
 * - Builder pattern: Configuración fluida de objetos complejos
 */
fun createMcpHttpBridge(
    configureServer: (McpServerImpl.() -> Unit)? = null
): McpHttpBridge {
    val mcpServer = McpServerImpl().apply {
        // PED: Configurar servidor MCP con handler por defecto
        val handler = DefaultMcpMessageHandler.Builder()
            .withLogging(true)
            .build()
        
        // PED: Aplicar configuración personalizada si se proporciona
        configureServer?.invoke(this)
    }
    
    return McpHttpBridge(mcpServer)
}
