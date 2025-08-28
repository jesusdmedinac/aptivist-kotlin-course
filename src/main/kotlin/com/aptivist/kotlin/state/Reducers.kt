
package com.aptivist.kotlin.state

/**
 * 🎯 PHASE 3.1 - SISTEMA DE REDUCERS PARA STATE MANAGEMENT
 * 
 * Este archivo demuestra conceptos avanzados de functional programming:
 * • Pure functions para transformaciones de estado
 * • Pattern matching exhaustivo con when expressions
 * • Immutable updates usando data class copy()
 * • Function composition y higher-order functions
 * • Type-safe state transformations
 * • Reducer composition para modularidad
 * 
 * PED: Los reducers son pure functions que toman el estado actual y una acción,
 * y retornan un nuevo estado. Son el corazón de la arquitectura Redux/Flux:
 * - Son predecibles (misma entrada = misma salida)
 * - No tienen side effects
 * - Son fáciles de testear
 * - Permiten time-travel debugging
 * - Facilitan la concurrencia y paralelización
 */

/**
 * PED: TIPO FUNCIONAL PARA REDUCERS
 * 
 * Un reducer es una función que toma un estado y una acción,
 * y retorna un nuevo estado. Esta definición de tipo hace
 * explícito el contrato de los reducers.
 */
typealias Reducer<State, Action> = (State, Action) -> State

/**
 * PED: REDUCER PRINCIPAL DE LA APLICACIÓN
 * 
 * Este es el root reducer que combina todos los reducers específicos.
 * Demuestra el patrón de composition de reducers y pattern matching exhaustivo.
 * 
 * @param state Estado actual de la aplicación
 * @param action Acción a procesar
 * @return Nuevo estado después de aplicar la acción
 */
fun appReducer(state: AppState, action: AppAction): AppState {
    // PED: Pattern matching exhaustivo usando when expression
    // El compilador verifica que manejemos todos los casos de la sealed class
    return when (action) {
        // PED: Delegamos a reducers específicos para cada tipo de acción
        is AppAction.Server -> state.copy(
            serverState = serverReducer(state.serverState, action),
            metadata = state.metadata.copy(lastUpdated = System.currentTimeMillis())
        )
        
        is AppAction.Connection -> state.copy(
            connectionState = connectionReducer(state.connectionState, action),
            metadata = state.metadata.copy(lastUpdated = System.currentTimeMillis())
        )
        
        is AppAction.Plugin -> state.copy(
            pluginState = pluginReducer(state.pluginState, action),
            metadata = state.metadata.copy(lastUpdated = System.currentTimeMillis())
        )
        
        is AppAction.UI -> state.copy(
            uiState = uiReducer(state.uiState, action),
            metadata = state.metadata.copy(lastUpdated = System.currentTimeMillis())
        )
        
        is AppAction.System -> systemReducer(state, action)
    }.withUpdatedTimestamp() // PED: Extension function para actualizar timestamp
}

/**
 * PED: REDUCER DEL SERVIDOR
 * 
 * Maneja todas las acciones relacionadas con el estado del servidor.
 * Demuestra transformaciones inmutables y validación de estado.
 */
fun serverReducer(state: ServerState, action: AppAction.Server): ServerState {
    return when (action) {
        is AppAction.Server.Start -> {
            // PED: Validación antes de la transformación
            if (state.isRunning) {
                // Si ya está corriendo, solo actualizamos la configuración
                state.copy(
                    port = action.port,
                    host = action.host,
                    protocol = action.protocol,
                    maxConnections = action.maxConnections,
                    lastError = null
                )
            } else {
                // Iniciamos el servidor
                state.copy(
                    isRunning = true,
                    port = action.port,
                    host = action.host,
                    protocol = action.protocol,
                    maxConnections = action.maxConnections,
                    startTime = System.currentTimeMillis(),
                    lastError = null
                )
            }
        }
        
        is AppAction.Server.Stop -> {
            if (state.isRunning) {
                state.copy(
                    isRunning = false,
                    startTime = null,
                    lastError = null,
                    // PED: Reseteamos las estadísticas al parar
                    statistics = ServerStatistics()
                )
            } else {
                state // No change if already stopped
            }
        }
        
        is AppAction.Server.UpdateCapabilities -> state.copy(
            capabilities = action.capabilities
        )
        
        is AppAction.Server.UpdateStatistics -> {
            val currentStats = state.statistics
            val newStats = currentStats.copy(
                totalRequests = currentStats.totalRequests + action.requestsIncrement,
                successfulRequests = currentStats.successfulRequests + action.successIncrement,
                failedRequests = currentStats.failedRequests + action.failIncrement,
                // PED: Calculamos el promedio de tiempo de respuesta
                averageResponseTime = action.responseTime?.let { newTime ->
                    if (currentStats.totalRequests == 0L) {
                        newTime
                    } else {
                        // Weighted average
                        val totalRequests = currentStats.totalRequests + action.requestsIncrement
                        ((currentStats.averageResponseTime * currentStats.totalRequests) + 
                         (newTime * action.requestsIncrement)) / totalRequests
                    }
                } ?: currentStats.averageResponseTime,
                bytesTransferred = currentStats.bytesTransferred + action.bytesTransferred
            )
            
            state.copy(statistics = newStats)
        }
        
        is AppAction.Server.SetError -> state.copy(
            lastError = action.error
        )
        
        is AppAction.Server.ClearError -> state.copy(
            lastError = null
        )
    }
}

