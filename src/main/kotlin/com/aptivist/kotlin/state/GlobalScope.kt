
package com.aptivist.kotlin.state

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes

/**
 * 🎯 PHASE 3.1 - COROUTINESCOPE GLOBAL Y OPERACIONES ASÍNCRONAS
 * 
 * Este archivo demuestra conceptos avanzados de programación asíncrona:
 * • CoroutineScope global para operaciones de larga duración
 * • Structured concurrency con SupervisorJob
 * • Timeout handling con withTimeout y withTimeoutOrNull
 * • Async operations con proper error handling
 * • Resource management y cleanup automático
 * • Monitoring y metrics para coroutines
 * • Cancellation handling y graceful shutdown
 * • Custom dispatchers para diferentes tipos de trabajo
 * 
 * PED: El CoroutineScope global es esencial para operaciones que trascienden
 * el ciclo de vida de componentes individuales:
 * - Background tasks de larga duración
 * - Periodic operations (cleanup, monitoring)
 * - Resource management global
 * - Cross-component communication
 * - System-wide async operations
 */

/**
 * PED: GLOBAL COROUTINE SCOPE PROVIDER
 * 
 * Singleton que proporciona un CoroutineScope global para toda la aplicación.
 * Usa SupervisorJob para aislar errores y evitar que una coroutine fallida
 * cancele todas las demás.
 */
object GlobalScopeProvider {
    
    // PED: SupervisorJob permite que las coroutines hijas fallen independientemente
    private val supervisorJob = SupervisorJob()
    
    // PED: Dispatcher personalizado para operaciones I/O intensivas
    private val ioDispatcher = Dispatchers.IO.limitedParallelism(50)
    
    // PED: Dispatcher para operaciones computacionales
    private val computationDispatcher = Dispatchers.Default
    
    // PED: CoroutineScope principal con exception handler
    val applicationScope: CoroutineScope = CoroutineScope(
        supervisorJob + 
        ioDispatcher + 
        CoroutineName("ApplicationScope") +
        CoroutineExceptionHandler { context, exception ->
            println("❌ Uncaught exception in ${context[CoroutineName]}: ${exception.message}")
            exception.printStackTrace()
        }
    )
    
    // PED: Scope específico para operaciones de background
    val backgroundScope: CoroutineScope = CoroutineScope(
        SupervisorJob(supervisorJob) +
        ioDispatcher +
        CoroutineName("BackgroundScope")
    )
    
    // PED: Scope para operaciones computacionales intensivas
    val computationScope: CoroutineScope = CoroutineScope(
        SupervisorJob(supervisorJob) +
        computationDispatcher +
        CoroutineName("ComputationScope")
    )
    
    // PED: Metrics para monitoreo
    private val activeCoroutines = AtomicLong(0)
    private val completedCoroutines = AtomicLong(0)
    private val failedCoroutines = AtomicLong(0)
    private val coroutineRegistry = ConcurrentHashMap<String, CoroutineInfo>()
    
    /**
     * PED: INFORMACIÓN DE COROUTINES ACTIVAS
     */
    data class CoroutineInfo(
        val id: String,
        val name: String,
        val startTime: Long,
        val scope: String,
        val job: Job
    )
    
    /**
     * PED: MÉTRICAS DEL SCOPE GLOBAL
     */
    data class ScopeMetrics(
        val activeCoroutines: Long,
        val completedCoroutines: Long,
        val failedCoroutines: Long,
        val registeredCoroutines: Int,
        val isActive: Boolean
    )
    
    /**
     * Obtiene métricas actuales del scope
     */
    fun getMetrics(): ScopeMetrics = ScopeMetrics(
        activeCoroutines = activeCoroutines.get(),
        completedCoroutines = completedCoroutines.get(),
        failedCoroutines = failedCoroutines.get(),
        registeredCoroutines = coroutineRegistry.size,
        isActive = applicationScope.isActive
    )
    
