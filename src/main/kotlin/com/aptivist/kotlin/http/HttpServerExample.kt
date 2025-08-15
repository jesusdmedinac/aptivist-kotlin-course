
package com.aptivist.kotlin.http

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * PED: EJEMPLO EJECUTABLE DEL SERVIDOR HTTP (Phase 2.1)
 * 
 * Esta clase demuestra:
 * - **Object Declaration**: Singleton thread-safe para ejemplos
 * - **Coroutine Builders**: runBlocking para bridge entre sync/async code
 * - **Exception Handling**: Try-catch en contexto de coroutines
 * - **Resource Management**: Proper cleanup de recursos
 * 
 * Este ejemplo muestra cómo integrar un servidor HTTP Ktor
 * en una aplicación Kotlin usando coroutines.
 */
object HttpServerExample {
    private val logger = LoggerFactory.getLogger(HttpServerExample::class.java)
    
    /**
     * PED: Función main que demuestra el uso del servidor HTTP
     * 
     * - runBlocking: Coroutine builder que bloquea hasta completarse
     * - try-catch-finally: Exception handling con proper cleanup
     * - suspend functions: Llamadas asíncronas dentro de coroutine scope
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        logger.info("🎓 Iniciando ejemplo de servidor HTTP con Ktor...")
        
        val server = KtorServer(port = 8080)
        
        try {
            // PED: start() es una suspend function
            server.start()
            
            logger.info("🌐 Servidor HTTP iniciado exitosamente!")
            logger.info("📋 Prueba estos endpoints:")
            logger.info("   curl http://localhost:8080/")
            logger.info("   curl http://localhost:8080/health")
            logger.info("   curl http://localhost:8080/info")
            
            // PED: Simula trabajo del servidor por 30 segundos
            logger.info("⏰ Servidor corriendo por 30 segundos...")
            delay(30_000)
            
        } catch (e: Exception) {
            logger.error("❌ Error en servidor HTTP: ${e.message}", e)
        } finally {
            // PED: Cleanup garantizado en el bloque finally
            logger.info("🛑 Deteniendo servidor HTTP...")
            server.stop()
            logger.info("✅ Ejemplo completado exitosamente")
        }
    }
}
