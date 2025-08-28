
package com.aptivist.kotlin.state

import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes

/**
 * 🎯 PHASE 3.1 - EJEMPLO COMPLETO DE STATE MANAGEMENT
 * 
 * Este archivo demuestra la integración completa de todos los componentes
 * del sistema de manejo de estado:
 * • Store con reactive state management
 * • Actions y reducers para updates inmutables
 * • Global CoroutineScope para operaciones asíncronas
 * • Async operations con timeout y error handling
 * • Background tasks y monitoring
 * • Real-world usage patterns
 * 
 * PED: Este ejemplo muestra cómo todos los componentes trabajan juntos
 * para crear un sistema robusto de manejo de estado que puede:
 * - Manejar operaciones síncronas y asíncronas
 * - Proporcionar updates reactivos del estado
 * - Gestionar recursos de manera eficiente
 * - Manejar errores y timeouts gracefully
 * - Monitorear performance y health del sistema
 */

/**
 * PED: APLICACIÓN DE EJEMPLO
 * 
 * Simula una aplicación real que usa todos los componentes del sistema
 * de state management.
 */
class StateManagementApplication {
    
    // PED: Store principal de la aplicación
    private val store = StoreFactory.createDevelopmentStore()
    
    // PED: Circuit breakers para operaciones críticas
    private val serverCircuitBreaker = AsyncOperations.CircuitBreaker(
        failureThreshold = 3,
        recoveryTimeout = 1.minutes
    )
    
    private val pluginCircuitBreaker = AsyncOperations.CircuitBreaker(
        failureThreshold = 5,
        recoveryTimeout = 30.seconds
    )
    
    // PED: Rate limiter para operaciones de conexión
    private val connectionRateLimiter = AsyncOperations.RateLimiter(
        maxOperations = 10,
        timeWindow = 1.minutes
    )
    
    // PED: IDs de tareas programadas
    private var monitoringTaskId: String? = null
    private var cleanupTaskId: String? = null
    private var healthCheckTaskId: String? = null
    
    /**
     * PED: INICIALIZACIÓN DE LA APLICACIÓN
     * 
     * Configura el store, suscripciones, y tareas de background.
     */
    suspend fun initialize() {
        println("🚀 Initializing State Management Application...")
        
        // PED: Configuramos suscripciones reactivas
        setupSubscriptions()
        
        // PED: Iniciamos tareas de background
        startBackgroundTasks()
        
        // PED: Iniciamos el servidor
        startServer()
        
        println("✅ Application initialized successfully")
    }
    
    /**
     * PED: CONFIGURACIÓN DE SUSCRIPCIONES
     * 
     * Demuestra diferentes tipos de suscripciones reactivas.
     */
    private fun setupSubscriptions() {
        // PED: Suscripción general a cambios de estado
        store.subscribe { oldState, newState ->
            if (oldState.metadata.version != newState.metadata.version) {
                println("📝 State updated: v${oldState.metadata.version} -> v${newState.metadata.version}")
            }
        }
        
        // PED: Suscripción específica al servidor
        store.subscribeToServer { serverState ->
            when {
                serverState.isRunning && serverState.lastError == null -> {
                    println("✅ Server is healthy on ${serverState.address}")
                }
                serverState.isRunning && serverState.lastError != null -> {
                    println("⚠️ Server running with error: ${serverState.lastError}")
                }
                !serverState.isRunning -> {
                    println("🔴 Server is stopped")
                }
            }
        }
        
        // PED: Suscripción a conexiones con filtrado
        store.subscribeToConnections { connectionState ->
            val activeCount = connectionState.activeConnections.size
            val pendingCount = connectionState.pendingConnections.size
            
            if (activeCount > 0 || pendingCount > 0) {
                println("🔗 Connections: $activeCount active, $pendingCount pending")
            }
            
            // PED: Alertas por alta carga
            if (activeCount > connectionState.maxConcurrentConnections * 0.8) {
                GlobalScopeProvider.launchTracked("HighLoadAlert") {
                    store.dispatch(
                        ActionCreators.UI.warning(
                            "High Load",
                            "Connection count is approaching limit: $activeCount/${connectionState.maxConcurrentConnections}"
                        )
                    )
                }
            }
        }
        
        // PED: Suscripción a plugins con manejo de errores
        store.subscribeToPlugins { pluginState ->
            val activePlugins = pluginState.activePlugins.size
            val errorPlugins = pluginState.errorPlugins.size
            
            if (errorPlugins > 0) {
                println("❌ $errorPlugins plugins in error state")
                
                // PED: Intentamos reactivar plugins con errores
                GlobalScopeProvider.launchTracked("PluginRecovery") {
                    pluginState.errorPlugins.forEach { plugin ->
                        delay(5.seconds) // Wait before retry
                        store.dispatch(ActionCreators.Plugin.activate(plugin.id))
                    }
                }
            }
            
            if (activePlugins > 0) {
                println("🔌 $activePlugins plugins active")
            }
        }
        
        // PED: Suscripción a notificaciones con auto-dismiss
        store.subscribeToNotifications { notifications ->
            val unreadCount = notifications.count { !it.read }
            if (unreadCount > 0) {
                println("🔔 $unreadCount unread notifications")
                
                // PED: Auto-dismiss notifications after 5 minutes
                GlobalScopeProvider.launchTracked("NotificationAutoDismiss") {
                    delay(5.minutes)
                    val oldNotifications = notifications.filter { 
                        System.currentTimeMillis() - it.timestamp > 5.minutes.inWholeMilliseconds 
                    }
                    oldNotifications.forEach { notification ->
                        store.dispatch(ActionCreators.UI.dismissNotification(notification.id))
                    }
                }
            }
        }
    }
    
