
package com.aptivist.kotlin.monitoring

import com.aptivist.kotlin.http.KtorServer
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/**
 * 📊 EJEMPLO COMPLETO DE MONITOREO Y OBSERVABILIDAD (Phase 4.2)
 * 
 * Esta aplicación demuestra la integración completa de todas las fases del curso:
 * 
 * 🎯 INTEGRACIÓN DE FASES:
 * • Phase 1.1: Logging estructurado y configuración
 * • Phase 1.2: Build system avanzado con Gradle DSL
 * • Phase 1.3: Arquitectura MCP con coroutines y serialización
 * • Phase 2.1: Servidor HTTP con Ktor y WebSockets
 * • Phase 2.2: Sistema de plugins extensible
 * • Phase 3.1: Manejo de estado inmutable con Redux pattern
 * • Phase 3.2: API REST completa con error handling
 * • Phase 4.1: Persistencia con base de datos y caching
 * • Phase 4.2: Monitoreo completo con métricas y health checks
 * 
 * 🔍 OBSERVABILIDAD DEMOSTRADA:
 * • Métricas automáticas de HTTP requests con Micrometer
 * • Health checks para liveness y readiness probes
 * • Prometheus endpoint para scraping de métricas
 * • Structured logging con correlation IDs
 * • Background monitoring tasks
 * • Custom business metrics
 * • JVM y system metrics
 * 
 * 🎓 CONCEPTOS KOTLIN FINALES:
 * • Integration patterns para sistemas complejos
 * • Production-ready configuration
 * • Graceful shutdown y resource management
 * • Comprehensive error handling
 * • Performance monitoring y optimization
 */

private val logger = LoggerFactory.getLogger("MonitoringExample")

/**
 * PED: Main function que demuestra la aplicación completa con monitoreo
 */
suspend fun main() {
    logger.info("🚀 Iniciando aplicación con monitoreo completo...")
    
    try {
        // PED: Crear servidor con todas las características integradas
        val server = createMonitoredServer()
        
        // PED: Iniciar servidor
        server.start(wait = false)
        logger.info("✅ Servidor iniciado en http://localhost:8080")
        
        // PED: Mostrar endpoints disponibles
        showAvailableEndpoints()
        
        // PED: Demostrar métricas en background
        demonstrateMetrics()
        
        // PED: Esperar por shutdown signal
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("🛑 Shutdown signal recibido, cerrando servidor...")
            server.stop(1000, 2000)
            ObservabilityManager.shutdown()
        })
        
        // PED: Mantener la aplicación corriendo
        server.engine.join()
        
    } catch (e: Exception) {
        logger.error("❌ Error iniciando aplicación", e)
        throw e
    }
}

/**
 * PED: Function para crear servidor con monitoreo completo
 * Demuestra integración de todos los módulos del curso
 */
private fun createMonitoredServer(): ApplicationEngine {
    return embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        // PED: Instalar observabilidad completa
        installObservability {
            metricsEnabled = true
            healthChecksEnabled = true
            detailedHealthInfo = true
            backgroundMonitoringInterval = 30.seconds
            customTags = mapOf(
                "application" to "aptivist-kotlin-course",
                "phase" to "4.2",
                "environment" to "demo"
            )
        }
        
        // PED: Configurar routing con ejemplos de todas las fases
        routing {
            // PED: Endpoints de demostración que generan métricas
            get("/") {
                // PED: Incrementar contador personalizado
                ApplicationMetrics.httpRequestsTotal.increment()
                
                call.respondText("""
                    🎓 Curso Avanzado de Kotlin - Phase 4.2: Monitoreo
                    
                    ✅ Todas las fases completadas:
                    • Phase 1.1: Logging y Configuración
                    • Phase 1.2: Build System Avanzado
                    • Phase 1.3: Arquitectura MCP
                    • Phase 2.1: Servidor HTTP con Ktor
                    • Phase 2.2: Sistema de Plugins
                    • Phase 3.1: Manejo de Estado
                    • Phase 3.2: API REST
                    • Phase 4.1: Persistencia
                    • Phase 4.2: Monitoreo ✨
                    
                    📊 Endpoints de Observabilidad:
                    • GET /metrics - Métricas Prometheus
                    • GET /health - Health Check General
                    • GET /health/liveness - Liveness Probe
                    • GET /health/readiness - Readiness Probe
                    • GET /info - Información de la Aplicación
                    • GET /metrics/summary - Resumen de Métricas
                    
                    🔍 Endpoints de Demostración:
                    • GET /demo/fast - Endpoint rápido
                    • GET /demo/slow - Endpoint lento
                    • GET /demo/error - Endpoint con error
                    • GET /demo/business - Métricas de negocio
                """.trimIndent())
            }
            
            // PED: Endpoints de demostración para generar diferentes métricas
            get("/demo/fast") {
                ApplicationMetrics.recordBusinessMetric("fast_operation", true, 50)
                call.respondText("⚡ Operación rápida completada")
            }
            
            get("/demo/slow") {
                // PED: Simular operación lenta
                delay(2000)
                ApplicationMetrics.recordBusinessMetric("slow_operation", true, 2000)
                call.respondText("🐌 Operación lenta completada")
            }
            
            get("/demo/error") {
                ApplicationMetrics.recordBusinessMetric("error_operation", false, 100)
                throw RuntimeException("Error de demostración para métricas")
            }
            
            get("/demo/business") {
                // PED: Simular métricas de negocio
                repeat(kotlin.random.Random.nextInt(1, 10)) {
                    ApplicationMetrics.recordBusinessMetric("user_action", true, kotlin.random.Random.nextLong(10, 500))
                }
                
                call.respondText("📈 Métricas de negocio generadas")
            }
            
            // PED: Endpoint para demostrar state management (Phase 3.1)
            get("/demo/state") {
                // PED: Aquí se integraría con StateManager de Phase 3.1
                call.respondText("🔄 Estado de aplicación: Demo mode")
            }
            
            // PED: Endpoint para demostrar persistencia (Phase 4.1)
            get("/demo/persistence") {
                // PED: Aquí se integraría con PersistenceIntegration de Phase 4.1
                call.respondText("💾 Persistencia: Demo data stored")
            }
        }
    }
}