/**
 * PED: REDUCER DE CONEXIONES
 * 
 * Maneja el estado de las conexiones de clientes.
 * Demuestra operaciones inmutables en collections.
 */
fun connectionReducer(state: ConnectionState, action: AppAction.Connection): ConnectionState {
    return when (action) {
        is AppAction.Connection.Add -> {
            // PED: Verificamos capacidad antes de agregar
            if (state.hasCapacity()) {
                val newConnections = state.activeConnections + (action.connection.id to action.connection)
                val connectEvent = ConnectionEvent.Connected(
                    connectionId = action.connection.id,
                    timestamp = action.connection.establishedAt,
                    clientInfo = action.connection.clientInfo
                )
                
                state.copy(
                    activeConnections = newConnections,
                    connectionHistory = state.connectionHistory + connectEvent
                )
            } else {
                // PED: Si no hay capacidad, agregamos a pending
                state.copy(
                    pendingConnections = state.pendingConnections + action.connection.id
                )
            }
        }
        
        is AppAction.Connection.Remove -> {
            val removedConnection = state.activeConnections[action.connectionId]
            if (removedConnection != null) {
                val disconnectEvent = ConnectionEvent.Disconnected(
                    connectionId = action.connectionId,
                    timestamp = System.currentTimeMillis(),
                    reason = action.reason
                )
                
                state.copy(
                    activeConnections = state.activeConnections - action.connectionId,
                    connectionHistory = state.connectionHistory + disconnectEvent
                )
            } else {
                // PED: Removemos de pending si está ahí
                state.copy(
                    pendingConnections = state.pendingConnections - action.connectionId
                )
            }
        }
        
        is AppAction.Connection.UpdateStatus -> {
            val connection = state.activeConnections[action.connectionId]
            if (connection != null) {
                val updatedConnection = connection.copy(
                    status = action.status,
                    lastActivity = System.currentTimeMillis()
                )
                
                state.copy(
                    activeConnections = state.activeConnections + (action.connectionId to updatedConnection)
                )
            } else {
                state // Connection not found, no change
            }
        }
        
        is AppAction.Connection.UpdateActivity -> {
            val connection = state.activeConnections[action.connectionId]
            if (connection != null) {
                val updatedConnection = connection.copy(
                    messageCount = connection.messageCount + action.messageCount,
                    bytesTransferred = connection.bytesTransferred + action.bytesTransferred,
                    lastActivity = System.currentTimeMillis()
                )
                
                state.copy(
                    activeConnections = state.activeConnections + (action.connectionId to updatedConnection)
                )
            } else {
                state
            }
        }
        
        is AppAction.Connection.AddEvent -> state.copy(
            connectionHistory = state.connectionHistory + action.event
        )
        
        is AppAction.Connection.ClearHistory -> {
            val filteredHistory = if (action.olderThan != null) {
                state.connectionHistory.filter { it.timestamp >= action.olderThan }
            } else {
                emptyList()
            }
            
            state.copy(connectionHistory = filteredHistory)
        }
    }
}

/**
 * PED: REDUCER DE PLUGINS
 * 
 * Gestiona el estado del sistema de plugins.
 * Demuestra manejo complejo de estado anidado.
 */
