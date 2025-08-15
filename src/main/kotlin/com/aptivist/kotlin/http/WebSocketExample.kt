
package com.aptivist.kotlin.http

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * PED: EJEMPLO COMPLETO DE SERVIDOR HTTP + WEBSOCKETS (Phase 2.1)
 * 
 * Esta clase demuestra la integración completa de:
 * - **HTTP Server**: Endpoints REST para información y health checks
 * - **WebSocket Server**: Comunicación bidireccional en tiempo real
 * - **Coroutines**: Manejo asíncrono de múltiples conexiones concurrentes
 * - **Resource Management**: Proper startup y shutdown del servidor
 * 
 * Este ejemplo muestra cómo un servidor Kotlin puede manejar tanto
 * HTTP tradicional como WebSockets para diferentes casos de uso.
 */
object WebSocketExample {
    private val logger = LoggerFactory.getLogger(WebSocketExample::class.java)
    
    /**
     * PED: Función main que demuestra servidor HTTP + WebSocket completo
     * 
     * - runBlocking: Bridge entre código síncrono y asíncrono
     * - Exception handling: Manejo robusto de errores
     * - Resource cleanup: Garantiza que el servidor se detenga correctamente
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        logger.info("🎓 Iniciando servidor HTTP + WebSocket completo...")
        
        val server = KtorServer(port = 8080)
        
        try {
            // PED: Iniciar servidor de manera asíncrona
            server.start()
            
            logger.info("🌐 Servidor HTTP + WebSocket iniciado exitosamente!")
            logger.info("")
            logger.info("📋 ENDPOINTS HTTP DISPONIBLES:")
            logger.info("   curl http://localhost:8080/")
            logger.info("   curl http://localhost:8080/health")
            logger.info("   curl http://localhost:8080/info")
            logger.info("")
            logger.info("🔌 ENDPOINTS WEBSOCKET DISPONIBLES:")
            logger.info("   ws://localhost:8080/ws - Endpoint principal MCP")
            logger.info("   ws://localhost:8080/ws/test - Endpoint de testing")
            logger.info("")
            logger.info("🧪 PRUEBAS WEBSOCKET SUGERIDAS:")
            logger.info("   1. Conectar a ws://localhost:8080/ws/test")
            logger.info("   2. Enviar mensaje de texto para echo")
            logger.info("   3. Conectar a ws://localhost:8080/ws")
            logger.info("   4. Enviar JSON: {\"type\":\"ping\",\"data\":\"test\"}")
            logger.info("   5. Enviar JSON: {\"type\":\"get_stats\",\"data\":\"\"}")
            logger.info("   6. Enviar JSON: {\"type\":\"broadcast\",\"data\":\"Hello everyone!\"}")
            logger.info("")
            
            // PED: Simular trabajo del servidor por tiempo extendido
            logger.info("⏰ Servidor corriendo por 60 segundos para pruebas...")
            logger.info("   Puedes usar herramientas como:")
            logger.info("   - Browser WebSocket extensions")
            logger.info("   - wscat (npm install -g wscat)")
            logger.info("   - Postman WebSocket client")
            logger.info("")
            
            // PED: Delay más largo para permitir pruebas manuales
            delay(60_000) // 60 segundos
            
        } catch (e: Exception) {
            logger.error("❌ Error en servidor HTTP + WebSocket: ${e.message}", e)
        } finally {
            // PED: Cleanup garantizado
            logger.info("🛑 Deteniendo servidor HTTP + WebSocket...")
            server.stop()
            logger.info("✅ Ejemplo completado exitosamente")
        }
    }
}