    /**
     * PED: INICIO DE TAREAS DE BACKGROUND
     * 
     * Programa tareas periódicas para mantenimiento y monitoreo.
     */
    private fun startBackgroundTasks() {
        // PED: Monitoreo del sistema
        monitoringTaskId = BackgroundTaskManager.schedulePeriodicTask(
            name = "SystemMonitoring",
            interval = 30.seconds,
            initialDelay = 10.seconds
        ) {
            val metrics = GlobalScopeProvider.getMetrics()
            val currentState = store.state.value
            
            // PED: Actualizamos estadísticas del servidor
            if (currentState.serverState.isRunning) {
                store.dispatch(
                    ActionCreators.Server.recordRequest(
                        success = true,
                        responseTime = (50..200).random().toDouble(),
                        bytesTransferred = (100..1000).random().toLong()
                    )
                )
            }
            
            // PED: Reportamos métricas
            println("📊 System Metrics - Active coroutines: ${metrics.activeCoroutines}, " +
                   "Completed: ${metrics.completedCoroutines}, Failed: ${metrics.failedCoroutines}")
        }
        
        // PED: Limpieza periódica
        cleanupTaskId = BackgroundTaskManager.schedulePeriodicTask(
            name = "PeriodicCleanup",
            interval = 5.minutes,
            initialDelay = 1.minutes
        ) {
            // PED: Limpiamos historial de conexiones antiguas
            store.dispatch(ActionCreators.Connection.clearOldHistory(24))
            
            // PED: Limpiamos errores de plugins antiguos
            store.dispatch(AppAction.Plugin.ClearErrors())
            
            // PED: Limpiamos notificaciones leídas
            val currentState = store.state.value
            val readNotifications = currentState.uiState.notifications
                .filter { it.read && System.currentTimeMillis() - it.timestamp > 1.minutes.inWholeMilliseconds }
            
            readNotifications.forEach { notification ->
                store.dispatch(ActionCreators.UI.dismissNotification(notification.id))
            }
            
            println("🧹 Cleanup completed")
        }
        
        // PED: Health check del servidor
        healthCheckTaskId = BackgroundTaskManager.schedulePeriodicTask(
            name = "ServerHealthCheck",
            interval = 1.minutes
        ) {
            val currentState = store.state.value
            if (currentState.serverState.isRunning) {
                // PED: Simulamos health check con circuit breaker
                val healthResult = serverCircuitBreaker.execute(timeout = 10.seconds) {
                    // PED: Simulamos operación de health check
                    delay(100) // Simulated network call
                    if ((1..10).random() > 8) { // 20% chance of failure
                        throw RuntimeException("Health check failed")
                    }
                    "OK"
                }
                
                when (healthResult) {
                    is AsyncOperations.AsyncResult.Success -> {
                        if (currentState.serverState.lastError != null) {
                            store.dispatch(ActionCreators.Server.clearError())
                        }
                    }
                    is AsyncOperations.AsyncResult.Error -> {
                        store.dispatch(ActionCreators.Server.setError("Health check failed: ${healthResult.exception.message}"))
                    }
                    is AsyncOperations.AsyncResult.Timeout -> {
                        store.dispatch(ActionCreators.Server.setError("Health check timed out"))
                    }
                    is AsyncOperations.AsyncResult.Cancelled -> {
                        println("Health check was cancelled")
                    }
                }
            }
        }
    }
    