    /**
     * Lista todas las coroutines activas
     */
    fun getActiveCoroutines(): List<CoroutineInfo> = 
        coroutineRegistry.values.filter { it.job.isActive }
    
    /**
     * PED: SHUTDOWN GRACEFUL
     * 
     * Cancela todas las coroutines y espera a que terminen.
     */
    suspend fun shutdown(timeout: Duration = 30.seconds) {
        println("🔄 Shutting down GlobalScopeProvider...")
        
        try {
            withTimeout(timeout) {
                // PED: Cancelamos el job principal
                supervisorJob.cancelAndJoin()
            }
            println("✅ GlobalScopeProvider shutdown completed")
        } catch (e: TimeoutCancellationException) {
            println("⚠️ GlobalScopeProvider shutdown timed out, forcing cancellation")
            supervisorJob.cancel()
        }
        
        // PED: Limpiamos el registry
        coroutineRegistry.clear()
    }
    
    /**
     * PED: REGISTRO DE COROUTINES PARA MONITORING
     */
    private fun registerCoroutine(
        id: String,
        name: String,
        scope: String,
        job: Job
    ) {
        val info = CoroutineInfo(
            id = id,
            name = name,
            startTime = System.currentTimeMillis(),
            scope = scope,
            job = job
        )
        
        coroutineRegistry[id] = info
        activeCoroutines.incrementAndGet()
        
        // PED: Cleanup automático cuando la coroutine termina
        job.invokeOnCompletion { exception ->
            coroutineRegistry.remove(id)
            activeCoroutines.decrementAndGet()
            
            if (exception != null) {
                failedCoroutines.incrementAndGet()
            } else {
                completedCoroutines.incrementAndGet()
            }
        }
    }
    
    /**
     * PED: LAUNCH CON REGISTRO AUTOMÁTICO
     */
    fun launchTracked(
        name: String,
        scope: CoroutineScope = applicationScope,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val id = "coroutine-${System.currentTimeMillis()}-${(0..999).random()}"
        val scopeName = scope.coroutineContext[CoroutineName]?.name ?: "Unknown"
        
        val job = scope.launch(
            context = CoroutineName(name),
            start = start
        ) {
            block()
        }
        
        registerCoroutine(id, name, scopeName, job)
        return job
    }
    
    /**
     * PED: ASYNC CON REGISTRO AUTOMÁTICO
     */
    fun <T> asyncTracked(
        name: String,
        scope: CoroutineScope = applicationScope,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        val id = "async-${System.currentTimeMillis()}-${(0..999).random()}"
        val scopeName = scope.coroutineContext[CoroutineName]?.name ?: "Unknown"
        
        val deferred = scope.async(
            context = CoroutineName(name),
            start = start
        ) {
            block()
        }
        
        registerCoroutine(id, name, scopeName, deferred)
        return deferred
    }
}

/**
 * PED: ASYNC OPERATIONS CON TIMEOUT Y ERROR HANDLING
 * 
 * Colección de operaciones asíncronas comunes con manejo robusto
 * de timeouts y errores.
 */
object AsyncOperations {
    
    /**
     * PED: RESULTADO DE OPERACIÓN ASÍNCRONA
     * 
     * Sealed class que encapsula el resultado de operaciones async,
     * incluyendo casos de éxito, error y timeout.
     */
    sealed class AsyncResult<out T> {
        data class Success<T>(val value: T) : AsyncResult<T>()
        data class Error(val exception: Throwable) : AsyncResult<Nothing>()
        object Timeout : AsyncResult<Nothing>()
        object Cancelled : AsyncResult<Nothing>()
        
        /**
         * PED: EXTENSION FUNCTIONS PARA MANEJO FUNCIONAL
         */
        inline fun <R> fold(
            onSuccess: (T) -> R,
            onError: (Throwable) -> R,
            onTimeout: () -> R,
            onCancelled: () -> R
        ): R = when (this) {
            is Success -> onSuccess(value)
            is Error -> onError(exception)
            is Timeout -> onTimeout()
            is Cancelled -> onCancelled()
        }
        
