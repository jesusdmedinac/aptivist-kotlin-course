
package com.aptivist.kotlin.monitoring

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * 🧪 TESTS PARA SISTEMA DE MONITOREO (Phase 4.2)
 * 
 * Esta suite de tests demuestra testing avanzado de sistemas de observabilidad:
 * 
 * 🎯 CONCEPTOS DE TESTING DEMOSTRADOS:
 * • Coroutine Testing: TestScope y runTest para testing asíncrono
 * • Ktor Testing: testApplication para testing de endpoints HTTP
 * • Metrics Testing: Verificación de métricas y contadores
 * • Health Check Testing: Testing de diferentes estados de salud
 * • Integration Testing: Testing de componentes integrados
 * • Mock Testing: Simulación de dependencias externas
 * • Timeout Testing: Testing de operaciones con límites de tiempo
 * • Concurrent Testing: Testing de operaciones concurrentes
 * 
 * 🏗️ PATRONES DE TESTING:
 * • Arrange-Act-Assert pattern para estructura clara
 * • Test fixtures para setup y cleanup
 * • Parameterized tests para múltiples escenarios
 * • Custom assertions para verificaciones específicas
 */

class MonitoringTest {
    
    private lateinit var testScope: TestScope
    private lateinit var healthCheckManager: HealthCheckManager
    
    @BeforeEach
    fun setup() {
        testScope = TestScope()
        
        // PED: Configurar health check manager para testing
        healthCheckManager = configureHealthChecks(testScope) {
            check(
                name = "test_liveness",
                timeout = 1.seconds,
                checkFunction = BuiltInHealthChecks.liveness
            )
            
            check(
                name = "test_memory",
                timeout = 2.seconds,
                critical = false,
                checkFunction = BuiltInHealthChecks.memory
            )
        }
    }
    
    @AfterEach
    fun cleanup() {
        healthCheckManager.shutdown()
        testScope.cancel()
    }
    
    /**
     * PED: Test de configuración de métricas
     */
    @Test
    fun `test metrics configuration`() {
        // Arrange
        val registry = MetricsConfig.prometheusRegistry
        val initialMeterCount = registry.meters.size
        
        // Act
        configureMetrics(registry) {
            counter("test_counter", "Test counter for unit testing")
            gauge("test_gauge", "Test gauge for unit testing") { 42.0 }
            timer("test_timer", "Test timer for unit testing")
        }
        
        // Assert
        val finalMeterCount = registry.meters.size
        assertTrue(finalMeterCount > initialMeterCount, "New metrics should be registered")
        
        // PED: Verificar que las métricas específicas existen
        val counterExists = registry.meters.any { it.id.name == "test_counter" }
        val gaugeExists = registry.meters.any { it.id.name == "test_gauge" }
        val timerExists = registry.meters.any { it.id.name == "test_timer" }
        
        assertTrue(counterExists, "Test counter should be registered")
        assertTrue(gaugeExists, "Test gauge should be registered")
        assertTrue(timerExists, "Test timer should be registered")
    }
    
    /**
     * PED: Test de health checks básicos
     */
    @Test
    fun `test basic health checks`() = runTest {
        // Act
        val healthStatus = healthCheckManager.checkAll()
        
        // Assert
        assertNotNull(healthStatus, "Health status should not be null")
        assertEquals("healthy", healthStatus.status, "Overall status should be healthy")
        assertTrue(healthStatus.checks.isNotEmpty(), "Should have health checks")
        
        // PED: Verificar checks específicos
        assertTrue(healthStatus.checks.containsKey("test_liveness"), "Should contain liveness check")
        assertTrue(healthStatus.checks.containsKey("test_memory"), "Should contain memory check")
        
        val livenessCheck = healthStatus.checks["test_liveness"]
        assertNotNull(livenessCheck, "Liveness check should exist")
        assertEquals("healthy", livenessCheck.status, "Liveness should be healthy")
    }
    
    /**
     * PED: Test de health check con fallo
     */
    @Test
    fun `test health check with failure`() = runTest {
        // Arrange
        val failingHealthCheck = HealthCheck {
            Result.success(
                HealthStatus.Unhealthy(
                    errors = listOf("Test failure"),
                    failedChecks = listOf("failing_check")
                )
            )
        }
        
        val testManager = configureHealthChecks(testScope) {
            check(
                name = "failing_check",
                timeout = 1.seconds,
                critical = true,
                checkFunction = failingHealthCheck
            )
        }
        
        // Act
        val healthStatus = testManager.checkAll()
        
        // Assert
        assertEquals("unhealthy", healthStatus.status, "Overall status should be unhealthy")
        
        val failingCheck = healthStatus.checks["failing_check"]
        assertNotNull(failingCheck, "Failing check should exist")
        assertEquals("unhealthy", failingCheck.status, "Failing check should be unhealthy")
        
        // Cleanup
        testManager.shutdown()
    }
    
