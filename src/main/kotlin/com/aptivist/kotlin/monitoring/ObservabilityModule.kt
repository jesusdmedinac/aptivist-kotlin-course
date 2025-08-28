
package com.aptivist.kotlin.monitoring

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * 🔍 MÓDULO DE OBSERVABILIDAD COMPLETO (Phase 4.2)
 * 
 * Esta implementación integra métricas, health checks y observabilidad en una solución completa:
 * 
 * 🎯 CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Extension Functions: Para extender Application con funcionalidad de observabilidad
 * • Higher-Order Functions: Para configuración flexible de módulos
 * • Coroutines: Para background tasks y monitoring asíncrono
 * • Suspend Functions: Para operaciones no bloqueantes de health checks
 * • Companion Objects: Para configuración estática y factory methods
 * • Object Singletons: Para managers globales thread-safe
 * • Lazy Initialization: Para recursos costosos inicializados bajo demanda
 * • Sealed Classes: Para configuración type-safe de observabilidad
 * • Data Classes: Para DTOs de métricas y health status
 * • When Expressions: Para routing y handling de diferentes tipos de requests
 * 
 * 🏗️ PATRONES DE DISEÑO:
 * • Module Pattern: Encapsulación de funcionalidad de observabilidad
 * • Facade Pattern: Interfaz unificada para todas las características de monitoring
 * • Observer Pattern: Para notificaciones de cambios de estado
 * • Strategy Pattern: Diferentes estrategias de monitoring según el ambiente
 * 
 * 🔧 INTEGRACIÓN:
 * • Ktor MicrometerMetrics plugin para métricas HTTP automáticas
 * • Prometheus endpoint para scraping de métricas
 * • Health check endpoints para liveness y readiness probes
 * • Structured logging con correlation IDs
 * • Background monitoring tasks
 */

private val logger = LoggerFactory.getLogger("ObservabilityModule")

/**
 * PED: Extension function para instalar observabilidad completa en Ktor Application
 * Demuestra cómo crear módulos reutilizables usando extension functions
 */
fun Application.installObservability(configure: ObservabilityConfig.() -> Unit = {}) {
    val config = ObservabilityConfig().apply(configure)
    
    logger.info("🔍 Instalando módulo de observabilidad...")
    
    // PED: Instalar métricas usando Micrometer
    installMetrics(config)
    
    // PED: Configurar health checks
    installHealthChecks(config)
    
    // PED: Configurar endpoints de observabilidad
    installObservabilityRoutes(config)
    
    // PED: Iniciar background monitoring tasks
    startBackgroundMonitoring(config)
    
    logger.info("✅ Módulo de observabilidad instalado correctamente")
}

/**
 * PED: Data class para configuración de observabilidad
 * Demuestra configuration object pattern con defaults sensatos
 */
data class ObservabilityConfig(
    val metricsEnabled: Boolean = true,
    val healthChecksEnabled: Boolean = true,
    val prometheusEndpoint: String = "/metrics",
    val healthEndpoint: String = "/health",
    val livenessEndpoint: String = "/health/liveness",
    val readinessEndpoint: String = "/health/readiness",
    val detailedHealthInfo: Boolean = true,
    val backgroundMonitoringInterval: kotlin.time.Duration = 1.minutes,
    val customTags: Map<String, String> = emptyMap()
)

/**
 * PED: Private extension function para instalar métricas
 * Demuestra encapsulación de lógica compleja en functions privadas
 */
