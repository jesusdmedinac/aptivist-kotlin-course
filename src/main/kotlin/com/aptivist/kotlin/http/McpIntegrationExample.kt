
package com.aptivist.kotlin.http

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * PED: EJEMPLO COMPLETO DE INTEGRACIÓN MCP + HTTP + WEBSOCKETS (Phase 2.1)
 * 
 * Esta clase demuestra la integración completa de todos los componentes:
 * - **MCP Server**: Servidor de protocolo MCP de Phase 1.3
 * - **HTTP Server**: Servidor HTTP con Ktor para endpoints REST
 * - **WebSocket Server**: Comunicación bidireccional en tiempo real
 * - **Bridge Pattern**: Integración entre diferentes protocolos
 * - **Coroutines Orchestration**: Coordinación de múltiples sistemas asíncronos
 * 
 * Este ejemplo muestra cómo un servidor Kotlin puede actuar como:
 * 1. Servidor HTTP tradicional (REST APIs)
 * 2. Servidor WebSocket (comunicación real-time)
 * 3. Servidor MCP (protocolo de comunicación con IA)
 */
object McpIntegrationExample {
    private val logger = LoggerFactory.getLogger(McpIntegrationExample::class.java)
    
    /**
     * PED: Función main que demuestra la integración completa
     * 
     * - Multi-protocol server: Un servidor que maneja múltiples protocolos
     * - Resource coordination: Coordinación de múltiples recursos
     * - Exception handling: Manejo robusto en sistemas complejos
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        logger.info("🎓 Iniciando servidor integrado MCP + HTTP + WebSocket...")
        
        val server = KtorServer(port = 8080)
        
        try {
            // PED: Iniciar servidor integrado
            server.start()
            
            logger.info("🌐 Servidor integrado iniciado exitosamente!")
            logger.info("")
            logger.info("📋 CAPACIDADES DEL SERVIDOR:")
            logger.info("   ✅ HTTP REST API")
            logger.info("   ✅ WebSocket bidireccional")
            logger.info("   ✅ Protocolo MCP sobre WebSocket")
            logger.info("   ✅ Manejo concurrente de múltiples conexiones")
            logger.info("")
            logger.info("📋 ENDPOINTS HTTP DISPONIBLES:")
            logger.info("   curl http://localhost:8080/")
            logger.info("   curl http://localhost:8080/health")
            logger.info("   curl http://localhost:8080/info")
            logger.info("")
            logger.info("🔌 ENDPOINTS WEBSOCKET DISPONIBLES:")
            logger.info("   ws://localhost:8080/ws - WebSocket general")
            logger.info("   ws://localhost:8080/mcp - Protocolo MCP")
            logger.info("   ws://localhost:8080/ws/test - Testing echo")
            logger.info("")
            logger.info("🧪 PRUEBAS MCP SUGERIDAS:")
            logger.info("   1. Conectar a ws://localhost:8080/mcp")
            logger.info("   2. Enviar initialize request:")
            logger.info("      {\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            logger.info("   3. Enviar ping request:")
            logger.info("      {\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\",\"params\":{}}")
            logger.info("   4. Enviar notification:")
            logger.info("      {\"jsonrpc\":\"2.0\",\"method\":\"initialized\",\"params\":{}}")
            logger.info("")
            logger.info("🛠️  HERRAMIENTAS RECOMENDADAS:")
            logger.info("   - wscat: npm install -g wscat")
            logger.info("   - Postman WebSocket client")
            logger.info("   - Browser WebSocket extensions")
            logger.info("   - MCP client libraries")
            logger.info("")
            
            // PED: Simular trabajo del servidor por tiempo extendido para pruebas
            logger.info("⏰ Servidor corriendo por 120 segundos para pruebas completas...")
            logger.info("   Tiempo suficiente para probar todos los protocolos")
            logger.info("")
            
            // PED: Delay extendido para permitir pruebas manuales completas
            delay(120_000) // 2 minutos
            
        } catch (e: Exception) {
            logger.error("❌ Error en servidor integrado: ${e.message}", e)
        } finally {
            // PED: Cleanup garantizado de todos los componentes
            logger.info("🛑 Deteniendo servidor integrado...")
            server.stop()
            logger.info("✅ Integración MCP + HTTP + WebSocket completada exitosamente")
        }
    }
}