    /**
     * PED: Test de health check con timeout
     */
    @Test
    fun `test health check with timeout`() = runTest {
        // Arrange
        val slowHealthCheck = HealthCheck {
            delay(5000) // 5 seconds delay
            Result.success(HealthStatus.Healthy())
        }
        
        val testManager = configureHealthChecks(testScope) {
            check(
                name = "slow_check",
                timeout = 1.seconds, // 1 second timeout
                checkFunction = slowHealthCheck
            )
        }
        
        // Act
        val healthStatus = testManager.checkAll()
        
        // Assert
        val slowCheck = healthStatus.checks["slow_check"]
        assertNotNull(slowCheck, "Slow check should exist")
        assertEquals("unhealthy", slowCheck.status, "Slow check should timeout and be unhealthy")
        assertNotNull(slowCheck.error, "Should have timeout error")
        assertTrue(
            slowCheck.error!!.contains("timed out"),
            "Error should mention timeout"
        )
        
        // Cleanup
        testManager.shutdown()
    }
    
    /**
     * PED: Test de endpoints de observabilidad con Ktor Testing
     */
    @Test
    fun `test observability endpoints`() = testApplication {
        // Arrange
        application {
            installBasicObservability()
        }
        
        // Act & Assert - Metrics endpoint
        client.get("/metrics").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(ContentType.Text.Plain, contentType())
            val content = bodyAsText()
            assertTrue(content.contains("# HELP"), "Should contain Prometheus metrics format")
        }
        