        fun isSuccess(): Boolean = this is Success
        fun isError(): Boolean = this is Error
        fun isTimeout(): Boolean = this is Timeout
        fun isCancelled(): Boolean = this is Cancelled
        
        fun getOrNull(): T? = if (this is Success) value else null
        
        fun getOrThrow(): T = when (this) {
            is Success -> value
            is Error -> throw exception
            is Timeout -> throw TimeoutCancellationException("Operation timed out")
            is Cancelled -> throw CancellationException("Operation was cancelled")
        }
    }
    
    /**
     * PED: EJECUTAR OPERACIÓN CON TIMEOUT
     * 
     * Wrapper que ejecuta cualquier operación suspendible con timeout.
     */
    suspend fun <T> withTimeout(
        timeout: Duration,
        operation: suspend () -> T
    ): AsyncResult<T> = try {
        val result = kotlinx.coroutines.withTimeout(timeout) {
            operation()
        }
        AsyncResult.Success(result)
    } catch (e: TimeoutCancellationException) {
        AsyncResult.Timeout
    } catch (e: CancellationException) {
        AsyncResult.Cancelled
    } catch (e: Exception) {
        AsyncResult.Error(e)
    }
    
    /**
     * PED: EJECUTAR OPERACIÓN CON TIMEOUT OPCIONAL
     * 
     * Similar a withTimeout pero retorna null en caso de timeout.
     */
    suspend fun <T> withTimeoutOrNull(
        timeout: Duration,
        operation: suspend () -> T
    ): T? = try {
        kotlinx.coroutines.withTimeoutOrNull(timeout) {
            operation()
        }
    } catch (e: Exception) {
        println("❌ Operation failed: ${e.message}")
        null
    }
    