fun pluginReducer(state: PluginState, action: AppAction.Plugin): PluginState {
    return when (action) {
        is AppAction.Plugin.Load -> {
            val newPlugins = state.loadedPlugins + (action.pluginInfo.id to action.pluginInfo)
            state.copy(loadedPlugins = newPlugins)
        }
        
        is AppAction.Plugin.Unload -> {
            val plugin = state.loadedPlugins[action.pluginId]
            if (plugin != null) {
                // PED: Limpiamos todos los datos relacionados con el plugin
                val newRegistry = state.pluginRegistry.copy(
                    registeredCommands = state.pluginRegistry.registeredCommands
                        .filterValues { it != action.pluginId },
                    eventHandlers = state.pluginRegistry.eventHandlers
                        .mapValues { (_, handlers) -> handlers.filter { it != action.pluginId } }
                        .filterValues { it.isNotEmpty() },
                    capabilities = state.pluginRegistry.capabilities - action.pluginId
                )
                
                state.copy(
                    loadedPlugins = state.loadedPlugins - action.pluginId,
                    pluginRegistry = newRegistry,
                    loadingErrors = state.loadingErrors.filter { it.pluginId != action.pluginId }
                )
            } else {
                state
            }
        }
        
        is AppAction.Plugin.UpdateStatus -> {
            val plugin = state.loadedPlugins[action.pluginId]
            if (plugin != null) {
                val updatedPlugin = plugin.copy(
                    status = action.status,
                    lastActivity = System.currentTimeMillis()
                )
                
                state.copy(
                    loadedPlugins = state.loadedPlugins + (action.pluginId to updatedPlugin)
                )
            } else {
                state
            }
        }
        
        is AppAction.Plugin.UpdateActivity -> {
            val plugin = state.loadedPlugins[action.pluginId]
            if (plugin != null) {
                val updatedPlugin = plugin.copy(
                    executionCount = plugin.executionCount + action.executionIncrement,
                    errorCount = plugin.errorCount + action.errorIncrement,
                    lastActivity = System.currentTimeMillis()
                )
                
                state.copy(
                    loadedPlugins = state.loadedPlugins + (action.pluginId to updatedPlugin)
                )
            } else {
                state
            }
        }
        
        is AppAction.Plugin.RegisterCommand -> {
            val newCommands = state.pluginRegistry.registeredCommands + 
                (action.commandName to action.pluginId)
            
            // PED: Actualizamos el contador de comandos del plugin
            val plugin = state.loadedPlugins[action.pluginId]
            val updatedPlugins = if (plugin != null) {
                val updatedPlugin = plugin.copy(commandCount = plugin.commandCount + 1)
                state.loadedPlugins + (action.pluginId to updatedPlugin)
            } else {
                state.loadedPlugins
            }
            
            state.copy(
                loadedPlugins = updatedPlugins,
                pluginRegistry = state.pluginRegistry.copy(registeredCommands = newCommands)
            )
        }
        
        is AppAction.Plugin.UnregisterCommand -> {
            val newCommands = state.pluginRegistry.registeredCommands - action.commandName
            
            // PED: Decrementamos el contador del plugin afectado
            val affectedPluginId = state.pluginRegistry.registeredCommands[action.commandName]
            val updatedPlugins = if (affectedPluginId != null) {
                val plugin = state.loadedPlugins[affectedPluginId]
                if (plugin != null) {
                    val updatedPlugin = plugin.copy(
                        commandCount = maxOf(0, plugin.commandCount - 1)
                    )
                    state.loadedPlugins + (affectedPluginId to updatedPlugin)
                } else {
                    state.loadedPlugins
                }
            } else {
                state.loadedPlugins
            }
            
            state.copy(
                loadedPlugins = updatedPlugins,
                pluginRegistry = state.pluginRegistry.copy(registeredCommands = newCommands)
            )
        }
        
        is AppAction.Plugin.AddError -> state.copy(
            loadingErrors = state.loadingErrors + action.error
        )
        
        is AppAction.Plugin.ClearErrors -> {
            val filteredErrors = if (action.pluginId != null) {
                state.loadingErrors.filter { it.pluginId != action.pluginId }
            } else {
                emptyList()
            }
            
            state.copy(loadingErrors = filteredErrors)
        }
    }
}

/**
 * PED: REDUCER DE UI
 * 
 * Maneja el estado de la interfaz de usuario.
 * Demuestra operaciones en listas inmutables.
 */
fun uiReducer(state: UiState, action: AppAction.UI): UiState {
    return when (action) {
        is AppAction.UI.SetTheme -> state.copy(theme = action.theme)
        
        is AppAction.UI.SetLanguage -> state.copy(language = action.language)
        
        is AppAction.UI.SetActiveView -> state.copy(activeView = action.view)
        
        is AppAction.UI.AddNotification -> state.copy(
            notifications = state.notifications + action.notification
        )
        
        is AppAction.UI.RemoveNotification -> state.copy(
            notifications = state.notifications.filter { it.id != action.notificationId }
        )
        
        is AppAction.UI.MarkNotificationsAsRead -> {
            val updatedNotifications = state.notifications.map { notification ->
                if (notification.id in action.notificationIds) {
                    notification.copy(read = true)
                } else {
                    notification
                }
            }
            
            state.copy(notifications = updatedNotifications)
        }
        
        is AppAction.UI.ClearAllNotifications -> state.copy(
            notifications = emptyList()
        )
        
        is AppAction.UI.UpdatePreferences -> state.copy(
            preferences = action.preferences
        )
    }
}

