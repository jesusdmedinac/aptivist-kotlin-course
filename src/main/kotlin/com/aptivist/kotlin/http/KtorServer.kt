

package com.aptivist.kotlin.http

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import com.aptivist.kotlin.mcp.json.JsonSerializer
import com.aptivist.kotlin.http.api.*
import java.time.Duration
import java.util.*

/**
 * PED: SERVIDOR HTTP AVANZADO CON KTOR (Phase 3.2)
 * 
 * Esta versión actualizada del servidor demuestra conceptos avanzados de APIs REST:
 * - **Plugin System**: Configuración modular usando el sistema de plugins de Ktor
 * - **Error Handling**: Manejo centralizado de errores con StatusPages
 * - **Content Negotiation**: Serialización automática bidireccional JSON
 * - **Middleware**: Compresión, CORS, logging, headers automáticos
 * - **Performance**: Optimizaciones de cache y compresión
 * - **Observability**: Logging estructurado y métricas
 * - **Security**: Headers de seguridad y CORS configurado
 * 
 * PED: Ktor utiliza un sistema de plugins que permite composición modular:
 * - Cada plugin se instala con install(Plugin) { config }
 * - Los plugins se ejecutan en orden de instalación
 * - Permiten interceptar y modificar requests/responses
 * - Facilitan testing al poder instalar plugins selectivamente
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
        logger.info("🚀 Iniciando servidor Ktor avanzado en $host:$port...")
        
        // PED: embeddedServer es una Higher-Order Function que recibe una lambda
        server = embeddedServer(Netty, port = port, host = host) {
            // PED: Este bloque es una lambda con receiver (Application.() -> Unit)
            // Dentro de este scope, 'this' es una instancia de Application
            configurePlugins()
            configureRouting()
            configureApiRoutes() // Nueva función para API REST
        }
        
        // PED: start() es una suspend function que no bloquea el thread
        server?.start(wait = false)
        logger.info("✅ Servidor Ktor avanzado iniciado exitosamente en http://$host:$port")
        
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
     * PED: Extension function para Application que configura plugins avanzados
     * 
     * Esta función demuestra:
     * - Plugin System: Instalación modular de funcionalidades
     * - Error Handling: Manejo centralizado con StatusPages
     * - Performance: Compresión y cache headers
     * - Security: CORS y headers de seguridad
     * - Observability: Logging estructurado
     */
    private fun Application.configurePlugins() {
        
        // PED: CONTENT NEGOTIATION - Serialización automática JSON
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        
        // PED: STATUS PAGES - Manejo centralizado de errores
        install(StatusPages) {
            // PED: Manejo específico de ApiError
            exception<ApiError> { call, cause ->
                logger.warn("API Error: ${cause.message}", cause)
                
                val errorResponse = cause.toErrorResponse(
                    path = call.request.path(),
                    requestId = call.request.headers["X-Request-ID"] ?: UUID.randomUUID().toString()
                )
                
                call.respond(cause.httpStatus, errorResponse)
            }
            
            // PED: Manejo de errores de validación de contenido
            exception<kotlinx.serialization.SerializationException> { call, cause ->
                logger.warn("Serialization error: ${cause.message}", cause)
                
                val apiError = ApiError.ValidationError(
                    message = "Invalid JSON format: ${cause.message}",
                    field = "request_body"
                )
                
                val errorResponse = apiError.toErrorResponse(
                    path = call.request.path(),
                    requestId = call.request.headers["X-Request-ID"] ?: UUID.randomUUID().toString()
                )
                
                call.respond(apiError.httpStatus, errorResponse)
            }
            
            // PED: Manejo de errores genéricos
            exception<Throwable> { call, cause ->
                logger.error("Unhandled exception: ${cause.message}", cause)
                
                val apiError = ApiError.InternalServerError(
                    errorId = UUID.randomUUID().toString()
                )
                
                val errorResponse = apiError.toErrorResponse(
                    path = call.request.path(),
                    requestId = call.request.headers["X-Request-ID"] ?: UUID.randomUUID().toString()
                )
                
                call.respond(apiError.httpStatus, errorResponse)
            }
            
            // PED: Manejo de códigos de estado HTTP específicos
            status(HttpStatusCode.NotFound) { call, status ->
                val apiError = ApiError.NotFoundError(
                    resourceType = "endpoint",
                    resourceId = call.request.path(),
                    message = "Endpoint not found"
                )
                
                val errorResponse = apiError.toErrorResponse(
                    path = call.request.path(),
                    requestId = call.request.headers["X-Request-ID"] ?: UUID.randomUUID().toString()
                )
                
                call.respond(status, errorResponse)
            }
        }
        
        // PED: COMPRESSION - Compresión automática de responses
        install(Compression) {
            gzip {
                priority = 1.0
                // PED: Comprimir solo responses grandes
                minimumSize(1024)
                // PED: Excluir tipos de contenido ya comprimidos
                excludeContentType(ContentType.Image.Any)
                excludeContentType(ContentType.Video.Any)
                excludeContentType(ContentType.Audio.Any)
            }
            deflate {
                priority = 10.0
                minimumSize(1024)
            }
        }
        
        // PED: CACHING HEADERS - Headers de cache automáticos
        install(CachingHeaders) {
            // PED: Cache para recursos estáticos
            options { call, outgoingContent ->
                when (outgoingContent.contentType?.withoutParameters()) {
                    ContentType.Application.Json -> CachingOptions(
                        cacheControl = CacheControl.NoCache(null)
                    )
                    else -> null
                }
            }
        }
        
        // PED: CONDITIONAL HEADERS - Headers condicionales (ETag, Last-Modified)
        install(ConditionalHeaders)
        
        // PED: DEFAULT HEADERS - Headers por defecto para seguridad
        install(DefaultHeaders) {
            header("X-Engine", "Ktor")
            header("X-Framework", "Kotlin")
            header("X-Phase", "3.2")
            // PED: Headers de seguridad básicos
            header("X-Content-Type-Options", "nosniff")
            header("X-Frame-Options", "DENY")
            header("X-XSS-Protection", "1; mode=block")
        }
        
        // PED: CALL LOGGING - Logging estructurado de requests
        install(CallLogging) {
            level = Level.INFO
            format { call ->
                val status = call.response.status()
                val httpMethod = call.request.httpMethod.value
                val uri = call.request.uri
                val userAgent = call.request.headers["User-Agent"]
                val requestId = call.request.headers["X-Request-ID"] ?: "unknown"
                
                "[$requestId] $httpMethod $uri -> $status (User-Agent: $userAgent)"
            }
            
            // PED: Filtrar endpoints de health check para reducir ruido
            filter { call ->
                !call.request.path().startsWith("/api/v1/health/")
            }
        }
        
        // PED: CORS - Configuración avanzada de CORS
        install(CORS) {
            // PED: En desarrollo, permitir cualquier host
            anyHost()
            
            // PED: Headers permitidos
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowHeader("X-Request-ID")
            allowHeader("X-Client-Version")
            
            // PED: Métodos HTTP permitidos
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            
            // PED: Permitir credentials para autenticación
            allowCredentials = true
            
            // PED: Tiempo de cache para preflight requests
            maxAgeInSeconds = 3600
        }
        
        // PED: WEBSOCKETS - Configuración avanzada de WebSockets
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
            
            // PED: Configuración de extensiones WebSocket
            extensions {
                install(WebSocketDeflateExtension)
            }
        }
    }
    
    /**
     * PED: Extension function para Application que configura routing básico
     * 
     * Mantiene los endpoints básicos mientras que las rutas de API REST
     * se configuran por separado para mejor organización.
     */
    private fun Application.configureRouting() {
        routing {
            // PED: Endpoint raíz con información del servidor
            get("/") {
                call.respond(mapOf(
                    "message" to "🎓 Servidor Ktor API REST funcionando!",
                    "phase" to "3.2",
                    "framework" to "Ktor",
                    "language" to "Kotlin",
                    "features" to listOf(
                        "REST API",
                        "WebSockets",
                        "Error Handling",
                        "Compression",
                        "CORS",
                        "Caching",
                        "Logging"
                    ),
                    "endpoints" to mapOf(
                        "api" to "/api/v1",
                        "health" to "/api/v1/health",
                        "websocket" to "/ws",
                        "mcp" to "/mcp"
                    ),
                    "timestamp" to System.currentTimeMillis()
                ))
            }
            
            // PED: Health check endpoint básico
            get("/health") {
                call.respond(mapOf(
                    "status" to "healthy",
                    "timestamp" to System.currentTimeMillis(),
                    "server" to "ktor-netty",
                    "version" to "3.2"
                ))
            }
            
            // PED: Info endpoint con estadísticas del servidor
            get("/info") {
                call.respond(mapOf(
                    "server" to mapOf(
                        "host" to host,
                        "port" to port,
                        "environment" to "development"
                    ),
                    "websocket" to mapOf(
                        "stats" to webSocketHandler.getConnectionStats(),
                        "endpoints" to listOf("/ws", "/mcp", "/ws/test")
                    ),
                    "api" to mapOf(
                        "version" to "v1",
                        "base_path" to "/api/v1",
                        "documentation" to "Available endpoints: /api/v1/state, /api/v1/connections, /api/v1/plugins"
                    ),
                    "features" to mapOf(
                        "compression" to "gzip, deflate",
                        "cors" to "enabled",
                        "error_handling" to "structured",
                        "logging" to "structured",
                        "caching" to "conditional headers"
                    )
                ))
            }
            
            // PED: WEBSOCKET ENDPOINTS - Comunicación bidireccional en tiempo real
            
            // PED: WebSocket endpoint principal para comunicación MCP
            webSocket("/ws") {
                webSocketHandler.handleConnection(this)
            }
            
            // PED: WebSocket endpoint específico para protocolo MCP
            webSocket("/mcp") {
                mcpBridge.handleMcpConnection(this)
            }
            
            // PED: WebSocket endpoint para testing y desarrollo
            webSocket("/ws/test") {
                logger.info("🧪 Nueva conexión WebSocket de testing")
                
                try {
                    // PED: Enviar mensaje de bienvenida con información del servidor
                    send(Frame.Text("""
                        {
                            "type": "welcome",
                            "message": "¡Conectado al endpoint de testing!",
                            "server": "Ktor 3.2",
                            "features": ["echo", "ping", "stats"],
                            "timestamp": ${System.currentTimeMillis()}
                        }
                    """.trimIndent()))
                    
                    // PED: Echo server mejorado para testing
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                logger.debug("📨 Test message: $text")
                                
                                // PED: Comandos especiales para testing
                                when {
                                    text.startsWith("/ping") -> {
                                        send(Frame.Text("""
                                            {
                                                "type": "pong",
                                                "timestamp": ${System.currentTimeMillis()},
                                                "latency": "0ms"
                                            }
                                        """.trimIndent()))
                                    }
                                    text.startsWith("/stats") -> {
                                        send(Frame.Text("""
                                            {
                                                "type": "stats",
                                                "connections": ${webSocketHandler.getConnectionStats()},
                                                "timestamp": ${System.currentTimeMillis()}
                                            }
                                        """.trimIndent()))
                                    }
                                    else -> {
                                        send(Frame.Text("""
                                            {
                                                "type": "echo",
                                                "original": "$text",
                                                "timestamp": ${System.currentTimeMillis()}
                                            }
                                        """.trimIndent()))
                                    }
                                }
                            }
                            is Frame.Close -> {
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
 * PED: FUNCIÓN DE UTILIDAD PARA TESTING Y DESARROLLO ACTUALIZADA
 * 
 * Versión mejorada que demuestra las nuevas capacidades del servidor.
 */
suspend fun startKtorServerExample() {
    val server = KtorServer(port = 8080)
    
    try {
        server.start()
        
        println("🌐 Servidor Ktor API REST corriendo en http://localhost:8080")
        println()
        println("📋 HTTP Endpoints disponibles:")
        println("   GET / - Información del servidor")
        println("   GET /health - Health check básico")
        println("   GET /info - Información detallada del servidor")
        println()
        println("🔗 API REST Endpoints (v1):")
        println("   GET /api/v1/state - Estado completo de la aplicación")
        println("   GET /api/v1/state/summary - Resumen del estado")
        println("   GET /api/v1/connections - Lista de conexiones (paginada)")
        println("   POST /api/v1/connections - Crear nueva conexión")
        println("   GET /api/v1/connections/{id} - Obtener conexión específica")
        println("   DELETE /api/v1/connections/{id} - Cerrar conexión")
        println("   GET /api/v1/plugins - Lista de plugins")
        println("   POST /api/v1/plugins - Cargar plugin")
        println("   GET /api/v1/server - Información del servidor")
        println("   PUT /api/v1/server/config - Actualizar configuración")
        println("   GET /api/v1/health - Health check completo")
        println("   GET /api/v1/health/live - Liveness probe")
        println("   GET /api/v1/health/ready - Readiness probe")
        println()
        println("🔌 WebSocket Endpoints disponibles:")
        println("   WS /ws - Endpoint general para comunicación WebSocket")
        println("   WS /mcp - Endpoint específico para protocolo MCP")
        println("   WS /ws/test - Endpoint de testing (echo server con comandos)")
        println()
        println("✨ Características habilitadas:")
        println("   • Manejo estructurado de errores")
        println("   • Compresión automática (gzip/deflate)")
        println("   • Headers de cache y seguridad")
        println("   • CORS configurado")
        println("   • Logging estructurado")
        println("   • Serialización JSON automática")
        println("   • Validación de requests")
        println()
        println("🧪 Comandos de testing WebSocket:")
        println("   /ping - Test de latencia")
        println("   /stats - Estadísticas de conexiones")
        println("   <cualquier texto> - Echo del mensaje")
        println()
        println("⏹️  Presiona Ctrl+C para detener...")
        
        // PED: En un ejemplo real, aquí esperaríamos una señal para detener
        delay(Long.MAX_VALUE) // Mantiene el servidor corriendo
        
    } catch (e: Exception) {
        println("❌ Error al iniciar servidor: ${e.message}")
    } finally {
        server.stop()
    }
}