    /**
     * PED: RETRY CON BACKOFF EXPONENCIAL
     * 
     * Reintenta una operación con backoff exponencial y timeout.
     */
    suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelay: Duration = 1.seconds,
        maxDelay: Duration = 30.seconds,
        backoffFactor: Double = 2.0,
        timeout: Duration = 1.minutes,
        operation: suspend (attempt: Int) -> T
    ): AsyncResult<T> {
        var currentDelay = initialDelay
        
        repeat(maxAttempts) { attempt ->
            val result = withTimeout(timeout) {
                operation(attempt + 1)
            }
            
            when (result) {
                is AsyncResult.Success -> return result
                is AsyncResult.Timeout -> return result
                is AsyncResult.Cancelled -> return result
                is AsyncResult.Error -> {
                    if (attempt == maxAttempts - 1) {
                        return result // Last attempt failed
                    }
                    
                    // PED: Esperamos antes del siguiente intento
                    delay(currentDelay)
                    currentDelay = minOf(
                        (currentDelay.inWholeMilliseconds * backoffFactor).toLong(),
                        maxDelay.inWholeMilliseconds
                    ).let { Duration.parse("${it}ms") }
                }
            }
        }
        
        return AsyncResult.Error(IllegalStateException("Retry loop completed without result"))
    }
    
    /**
     * PED: OPERACIONES PARALELAS CON TIMEOUT
     * 
     * Ejecuta múltiples operaciones en paralelo con timeout global.
     */
    suspend fun <T> parallel(
        timeout: Duration,
        operations: List<suspend () -> T>
    ): AsyncResult<List<T>> = withTimeout(timeout) {
        operations.map { operation ->
            GlobalScopeProvider.asyncTracked("ParallelOperation") {
                operation()
            }
        }.awaitAll()
    }
    
    /**
     * PED: OPERACIÓN CON CIRCUIT BREAKER
     * 
     * Implementa el patrón Circuit Breaker para operaciones que pueden fallar.
     */
    class CircuitBreaker(
        private val failureThreshold: Int = 5,
        private val recoveryTimeout: Duration = 1.minutes,
        private val halfOpenMaxCalls: Int = 3
    ) {
        private enum class State { CLOSED, OPEN, HALF_OPEN }
        
        private var state = State.CLOSED
        private var failureCount = 0
        private var lastFailureTime = 0L
        private var halfOpenCalls = 0
        
        suspend fun <T> execute(
            timeout: Duration = 30.seconds,
            operation: suspend () -> T
        ): AsyncResult<T> {
            when (state) {
                State.OPEN -> {
                    if (System.currentTimeMillis() - lastFailureTime > recoveryTimeout.inWholeMilliseconds) {
                        state = State.HALF_OPEN
                        halfOpenCalls = 0
                    } else {
                        return AsyncResult.Error(IllegalStateException("Circuit breaker is OPEN"))
                    }
                }
                State.HALF_OPEN -> {
                    if (halfOpenCalls >= halfOpenMaxCalls) {
                        return AsyncResult.Error(IllegalStateException("Circuit breaker HALF_OPEN limit reached"))
                    }
                    halfOpenCalls++
                }
                State.CLOSED -> {
                    // Normal operation
                }
            }
            
            val result = withTimeout(timeout, operation)
            
            when (result) {
                is AsyncResult.Success -> {
                    if (state == State.HALF_OPEN) {
                        state = State.CLOSED
                        failureCount = 0
                    }
                }
                is AsyncResult.Error, AsyncResult.Timeout -> {
                    failureCount++
                    lastFailureTime = System.currentTimeMillis()
                    
                    if (failureCount >= failureThreshold) {
                        state = State.OPEN
                    }
                }
                AsyncResult.Cancelled -> {
                    // No change in circuit breaker state for cancellation
                }
            }
            
            return result
        }
    }
    
    /**
     * PED: RATE LIMITER PARA OPERACIONES ASÍNCRONAS
     * 
     * Limita la frecuencia de ejecución de operaciones.
     */
    class RateLimiter(
        private val maxOperations: Int,
        private val timeWindow: Duration
    ) {
        private val operationTimes = mutableListOf<Long>()
        
        suspend fun <T> execute(
            operation: suspend () -> T
        ): AsyncResult<T> {
            val now = System.currentTimeMillis()
            val windowStart = now - timeWindow.inWholeMilliseconds
            
            // PED: Limpiamos operaciones antiguas
            operationTimes.removeAll { it < windowStart }
            
            if (operationTimes.size >= maxOperations) {
                return AsyncResult.Error(
                    IllegalStateException("Rate limit exceeded: $maxOperations operations per $timeWindow")
                )
            }
            
            operationTimes.add(now)
            
            return try {
                AsyncResult.Success(operation())
            } catch (e: CancellationException) {
                AsyncResult.Cancelled
            } catch (e: Exception) {
                AsyncResult.Error(e)
            }
        }
    }
}

/**
 * PED: BACKGROUND TASKS MANAGER
 * 
 * Gestiona tareas de background con scheduling y lifecycle management.
 */
object BackgroundTaskManager {
    
    private val scheduledTasks = ConcurrentHashMap<String, ScheduledTask>()
    
    /**
     * PED: INFORMACIÓN DE TAREA PROGRAMADA
     */
    data class ScheduledTask(
        val id: String,
        val name: String,
        val interval: Duration,
        val job: Job,
        val startTime: Long,
        var executionCount: Long = 0,
        var lastExecution: Long = 0,
        var lastError: String? = null
    )
    
