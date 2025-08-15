
package com.aptivist.kotlin.http

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory
import com.aptivist.kotlin.mcp.json.JsonSerializer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * PED: WEBSOCKET HANDLER PARA COMUNICACIÓN BIDIRECCIONAL (Phase 2.1)
 * 
 * Esta clase demuestra conceptos avanzados de Kotlin para WebSockets:
 * - **Coroutines**: Manejo asíncrono de múltiples conexiones WebSocket
 * - **Channels**: Comunicación entre coroutines usando canales
 * - **Concurrent Collections**: ConcurrentHashMap para thread-safety
 * - **Atomic Operations**: AtomicLong para contadores thread-safe
 * - **Serialization**: Conversión automática entre objetos Kotlin y JSON
 * - **Exception Handling**: Manejo robusto de errores en contexto asíncrono
 * 
 * WebSockets permiten comunicación bidireccional en tiempo real,
 * ideal para aplicaciones como chat, notificaciones push, o protocolos
 * como MCP que requieren intercambio de mensajes asíncrono.
 */
class WebSocketHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketHandler::class.java)
        
        // PED: AtomicLong para generar IDs únicos de manera thread-safe
        private val connectionIdGenerator = AtomicLong(0)
    }
    
    // PED: ConcurrentHashMap para almacenar conexiones activas de manera thread-safe
    private val activeConnections = ConcurrentHashMap<String, WebSocketConnection>()
    
    /**
     * PED: Data class para representar mensajes WebSocket
     * 
     * - @Serializable: Permite serialización automática con kotlinx.serialization
     * - Data class: Genera equals(), hashCode(), toString() automáticamente
     * - Default parameters: Valores por defecto para parámetros opcionales
     */
    @Serializable
    data class WebSocketMessage(
        val type: String,
        val data: String,
        val timestamp: Long = System.currentTimeMillis(),
        val connectionId: String? = null
    )
    
    /**
     * PED: Data class para representar una conexión WebSocket activa
     * 
     * - WebSocketSession: Interfaz de Ktor para manejar conexiones WebSocket
     * - Channel: Canal para enviar mensajes de manera asíncrona
     * - Coroutines: Cada conexión maneja sus mensajes en coroutines separadas
     */
    data class WebSocketConnection(
        val id: String,
        val session: WebSocketSession,
        val outgoingChannel: Channel<WebSocketMessage> = Channel(Channel.UNLIMITED)
    )
    
    /**
     * PED: Función suspend para manejar una nueva conexión WebSocket
     * 
     * Esta función demuestra:
     * - Suspend functions: Para operaciones I/O asíncronas
     * - Resource management: Proper cleanup de conexiones
     * - Exception handling: Try-catch en contexto de coroutines
     * - Concurrent programming: Múltiples coroutines trabajando juntas
     */
    suspend fun handleConnection(session: WebSocketSession) {
        // PED: Generar ID único para la conexión
        val connectionId = "ws-${connectionIdGenerator.incrementAndGet()}"
        val connection = WebSocketConnection(connectionId, session)
        
        logger.info("🔌 Nueva conexión WebSocket: $connectionId")
        
        try {
            // PED: Registrar la conexión en el mapa thread-safe
            activeConnections[connectionId] = connection
            
            // PED: Enviar mensaje de bienvenida
            sendWelcomeMessage(connection)
            
            // PED: Iniciar coroutines para manejar mensajes entrantes y salientes
            coroutineScope {
                // PED: Launch crea una coroutine que maneja mensajes salientes
                launch { handleOutgoingMessages(connection) }
                
                // PED: Launch crea otra coroutine que maneja mensajes entrantes
                launch { handleIncomingMessages(connection) }
            }
            
        } catch (e: Exception) {
            logger.error("❌ Error en conexión WebSocket $connectionId: ${e.message}", e)
        } finally {
            // PED: Cleanup garantizado - remover conexión del mapa
            activeConnections.remove(connectionId)
            connection.outgoingChannel.close()
            logger.info("🔌 Conexión WebSocket cerrada: $connectionId")
        }
    }
    
    /**
     * PED: Función suspend para manejar mensajes entrantes
     * 
     * - for loop con suspend: Itera sobre mensajes de manera asíncrona
     * - Pattern matching: when expression para diferentes tipos de Frame
     * - Exception handling: Try-catch para parsing de JSON
     */
    private suspend fun handleIncomingMessages(connection: WebSocketConnection) {
        try {
            // PED: incoming es un ReceiveChannel que produce Frame objects
            for (frame in connection.session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("📨 Mensaje recibido en ${connection.id}: $text")
                        
                        try {
                            // PED: Deserializar JSON usando kotlinx.serialization
                            val message = JsonSerializer.jsonConfig.decodeFromString<WebSocketMessage>(text)
                            processIncomingMessage(connection, message)
                        } catch (e: Exception) {
                            logger.warn("⚠️ Error al parsear mensaje JSON: ${e.message}")
                            sendErrorMessage(connection, "Invalid JSON format")
                        }
                    }
                    is Frame.Close -> {
                        logger.info("🔌 Cliente cerró conexión: ${connection.id}")
                        break
                    }
                    else -> {
                        logger.debug("🔍 Frame no soportado: ${frame.frameType}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Error procesando mensajes entrantes: ${e.message}", e)
        }
    }
    
    /**
     * PED: Función suspend para manejar mensajes salientes
     * 
     * - Channel.consumeEach: Consume mensajes del canal de manera asíncrona
     * - Serialization: Convierte objetos Kotlin a JSON automáticamente
     * - WebSocket send: Envía mensajes al cliente de manera asíncrona
     */
    private suspend fun handleOutgoingMessages(connection: WebSocketConnection) {
        try {
            // PED: consumeEach es una extension function para Channel
            connection.outgoingChannel.consumeEach { message ->
                try {
                    // PED: Serializar mensaje a JSON
                    val json = JsonSerializer.jsonConfig.encodeToString(message)
                    
                    // PED: send() es una suspend function que envía al cliente
                    connection.session.send(Frame.Text(json))
                    logger.debug("📤 Mensaje enviado a ${connection.id}: ${message.type}")
                    
                } catch (e: Exception) {
                    logger.error("❌ Error enviando mensaje: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Error en canal de mensajes salientes: ${e.message}", e)
        }
    }
    
    /**
     * PED: Procesa diferentes tipos de mensajes entrantes
     * 
     * - When expression: Pattern matching para diferentes tipos de mensaje
     * - String templates: Interpolación de variables en strings
     * - Function calls: Delegación a funciones especializadas
     */
    private suspend fun processIncomingMessage(connection: WebSocketConnection, message: WebSocketMessage) {
        when (message.type) {
            "ping" -> {
                // PED: Responder a ping con pong
                sendMessage(connection, WebSocketMessage(
                    type = "pong",
                    data = "Server received ping at ${System.currentTimeMillis()}"
                ))
            }
            "echo" -> {
                // PED: Echo del mensaje recibido
                sendMessage(connection, WebSocketMessage(
                    type = "echo_response",
                    data = "Echo: ${message.data}"
                ))
            }
            "broadcast" -> {
                // PED: Broadcast a todas las conexiones activas
                broadcastMessage(WebSocketMessage(
                    type = "broadcast",
                    data = "Broadcast from ${connection.id}: ${message.data}"
                ))
            }
            "get_stats" -> {
                // PED: Enviar estadísticas del servidor
                sendServerStats(connection)
            }
            else -> {
                logger.warn("⚠️ Tipo de mensaje no reconocido: ${message.type}")
                sendErrorMessage(connection, "Unknown message type: ${message.type}")
            }
        }
    }
    
    /**
     * PED: Envía un mensaje a una conexión específica
     * 
     * - Channel.trySend: Envío no bloqueante al canal
     * - Result handling: Manejo del resultado de la operación
     */
    private suspend fun sendMessage(connection: WebSocketConnection, message: WebSocketMessage) {
        val messageWithId = message.copy(connectionId = connection.id)
        val result = connection.outgoingChannel.trySend(messageWithId)
        
        if (result.isFailure) {
            logger.warn("⚠️ No se pudo enviar mensaje a ${connection.id}: ${result.exceptionOrNull()?.message}")
        }
    }
    
    /**
     * PED: Broadcast un mensaje a todas las conexiones activas
     * 
     * - ConcurrentHashMap.values: Acceso thread-safe a todas las conexiones
     * - forEach: Iteración funcional sobre colecciones
     * - Async operations: Envío paralelo a múltiples conexiones
     */
    private suspend fun broadcastMessage(message: WebSocketMessage) {
        logger.info("📢 Broadcasting mensaje a ${activeConnections.size} conexiones")
        
        activeConnections.values.forEach { connection ->
            sendMessage(connection, message)
        }
    }
    
    /**
     * PED: Envía mensaje de bienvenida a nueva conexión
     */
    private suspend fun sendWelcomeMessage(connection: WebSocketConnection) {
        sendMessage(connection, WebSocketMessage(
            type = "welcome",
            data = "Conectado al servidor WebSocket. ID: ${connection.id}"
        ))
    }
    
    /**
     * PED: Envía mensaje de error a una conexión
     */
    private suspend fun sendErrorMessage(connection: WebSocketConnection, error: String) {
        sendMessage(connection, WebSocketMessage(
            type = "error",
            data = error
        ))
    }
    
    /**
     * PED: Envía estadísticas del servidor
     * 
     * - Map creation: Creación de mapas con datos estadísticos
     * - JSON serialization: Conversión automática a JSON
     */
    private suspend fun sendServerStats(connection: WebSocketConnection) {
        val stats = mapOf(
            "active_connections" to activeConnections.size,
            "total_connections" to connectionIdGenerator.get(),
            "server_uptime" to System.currentTimeMillis(),
            "connection_ids" to activeConnections.keys.toList()
        )
        
        sendMessage(connection, WebSocketMessage(
            type = "server_stats",
            data = JsonSerializer.jsonConfig.encodeToString(stats)
        ))
    }
    
    /**
     * PED: Función para obtener estadísticas (útil para monitoring)
     */
    fun getConnectionStats(): Map<String, Any> {
        return mapOf(
            "active_connections" to activeConnections.size,
            "total_connections_created" to connectionIdGenerator.get(),
            "connection_ids" to activeConnections.keys.toList()
        )
    }
}