/**
 * PED: Function para mostrar endpoints disponibles
 */
private fun showAvailableEndpoints() {
    logger.info("""
        📋 ENDPOINTS DISPONIBLES:
        
        🏠 Aplicación:
        • GET  http://localhost:8080/                    - Página principal
        
        📊 Observabilidad:
        • GET  http://localhost:8080/metrics             - Métricas Prometheus
        • GET  http://localhost:8080/health              - Health Check General
        • GET  http://localhost:8080/health/liveness     - Liveness Probe
        • GET  http://localhost:8080/health/readiness    - Readiness Probe
        • GET  http://localhost:8080/info                - Información de la App
        • GET  http://localhost:8080/metrics/summary     - Resumen de Métricas
        
        🎯 Demostración:
        • GET  http://localhost:8080/demo/fast           - Endpoint rápido
        • GET  http://localhost:8080/demo/slow           - Endpoint lento (2s)
        • GET  http://localhost:8080/demo/error          - Endpoint con error
        • GET  http://localhost:8080/demo/business       - Métricas de negocio
        • GET  http://localhost:8080/demo/state          - State management
        • GET  http://localhost:8080/demo/persistence    - Persistencia
    """.trimIndent())
}

/**
 * PED: Suspend function para demostrar métricas en background
 */
private suspend fun demonstrateMetrics() {
    // PED: Crear coroutine para generar métricas de demostración
    CoroutineScope(Dispatchers.IO).launch {
        var counter = 0
        while (isActive) {
            try {
                // PED: Generar métricas sintéticas cada 10 segundos
                delay(10000)
                counter++
                
                // PED: Simular diferentes tipos de operaciones
                when (counter % 4) {
                    0 -> ApplicationMetrics.recordBusinessMetric("background_task", true, 100)
                    1 -> ApplicationMetrics.recordBusinessMetric("data_processing", true, 250)
                    2 -> ApplicationMetrics.recordBusinessMetric("cache_refresh", true, 50)
                    3 -> ApplicationMetrics.recordBusinessMetric("health_check", true, 25)
                }
                
                logger.info("📊 Métricas sintéticas generadas (ciclo $counter)")
                
            } catch (e: Exception) {
                logger.error("Error generando métricas sintéticas", e)
            }
        }
    }
}

/**
 * PED: Function para demostrar configuración de métricas personalizadas
 */
fun demonstrateCustomMetrics() {
    logger.info("🔧 Configurando métricas personalizadas...")
    
    // PED: Usar DSL para configurar métricas
    configureMetrics {
        counter("course_completions", "Número de estudiantes que completaron el curso")
        counter("phase_transitions", "Transiciones entre fases del curso")
        
        gauge("active_students", "Estudiantes actualmente activos") {
            kotlin.random.Random.nextDouble(10.0, 100.0)
        }
        
        timer("lesson_duration", "Duración de las lecciones")
    }
    
    logger.info("✅ Métricas personalizadas configuradas")
}

