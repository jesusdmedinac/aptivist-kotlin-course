
package com.aptivist.kotlin.state

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

/**
 * 🧪 TESTS PARA GLOBAL SCOPE Y ASYNC OPERATIONS
 * 
 * Esta clase de test demuestra:
 * • Testing de CoroutineScope global y lifecycle management
 * • Testing de async operations con timeout
 * • Testing de circuit breaker y rate limiter
 * • Testing de background tasks y scheduling
 * • Testing de error handling y recovery
 * • Testing de performance y monitoring
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GlobalScopeTest {
    
    private lateinit var testScope: TestScope
    
    @BeforeEach
    fun setup() {
        testScope = TestScope()
    }
    
    @AfterEach
    fun cleanup() {
        // PED: Limpiamos cualquier tarea programada
        runBlocking {
            BackgroundTaskManager.cancelAllTasks()
        }
    }
    
    @Nested
    @DisplayName("GlobalScopeProvider Tests")
    inner class GlobalScopeProviderTests {
        
        @Test
        @DisplayName("Should track coroutine metrics")
        fun shouldTrackCoroutineMetrics() = testScope.runTest {
            // Given
            val initialMetrics = GlobalScopeProvider.getMetrics()
            
            // When - Launch some coroutines
            val job1 = GlobalScopeProvider.launchTracked("TestCoroutine1") {
                delay(100)
            }
            val job2 = GlobalScopeProvider.launchTracked("TestCoroutine2") {
                delay(200)
            }
            
            advanceTimeBy(50) // Advance time but not enough to complete
            
            // Then - Should track active coroutines
            val activeMetrics = GlobalScopeProvider.getMetrics()
            assertTrue(activeMetrics.activeCoroutines >= initialMetrics.activeCoroutines + 2)
            
            // When - Complete coroutines
            advanceTimeBy(200)
            job1.join()
            job2.join()
            
            // Then - Should track completed coroutines
            val finalMetrics = GlobalScopeProvider.getMetrics()
            assertTrue(finalMetrics.completedCoroutines >= initialMetrics.completedCoroutines + 2)
        }
        
        @Test
        @DisplayName("Should track failed coroutines")
        fun shouldTrackFailedCoroutines() = testScope.runTest {
            // Given
            val initialMetrics = GlobalScopeProvider.getMetrics()
            
            // When - Launch a failing coroutine
            val job = GlobalScopeProvider.launchTracked("FailingCoroutine") {
                throw RuntimeException("Test error")
            }
            
            advanceUntilIdle()
            
            // Then - Should track failed coroutine
            val finalMetrics = GlobalScopeProvider.getMetrics()
            assertTrue(finalMetrics.failedCoroutines >= initialMetrics.failedCoroutines + 1)
        }
        
        @Test
        @DisplayName("Should list active coroutines")
        fun shouldListActiveCoroutines() = testScope.runTest {
            // When - Launch tracked coroutines
            val job1 = GlobalScopeProvider.launchTracked("LongRunningTask1") {
                delay(1000)
            }
            val job2 = GlobalScopeProvider.launchTracked("LongRunningTask2") {
                delay(1000)
            }
            
            advanceTimeBy(100) // Advance time but not enough to complete
            
            // Then - Should list active coroutines
            val activeCoroutines = GlobalScopeProvider.getActiveCoroutines()
            assertTrue(activeCoroutines.size >= 2)
            assertTrue(activeCoroutines.any { it.name == "LongRunningTask1" })
            assertTrue(activeCoroutines.any { it.name == "LongRunningTask2" })
            
            // Cleanup
            job1.cancel()
            job2.cancel()
        }
    }
    
    @Nested
    @DisplayName("AsyncOperations Tests")
    inner class AsyncOperationsTests {
        
        @Test
        @DisplayName("Should handle successful operations")
        fun shouldHandleSuccessfulOperations() = testScope.runTest {
            // When
            val result = AsyncOperations.withTimeout(1.seconds) {
                delay(100)
                "Success"
            }
            
            // Then
            assertTrue(result.isSuccess())
            assertEquals("Success", result.getOrNull())
        }
        
        @Test
        @DisplayName("Should handle timeout")
        fun shouldHandleTimeout() = testScope.runTest {
            // When
            val result = AsyncOperations.withTimeout(100.milliseconds) {
                delay(200) // Longer than timeout
                "Should not complete"
            }
            
            // Then
            assertTrue(result.isTimeout())
            assertNull(result.getOrNull())
        }
        
        @Test
        @DisplayName("Should handle errors")
        fun shouldHandleErrors() = testScope.runTest {
            // When
            val result = AsyncOperations.withTimeout(1.seconds) {
                throw RuntimeException("Test error")
            }
            
            // Then
            assertTrue(result.isError())
            assertNull(result.getOrNull())
            
            result.fold(
                onSuccess = { fail("Should not be success") },
                onError = { assertEquals("Test error", it.message) },
                onTimeout = { fail("Should not be timeout") },
                onCancelled = { fail("Should not be cancelled") }
            )
        }
        
        @Test
        @DisplayName("Should retry with backoff")
        fun shouldRetryWithBackoff() = testScope.runTest {
            // Given
            var attemptCount = 0
            
            // When
            val result = AsyncOperations.retryWithBackoff(
                maxAttempts = 3,
                initialDelay = 10.milliseconds,
                timeout = 1.seconds
            ) { attempt ->
                attemptCount = attempt
                if (attempt < 3) {
                    throw RuntimeException("Attempt $attempt failed")
                }
                "Success on attempt $attempt"
            }
            
            // Then
            assertTrue(result.isSuccess())
            assertEquals(3, attemptCount)
            assertEquals("Success on attempt 3", result.getOrNull())
        }
        
        @Test
        @DisplayName("Should handle parallel operations")
        fun shouldHandleParallelOperations() = testScope.runTest {
            // Given
            val operations = listOf(
                { delay(100); "Result1" },
                { delay(150); "Result2" },
                { delay(200); "Result3" }
            )
            
            // When
            val result = AsyncOperations.parallel(1.seconds, operations)
            
            // Then
            assertTrue(result.isSuccess())
            val results = result.getOrNull()!!
            assertEquals(3, results.size)
            assertEquals(listOf("Result1", "Result2", "Result3"), results)
        }
    }
    
    @Nested
    @DisplayName("CircuitBreaker Tests")
    inner class CircuitBreakerTests {
        
        @Test
        @DisplayName("Should allow operations when closed")
        fun shouldAllowOperationsWhenClosed() = testScope.runTest {
            // Given
            val circuitBreaker = AsyncOperations.CircuitBreaker(
                failureThreshold = 3,
                recoveryTimeout = 1.seconds
            )
            
            // When
            val result = circuitBreaker.execute {
                "Success"
            }
            
            // Then
            assertTrue(result.isSuccess())
            assertEquals("Success", result.getOrNull())
        }
        
        @Test
        @DisplayName("Should open after failure threshold")
        fun shouldOpenAfterFailureThreshold() = testScope.runTest {
            // Given
            val circuitBreaker = AsyncOperations.CircuitBreaker(
                failureThreshold = 2,
                recoveryTimeout = 1.seconds
            )
            
            // When - Cause failures to reach threshold
            repeat(2) {
                circuitBreaker.execute<Unit> {
                    throw RuntimeException("Test failure")
                }
            }
            
            // Then - Circuit should be open and reject operations
            val result = circuitBreaker.execute {
                "Should not execute"
            }
            
            assertTrue(result.isError())
            assertTrue(result.fold(
                onSuccess = { false },
                onError = { it.message?.contains("Circuit breaker is OPEN") == true },
                onTimeout = { false },
                onCancelled = { false }
            ))
        }
        
        @Test
        @DisplayName("Should recover after timeout")
        fun shouldRecoverAfterTimeout() = testScope.runTest {
            // Given
            val circuitBreaker = AsyncOperations.CircuitBreaker(
                failureThreshold = 1,
                recoveryTimeout = 100.milliseconds
            )
            
            // When - Cause failure to open circuit
            circuitBreaker.execute<Unit> {
                throw RuntimeException("Test failure")
            }
            
            // Wait for recovery timeout
            advanceTimeBy(150)
            
            // Then - Should allow operations again
            val result = circuitBreaker.execute {
                "Recovered"
            }
            
            assertTrue(result.isSuccess())
            assertEquals("Recovered", result.getOrNull())
        }
    }
    
    @Nested
    @DisplayName("RateLimiter Tests")
    inner class RateLimiterTests {
        
        @Test
        @DisplayName("Should allow operations within limit")
        fun shouldAllowOperationsWithinLimit() = testScope.runTest {
            // Given
            val rateLimiter = AsyncOperations.RateLimiter(
                maxOperations = 3,
                timeWindow = 1.seconds
            )
            
            // When - Execute operations within limit
            val results = mutableListOf<AsyncOperations.AsyncResult<String>>()
            repeat(3) { i ->
                val result = rateLimiter.execute {
                    "Operation $i"
                }
                results.add(result)
            }
            
            // Then - All should succeed
            assertTrue(results.all { it.isSuccess() })
        }
        
        @Test
        @DisplayName("Should reject operations over limit")
        fun shouldRejectOperationsOverLimit() = testScope.runTest {
            // Given
            val rateLimiter = AsyncOperations.RateLimiter(
                maxOperations = 2,
                timeWindow = 1.seconds
            )
            
            // When - Execute more operations than limit
            val results = mutableListOf<AsyncOperations.AsyncResult<String>>()
            repeat(3) { i ->
                val result = rateLimiter.execute {
                    "Operation $i"
                }
                results.add(result)
            }
            
            // Then - First two should succeed, third should fail
            assertTrue(results[0].isSuccess())
            assertTrue(results[1].isSuccess())
            assertTrue(results[2].isError())
        }
        
        @Test
        @DisplayName("Should reset after time window")
        fun shouldResetAfterTimeWindow() = testScope.runTest {
            // Given
            val rateLimiter = AsyncOperations.RateLimiter(
                maxOperations = 1,
                timeWindow = 100.milliseconds
            )
            
            // When - Execute operation
            val result1 = rateLimiter.execute { "First" }
            assertTrue(result1.isSuccess())
            
            // Execute another immediately (should fail)
            val result2 = rateLimiter.execute { "Second" }
            assertTrue(result2.isError())
            
            // Wait for time window to pass
            advanceTimeBy(150)
            
            // Execute again (should succeed)
            val result3 = rateLimiter.execute { "Third" }
            assertTrue(result3.isSuccess())
        }
    }
    
    @Nested
    @DisplayName("BackgroundTaskManager Tests")
    inner class BackgroundTaskManagerTests {
        
        @Test
        @DisplayName("Should schedule periodic tasks")
        fun shouldSchedulePeriodicTasks() = testScope.runTest {
            // Given
            var executionCount = 0
            
            // When
            val taskId = BackgroundTaskManager.schedulePeriodicTask(
                name = "TestTask",
                interval = 100.milliseconds
            ) {
                executionCount++
            }
            
            // Advance time to allow multiple executions
            advanceTimeBy(350) // Should execute 3 times
            
            // Then
            assertTrue(executionCount >= 3)
            
            // Cleanup
            BackgroundTaskManager.cancelTask(taskId)
        }
        
        @Test
        @DisplayName("Should cancel tasks")
        fun shouldCancelTasks() = testScope.runTest {
            // Given
            var executionCount = 0
            val taskId = BackgroundTaskManager.schedulePeriodicTask(
                name = "CancellableTask",
                interval = 100.milliseconds
            ) {
                executionCount++
            }
            
            // Let it execute once
            advanceTimeBy(150)
            val countAfterFirst = executionCount
            
            // When - Cancel task
            val cancelled = BackgroundTaskManager.cancelTask(taskId)
            assertTrue(cancelled)
            
            // Advance more time
            advanceTimeBy(300)
            
            // Then - Should not execute more
            assertEquals(countAfterFirst, executionCount)
        }
        
        @Test
        @DisplayName("Should track task information")
        fun shouldTrackTaskInformation() = testScope.runTest {
            // When
            val taskId = BackgroundTaskManager.schedulePeriodicTask(
                name = "TrackedTask",
                interval = 100.milliseconds
            ) {
                // Task body
            }
            
            advanceTimeBy(250) // Allow some executions
            
            // Then
            val task = BackgroundTaskManager.getTaskById(taskId)
            assertNotNull(task)
            assertEquals("TrackedTask", task!!.name)
            assertEquals(100.milliseconds, task.interval)
            assertTrue(task.executionCount >= 2)
            
            // Cleanup
            BackgroundTaskManager.cancelTask(taskId)
        }
        
        @Test
        @DisplayName("Should handle task errors gracefully")
        fun shouldHandleTaskErrorsGracefully() = testScope.runTest {
            // Given
            var executionCount = 0
            
            // When - Schedule task that fails sometimes
            val taskId = BackgroundTaskManager.schedulePeriodicTask(
                name = "ErrorProneTask",
                interval = 100.milliseconds
            ) {
                executionCount++
                if (executionCount == 2) {
                    throw RuntimeException("Task error")
                }
            }
            
            // Let it execute multiple times
            advanceTimeBy(350)
            
            // Then - Should continue executing despite error
            assertTrue(executionCount >= 3)
            
            val task = BackgroundTaskManager.getTaskById(taskId)
            assertNotNull(task)
            assertEquals("Task error", task!!.lastError)
            
            // Cleanup
            BackgroundTaskManager.cancelTask(taskId)
        }
    }
    
    @Nested
    @DisplayName("AsyncOperation DSL Tests")
    inner class AsyncOperationDslTests {
        
        @Test
        @DisplayName("Should execute simple async operation")
        fun shouldExecuteSimpleAsyncOperation() = testScope.runTest {
            // When
            val result = asyncOperation<String> {
                timeout = 1.seconds
                
                operation {
                    delay(100)
                    "DSL Success"
                }
            }
            
            // Then
            assertTrue(result.isSuccess())
            assertEquals("DSL Success", result.getOrNull())
        }
        
        @Test
        @DisplayName("Should handle retry in DSL")
        fun shouldHandleRetryInDsl() = testScope.runTest {
            // Given
            var attemptCount = 0
            
            // When
            val result = asyncOperation<String> {
                timeout = 1.seconds
                retryAttempts = 3
                retryDelay = 10.milliseconds
                
                operation {
                    attemptCount++
                    if (attemptCount < 3) {
                        throw RuntimeException("Attempt $attemptCount failed")
                    }
                    "Success on attempt $attemptCount"
                }
            }
            
            // Then
            assertTrue(result.isSuccess())
            assertEquals(3, attemptCount)
            assertEquals("Success on attempt 3", result.getOrNull())
        }
        
        @Test
        @DisplayName("Should handle timeout in DSL")
        fun shouldHandleTimeoutInDsl() = testScope.runTest {
            // When
            val result = asyncOperation<String> {
                timeout = 100.milliseconds
                
                operation {
                    delay(200) // Longer than timeout
                    "Should not complete"
                }
            }
            
            // Then
            assertTrue(result.isTimeout())
        }
    }
}
