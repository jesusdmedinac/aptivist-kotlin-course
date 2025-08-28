
package com.aptivist.kotlin.monitoring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.core.instrument.binder.system.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.slf4j.LoggerFactory

/**
 * 📊 CONFIGURACIÓN DE MÉTRICAS CON MICROMETER (Phase 4.2)
 * 
 * Esta clase demuestra conceptos avanzados de observabilidad y monitoreo en Kotlin:
 * 
 * 🎯 CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Object Singleton: MetricsConfig como singleton thread-safe para configuración global
 * • Lazy Initialization: registry inicializado solo cuando se necesita
 * • Extension Functions: Funciones que extienden MeterRegistry para operaciones específicas
 * • Higher-Order Functions: Configuración usando lambdas para flexibilidad
 * • Companion Object: Factory methods y configuración estática
 * • Data Classes: Para configuración inmutable de métricas
 * • Sealed Classes: Para diferentes tipos de configuración de métricas
 * • Inline Functions: Para performance optimization en hot paths de métricas
 * 
 * 🏗️ PATRONES DE DISEÑO:
 * • Singleton Pattern: Configuración global de métricas
 * • Factory Pattern: Creación de registries configurados
 * • Builder Pattern: Configuración fluida de métricas
 * • Facade Pattern: Interfaz simplificada para Micrometer
 * 
 * 📈 OBSERVABILIDAD:
 * • Métricas JVM: Memoria, GC, threads, class loading
 * • Métricas de sistema: CPU, disk, network
 * • Métricas personalizadas: Contadores, gauges, timers
 * • Prometheus integration: Export para scraping
 */

private val logger = LoggerFactory.getLogger("MetricsConfig")

/**
 * PED: Object singleton para configuración global de métricas
 * Demuestra el patrón Singleton thread-safe en Kotlin usando 'object'
 */
object MetricsConfig {
    
    /**
     * PED: Lazy initialization del registry de Prometheus
     * El registry se crea solo cuando se accede por primera vez
     */
    val prometheusRegistry: PrometheusMeterRegistry by lazy {
        logger.info("🔧 Inicializando Prometheus MeterRegistry...")
        
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT).apply {
            // PED: apply scope function para configuración fluida
            configureCommonTags()
            registerJvmMetrics()
            registerSystemMetrics()
            
            logger.info("✅ Prometheus MeterRegistry configurado con métricas JVM y sistema")
        }
    }
    
    /**
     * PED: Extension function para configurar tags comunes
     * Demuestra cómo extender funcionalidad de clases existentes
     */
    private fun MeterRegistry.configureCommonTags() {
        config().commonTags(
            "application", "aptivist-kotlin-course",
            "version", "1.0-SNAPSHOT",
            "environment", System.getProperty("app.environment", "development"),
            "instance", System.getProperty("app.instance", "local")
        )
    }
    
    /**
     * PED: Extension function para registrar métricas JVM
     * Usa Higher-Order Functions para configuración flexible
     */
    private fun MeterRegistry.registerJvmMetrics() {
        // PED: listOf con lambda trailing para configuración declarativa
        listOf(
            JvmMemoryMetrics(), // Heap, non-heap, buffer pools
            JvmGcMetrics(), // Garbage collection stats
            JvmThreadMetrics(), // Thread count, daemon threads
            JvmClassLoaderMetrics(), // Classes loaded/unloaded
            JvmCompilationMetrics() // JIT compilation time
        ).forEach { binder ->
            // PED: forEach con lambda para registro de métricas
            binder.bindTo(this)
        }
    }
    
    /**
     * PED: Extension function para registrar métricas del sistema
     * Demuestra composición de funcionalidad usando extension functions
     */
    private fun MeterRegistry.registerSystemMetrics() {
        listOf(
            ProcessorMetrics(), // CPU usage
            FileDescriptorMetrics(), // Open file descriptors
            UptimeMetrics(), // Application uptime
            DiskSpaceMetrics(java.io.File(".")), // Disk space usage
        ).forEach { binder ->
            binder.bindTo(this)
        }
    }
    
    /**
     * PED: Factory method usando companion object pattern
     * Crea registries configurados para diferentes propósitos
     */
    fun createCustomRegistry(configure: MeterRegistry.() -> Unit = {}): MeterRegistry {
        return PrometheusMeterRegistry(PrometheusConfig.DEFAULT).apply {
            configureCommonTags()
            configure() // PED: Higher-Order Function para configuración personalizada
        }
    }
}

/**
 * PED: Data class para configuración inmutable de métricas personalizadas
 * Demuestra immutability y data classes para configuración type-safe
 */
data class CustomMetricConfig(
    val name: String,
    val description: String,
    val tags: Map<String, String> = emptyMap(),
    val unit: String? = null
) {
    /**
     * PED: Validation usando require() para fail-fast behavior
     */
    init {
        require(name.isNotBlank()) { "Metric name cannot be blank" }
        require(description.isNotBlank()) { "Metric description cannot be blank" }
    }
}