    /**
     * PED: INICIO DEL SERVIDOR CON ASYNC OPERATIONS
     * 
     * Demuestra el uso de async operations con timeout y error handling.
     */
    private suspend fun startServer() {
        val startResult = asyncOperation<Unit> {
            timeout = 30.seconds
            retryAttempts = 3
            retryDelay = 5.seconds
            
            operation {
                // PED: Simulamos inicio del servidor
                delay(2.seconds) // Simulated startup time
                
                if ((1..10).random() > 7) { // 30% chance of failure
                    throw RuntimeException("Failed to bind to port")
                }
                
                println("🌐 Server started successfully")
            }
        }
        
        when (startResult) {
            is AsyncOperations.AsyncResult.Success -> {
                store.dispatch(ActionCreators.Server.start(port = 8080))
                store.dispatch(ActionCreators.UI.success("Server Started", "Server is now running on port 8080"))
            }
            is AsyncOperations.AsyncResult.Error -> {
                store.dispatch(ActionCreators.Server.setError("Failed to start server: ${startResult.exception.message}"))
                store.dispatch(ActionCreators.UI.error("Server Error", "Failed to start server"))
            }
            is AsyncOperations.AsyncResult.Timeout -> {
                store.dispatch(ActionCreators.Server.setError("Server startup timed out"))
                store.dispatch(ActionCreators.UI.error("Timeout", "Server startup timed out"))
            }
            is AsyncOperations.AsyncResult.Cancelled -> {
                println("Server startup was cancelled")
            }
        }
    }
    
    /**
     * PED: SIMULACIÓN DE CONEXIONES DE CLIENTES
     * 
     * Demuestra el manejo de conexiones con rate limiting y async operations.
     */
    suspend fun simulateClientConnections(count: Int = 5) {
        println("🔗 Simulating $count client connections...")
        
        repeat(count) { i ->
            GlobalScopeProvider.launchTracked("ClientConnection-$i") {
                val connectionResult = connectionRateLimiter.execute {
                    // PED: Simulamos establecimiento de conexión
                    delay((100..500).random().toLong())
                    
                    val clientInfo = ClientInfo(
                        userAgent = "TestClient/1.0",
                        ipAddress = "192.168.1.${100 + i}",
                        version = "1.0.0",
                        capabilities = setOf("test", "demo")
                    )
                    
                    ActionCreators.Connection.connect(
                        id = "conn-$i",
                        type = ConnectionType.WEBSOCKET,
                        clientInfo = clientInfo
                    )
                }
                
                when (connectionResult) {
                    is AsyncOperations.AsyncResult.Success -> {
                        store.dispatch(connectionResult.value)
                        
                        // PED: Simulamos actividad de la conexión
                        repeat(5) { messageIndex ->
                            delay(1000) // 1 second
                            store.dispatch(
                                ActionCreators.Connection.updateActivity(
                                    connectionId = "conn-$i",
                                    messageIncrement = 1,
                                    bytesTransferred = (50..200).random().toLong()
                                )
                            )
                        }
                        
                        // PED: Desconectamos después de un tiempo
                        delay(10.seconds)
                        store.dispatch(ActionCreators.Connection.disconnect("conn-$i", "Client disconnected"))
                    }
                    is AsyncOperations.AsyncResult.Error -> {
                        println("❌ Failed to connect client $i: ${connectionResult.exception.message}")
                        store.dispatch(ActionCreators.UI.warning("Connection Failed", "Client $i failed to connect"))
                    }
                    else -> {
                        println("⚠️ Connection attempt $i was rate limited or timed out")
                    }
                }
            }
        }
    }
    
    /**
     * PED: SIMULACIÓN DE CARGA DE PLUGINS
     * 
     * Demuestra operaciones asíncronas con plugins y circuit breaker.
     */
    suspend fun simulatePluginLoading() {
        println("🔌 Loading plugins...")
        
        val pluginNames = listOf("EchoPlugin", "UtilityPlugin", "DatabasePlugin", "CachePlugin", "LoggingPlugin")
        
        pluginNames.forEachIndexed { index, pluginName ->
            GlobalScopeProvider.launchTracked("PluginLoader-$pluginName") {
                val loadResult = pluginCircuitBreaker.execute(timeout = 15.seconds) {
                    // PED: Simulamos carga de plugin
                    delay((1..3).random().seconds)
                    
                    if ((1..10).random() > 8) { // 20% chance of failure
                        throw RuntimeException("Plugin dependency not found")
                    }
                    
                    ActionCreators.Plugin.load(
                        id = "plugin-$index",
                        name = pluginName,
                        version = "1.0.0"
                    )
                }
                
                when (loadResult) {
                    is AsyncOperations.AsyncResult.Success -> {
                        store.dispatch(loadResult.value)
                        delay(1000) // Simulate initialization - 1 second
                        store.dispatch(ActionCreators.Plugin.activate("plugin-$index"))
                        
                        // PED: Registramos algunos comandos
                        repeat(3) { cmdIndex ->
                            store.dispatch(
                                ActionCreators.Plugin.registerCommand(
                                    pluginId = "plugin-$index",
                                    commandName = "${pluginName.lowercase()}_command_$cmdIndex"
                                )
                            )
                        }
                        
                        store.dispatch(ActionCreators.UI.info("Plugin Loaded", "$pluginName loaded successfully"))
                    }
                    is AsyncOperations.AsyncResult.Error -> {
                        store.dispatch(
                            ActionCreators.Plugin.reportError(
                                pluginId = "plugin-$index",
                                error = "Failed to load: ${loadResult.exception.message}",
                                severity = ErrorSeverity.HIGH
                            )
                        )
                        store.dispatch(ActionCreators.UI.error("Plugin Error", "Failed to load $pluginName"))
                    }
                    is AsyncOperations.AsyncResult.Timeout -> {
                        store.dispatch(
                            ActionCreators.Plugin.reportError(
                                pluginId = "plugin-$index",
                                error = "Plugin loading timed out",
                                severity = ErrorSeverity.MEDIUM
                            )
                        )
                    }
                    is AsyncOperations.AsyncResult.Cancelled -> {
                        println("Plugin loading for $pluginName was cancelled")
                    }
                }
            }
        }
    }
    