/**
 * PED: REDUCER DEL SISTEMA
 * 
 * Maneja acciones que afectan todo el estado de la aplicación.
 * Demuestra el manejo de acciones especiales como reset y batch.
 */
fun systemReducer(state: AppState, action: AppAction.System): AppState {
    return when (action) {
        is AppAction.System.Reset -> {
            // PED: Creamos un estado completamente nuevo
            AppState().copy(
                metadata = StateMetadata(
                    version = state.metadata.version + 1,
                    source = "reset",
                    tags = setOf("reset")
                )
            )
        }
        
        is AppAction.System.LoadState -> {
            // PED: Cargamos el estado proporcionado, pero actualizamos metadatos
            action.state.copy(
                metadata = action.state.metadata.copy(
                    lastUpdated = System.currentTimeMillis(),
                    source = "loaded"
                )
            )
        }
        
        is AppAction.System.UpdateMetadata -> {
            val currentMetadata = state.metadata
            val updatedMetadata = currentMetadata.copy(
                source = action.source ?: currentMetadata.source,
                tags = action.tags ?: currentMetadata.tags,
                version = action.version ?: currentMetadata.version,
                lastUpdated = System.currentTimeMillis()
            )
            
            state.copy(metadata = updatedMetadata)
        }
        
        is AppAction.System.Batch -> {
            // PED: Aplicamos todas las acciones secuencialmente
            // Esto demuestra function composition y fold operation
            action.actions.fold(state) { currentState, batchAction ->
                appReducer(currentState, batchAction)
            }
        }
    }
}

/**
 * PED: HIGHER-ORDER FUNCTIONS PARA COMPOSICIÓN DE REDUCERS
 * 
 * Estas funciones demuestran conceptos avanzados de functional programming
 * para crear reducers más modulares y reutilizables.
 */

/**
 * Combina múltiples reducers en uno solo
 */
fun <State, Action> combineReducers(
    vararg reducers: Reducer<State, Action>
): Reducer<State, Action> = { state, action ->
    reducers.fold(state) { currentState, reducer ->
        reducer(currentState, action)
    }
}

/**
 * Crea un reducer que solo procesa acciones de un tipo específico
 */
inline fun <State, reified ActionType : Any> filterReducer(
    crossinline reducer: (State, ActionType) -> State
): Reducer<State, Any> = { state, action ->
    if (action is ActionType) {
        reducer(state, action)
    } else {
        state
    }
}

/**
 * Crea un reducer con logging automático
 */
fun <State, Action> loggingReducer(
    name: String,
    reducer: Reducer<State, Action>
): Reducer<State, Action> = { state, action ->
    println("🔄 [$name] Processing action: ${action?.let { it::class.simpleName } ?: "null"}")
    val newState = reducer(state, action)
    if (newState != state) {
        println("✅ [$name] State updated")
    } else {
        println("⚪ [$name] No state change")
    }
    newState
}

/**
 * Crea un reducer con validación automática
 */
fun <State, Action> validatingReducer(
    validator: (State) -> List<String>,
    reducer: Reducer<State, Action>
): Reducer<State, Action> = { state, action ->
    val newState = reducer(state, action)
    val validationErrors = validator(newState)
    
    if (validationErrors.isNotEmpty()) {
        println("⚠️ State validation failed after action ${action?.let { it::class.simpleName } ?: "null"}:")
        validationErrors.forEach { error ->
            println("  - $error")
        }
        // PED: En un sistema real, podríamos lanzar una excepción o revertir el estado
        state // Revert to previous state
    } else {
        newState
    }
}

/**
 * PED: EXTENSION FUNCTIONS PARA REDUCERS
 * 
 * Estas funciones extienden la funcionalidad de los reducers
 * con operaciones comunes y útiles.
 */

/**
 * Extension function para aplicar múltiples acciones secuencialmente
 */
fun <State, Action> Reducer<State, Action>.applyActions(
    state: State,
    actions: List<Action>
): State = actions.fold(state, this)

/**
 * Extension function para crear un reducer con middleware
 */
fun <State, Action> Reducer<State, Action>.withMiddleware(
    middleware: (State, Action, Reducer<State, Action>) -> State
): Reducer<State, Action> = { state, action ->
    middleware(state, action, this)
}

/**
 * Extension function para crear un reducer con cache
 */
fun <State, Action> Reducer<State, Action>.withCache(
    cacheSize: Int = 100
): Reducer<State, Action> {
    val cache = mutableMapOf<Pair<State, Action>, State>()
    
    return { state, action ->
        val key = state to action
        cache.getOrPut(key) {
            if (cache.size >= cacheSize) {
                cache.clear() // Simple cache eviction
            }
            this(state, action)
        }
    }
}
