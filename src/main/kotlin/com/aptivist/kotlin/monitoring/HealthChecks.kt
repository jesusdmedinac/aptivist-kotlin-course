
package com.aptivist.kotlin.monitoring

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 🏥 SISTEMA DE HEALTH CHECKS (Phase 4.2)
 * 
 * Esta implementación demuestra conceptos avanzados de monitoreo y observabilidad:
 * 
 * 🎯 CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Sealed Classes: Para modelar diferentes estados de salud de forma type-safe
 * • Data Classes: Para DTOs inmutables de health status con serialización automática
 * • Suspend Functions: Para health checks asíncronos no bloqueantes
 * • Coroutines: Para ejecutar múltiples health checks concurrentemente
 * • Higher-Order Functions: Para configuración flexible de health checks
 * • Extension Functions: Para APIs fluidas de health checking
 * • Companion Objects: Para factory methods y configuración estática
 * • Inline Functions: Para performance optimization en checks frecuentes
 * • When Expressions: Para pattern matching exhaustivo con sealed classes
 * • Result<T>: Para manejo funcional de errores sin exceptions
 * 
 * 🏗️ PATRONES DE DISEÑO:
 * • Strategy Pattern: Diferentes estrategias de health checking
 * • Observer Pattern: Notificación de cambios de estado
 * • Circuit Breaker: Prevención de cascading failures
 * • Builder Pattern: Configuración fluida de health checks
 * 
 * 🔍 OBSERVABILIDAD:
 * • Health endpoints para liveness y readiness probes
 * • Métricas de health status para monitoring
 * • Detailed health information para debugging
 * • Dependency health tracking para service mesh
 */

private val logger = LoggerFactory.getLogger("HealthChecks")

/**
 * PED: Sealed class para representar estados de salud de forma type-safe
 * Cada estado puede contener información específica sin casting unsafe
 */
sealed class HealthStatus {
    abstract val timestamp: Instant
    abstract val details: Map<String, Any>
    
    @Serializable
    data class Healthy(
        override val timestamp: Instant = Instant.now(),
        override val details: Map<String, String> = emptyMap(),
        val uptime: Long = 0L
    ) : HealthStatus()
    
    @Serializable
    data class Degraded(
        override val timestamp: Instant = Instant.now(),
        override val details: Map<String, String> = emptyMap(),
        val warnings: List<String> = emptyList(),
        val affectedServices: List<String> = emptyList()
    ) : HealthStatus()
    
    @Serializable
    data class Unhealthy(
        override val timestamp: Instant = Instant.now(),
        override val details: Map<String, String> = emptyMap(),
        val errors: List<String> = emptyList(),
        val failedChecks: List<String> = emptyList()
    ) : HealthStatus()
    
    /**
     * PED: Computed property usando when expression exhaustivo
     * Demuestra pattern matching type-safe con sealed classes
     */
    val isHealthy: Boolean
        get() = when (this) {
            is Healthy -> true
            is Degraded -> false
            is Unhealthy -> false
        }
    
    /**
     * PED: Extension function para convertir a HTTP status code
     */
    fun toHttpStatusCode(): Int = when (this) {
        is Healthy -> 200
        is Degraded -> 200 // Degraded pero aún funcional
        is Unhealthy -> 503 // Service Unavailable
    }
}

/**
 * PED: Data class para configuración de health check
 * Demuestra immutability y validation en init block
 */
@Serializable
data class HealthCheckConfig(
    val name: String,
    val timeout: Long = 5000L, // milliseconds
    val interval: Long = 30000L, // milliseconds
    val retries: Int = 3,
    val enabled: Boolean = true,
    val critical: Boolean = true // Si falla, marca toda la app como unhealthy
) {
    init {
        require(name.isNotBlank()) { "Health check name cannot be blank" }
        require(timeout > 0) { "Timeout must be positive" }
        require(interval > 0) { "Interval must be positive" }
        require(retries >= 0) { "Retries cannot be negative" }
    }
}

/**
 * PED: Functional interface para health check implementations
 * Demuestra functional programming con suspend functions
 */
fun interface HealthCheck {
    suspend fun check(): Result<HealthStatus>
}

/**
 * PED: Data class para resultado agregado de health checks
 */
@Serializable
data class OverallHealthStatus(
    val status: String, // "healthy", "degraded", "unhealthy"
    val timestamp: String,
    val uptime: Long,
    val version: String = "1.0-SNAPSHOT",
    val checks: Map<String, IndividualHealthCheck>
) {
    @Serializable
    data class IndividualHealthCheck(
        val status: String,
        val timestamp: String,
        val duration: Long, // milliseconds
        val details: Map<String, String> = emptyMap(),
        val error: String? = null
    )
}

/**
 * PED: Health Check Manager usando coroutines para concurrencia
 * Demuestra structured concurrency y resource management
 */
class HealthCheckManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val checks = mutableMapOf<String, Pair<HealthCheckConfig, HealthCheck>>()
    private val results = mutableMapOf<String, Pair<HealthStatus, Long>>() // status + duration
    private val startTime = System.currentTimeMillis()
    
    /**
     * PED: Function para registrar health check usando Builder Pattern
     */
    fun register(config: HealthCheckConfig, check: HealthCheck) {
        checks[config.name] = config to check
        logger.info("🔍 Registrado health check: ${config.name}")
    }
    
    /**
     * PED: Suspend function para ejecutar todos los health checks concurrentemente
     * Demuestra async/await pattern para concurrencia
     */
    suspend fun checkAll(): OverallHealthStatus = withContext(scope.coroutineContext) {
        val startTime = System.currentTimeMillis()
        
        // PED: map + async para ejecutar checks concurrentemente
        val checkResults = checks.map { (name, configAndCheck) ->
            val (config, check) = configAndCheck
            
            async {
                if (!config.enabled) {
                    name to Result.success(HealthStatus.Healthy(details = mapOf("status" to "disabled")))
                } else {
                    name to executeWithTimeout(config, check)
                }
            }
        }.awaitAll() // PED: awaitAll para esperar todos los resultados
        
        // PED: Process results usando functional programming
        val processedResults = checkResults.associate { (name, result) ->
            val duration = System.currentTimeMillis() - startTime
            
            when {
                result.isSuccess -> {
                    val status = result.getOrThrow()
                    results[name] = status to duration
                    name to OverallHealthStatus.IndividualHealthCheck(
                        status = when (status) {
                            is HealthStatus.Healthy -> "healthy"
                            is HealthStatus.Degraded -> "degraded"
                            is HealthStatus.Unhealthy -> "unhealthy"
                        },
                        timestamp = status.timestamp.toString(),
                        duration = duration,
                        details = status.details.mapValues { it.value.toString() }
                    )
                }
                else -> {
                    val error = result.exceptionOrNull()
                    results[name] = HealthStatus.Unhealthy(
                        errors = listOf(error?.message ?: "Unknown error")
                    ) to duration
                    
                    name to OverallHealthStatus.IndividualHealthCheck(
                        status = "unhealthy",
                        timestamp = Instant.now().toString(),
                        duration = duration,
                        error = error?.message
                    )
                }
            }
        }
        
        // PED: Determine overall status usando when expression
        val overallStatus = determineOverallStatus(processedResults.values)
        
        OverallHealthStatus(
            status = overallStatus,
            timestamp = Instant.now().toString(),
            uptime = System.currentTimeMillis() - this@HealthCheckManager.startTime,
            checks = processedResults
        )
    }
    
    /**
     * PED: Private suspend function con timeout handling
     * Demuestra withTimeout para operaciones con límite de tiempo
     */
    private suspend fun executeWithTimeout(
        config: HealthCheckConfig, 
        check: HealthCheck
    ): Result<HealthStatus> = try {
        withTimeout(config.timeout) {
            // PED: Retry logic usando repeat y exception handling
            var lastException: Exception? = null
            
            repeat(config.retries + 1) { attempt ->
                try {
                    return@withTimeout check.check()
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < config.retries) {
                        delay(1000) // Wait before retry
                    }
                }
            }
            
            Result.failure(lastException ?: Exception("All retries failed"))
        }
    } catch (e: TimeoutCancellationException) {
        Result.failure(Exception("Health check timed out after ${config.timeout}ms"))
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * PED: Private function para determinar estado general
     * Usa functional programming para aggregation
     */
    private fun determineOverallStatus(individualChecks: Collection<OverallHealthStatus.IndividualHealthCheck>): String {
        val criticalChecks = checks.filter { it.value.first.critical }.keys
        
        return when {
            individualChecks.any { it.status == "unhealthy" && it.error != null } -> "unhealthy"
            individualChecks.any { check -> 
                check.status == "unhealthy" && criticalChecks.any { it == checks.keys.find { key -> 
                    checks[key]?.first?.name == check.toString() 
                } } 
            } -> "unhealthy"
            individualChecks.any { it.status == "degraded" } -> "degraded"
            else -> "healthy"
        }
    }
    
    /**
     * PED: Function para cleanup de recursos
     */
    fun shutdown() {
        scope.cancel()
        logger.info("🔍 Health Check Manager shutdown completed")
    }
}

/**
 * PED: Built-in health checks usando object singletons
 * Demuestra implementaciones comunes de health checks
 */
object BuiltInHealthChecks {
    
    /**
     * PED: Simple liveness check - siempre healthy si la app está running
     */
    val liveness = HealthCheck {
        Result.success(
            HealthStatus.Healthy(
                details = mapOf(
                    "status" to "alive",
                    "timestamp" to LocalDateTime.now().toString()
                )
            )
        )
    }
    
