
package com.aptivist.kotlin.http

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import com.aptivist.kotlin.mcp.json.JsonSerializer
import java.time.Duration

/**
 * PED: SERVIDOR HTTP CON KTOR (Phase 2.1)
 * 
 * Esta clase demuestra conceptos avanzados de Kotlin aplicados a servidores web:
 * - **Coroutines**: Servidor completamente asíncrono usando suspend functions
 * - **Extension Functions**: Configuración modular usando Application.() -> Unit
 * - **DSL**: Routing DSL de Ktor para definir endpoints de manera declarativa
 * - **Companion Objects**: Logger estático y configuración compartida
 * - **Object Expressions**: Configuración inline de plugins
 * - **Higher-Order Functions**: Callbacks para lifecycle del servidor
 * 
 * Ktor es un framework web asíncrono construido específicamente para Kotlin,
 * que aprovecha las coroutines para manejar miles de conexiones concurrentes
 * de manera eficiente sin bloquear threads.
 */
class KtorServer(
    private val port: Int = 8080,
    private val host: String = "0.0.0.0"
) {
    companion object {
        // PED: Logger estático usando companion object
        private val logger = LoggerFactory.getLogger(KtorServer::class.java)
        
        // PED: Configuración JSON reutilizando el JsonSerializer del proyecto
        private val jsonConfig = JsonSerializer.jsonConfig
    }
    
    // PED: Nullable property para el servidor - demuestra null safety
    private var server: NettyApplicationEngine? = null
    
    // PED: WebSocket handler para manejar conexiones WebSocket
    private val webSocketHandler = WebSocketHandler()
    
    // PED: MCP bridge para integrar servidor MCP con WebSockets
    private val mcpBridge = createMcpHttpBridge()
    
    /**
     * PED: Función suspend que inicia el servidor de manera asíncrona
     * 
     * - suspend: Permite que la función sea pausada y reanudada
     * - embeddedServer: Factory function de Ktor que crea un servidor embebido
     * - configure: Lambda con receiver (Application.() -> Unit) para configuración
     */
    suspend fun start(): KtorServer {
        logger.info("🚀 Iniciando servidor Ktor en $host:$port...")
        
        // PED: embeddedServer es una Higher-Order Function que recibe una lambda
        server = embeddedServer(Netty, port = port, host = host) {
            // PED: Este bloque es una lambda con receiver (Application.() -> Unit)
            // Dentro de este scope, 'this' es una instancia de Application
            configurePlugins()
            configureRouting()
        }
        
        // PED: start() es una suspend function que no bloquea el thread
        server?.start(wait = false)
        logger.info("✅ Servidor Ktor iniciado exitosamente en http://$host:$port")
        
        return this
    }
    
    /**
     * PED: Función suspend para detener el servidor de manera elegante
     * 
     * - gracePeriodMillis: Tiempo para completar requests en progreso
     * - timeoutMillis: Tiempo máximo antes de forzar el cierre
     */
    suspend fun stop() {
        logger.info("🛑 Deteniendo servidor Ktor...")
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
        server = null
        logger.info("✅ Servidor Ktor detenido exitosamente")
    }
    
    /**
     * PED: Extension function para Application que configura plugins
     * 
     * Esta función demuestra:
     * - Extension Functions: Extiende Application sin herencia
     * - DSL: Cada install { } es un DSL para configurar plugins
     * - Object Expressions: Configuración inline usando object : Plugin
     */
    private fun Application.configurePlugins() {
        // PED: ContentNegotiation plugin para serialización automática JSON
        install(ContentNegotiation) {
            // PED: Usa nuestro JsonSerializer configurado en Phase 1.3
            json(jsonConfig)
        }
        
        // PED: CORS plugin para permitir requests desde el browser
        install(CORS) {
            // PED: Lambda con receiver para configurar CORS
            anyHost() // PED: En producción, especificar hosts permitidos
            allowHeader("Content-Type")
            allowMethod(io.ktor.http.HttpMethod.Options)
            allowMethod(io.ktor.http.HttpMethod.Get)
            allowMethod(io.ktor.http.HttpMethod.Post)
        }
        
        // PED: CallLogging plugin para logging automático de requests
        install(CallLogging) {
            // PED: Configuración usando DSL
            level = org.slf4j.event.Level.INFO
        }
        
        // PED: WebSockets plugin para comunicación bidireccional en tiempo real
        install(WebSockets) {
            // PED: Configuración de timeouts para WebSocket connections
            pingPeriod = Duration.ofSeconds(15) // Ping cada 15 segundos
            timeout = Duration.ofSeconds(15)    // Timeout de 15 segundos
            maxFrameSize = Long.MAX_VALUE       // Tamaño máximo de frame
            masking = false                     // Desactivar masking para mejor performance
        }
    }
    
    /**
     * PED: Extension function para Application que configura routing
     * 
     * Demuestra:
     * - Routing DSL: Sintaxis declarativa para definir endpoints
     * - Suspend Functions: Handlers que pueden hacer I/O asíncrono
     * - String Templates: Interpolación de variables en responses
     */
    private fun Application.configureRouting() {
        routing {
            // PED: GET endpoint básico - demuestra routing DSL
            get("/") {
                // PED: call.respond es una suspend function
                call.respond(mapOf(
                    "message" to "🎓 Servidor Ktor funcionando!",
                    "phase" to "2.1",
                    "framework" to "Ktor",
                    "language" to "Kotlin"
                ))
            }
            
            // PED: Health check endpoint - patrón común en microservicios
            get("/health") {
                call.respond(mapOf(
                    "status" to "healthy",
                    "timestamp" to System.currentTimeMillis(),
                    "server" to "ktor-netty"
                ))
            }
            
            // PED: Info endpoint que demuestra acceso a Application properties
            get("/info") {
                call.respond(mapOf(
                    "host" to host,
                    "port" to port,
                    "environment" to (environment.config.propertyOrNull("ktor.environment")?.getString() ?: "development"),
                    "websocket_stats" to webSocketHandler.getConnectionStats()
                ))
            }
            
            // PED: WEBSOCKET ENDPOINTS - Comunicación bidireccional en tiempo real
            
            // PED: WebSocket endpoint principal para comunicación MCP
            webSocket("/ws") {
                // PED: Este bloque se ejecuta para cada nueva conexión WebSocket
                // 'this' es una WebSocketSession que representa la conexión
                webSocketHandler.handleConnection(this)
            }
            
            // PED: WebSocket endpoint específico para protocolo MCP
            webSocket("/mcp") {
                // PED: Bridge que conecta WebSocket con el servidor MCP
                // Permite que clientes MCP se conecten vía WebSocket
                mcpBridge.handleMcpConnection(this)
            }
            
            // PED: WebSocket endpoint para testing y desarrollo
            webSocket("/ws/test") {
                logger.info("🧪 Nueva conexión WebSocket de testing")
                
                try {
                    // PED: Enviar mensaje de bienvenida
                    send("¡Conectado al endpoint de testing!")
                    
                    // PED: Echo server simple para testing
                    for (frame in incoming) {
                        when (frame) {
                            is io.ktor.websocket.Frame.Text -> {
                                val text = frame.readText()
                                logger.debug("📨 Test message: $text")
                                send("Echo: $text")
                            }
                            is io.ktor.websocket.Frame.Close -> {
                                logger.info("🔌 Test connection closed")
                                break
                            }
                            else -> {
                                logger.debug("🔍 Unsupported frame type in test endpoint")
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("❌ Error in test WebSocket: ${e.message}", e)
                }
            }
        }
    }
}

/**
 * PED: FUNCIÓN DE UTILIDAD PARA TESTING Y DESARROLLO
 * 
 * Esta función demuestra:
 * - Top-level functions: Funciones fuera de clases
 * - Suspend functions: Para operaciones asíncronas
 * - Exception handling: Try-catch con coroutines
 */
suspend fun startKtorServerExample() {
    val server = KtorServer(port = 8080)
    
    try {
        server.start()
        
        // PED: delay es una suspend function que no bloquea threads
        println("🌐 Servidor corriendo en http://localhost:8080")
        println("📋 HTTP Endpoints disponibles:")
        println("   GET / - Mensaje de bienvenida")
        println("   GET /health - Health check")
        println("   GET /info - Información del servidor (incluye stats WebSocket)")
        println("🔌 WebSocket Endpoints disponibles:")
        println("   WS /ws - Endpoint general para comunicación WebSocket")
        println("   WS /mcp - Endpoint específico para protocolo MCP")
        println("   WS /ws/test - Endpoint de testing (echo server)")
        println("⏹️  Presiona Ctrl+C para detener...")
        
        // PED: En un ejemplo real, aquí esperaríamos una señal para detener
        delay(Long.MAX_VALUE) // Mantiene el servidor corriendo
        
    } catch (e: Exception) {
        println("❌ Error al iniciar servidor: ${e.message}")
    } finally {
        server.stop()
    }
}