    /**
     * PED: PROGRAMAR TAREA PERIÓDICA
     * 
     * Programa una tarea para ejecutarse periódicamente.
     */
    fun schedulePeriodicTask(
        name: String,
        interval: Duration,
        initialDelay: Duration = Duration.ZERO,
        task: suspend () -> Unit
    ): String {
        val id = "task-${System.currentTimeMillis()}-${(0..999).random()}"
        
        val job = GlobalScopeProvider.launchTracked("PeriodicTask-$name") {
            if (initialDelay > Duration.ZERO) {
                delay(initialDelay)
            }
            
            while (isActive) {
                try {
                    val startTime = System.currentTimeMillis()
                    task()
                    
                    scheduledTasks[id]?.let { taskInfo ->
                        scheduledTasks[id] = taskInfo.copy(
                            executionCount = taskInfo.executionCount + 1,
                            lastExecution = startTime,
                            lastError = null
                        )
                    }
                } catch (e: Exception) {
                    scheduledTasks[id]?.let { taskInfo ->
                        scheduledTasks[id] = taskInfo.copy(
                            lastError = e.message
                        )
                    }
                    println("❌ Error in periodic task '$name': ${e.message}")
                }
                
                delay(interval)
            }
        }
        
        val scheduledTask = ScheduledTask(
            id = id,
            name = name,
            interval = interval,
            job = job,
            startTime = System.currentTimeMillis()
        )
        
        scheduledTasks[id] = scheduledTask
        return id
    }
    
    /**
     * PED: CANCELAR TAREA PROGRAMADA
     */
    fun cancelTask(taskId: String): Boolean {
        val task = scheduledTasks.remove(taskId)
        return if (task != null) {
            task.job.cancel()
            true
        } else {
            false
        }
    }
    
    /**
     * PED: OBTENER INFORMACIÓN DE TAREAS
     */
    fun getScheduledTasks(): List<ScheduledTask> = scheduledTasks.values.toList()
    
    fun getTaskById(taskId: String): ScheduledTask? = scheduledTasks[taskId]
    
    /**
     * PED: CANCELAR TODAS LAS TAREAS
     */
    suspend fun cancelAllTasks() {
        val tasks = scheduledTasks.values.toList()
        scheduledTasks.clear()
        
        tasks.forEach { task ->
            task.job.cancel()
        }
        
        // PED: Esperamos a que todas las tareas terminen
        tasks.forEach { task ->
            try {
                task.job.join()
            } catch (e: Exception) {
                // Ignore cancellation exceptions
            }
        }
    }
}

/**
 * PED: EXTENSION FUNCTIONS PARA ASYNC OPERATIONS
 * 
 * Funciones de conveniencia para trabajar con operaciones asíncronas.
 */

/**
 * Extension function para ejecutar con timeout en cualquier CoroutineScope
 */
suspend fun <T> CoroutineScope.withTimeout(
    timeout: Duration,
    operation: suspend CoroutineScope.() -> T
): AsyncOperations.AsyncResult<T> = AsyncOperations.withTimeout(timeout) {
    operation()
}

/**
 * Extension function para retry en CoroutineScope
 */
suspend fun <T> CoroutineScope.retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelay: Duration = 1.seconds,
    operation: suspend CoroutineScope.(attempt: Int) -> T
): AsyncOperations.AsyncResult<T> = AsyncOperations.retryWithBackoff(
    maxAttempts = maxAttempts,
    initialDelay = initialDelay,
    operation = { attempt -> operation(attempt) }
)

/**
 * Extension function para crear circuit breaker
 */
fun CoroutineScope.circuitBreaker(
    failureThreshold: Int = 5,
    recoveryTimeout: Duration = 1.minutes
): AsyncOperations.CircuitBreaker = AsyncOperations.CircuitBreaker(
    failureThreshold = failureThreshold,
    recoveryTimeout = recoveryTimeout
)

/**
 * Extension function para crear rate limiter
 */
fun CoroutineScope.rateLimiter(
    maxOperations: Int,
    timeWindow: Duration
): AsyncOperations.RateLimiter = AsyncOperations.RateLimiter(
    maxOperations = maxOperations,
    timeWindow = timeWindow
)

/**
 * PED: DSL PARA CONFIGURACIÓN DE ASYNC OPERATIONS
 * 
 * DSL que permite configurar operaciones asíncronas de manera declarativa.
 */