        // Act & Assert - Health endpoint
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            val content = bodyAsText()
            assertTrue(content.contains("healthy") || content.contains("status"), "Should contain health status")
        }
        
        // Act & Assert - Liveness endpoint
        client.get("/health/liveness").apply {
            assertEquals(HttpStatusCode.OK, status)
            val content = bodyAsText()
            assertTrue(content.contains("alive"), "Should contain alive status")
        }
        
        // Act & Assert - Info endpoint
        client.get("/info").apply {
            assertEquals(HttpStatusCode.OK, status)
            val content = bodyAsText()
            assertTrue(content.contains("application"), "Should contain application info")
        }
    }
    
    /**
     * PED: Test de métricas HTTP automáticas
     */
    @Test
    fun `test automatic HTTP metrics`() = testApplication {
        // Arrange
        application {
            installBasicObservability()
            routing {
                get("/test") {
                    call.respondText("Test response")
                }
            }
        }
        
        val registry = MetricsConfig.prometheusRegistry
        val initialRequestCount = registry.meters
            .filterIsInstance<io.micrometer.core.instrument.Timer>()
            .find { it.id.name == "ktor.http.server.requests" }
            ?.count() ?: 0.0
        
        // Act
        client.get("/test").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        
        // Assert
        val finalRequestCount = registry.meters
            .filterIsInstance<io.micrometer.core.instrument.Timer>()
            .find { it.id.name == "ktor.http.server.requests" }
            ?.count() ?: 0.0
        
        assertTrue(
            finalRequestCount > initialRequestCount,
            "HTTP request metrics should be incremented"
        )
    }
    
    /**
     * PED: Test de métricas personalizadas
     */
    @Test
    fun `test custom metrics`() {
        // Arrange
        val registry = MetricsConfig.prometheusRegistry
        
        // Act
        ApplicationMetrics.httpRequestsTotal.increment()
        ApplicationMetrics.recordBusinessMetric("test_operation", true, 100)
        
        // Assert
        val httpCounter = registry.meters
            .filterIsInstance<io.micrometer.core.instrument.Counter>()
            .find { it.id.name == "http_requests_total" }
        
        assertNotNull(httpCounter, "HTTP requests counter should exist")
        assertTrue(httpCounter.count() > 0, "Counter should be incremented")
        
        val businessCounter = registry.meters
            .filterIsInstance<io.micrometer.core.instrument.Counter>()
            .find { it.id.name == "business_operations_total" }
        
        assertNotNull(businessCounter, "Business operations counter should exist")
        assertTrue(businessCounter.count() > 0, "Business counter should be incremented")
    }
    
    /**
     * PED: Test de concurrent health checks
     */
    @Test
    fun `test concurrent health checks`() = runTest {
        // Arrange
        val concurrentManager = configureHealthChecks(testScope) {
            repeat(5) { index ->
                check(
                    name = "concurrent_check_$index",
                    timeout = 1.seconds,
                    checkFunction = HealthCheck {
                        delay(100) // Small delay to simulate work
                        Result.success(HealthStatus.Healthy(details = mapOf("index" to index.toString())))
                    }
                )
            }
        }
        
        // Act
        val startTime = System.currentTimeMillis()
        val healthStatus = concurrentManager.checkAll()
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Assert
        assertEquals("healthy", healthStatus.status, "All checks should be healthy")
        assertEquals(5, healthStatus.checks.size, "Should have 5 health checks")
        
        // PED: Verificar que se ejecutaron concurrentemente (no secuencialmente)
        assertTrue(
            duration < 400, // Should be much less than 5 * 100ms if concurrent
            "Health checks should run concurrently, took ${duration}ms"
        )
        
        // Cleanup
        concurrentManager.shutdown()
    }
    
    /**
     * PED: Test de extension functions para métricas
     */
    @Test
    fun `test metrics extension functions`() {
        // Arrange
        val registry = MetricsConfig.prometheusRegistry
        
        // Act
        registry.incrementCounter("test_extension_counter", "type", "unit_test")
        registry.recordTimer("test_extension_timer", 150, mapOf("operation" to "test"))
        
        val result = registry.timeExecution("test_timed_operation") {
            Thread.sleep(10) // Small delay
            "test_result"
        }
        
        // Assert
        assertEquals("test_result", result, "Timed execution should return result")
        
        val counter = registry.meters
            .filterIsInstance<io.micrometer.core.instrument.Counter>()
            .find { it.id.name == "test_extension_counter" }
        assertNotNull(counter, "Extension counter should exist")
        
        val timer = registry.meters
            .filterIsInstance<io.micrometer.core.instrument.Timer>()
            .find { it.id.name == "test_timed_operation" }
        assertNotNull(timer, "Timed operation timer should exist")
        assertTrue(timer.count() > 0, "Timer should have recorded execution")
    }
    
    /**
     * PED: Test de ObservabilityManager
     */
    @Test
    fun `test observability manager`() = runTest {
        // Arrange
        ObservabilityManager.healthCheckManager = healthCheckManager
        
        // Act
        val metrics = ObservabilityManager.getCurrentMetrics()
        val healthStatus = ObservabilityManager.performHealthCheck()
        
        // Assert
        assertNotNull(metrics, "Current metrics should not be null")
        assertTrue(metrics.containsKey("total_meters"), "Should contain total meters count")
        assertTrue(metrics["total_meters"] is Int, "Total meters should be integer")
        
        assertNotNull(healthStatus, "Health status should not be null")
        assertEquals("healthy", healthStatus.status, "Health status should be healthy")
    }
}

/**
 * PED: Custom assertions para testing de métricas
 */
object MetricsAssertions {
    
    fun assertCounterExists(registry: io.micrometer.core.instrument.MeterRegistry, name: String) {
        val counter = registry.meters
            .filterIsInstance<io.micrometer.core.instrument.Counter>()
            .find { it.id.name == name }
        assertNotNull(counter, "Counter '$name' should exist")
    }
    
    fun assertCounterValue(
        registry: io.micrometer.core.instrument.MeterRegistry, 
        name: String, 
        expectedValue: Double
    ) {
        val counter = registry.meters
            .filterIsInstance<io.micrometer.core.instrument.Counter>()
            .find { it.id.name == name }
        assertNotNull(counter, "Counter '$name' should exist")
        assertEquals(expectedValue, counter.count(), "Counter '$name' should have expected value")
    }
    
    fun assertTimerExists(registry: io.micrometer.core.instrument.MeterRegistry, name: String) {
        val timer = registry.meters
            .filterIsInstance<io.micrometer.core.instrument.Timer>()
            .find { it.id.name == name }
        assertNotNull(timer, "Timer '$name' should exist")
    }
    
    fun assertGaugeExists(registry: io.micrometer.core.instrument.MeterRegistry, name: String) {
        val gauge = registry.meters
            .filterIsInstance<io.micrometer.core.instrument.Gauge>()
            .find { it.id.name == name }
        assertNotNull(gauge, "Gauge '$name' should exist")
    }
}
