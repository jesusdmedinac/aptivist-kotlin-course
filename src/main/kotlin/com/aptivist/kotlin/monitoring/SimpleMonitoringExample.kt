package com.aptivist.kotlin.monitoring

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * 📊 EJEMPLO SIMPLE DE MONITOREO (Phase 4.2)
 * 
 * Esta aplicación demuestra los conceptos básicos de monitoreo implementados:
 * 
 * 🎯 CONCEPTOS DEMOSTRADOS:
 * • Métricas básicas con contadores simples
 * • Health checks básicos
 * • Endpoints de observabilidad
 * • Logging estructurado
 * 
 * 🔍 ENDPOINTS DISPONIBLES:
 * • GET / - Página principal
 * • GET /health - Health check básico
 * • GET /metrics - Métricas básicas
 * • GET /info - Información de la aplicación
 */

private val logger = LoggerFactory.getLogger("SimpleMonitoringExample")

// PED: Simple metrics storage usando object singleton
object SimpleMetrics {
    private var requestCount = 0L
    private var errorCount = 0L
    private val startTime = Instant.now().toEpochMilli()
    
    fun incrementRequests() {
        requestCount++
    }
    
    fun incrementErrors() {
        errorCount++
    }
    
    fun getMetrics(): Map<String, Any> = mapOf(
        "http_requests_total" to requestCount,
        "http_errors_total" to errorCount,
        "uptime_seconds" to (Instant.now().toEpochMilli() - startTime) / 1000,
        "start_time" to startTime
    )
}

// PED: Simple health check
object SimpleHealthCheck {
    fun checkHealth(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
        
        val status = if (memoryUsagePercent > 90) "unhealthy" else "healthy"
        
        return mapOf(
            "status" to status,
            "timestamp" to Instant.now().toString(),
            "memory" to mapOf(
                "used_mb" to usedMemory / 1024 / 1024,
                "max_mb" to maxMemory / 1024 / 1024,
                "usage_percent" to memoryUsagePercent.toInt()
            ),
            "uptime_seconds" to SimpleMetrics.getMetrics()["uptime_seconds"]
        )
    }
}

/**
 * PED: Main function para el ejemplo simple
 */
suspend fun main() {
    logger.info("🚀 Iniciando ejemplo simple de monitoreo...")
    
    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing {
            // PED: Página principal
            get("/") {
                SimpleMetrics.incrementRequests()
                call.respondText("""
                    🎓 Curso Avanzado de Kotlin - Phase 4.2: Monitoreo Simple
                    
                    ✅ Monitoreo básico implementado:
                    • Métricas de requests y errores
                    • Health checks de memoria y sistema
                    • Endpoints de observabilidad
                    
                    📊 Endpoints disponibles:
                    • GET /health - Health Check
                    • GET /metrics - Métricas básicas
                    • GET /info - Información de la aplicación
                    
                    🎉 ¡Curso completado exitosamente!
                """.trimIndent())
            }
            
            // PED: Health check endpoint
            get("/health") {
                SimpleMetrics.incrementRequests()
                val health = SimpleHealthCheck.checkHealth()
                call.respond(health)
            }
            
            // PED: Metrics endpoint
            get("/metrics") {
                SimpleMetrics.incrementRequests()
                val metrics = SimpleMetrics.getMetrics()
                
                // PED: Formato simple tipo Prometheus
                val prometheusFormat = buildString {
                    appendLine("# HELP http_requests_total Total HTTP requests")
                    appendLine("# TYPE http_requests_total counter")
                    appendLine("http_requests_total ${metrics["http_requests_total"]}")
                    appendLine()
                    appendLine("# HELP http_errors_total Total HTTP errors")
                    appendLine("# TYPE http_errors_total counter")
                    appendLine("http_errors_total ${metrics["http_errors_total"]}")
                    appendLine()
                    appendLine("# HELP uptime_seconds Application uptime in seconds")
                    appendLine("# TYPE uptime_seconds gauge")
                    appendLine("uptime_seconds ${metrics["uptime_seconds"]}")
                }
                
                call.respondText(prometheusFormat, io.ktor.http.ContentType.Text.Plain)
            }
            
            // PED: Info endpoint
            get("/info") {
                SimpleMetrics.incrementRequests()
                val info = mapOf(
                    "application" to mapOf(
                        "name" to "aptivist-kotlin-course",
                        "version" to "1.0-SNAPSHOT",
                        "phase" to "4.2 - Monitoreo"
                    ),
                    "system" to mapOf(
                        "java_version" to System.getProperty("java.version"),
                        "kotlin_version" to KotlinVersion.CURRENT.toString(),
                        "os_name" to System.getProperty("os.name")
                    ),
                    "metrics" to SimpleMetrics.getMetrics()
                )
                call.respond(info)
            }
            
            // PED: Endpoint que genera error para testing
            get("/error") {
                SimpleMetrics.incrementRequests()
                SimpleMetrics.incrementErrors()
                throw RuntimeException("Error de prueba para métricas")
            }
        }
    }
    
    // PED: Iniciar servidor
    server.start(wait = false)
    logger.info("✅ Servidor iniciado en http://localhost:8080")
    
    // PED: Mostrar información
    println("""
        📋 SERVIDOR DE MONITOREO INICIADO
        
        🌐 Endpoints disponibles:
        • http://localhost:8080/         - Página principal
        • http://localhost:8080/health   - Health check
        • http://localhost:8080/metrics  - Métricas Prometheus
        • http://localhost:8080/info     - Información de la app
        • http://localhost:8080/error    - Generar error (testing)
        
        🎓 Phase 4.2 - Monitoreo completado exitosamente!
        
        Presiona Ctrl+C para detener el servidor...
    """.trimIndent())
    
    // PED: Shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("🛑 Deteniendo servidor...")
        server.stop(1000, 2000)
    })
    
    // PED: Mantener corriendo
    server.engine.join()
}