private fun Application.installMetrics(config: ObservabilityConfig) {
    if (!config.metricsEnabled) {
        logger.info("📊 Métricas deshabilitadas por configuración")
        return
    }
    
    install(MicrometerMetrics) {
        // PED: Usar el registry configurado globalmente
        registry = MetricsConfig.prometheusRegistry
        
        // PED: Configurar distribution statistics para percentiles
        distributionStatisticConfig = DistributionStatisticConfig.Builder()
            .percentilesHistogram(true)
            .maximumExpectedValue(Duration.ofSeconds(20).toNanos().toDouble())
            .serviceLevelObjectives(
                Duration.ofMillis(100).toNanos().toDouble(),
                Duration.ofMillis(500).toNanos().toDouble(),
                Duration.ofSeconds(1).toNanos().toDouble()
            )
            .build()
        
        // PED: Configurar tags personalizados para requests HTTP
        timers { call, exception ->
            // PED: Tags automáticos basados en el request
            tag("method", call.request.httpMethod.value)
            tag("status", call.response.status()?.value?.toString() ?: "unknown")
            tag("route", call.request.path())
            
            // PED: Tags personalizados de configuración
            config.customTags.forEach { (key, value) ->
                tag(key, value)
            }
            
            // PED: Tag de error si hay exception
            exception?.let { 
                tag("exception", it::class.simpleName ?: "unknown")
            }
        }
    }
    
    logger.info("📊 Métricas Micrometer instaladas con Prometheus registry")
}

/**
 * PED: Private extension function para configurar health checks
 */
private fun Application.installHealthChecks(config: ObservabilityConfig) {
    if (!config.healthChecksEnabled) {
        logger.info("🏥 Health checks deshabilitados por configuración")
        return
    }
    
    // PED: Configurar health check manager usando DSL
    ObservabilityManager.healthCheckManager = configureHealthChecks {
        // PED: Built-in health checks básicos
        check(
            name = "liveness",
            timeout = kotlin.time.Duration.parse("5s"),
            critical = true,
            checkFunction = BuiltInHealthChecks.liveness
        )
        
        check(
            name = "memory",
            timeout = kotlin.time.Duration.parse("2s"),
            critical = false,
            checkFunction = BuiltInHealthChecks.memory
        )
        
        check(
            name = "disk_space",
            timeout = kotlin.time.Duration.parse("3s"),
            critical = false,
            checkFunction = BuiltInHealthChecks.diskSpace
        )
        
        // PED: Database health check (mock)
        check(
            name = "database",
            timeout = kotlin.time.Duration.parse("10s"),
            critical = true,
            checkFunction = BuiltInHealthChecks.databaseConnectivity {
                // PED: Mock database connectivity check
                delay(100) // Simulate database call
                true // Always healthy for demo
            }
        )
    }
    
    logger.info("🏥 Health checks configurados")
}

/**
 * PED: Private extension function para configurar routes de observabilidad
 */
private fun Application.installObservabilityRoutes(config: ObservabilityConfig) {
    routing {
        // PED: Prometheus metrics endpoint
        if (config.metricsEnabled) {
            get(config.prometheusEndpoint) {
                // PED: Incrementar contador de requests a métricas
                MetricsConfig.prometheusRegistry.incrementCounter("metrics_requests_total")
                
                call.respondText(
                    text = MetricsConfig.prometheusRegistry.scrape(),
                    contentType = ContentType.Text.Plain
                )
            }
        }
        
        // PED: Health check endpoints
        if (config.healthChecksEnabled) {
            // PED: General health endpoint
            get(config.healthEndpoint) {
                handleHealthCheck(config.detailedHealthInfo)
            }
            
            // PED: Liveness probe (Kubernetes-style)
            get(config.livenessEndpoint) {
                handleLivenessCheck()
            }
            
            // PED: Readiness probe (Kubernetes-style)
            get(config.readinessEndpoint) {
                handleReadinessCheck(config.detailedHealthInfo)
            }
        }
        
        // PED: Info endpoint con información de la aplicación
        get("/info") {
            handleInfoEndpoint()
        }
        
        // PED: Metrics summary endpoint (custom)
        get("/metrics/summary") {
            handleMetricsSummary()
        }
    }
}

/**
 * PED: Suspend function para manejar health check general
 */