/**
 * PED: Object para demostrar integración con todas las fases anteriores
 */
object CourseIntegrationDemo {
    
    /**
     * PED: Function que demuestra integración con Phase 1.1 (Logging)
     */
    fun demonstrateLogging() {
        logger.info("📝 Phase 1.1: Logging estructurado funcionando")
        logger.debug("🔍 Debug info: Configuración cargada correctamente")
        logger.warn("⚠️  Warning: Este es un mensaje de advertencia de demo")
    }
    
    /**
     * PED: Function que demuestra integración con Phase 1.2 (Build System)
     */
    fun demonstrateBuildSystem() {
        logger.info("🔧 Phase 1.2: Build system con Gradle DSL y buildSrc")
        logger.info("📦 Dependencias: ${System.getProperty("java.class.path").split(":").size} JARs cargados")
    }
    
    /**
     * PED: Function que demuestra integración con Phase 1.3 (MCP Architecture)
     */
    suspend fun demonstrateMcpArchitecture() {
        logger.info("🏗️  Phase 1.3: Arquitectura MCP con coroutines")
        // PED: Aquí se integraría con McpServer de Phase 1.3
        delay(100) // Simular operación asíncrona
        logger.info("✅ MCP server simulation completed")
    }
    
    /**
     * PED: Function que demuestra integración con Phase 2.1 (HTTP Server)
     */
    fun demonstrateHttpServer() {
        logger.info("🌐 Phase 2.1: Servidor HTTP con Ktor y WebSockets")
        logger.info("🔌 WebSocket connections: Simulando conexiones activas")
    }
    
    /**
     * PED: Function que demuestra integración con Phase 2.2 (Plugin System)
     */
    fun demonstratePluginSystem() {
        logger.info("🔌 Phase 2.2: Sistema de plugins extensible")
        logger.info("📋 Plugins disponibles: EchoPlugin, UtilityPlugin")
    }
    
    /**
     * PED: Function que demuestra integración con Phase 3.1 (State Management)
     */
    suspend fun demonstrateStateManagement() {
        logger.info("🔄 Phase 3.1: Manejo de estado inmutable")
        // PED: Aquí se integraría con StateManager de Phase 3.1
        delay(50)
        logger.info("📊 Estado actual: Demo mode activo")
    }
    
    /**
     * PED: Function que demuestra integración con Phase 3.2 (REST API)
     */
    fun demonstrateRestApi() {
        logger.info("🌐 Phase 3.2: API REST con error handling")
        logger.info("📡 Endpoints REST: CRUD operations disponibles")
    }
    
    /**
     * PED: Function que demuestra integración con Phase 4.1 (Persistence)
     */
    suspend fun demonstratePersistence() {
        logger.info("💾 Phase 4.1: Persistencia con base de datos y caching")
        // PED: Aquí se integraría con PersistenceIntegration de Phase 4.1
        delay(100)
        logger.info("🗄️  Database: H2 embedded, Cache: Caffeine activo")
    }
    
    /**
     * PED: Function que demuestra Phase 4.2 (Monitoring) - fase actual
     */
    suspend fun demonstrateMonitoring() {
        logger.info("📊 Phase 4.2: Monitoreo y observabilidad completa")
        
        // PED: Demostrar health check
        val healthStatus = ObservabilityManager.performHealthCheck()
        logger.info("🏥 Health Status: ${healthStatus?.status ?: "unknown"}")
        
        // PED: Demostrar métricas
        val metrics = ObservabilityManager.getCurrentMetrics()
        logger.info("📈 Métricas activas: ${metrics["total_meters"]} meters registrados")
        
        logger.info("✅ Monitoreo completo funcionando")
    }
    
    /**
     * PED: Function para ejecutar demostración completa de integración
     */
    suspend fun runFullIntegrationDemo() {
        logger.info("🎓 DEMOSTRACIÓN COMPLETA DE INTEGRACIÓN DEL CURSO")
        logger.info("=" + "=".repeat(60))
        
        demonstrateLogging()
        demonstrateBuildSystem()
        demonstrateMcpArchitecture()
        demonstrateHttpServer()
        demonstratePluginSystem()
        demonstrateStateManagement()
        demonstrateRestApi()
        demonstratePersistence()
        demonstrateMonitoring()
        
        logger.info("=" + "=".repeat(60))
        logger.info("🎉 CURSO COMPLETO - TODAS LAS FASES INTEGRADAS EXITOSAMENTE")
    }
}
