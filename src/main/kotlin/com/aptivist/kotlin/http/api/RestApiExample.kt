

package com.aptivist.kotlin.http.api

import com.aptivist.kotlin.http.KtorServer
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * 🎯 PHASE 3.2 - EJEMPLO COMPLETO DE API REST
 * 
 * Este archivo demuestra cómo usar la API REST completa implementada:
 * • **Servidor Completo**: Integración de todos los componentes
 * • **Ejemplos de Uso**: Demostraciones prácticas de endpoints
 * • **Testing**: Ejemplos de cómo probar la API
 * • **Documentación**: Guía completa de uso
 * • **Mejores Prácticas**: Patrones recomendados para APIs REST
 * 
 * PED: Este ejemplo sirve como:
 * - Punto de entrada principal para la aplicación
 * - Documentación ejecutable de la API
 * - Base para testing y desarrollo
 * - Referencia de integración de componentes
 */

/**
 * PED: CLASE PRINCIPAL PARA EJECUTAR LA API REST
 * 
 * Encapsula la lógica de inicio y configuración del servidor completo.
 */
class RestApiExample {
    companion object {
        private val logger = LoggerFactory.getLogger(RestApiExample::class.java)
        
        /**
         * PED: FUNCIÓN PRINCIPAL PARA EJECUTAR EL SERVIDOR
         * 
         * Demuestra el patrón de inicialización completa del servidor
         * con manejo de errores y shutdown elegante.
         */
        suspend fun runServer(port: Int = 8080, host: String = "0.0.0.0") {
            logger.info("🚀 Iniciando API REST completa...")
            
            val server = KtorServer(port = port, host = host)
            
            try {
                // PED: Iniciar servidor
                server.start()
                
                // PED: Mostrar información de la API
                printApiDocumentation(port, host)
                
                // PED: Configurar shutdown hook para cierre elegante
                Runtime.getRuntime().addShutdownHook(Thread {
                    runBlocking {
                        logger.info("🛑 Cerrando servidor...")
                        server.stop()
                        logger.info("✅ Servidor cerrado exitosamente")
                    }
                })
                
                // PED: Mantener el servidor corriendo
                delay(Long.MAX_VALUE)
                
            } catch (e: Exception) {
                logger.error("❌ Error al ejecutar servidor: ${e.message}", e)
                throw e
            }
        }
        
        /**
         * PED: FUNCIÓN PARA MOSTRAR DOCUMENTACIÓN DE LA API
         * 
         * Proporciona una guía completa de todos los endpoints disponibles
         * con ejemplos de uso usando curl.
         */
        private fun printApiDocumentation(port: Int, host: String) {
            val baseUrl = "http://$host:$port"
            
            println()
            println("🎓 " + "=".repeat(70))
            println("   CURSO AVANZADO DE KOTLIN - PHASE 3.2")
            println("   API REST COMPLETA CON KTOR")
            println("🎓 " + "=".repeat(70))
            println()
            
            println("🌐 Servidor corriendo en: $baseUrl")
            println()
            
            println("📋 ENDPOINTS BÁSICOS:")
            println("   GET  $baseUrl/")
            println("   GET  $baseUrl/health")
            println("   GET  $baseUrl/info")
            println()
            
            println("🔗 API REST v1 - ESTADO:")
            println("   GET  $baseUrl/api/v1/state")
            println("        Obtiene el estado completo de la aplicación")
            println("        Ejemplo: curl -X GET $baseUrl/api/v1/state")
            println()
            println("   GET  $baseUrl/api/v1/state/summary")
            println("        Obtiene un resumen del estado")
            println("        Ejemplo: curl -X GET $baseUrl/api/v1/state/summary")
            println()
            
            println("🔗 API REST v1 - CONEXIONES:")
            println("   GET  $baseUrl/api/v1/connections")
            println("        Lista conexiones con paginación")
            println("        Parámetros: ?page=0&size=20&sort=id&direction=asc")
            println("        Ejemplo: curl -X GET '$baseUrl/api/v1/connections?page=0&size=10'")
            println()
            println("   POST $baseUrl/api/v1/connections")
            println("        Crea una nueva conexión")
            println("        Ejemplo: curl -X POST $baseUrl/api/v1/connections \\")
            println("                      -H 'Content-Type: application/json' \\")
            println("                      -d '{\"type\":\"WEBSOCKET\",\"clientInfo\":{\"userAgent\":\"Test Client\",\"version\":\"1.0.0\",\"capabilities\":[\"test\"]}}'")
            println()
            println("   GET  $baseUrl/api/v1/connections/{id}")
            println("        Obtiene una conexión específica")
            println("        Ejemplo: curl -X GET $baseUrl/api/v1/connections/123")
            println()
            println("   DELETE $baseUrl/api/v1/connections/{id}")
            println("        Cierra una conexión")
            println("        Ejemplo: curl -X DELETE $baseUrl/api/v1/connections/123")
            println()
            
            println("🔗 API REST v1 - PLUGINS:")
            println("   GET  $baseUrl/api/v1/plugins")
            println("        Lista plugins cargados")
            println("        Ejemplo: curl -X GET $baseUrl/api/v1/plugins")
            println()
            println("   POST $baseUrl/api/v1/plugins")
            println("        Carga un nuevo plugin")
            println("        Ejemplo: curl -X POST $baseUrl/api/v1/plugins \\")
            println("                      -H 'Content-Type: application/json' \\")
            println("                      -d '{\"id\":\"test-plugin\",\"version\":\"1.0.0\"}'")
            println()
            
            println("🔗 API REST v1 - SERVIDOR:")
            println("   GET  $baseUrl/api/v1/server")
            println("        Información del servidor")
            println("        Ejemplo: curl -X GET $baseUrl/api/v1/server")
            println()
            println("   PUT  $baseUrl/api/v1/server/config")
            println("        Actualiza configuración del servidor")
            println("        Ejemplo: curl -X PUT $baseUrl/api/v1/server/config \\")
            println("                      -H 'Content-Type: application/json' \\")
            println("                      -d '{\"maxConnections\":200,\"capabilities\":[\"tools\",\"resources\"]}'")
            println()
            
            println("🔗 API REST v1 - SALUD:")
            println("   GET  $baseUrl/api/v1/health")
            println("        Health check completo")
            println("        Ejemplo: curl -X GET $baseUrl/api/v1/health")
            println()
            println("   GET  $baseUrl/api/v1/health/live")
            println("        Liveness probe (para Kubernetes)")
            println("        Ejemplo: curl -X GET $baseUrl/api/v1/health/live")
            println()
            println("   GET  $baseUrl/api/v1/health/ready")
            println("        Readiness probe (para Kubernetes)")
            println("        Ejemplo: curl -X GET $baseUrl/api/v1/health/ready")
            println()
            
            println("🔌 WEBSOCKET ENDPOINTS:")
            println("   WS   $baseUrl/ws")
            println("        Endpoint general WebSocket")
            println("        Ejemplo: wscat -c ws://localhost:$port/ws")
            println()
            println("   WS   $baseUrl/mcp")
            println("        Endpoint MCP WebSocket")
            println("        Ejemplo: wscat -c ws://localhost:$port/mcp")
            println()
            println("   WS   $baseUrl/ws/test")
            println("        Endpoint de testing con comandos:")
            println("        - /ping: Test de latencia")
            println("        - /stats: Estadísticas de conexiones")
            println("        - <texto>: Echo del mensaje")
            println("        Ejemplo: wscat -c ws://localhost:$port/ws/test")
            println()
            
            println("✨ CARACTERÍSTICAS HABILITADAS:")
            println("   • Manejo estructurado de errores con códigos HTTP semánticos")
            println("   • Compresión automática (gzip/deflate) para mejor performance")
            println("   • Headers de cache y seguridad automáticos")
            println("   • CORS configurado para desarrollo")
            println("   • Logging estructurado con request IDs")
            println("   • Serialización JSON automática bidireccional")
            println("   • Validación de requests con mensajes de error detallados")
            println("   • Paginación para endpoints que devuelven listas")
            println("   • DTOs para separar API pública del modelo interno")
            println("   • Thread safety con Mutex para estado compartido")
            println()
            
            println("🧪 EJEMPLOS DE TESTING:")
            println("   # Test básico de conectividad")
            println("   curl -X GET $baseUrl/health")
            println()
            println("   # Obtener estado completo")
            println("   curl -X GET $baseUrl/api/v1/state | jq .")
            println()
            println("   # Crear una conexión de prueba")
            println("   curl -X POST $baseUrl/api/v1/connections \\")
            println("        -H 'Content-Type: application/json' \\")
            println("        -d '{\"type\":\"HTTP\",\"clientInfo\":{\"userAgent\":\"curl/7.68.0\",\"capabilities\":[\"test\"]}}' | jq .")
            println()
            println("   # Listar conexiones")
            println("   curl -X GET '$baseUrl/api/v1/connections?page=0&size=5' | jq .")
            println()
            println("   # Health check completo")
            println("   curl -X GET $baseUrl/api/v1/health | jq .")
            println()
            
            println("📚 CONCEPTOS KOTLIN DEMOSTRADOS:")
            println("   • Sealed Classes para errores type-safe")
            println("   • Extension Functions para conversiones y utilidades")
            println("   • Suspend Functions para operaciones asíncronas")
            println("   • Thread Safety con Mutex y coroutines")
            println("   • DSL para routing declarativo")
            println("   • Data Classes para DTOs inmutables")
            println("   • Higher-Order Functions para transformaciones")
            println("   • Pattern Matching con when expressions")
            println("   • Plugin System para arquitectura modular")
            println("   • Content Negotiation para serialización automática")
            println()
            
            println("⏹️  Presiona Ctrl+C para detener el servidor")
            println("🎓 " + "=".repeat(70))
            println()
        }
    }
}

/**
 * PED: FUNCIÓN MAIN PARA EJECUTAR EL EJEMPLO
 * 
 * Punto de entrada principal que demuestra cómo iniciar
 * la API REST completa.
 */
suspend fun main() {
    try {
        RestApiExample.runServer(port = 8080, host = "0.0.0.0")
    } catch (e: Exception) {
        println("❌ Error fatal: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * PED: FUNCIÓN ALTERNATIVA PARA TESTING
 * 
 * Versión simplificada para usar en tests o desarrollo rápido.
 */
suspend fun startRestApiExample(port: Int = 8080) {
    val server = KtorServer(port = port)
    
    try {
        server.start()
        println("🌐 API REST iniciada en http://localhost:$port")
        println("📋 Documentación disponible en: http://localhost:$port/")
        
        // PED: Para testing, no mantener corriendo indefinidamente
        delay(1000) // Dar tiempo para que el servidor se inicie completamente
        
    } catch (e: Exception) {
        println("❌ Error al iniciar API REST: ${e.message}")
        throw e
    }
}