private suspend fun ApplicationCall.handleHealthCheck(detailed: Boolean) {
    try {
        val healthStatus = ObservabilityManager.healthCheckManager?.checkAll()
        
        if (healthStatus != null) {
            val httpStatus = when (healthStatus.status) {
                "healthy" -> HttpStatusCode.OK
                "degraded" -> HttpStatusCode.OK // Degraded pero funcional
                "unhealthy" -> HttpStatusCode.ServiceUnavailable
                else -> HttpStatusCode.InternalServerError
            }
            
            if (detailed) {
                respond(httpStatus, healthStatus)
            } else {
                respond(httpStatus, mapOf("status" to healthStatus.status))
            }
        } else {
            respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "health_checks_not_configured"))
        }
    } catch (e: Exception) {
        logger.error("Error en health check", e)
        respond(HttpStatusCode.InternalServerError, mapOf("status" to "error", "message" to e.message))
    }
}

/**
 * PED: Suspend function para liveness check simple
 */
private suspend fun ApplicationCall.handleLivenessCheck() {
    // PED: Liveness check simple - si la app responde, está viva
    respond(HttpStatusCode.OK, mapOf("status" to "alive"))
}

/**
 * PED: Suspend function para readiness check
 */
private suspend fun ApplicationCall.handleReadinessCheck(detailed: Boolean) {
    try {
        val healthStatus = ObservabilityManager.healthCheckManager?.checkAll()
        
        if (healthStatus != null) {
            val isReady = healthStatus.status == "healthy"
            val httpStatus = if (isReady) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            
            if (detailed) {
                respond(httpStatus, healthStatus)
            } else {
                respond(httpStatus, mapOf("status" to if (isReady) "ready" else "not_ready"))
            }
        } else {
            respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "not_ready", "reason" to "health_checks_not_configured"))
        }
    } catch (e: Exception) {
        logger.error("Error en readiness check", e)
        respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "not_ready", "error" to e.message))
    }
}

/**
 * PED: Suspend function para info endpoint
 */
private suspend fun ApplicationCall.handleInfoEndpoint() {
    val runtime = Runtime.getRuntime()
    val info = mapOf(
        "application" to mapOf(
            "name" to "aptivist-kotlin-course",
            "version" to "1.0-SNAPSHOT",
            "description" to "Curso avanzado de Kotlin con ejemplos prácticos"
        ),
        "system" to mapOf(
            "java_version" to System.getProperty("java.version"),
            "kotlin_version" to KotlinVersion.CURRENT.toString(),
            "os_name" to System.getProperty("os.name"),
            "os_version" to System.getProperty("os.version"),
            "processors" to runtime.availableProcessors(),
            "max_memory_mb" to runtime.maxMemory() / 1024 / 1024,
            "total_memory_mb" to runtime.totalMemory() / 1024 / 1024,
            "free_memory_mb" to runtime.freeMemory() / 1024 / 1024
        ),
        "build" to mapOf(
            "timestamp" to System.currentTimeMillis(),
            "environment" to System.getProperty("app.environment", "development")
        )
    )
    
    respond(HttpStatusCode.OK, info)
}

/**
 * PED: Suspend function para metrics summary
 */
private suspend fun ApplicationCall.handleMetricsSummary() {
    val registry = MetricsConfig.prometheusRegistry
    
    // PED: Recopilar métricas básicas
    val summary = mapOf(
        "metrics_count" to registry.meters.size,
        "counters" to registry.meters.filterIsInstance<io.micrometer.core.instrument.Counter>().size,
        "gauges" to registry.meters.filterIsInstance<io.micrometer.core.instrument.Gauge>().size,
        "timers" to registry.meters.filterIsInstance<io.micrometer.core.instrument.Timer>().size,
        "distribution_summaries" to registry.meters.filterIsInstance<io.micrometer.core.instrument.DistributionSummary>().size,
        "sample_metrics" to registry.meters.take(5).map { meter ->
            mapOf(
                "name" to meter.id.name,
                "type" to meter.id.type.name,
                "tags" to meter.id.tags.associate { it.key to it.value }
            )
        }
    )
    
    respond(HttpStatusCode.OK, summary)
}

/**
 * PED: Private function para iniciar background monitoring
 */