    /**
     * PED: EJECUCIÓN DE LA DEMO
     * 
     * Ejecuta una demostración completa del sistema.
     */
    suspend fun runDemo() {
        println("🎬 Starting State Management Demo...")
        
        // PED: Esperamos un poco para que se establezcan las suscripciones
        delay(2.seconds)
        
        // PED: Cargamos plugins
        simulatePluginLoading()
        delay(5.seconds)
        
        // PED: Simulamos conexiones de clientes
        simulateClientConnections(3)
        delay(10.seconds)
        
        // PED: Cambiamos algunas configuraciones de UI
        store.dispatch(ActionCreators.UI.setTheme(Theme.LIGHT))
        store.dispatch(ActionCreators.UI.navigateTo("connections"))
        
        // PED: Agregamos algunas notificaciones
        store.dispatch(ActionCreators.UI.info("Demo", "State management demo is running"))
        store.dispatch(ActionCreators.UI.warning("Memory", "Memory usage is at 75%"))
        
        delay(5.seconds)
        
        // PED: Mostramos el estado final
        val finalState = store.state.value
        println("\n📊 FINAL STATE SUMMARY")
        println("=".repeat(50))
        println("Server: ${if (finalState.serverState.isRunning) "Running" else "Stopped"} on ${finalState.serverState.address}")
        println("Connections: ${finalState.connectionState.activeConnections.size} active")
        println("Plugins: ${finalState.pluginState.activePlugins.size} active, ${finalState.pluginState.errorPlugins.size} errors")
        println("Notifications: ${finalState.uiState.notifications.size} total")
        println("Theme: ${finalState.uiState.theme}")
        println("Requests processed: ${finalState.serverState.statistics.totalRequests}")
        println("Success rate: ${"%.1f".format(finalState.serverState.statistics.successRate)}%")
        println("=".repeat(50))
    }
    
    /**
     * PED: SHUTDOWN GRACEFUL
     * 
     * Cierra la aplicación de manera limpia.
     */
    suspend fun shutdown() {
        println("🔄 Shutting down application...")
        
        // PED: Cancelamos tareas programadas
        monitoringTaskId?.let { BackgroundTaskManager.cancelTask(it) }
        cleanupTaskId?.let { BackgroundTaskManager.cancelTask(it) }
        healthCheckTaskId?.let { BackgroundTaskManager.cancelTask(it) }
        
        // PED: Paramos el servidor
        store.dispatch(ActionCreators.Server.stop())
        
        // PED: Desconectamos todas las conexiones
        val currentState = store.state.value
        currentState.connectionState.activeConnections.keys.forEach { connectionId ->
            store.dispatch(ActionCreators.Connection.disconnect(connectionId, "Server shutdown"))
        }
        
        // PED: Descargamos todos los plugins
        currentState.pluginState.loadedPlugins.keys.forEach { pluginId ->
            store.dispatch(AppAction.Plugin.Unload(pluginId))
        }
        
        // PED: Cerramos el store
        store.close()
        
        // PED: Shutdown del GlobalScopeProvider
        GlobalScopeProvider.shutdown()
        
        println("✅ Application shutdown completed")
    }
}

/**
 * PED: FUNCIÓN MAIN PARA EJECUTAR EL EJEMPLO
 * 
 * Punto de entrada que demuestra el uso completo del sistema.
 */
suspend fun main() {
    val application = StateManagementApplication()
    
    try {
        // PED: Iniciamos la aplicación
        application.initialize()
        
        // PED: Ejecutamos la demo
        application.runDemo()
        
        // PED: Esperamos un poco más para ver el sistema en acción
        delay(30.seconds)
        
    } catch (e: Exception) {
        println("❌ Application error: ${e.message}")
        e.printStackTrace()
    } finally {
        // PED: Shutdown graceful
        application.shutdown()
    }
}