/**
 * PED: Sealed class para diferentes tipos de métricas
 * Demuestra type-safe configuration usando sealed classes
 */
sealed class MetricType {
    data class Counter(val config: CustomMetricConfig) : MetricType()
    data class Gauge(val config: CustomMetricConfig, val valueSupplier: () -> Double) : MetricType()
    data class Timer(val config: CustomMetricConfig) : MetricType()
    data class DistributionSummary(val config: CustomMetricConfig) : MetricType()
}

/**
 * PED: Extension functions para operaciones comunes de métricas
 * Demuestra cómo crear APIs fluidas usando extension functions
 */

/**
 * PED: Inline extension function para performance optimization
 * inline evita overhead de lambda en hot paths
 */
inline fun MeterRegistry.incrementCounter(name: String, vararg tags: String) {
    counter(name, *tags).increment()
}

/**
 * PED: Extension function con default parameters
 */
fun MeterRegistry.recordTimer(name: String, duration: Long, tags: Map<String, String> = emptyMap()) {
    timer(name, tags.flatMap { listOf(it.key, it.value) }.toTypedArray())
        .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)
}

/**
 * PED: Extension function usando Higher-Order Function
 * Permite medir tiempo de ejecución de cualquier bloque de código
 */
inline fun <T> MeterRegistry.timeExecution(name: String, block: () -> T): T {
    val timer = timer(name)
    return timer.recordCallable(block)!!
}

/**
 * PED: Builder class para configuración fluida de métricas
 * Demuestra Builder Pattern con DSL-like syntax
 */
class MetricsBuilder {
    private val metrics = mutableListOf<MetricType>()
    
    /**
     * PED: DSL function para agregar counter
     */
    fun counter(name: String, description: String, configure: CustomMetricConfig.() -> CustomMetricConfig = { this }) {
        val config = CustomMetricConfig(name, description).configure()
        metrics.add(MetricType.Counter(config))
    }
    
    /**
     * PED: DSL function para agregar gauge con lambda
     */
    fun gauge(name: String, description: String, valueSupplier: () -> Double) {
        val config = CustomMetricConfig(name, description)
        metrics.add(MetricType.Gauge(config, valueSupplier))
    }
    
    /**
     * PED: DSL function para agregar timer
     */
    fun timer(name: String, description: String) {
        val config = CustomMetricConfig(name, description)
        metrics.add(MetricType.Timer(config))
    }
    
    /**
     * PED: Function para construir y registrar todas las métricas
     */
    fun build(registry: MeterRegistry) {
        metrics.forEach { metric ->
            when (metric) {
                is MetricType.Counter -> {
                    registry.counter(metric.config.name, metric.config.tags.flatMap { listOf(it.key, it.value) }.toTypedArray())
                }
                is MetricType.Gauge -> {
                    registry.gauge(metric.config.name, metric.config.tags, metric.config, { it.valueSupplier() })
                }
                is MetricType.Timer -> {
                    registry.timer(metric.config.name, metric.config.tags.flatMap { listOf(it.key, it.value) }.toTypedArray())
                }
                is MetricType.DistributionSummary -> {
                    registry.summary(metric.config.name, metric.config.tags.flatMap { listOf(it.key, it.value) }.toTypedArray())
                }
            }
        }
        
        logger.info("📊 Registradas ${metrics.size} métricas personalizadas")
    }
}

/**
 * PED: DSL function para configurar métricas usando Builder Pattern
 * Demuestra cómo crear DSLs expresivos en Kotlin
 */
fun configureMetrics(registry: MeterRegistry = MetricsConfig.prometheusRegistry, configure: MetricsBuilder.() -> Unit) {
    val builder = MetricsBuilder()
    builder.configure()
    builder.build(registry)
}

/**
 * PED: Utility object para métricas de aplicación específicas
 * Demuestra encapsulación de lógica de métricas de dominio
 */
object ApplicationMetrics {
    private val registry = MetricsConfig.prometheusRegistry
    
    // PED: Lazy initialization de métricas específicas
    val httpRequestsTotal by lazy { 
        registry.counter("http_requests_total", "Total HTTP requests") 
    }
    
    val httpRequestDuration by lazy { 
        registry.timer("http_request_duration_seconds", "HTTP request duration") 
    }
    
    val activeConnections by lazy {
        registry.gauge("active_connections", mutableListOf<String>()) { it.size.toDouble() }
    }
    
    val databaseConnectionPool by lazy {
        registry.gauge("database_connection_pool_active", this) { 
            // PED: Placeholder para métricas reales de connection pool
            kotlin.random.Random.nextDouble(0.0, 10.0)
        }
    }
    
    /**
     * PED: Function para registrar métricas de negocio
     */
    fun recordBusinessMetric(operation: String, success: Boolean, duration: Long) {
        registry.incrementCounter("business_operations_total", "operation", operation, "success", success.toString())
        registry.recordTimer("business_operation_duration", duration, mapOf("operation" to operation))
    }
}