private fun Application.startBackgroundMonitoring(config: ObservabilityConfig) {
    // PED: Crear coroutine scope para background tasks
    val monitoringScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // PED: Background task para métricas periódicas
    monitoringScope.launch {
        while (isActive) {
            try {
                // PED: Actualizar métricas de sistema
                updateSystemMetrics()
                
                // PED: Log health status periódicamente
                logHealthStatus()
                
                delay(config.backgroundMonitoringInterval)
            } catch (e: Exception) {
                logger.error("Error en background monitoring", e)
                delay(config.backgroundMonitoringInterval)
            }
        }
    }
    
    // PED: Cleanup al shutdown de la aplicación
    environment.monitor.subscribe(ApplicationStopped) {
        monitoringScope.cancel()
        ObservabilityManager.shutdown()
        logger.info("🔍 Background monitoring stopped")
    }
    
    logger.info("🔄 Background monitoring iniciado")
}

/**
 * PED: Private suspend function para actualizar métricas de sistema
 */
private suspend fun updateSystemMetrics() {
    val registry = MetricsConfig.prometheusRegistry
    
    // PED: Actualizar métricas personalizadas
    ApplicationMetrics.recordBusinessMetric(
        operation = "system_check",
        success = true,
        duration = kotlin.random.Random.nextLong(10, 100)
    )
    
    // PED: Gauge para active connections (mock)
    registry.gauge("active_connections_current", kotlin.random.Random.nextDouble(0.0, 50.0))
}

/**
 * PED: Private suspend function para logging de health status
 */
private suspend fun logHealthStatus() {
    try {
        val healthStatus = ObservabilityManager.healthCheckManager?.checkAll()
        if (healthStatus != null) {
            logger.info("🏥 Health Status: ${healthStatus.status} - Checks: ${healthStatus.checks.size}")
            
            // PED: Log warnings para checks degraded o unhealthy
            healthStatus.checks.forEach { (name, check) ->
                when (check.status) {
                    "degraded" -> logger.warn("⚠️  Health check '$name' is degraded")
                    "unhealthy" -> logger.error("❌ Health check '$name' is unhealthy: ${check.error}")
                }
            }
        }
    } catch (e: Exception) {
        logger.error("Error logging health status", e)
    }
}

/**
 * PED: Object singleton para gestión global de observabilidad
 * Demuestra resource management centralizado
 */
object ObservabilityManager {
    var healthCheckManager: HealthCheckManager? = null
        internal set
    
    /**
     * PED: Function para shutdown limpio de recursos
     */
    fun shutdown() {
        healthCheckManager?.shutdown()
        logger.info("🔍 ObservabilityManager shutdown completed")
    }
    
    /**
     * PED: Function para obtener métricas actuales
     */
    fun getCurrentMetrics(): Map<String, Any> {
        val registry = MetricsConfig.prometheusRegistry
        return mapOf(
            "total_meters" to registry.meters.size,
            "counters" to registry.meters.filterIsInstance<io.micrometer.core.instrument.Counter>().size,
            "gauges" to registry.meters.filterIsInstance<io.micrometer.core.instrument.Gauge>().size,
            "timers" to registry.meters.filterIsInstance<io.micrometer.core.instrument.Timer>().size
        )
    }
    
    /**
     * PED: Suspend function para health check on-demand
     */
    suspend fun performHealthCheck(): OverallHealthStatus? {
        return healthCheckManager?.checkAll()
    }
}

/**
 * PED: Extension function para configurar observabilidad con defaults
 */
fun Application.installBasicObservability() {
    installObservability {
        // PED: Usar configuración por defecto
    }
}

/**
 * PED: Extension function para configuración de producción
 */
fun Application.installProductionObservability() {
    installObservability {
        metricsEnabled = true
        healthChecksEnabled = true
        detailedHealthInfo = false // Menos información en producción
        backgroundMonitoringInterval = 30.seconds
        customTags = mapOf(
            "environment" to "production",
            "service" to "aptivist-kotlin-course"
        )
    }
}