class AsyncOperationBuilder<T> {
    var timeout: Duration = 30.seconds
    var retryAttempts: Int = 1
    var retryDelay: Duration = 1.seconds
    var circuitBreakerEnabled: Boolean = false
    var rateLimitEnabled: Boolean = false
    var rateLimitOperations: Int = 10
    var rateLimitWindow: Duration = 1.minutes
    
    private var operation: (suspend () -> T)? = null
    
    fun operation(block: suspend () -> T) {
        operation = block
    }
    
    suspend fun execute(): AsyncOperations.AsyncResult<T> {
        val op = operation ?: throw IllegalStateException("Operation not defined")
        
        var wrappedOperation = op
        
        // PED: Aplicamos rate limiting si está habilitado
        if (rateLimitEnabled) {
            val rateLimiter = AsyncOperations.RateLimiter(rateLimitOperations, rateLimitWindow)
            val originalOp = wrappedOperation
            wrappedOperation = {
                rateLimiter.execute { originalOp() }.getOrThrow()
            }
        }
        
        // PED: Aplicamos circuit breaker si está habilitado
        if (circuitBreakerEnabled) {
            val circuitBreaker = AsyncOperations.CircuitBreaker()
            val originalOp = wrappedOperation
            wrappedOperation = {
                circuitBreaker.execute(timeout) { originalOp() }.getOrThrow()
            }
        }
        
        // PED: Aplicamos retry si está configurado
        return if (retryAttempts > 1) {
            AsyncOperations.retryWithBackoff(
                maxAttempts = retryAttempts,
                initialDelay = retryDelay,
                timeout = timeout
            ) { _ ->
                wrappedOperation()
            }
        } else {
            AsyncOperations.withTimeout(timeout, wrappedOperation)
        }
    }
}

/**
 * PED: FUNCIÓN DSL PARA CREAR ASYNC OPERATIONS
 */
suspend fun <T> asyncOperation(
    configure: AsyncOperationBuilder<T>.() -> Unit
): AsyncOperations.AsyncResult<T> {
    val builder = AsyncOperationBuilder<T>()
    builder.configure()
    return builder.execute()
}

/**
 * PED: UTILIDADES PARA MONITORING Y DEBUGGING
 */
object AsyncMonitoring {
    
    /**
     * Monitorea el estado del GlobalScopeProvider
     */
    fun startMonitoring(interval: Duration = 30.seconds): String {
        return BackgroundTaskManager.schedulePeriodicTask(
            name = "AsyncMonitoring",
            interval = interval
        ) {
            val metrics = GlobalScopeProvider.getMetrics()
            val activeCoroutines = GlobalScopeProvider.getActiveCoroutines()
            val scheduledTasks = BackgroundTaskManager.getScheduledTasks()
            
            println("📊 ASYNC MONITORING REPORT")
            println("=" * 40)
            println("Global Scope Metrics:")
            println("  Active coroutines: ${metrics.activeCoroutines}")
            println("  Completed coroutines: ${metrics.completedCoroutines}")
            println("  Failed coroutines: ${metrics.failedCoroutines}")
            println("  Registered coroutines: ${metrics.registeredCoroutines}")
            println("  Scope active: ${metrics.isActive}")
            println()
            println("Active Coroutines:")
            activeCoroutines.take(5).forEach { coroutine ->
                val duration = System.currentTimeMillis() - coroutine.startTime
                println("  - ${coroutine.name} (${coroutine.scope}) - ${duration}ms")
            }
            if (activeCoroutines.size > 5) {
                println("  ... and ${activeCoroutines.size - 5} more")
            }
            println()
            println("Scheduled Tasks:")
            scheduledTasks.forEach { task ->
                println("  - ${task.name}: ${task.executionCount} executions, last: ${task.lastExecution}")
                task.lastError?.let { error ->
                    println("    Last error: $error")
                }
            }
            println("=" * 40)
        }
    }
    
    /**
     * Para el monitoreo
     */
    fun stopMonitoring(monitoringTaskId: String) {
        BackgroundTaskManager.cancelTask(monitoringTaskId)
    }
}