    /**
     * PED: Memory health check usando JVM metrics
     */
    val memory = HealthCheck {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
        
        val status = when {
            memoryUsagePercent > 90 -> HealthStatus.Unhealthy(
                errors = listOf("Memory usage critical: ${memoryUsagePercent.toInt()}%"),
                details = mapOf(
                    "used_memory_mb" to "${usedMemory / 1024 / 1024}",
                    "max_memory_mb" to "${maxMemory / 1024 / 1024}",
                    "usage_percent" to "${memoryUsagePercent.toInt()}"
                )
            )
            memoryUsagePercent > 75 -> HealthStatus.Degraded(
                warnings = listOf("Memory usage high: ${memoryUsagePercent.toInt()}%"),
                details = mapOf(
                    "used_memory_mb" to "${usedMemory / 1024 / 1024}",
                    "max_memory_mb" to "${maxMemory / 1024 / 1024}",
                    "usage_percent" to "${memoryUsagePercent.toInt()}"
                )
            )
            else -> HealthStatus.Healthy(
                details = mapOf(
                    "used_memory_mb" to "${usedMemory / 1024 / 1024}",
                    "max_memory_mb" to "${maxMemory / 1024 / 1024}",
                    "usage_percent" to "${memoryUsagePercent.toInt()}"
                )
            )
        }
        
        Result.success(status)
    }
    
    /**
     * PED: Disk space health check
     */
    val diskSpace = HealthCheck {
        val file = java.io.File(".")
        val totalSpace = file.totalSpace
        val freeSpace = file.freeSpace
        val usedSpace = totalSpace - freeSpace
        val usagePercent = (usedSpace.toDouble() / totalSpace.toDouble()) * 100
        
        val status = when {
            usagePercent > 95 -> HealthStatus.Unhealthy(
                errors = listOf("Disk space critical: ${usagePercent.toInt()}%"),
                details = mapOf(
                    "free_space_gb" to "${freeSpace / 1024 / 1024 / 1024}",
                    "total_space_gb" to "${totalSpace / 1024 / 1024 / 1024}",
                    "usage_percent" to "${usagePercent.toInt()}"
                )
            )
            usagePercent > 85 -> HealthStatus.Degraded(
                warnings = listOf("Disk space low: ${usagePercent.toInt()}%"),
                details = mapOf(
                    "free_space_gb" to "${freeSpace / 1024 / 1024 / 1024}",
                    "total_space_gb" to "${totalSpace / 1024 / 1024 / 1024}",
                    "usage_percent" to "${usagePercent.toInt()}"
                )
            )
            else -> HealthStatus.Healthy(
                details = mapOf(
                    "free_space_gb" to "${freeSpace / 1024 / 1024 / 1024}",
                    "total_space_gb" to "${totalSpace / 1024 / 1024 / 1024}",
                    "usage_percent" to "${usagePercent.toInt()}"
                )
            )
        }
        
        Result.success(status)
    }
    
    /**
     * PED: Database connectivity check (mock implementation)
     */
    fun databaseConnectivity(connectionTest: suspend () -> Boolean) = HealthCheck {
        try {
            val isConnected = connectionTest()
            if (isConnected) {
                Result.success(
                    HealthStatus.Healthy(
                        details = mapOf("database" to "connected")
                    )
                )
            } else {
                Result.success(
                    HealthStatus.Unhealthy(
                        errors = listOf("Database connection failed"),
                        details = mapOf("database" to "disconnected")
                    )
                )
            }
        } catch (e: Exception) {
            Result.success(
                HealthStatus.Unhealthy(
                    errors = listOf("Database check error: ${e.message}"),
                    details = mapOf("database" to "error")
                )
            )
        }
    }
}

/**
 * PED: DSL para configurar health checks de forma fluida
 * Demuestra cómo crear DSLs expresivos en Kotlin
 */
class HealthCheckBuilder {
    private val checks = mutableListOf<Pair<HealthCheckConfig, HealthCheck>>()
    
    /**
     * PED: DSL function para agregar health check
     */
    fun check(
        name: String,
        timeout: Duration = 5.seconds,
        interval: Duration = 30.seconds,
        retries: Int = 3,
        critical: Boolean = true,
        enabled: Boolean = true,
        checkFunction: HealthCheck
    ) {
        val config = HealthCheckConfig(
            name = name,
            timeout = timeout.inWholeMilliseconds,
            interval = interval.inWholeMilliseconds,
            retries = retries,
            critical = critical,
            enabled = enabled
        )
        checks.add(config to checkFunction)
    }
    
    /**
     * PED: Function para construir el manager con todos los checks
     */
    fun build(scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())): HealthCheckManager {
        val manager = HealthCheckManager(scope)
        checks.forEach { (config, check) ->
            manager.register(config, check)
        }
        return manager
    }
}

/**
 * PED: DSL function para configurar health checks
 */
fun configureHealthChecks(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    configure: HealthCheckBuilder.() -> Unit
): HealthCheckManager {
    val builder = HealthCheckBuilder()
    builder.configure()
    return builder.build(scope)
}
