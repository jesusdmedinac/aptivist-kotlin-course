
package com.aptivist.kotlin.state

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 🎯 PHASE 3.1 - STORE CON SUSCRIPCIÓN A CAMBIOS DE ESTADO
 * 
 * Este archivo demuestra conceptos avanzados de reactive programming:
 * • StateFlow para reactive state management
 * • Observer pattern con type-safe subscriptions
 * • Thread-safe state updates con Mutex
 * • Coroutines para operaciones asíncronas
 * • Flow operators para transformaciones reactivas
 * • Middleware pattern para interceptar acciones
 * • Time-travel debugging capabilities
 * • Performance monitoring y metrics
 * 
 * PED: El Store es el contenedor central del estado de la aplicación.
 * Proporciona una API reactiva para:
 * - Despachar acciones de manera thread-safe
 * - Suscribirse a cambios de estado
 * - Aplicar middleware para logging, debugging, etc.
 * - Mantener historial para time-travel debugging
 */

/**
 * PED: INTERFAZ DEL STORE
 * 
 * Define el contrato público del store, permitiendo diferentes implementaciones
 * y facilitando el testing con mocks.
 */
interface Store<State, Action> {
    /**
     * Estado actual como StateFlow (reactive)
     */
    val state: StateFlow<State>
    
    /**
     * Despacha una acción para actualizar el estado
     */
    suspend fun dispatch(action: Action)
    
    /**
     * Despacha múltiples acciones de manera atómica
     */
    suspend fun dispatchBatch(actions: List<Action>)
    
    /**
     * Suscribe un observer a cambios de estado
     */
    fun subscribe(observer: StateObserver<State>): Subscription
    
    /**
     * Suscribe un observer a un subset específico del estado
     */
    fun <T> subscribe(
        selector: (State) -> T,
        observer: (T) -> Unit
    ): Subscription
    
    /**
     * Agrega middleware para interceptar acciones
     */
    fun addMiddleware(middleware: Middleware<State, Action>)
    
    /**
     * Cierra el store y libera recursos
     */
    fun close()
}

/**
 * PED: INTERFAZ PARA OBSERVERS
 * 
 * Los observers reciben notificaciones cuando el estado cambia.
 */
fun interface StateObserver<State> {
    suspend fun onStateChanged(oldState: State, newState: State)
}

/**
 * PED: INTERFAZ PARA MIDDLEWARE
 * 
 * El middleware puede interceptar acciones antes de que lleguen al reducer.
 */
fun interface Middleware<State, Action> {
    suspend fun process(
        state: State,
        action: Action,
        next: suspend (Action) -> Unit
    )
}

/**
 * PED: SUBSCRIPTION PARA CANCELAR OBSERVERS
 * 
 * Permite cancelar suscripciones de manera limpia.
 */
interface Subscription {
    fun cancel()
    val isActive: Boolean
}

/**
 * PED: IMPLEMENTACIÓN CONCRETA DEL STORE
 * 
 * Store thread-safe con soporte completo para reactive programming.
 */
class AppStore(
    initialState: AppState = AppState(),
    private val reducer: Reducer<AppState, AppAction> = ::appReducer,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : Store<AppState, AppAction> {
    
    // PED: StateFlow para estado reactivo
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<AppState> = _state.asStateFlow()
    
    // PED: Mutex para thread-safety en updates
    private val stateMutex = Mutex()
    
    // PED: Collections thread-safe para observers y middleware
    private val observers = ConcurrentHashMap<String, StateObserver<AppState>>()
    private val selectorObservers = ConcurrentHashMap<String, SelectorObserver<*>>()
    private val middleware = mutableListOf<Middleware<AppState, AppAction>>()
    private val middlewareMutex = Mutex()
    
    // PED: Metrics y debugging
    private val actionCounter = AtomicLong(0)
    private val stateHistory = mutableListOf<StateSnapshot>()
    private val maxHistorySize = 100
    
    // PED: Channels para action processing
    private val actionChannel = Channel<ActionWithMetadata>(Channel.UNLIMITED)
    
    init {
        // PED: Iniciamos el procesamiento de acciones en background
        scope.launch {
            processActions()
        }
        
        // PED: Monitoreamos cambios de estado para notificar observers
        scope.launch {
            monitorStateChanges()
        }
    }
    
    /**
     * PED: DISPATCH DE ACCIONES
     * 
     * Método principal para despachar acciones. Es thread-safe y asíncrono.
     */
    override suspend fun dispatch(action: AppAction) {
        val metadata = ActionMetadata(
            id = actionCounter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            source = "dispatch"
        )
        
        actionChannel.send(ActionWithMetadata(action, metadata))
    }
    
    /**
     * PED: DISPATCH BATCH DE ACCIONES
     * 
     * Permite despachar múltiples acciones de manera atómica.
     */
    override suspend fun dispatchBatch(actions: List<AppAction>) {
        if (actions.isEmpty()) return
        
        // PED: Usamos System.Batch para procesar todas las acciones juntas
        val batchAction = AppAction.System.Batch(actions)
        dispatch(batchAction)
    }
    
    /**
     * PED: SUSCRIPCIÓN A CAMBIOS DE ESTADO
     * 
     * Permite suscribirse a todos los cambios de estado.
     */
    override fun subscribe(observer: StateObserver<AppState>): Subscription {
        val id = "observer-${System.currentTimeMillis()}-${(0..999).random()}"
        observers[id] = observer
        
        return object : Subscription {
            override fun cancel() {
                observers.remove(id)
            }
            
            override val isActive: Boolean
                get() = observers.containsKey(id)
        }
    }
    
    /**
     * PED: SUSCRIPCIÓN CON SELECTOR
     * 
     * Permite suscribirse solo a cambios en una parte específica del estado.
     */
    override fun <T> subscribe(
        selector: (AppState) -> T,
        observer: (T) -> Unit
    ): Subscription {
        val id = "selector-${System.currentTimeMillis()}-${(0..999).random()}"
        val selectorObserver = SelectorObserver(selector, observer)
        selectorObservers[id] = selectorObserver
        
        return object : Subscription {
            override fun cancel() {
                selectorObservers.remove(id)
            }
            
            override val isActive: Boolean
                get() = selectorObservers.containsKey(id)
        }
    }
    
    /**
     * PED: AGREGAR MIDDLEWARE
     * 
     * Permite agregar middleware para interceptar acciones.
     */
    override fun addMiddleware(middleware: Middleware<AppState, AppAction>) {
        scope.launch {
            middlewareMutex.withLock {
                this@AppStore.middleware.add(middleware)
            }
        }
    }
    
    /**
     * PED: CERRAR STORE
     * 
     * Libera todos los recursos y cancela coroutines.
     */
    override fun close() {
        actionChannel.close()
        observers.clear()
        selectorObservers.clear()
        scope.cancel()
    }
    
    /**
     * PED: PROCESAMIENTO DE ACCIONES EN BACKGROUND
     * 
     * Coroutine que procesa acciones de manera secuencial.
     */
    private suspend fun processActions() {
        for (actionWithMetadata in actionChannel) {
            try {
                processAction(actionWithMetadata)
            } catch (e: Exception) {
                // PED: Manejo de errores en procesamiento de acciones
                println("❌ Error processing action ${actionWithMetadata.action::class.simpleName}: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * PED: PROCESAMIENTO INDIVIDUAL DE ACCIONES
     * 
     * Aplica middleware y reducer para actualizar el estado.
     */
    private suspend fun processAction(actionWithMetadata: ActionWithMetadata) {
        val (action, metadata) = actionWithMetadata
        val currentState = _state.value
        
        // PED: Aplicamos middleware en orden
        var processedAction = action
        middlewareMutex.withLock {
            for (mw in middleware) {
                var nextCalled = false
                mw.process(currentState, processedAction) { nextAction ->
                    processedAction = nextAction
                    nextCalled = true
                }
                if (!nextCalled) {
                    // Middleware interceptó la acción
                    return
                }
            }
        }
        
        // PED: Aplicamos el reducer de manera thread-safe
        stateMutex.withLock {
            val oldState = _state.value
            val newState = reducer(oldState, processedAction)
            
            if (newState != oldState) {
                // PED: Actualizamos el estado
                _state.value = newState
                
                // PED: Guardamos en historial para debugging
                saveStateSnapshot(oldState, newState, processedAction, metadata)
            }
        }
    }
    
    /**
     * PED: MONITOREO DE CAMBIOS DE ESTADO
     * 
     * Coroutine que notifica a observers cuando el estado cambia.
     */
    private suspend fun monitorStateChanges() {
        var previousState = _state.value
        
        _state.collect { newState ->
            if (newState != previousState) {
                // PED: Notificamos a observers generales
                notifyObservers(previousState, newState)
                
                // PED: Notificamos a selector observers
                notifySelectorObservers(previousState, newState)
                
                previousState = newState
            }
        }
    }
    
    /**
     * PED: NOTIFICACIÓN A OBSERVERS GENERALES
     */
    private suspend fun notifyObservers(oldState: AppState, newState: AppState) {
        observers.values.forEach { observer ->
            try {
                observer.onStateChanged(oldState, newState)
            } catch (e: Exception) {
                println("❌ Error in state observer: ${e.message}")
            }
        }
    }
    
    /**
     * PED: NOTIFICACIÓN A SELECTOR OBSERVERS
     */
    private suspend fun notifySelectorObservers(oldState: AppState, newState: AppState) {
        selectorObservers.values.forEach { selectorObserver ->
            try {
                selectorObserver.notifyIfChanged(oldState, newState)
            } catch (e: Exception) {
                println("❌ Error in selector observer: ${e.message}")
            }
        }
    }
    
    /**
     * PED: GUARDAR SNAPSHOT PARA DEBUGGING
     */
    private fun saveStateSnapshot(
        oldState: AppState,
        newState: AppState,
        action: AppAction,
        metadata: ActionMetadata
    ) {
        val snapshot = StateSnapshot(
            id = metadata.id,
            timestamp = metadata.timestamp,
            action = action,
            stateBefore = oldState,
            stateAfter = newState
        )
        
        stateHistory.add(snapshot)
        
        // PED: Limitamos el tamaño del historial
        if (stateHistory.size > maxHistorySize) {
            stateHistory.removeAt(0)
        }
    }
    
    /**
     * PED: MÉTODOS DE DEBUGGING Y INTROSPECCIÓN
     */
    
    fun getStateHistory(): List<StateSnapshot> = stateHistory.toList()
    
    fun getActionCount(): Long = actionCounter.get()
    
    fun getObserverCount(): Int = observers.size + selectorObservers.size
    
    fun getMiddlewareCount(): Int = middleware.size
    
    /**
     * PED: TIME-TRAVEL DEBUGGING
     * 
     * Permite volver a un estado anterior para debugging.
     */
    suspend fun timeTravel(snapshotId: Long) {
        val snapshot = stateHistory.find { it.id == snapshotId }
        if (snapshot != null) {
            stateMutex.withLock {
                _state.value = snapshot.stateBefore
            }
        }
    }
    
    /**
     * PED: REPLAY DE ACCIONES
     * 
     * Permite reproducir una secuencia de acciones para testing.
     */
    suspend fun replayActions(fromSnapshotId: Long? = null) {
        val startIndex = if (fromSnapshotId != null) {
            stateHistory.indexOfFirst { it.id == fromSnapshotId }
        } else {
            0
        }
        
        if (startIndex >= 0) {
            val actionsToReplay = stateHistory.drop(startIndex).map { it.action }
            dispatchBatch(actionsToReplay)
        }
    }
}

/**
 * PED: CLASES DE DATOS PARA METADATA Y DEBUGGING
 */

/**
 * Metadata asociada con cada acción
 */
data class ActionMetadata(
    val id: Long,
    val timestamp: Long,
    val source: String,
    val correlationId: String? = null
)

/**
 * Acción con metadata
 */
data class ActionWithMetadata(
    val action: AppAction,
    val metadata: ActionMetadata
)

/**
 * Snapshot del estado para debugging
 */
data class StateSnapshot(
    val id: Long,
    val timestamp: Long,
    val action: AppAction,
    val stateBefore: AppState,
    val stateAfter: AppState
)

/**
 * Observer con selector para optimización
 */
private class SelectorObserver<T>(
    private val selector: (AppState) -> T,
    private val observer: (T) -> Unit
) {
    private var lastValue: T? = null
    
    suspend fun notifyIfChanged(oldState: AppState, newState: AppState) {
        val newValue = selector(newState)
        if (newValue != lastValue) {
            lastValue = newValue
            observer(newValue)
        }
    }
}

/**
 * PED: MIDDLEWARE PREDEFINIDOS
 * 
 * Colección de middleware útiles para casos comunes.
 */
object Middlewares {
    
    /**
     * Middleware de logging que registra todas las acciones
     */
    fun <State : Any, Action : Any> logging(
        name: String = "Store"
    ): Middleware<State, Action> = Middleware { state, action, next ->
        val startTime = System.currentTimeMillis()
        println("🔄 [$name] Dispatching: ${action::class.simpleName ?: "Unknown"}")
        
        next(action)
        
        val duration = System.currentTimeMillis() - startTime
        println("✅ [$name] Completed in ${duration}ms")
    }
    
    /**
     * Middleware de validación que valida acciones antes de procesarlas
     */
    fun <Action : Any> validation(
        validator: (Action) -> List<String>
    ): Middleware<AppState, Action> = Middleware { _, action, next ->
        val errors = validator(action)
        if (errors.isEmpty()) {
            next(action)
        } else {
            println("❌ Action validation failed for ${action::class.simpleName ?: "Unknown"}:")
            errors.forEach { error ->
                println("  - $error")
            }
        }
    }
    
    /**
     * Middleware de throttling que limita la frecuencia de acciones
     */
    fun <State : Any, Action : Any> throttling(
        windowMs: Long = 1000,
        maxActions: Int = 10
    ): Middleware<State, Action> {
        val actionTimes = mutableListOf<Long>()
        
        return Middleware { _, action, next ->
            val now = System.currentTimeMillis()
            
            // PED: Limpiamos acciones antiguas
            actionTimes.removeAll { it < now - windowMs }
            
            if (actionTimes.size < maxActions) {
                actionTimes.add(now)
                next(action)
            } else {
                println("⚠️ Action throttled: ${action::class.simpleName ?: "Unknown"}")
            }
        }
    }
    
    /**
     * Middleware de performance que mide el tiempo de procesamiento
     */
    fun <State : Any, Action : Any> performance(): Middleware<State, Action> {
        val actionTimes = mutableMapOf<String, MutableList<Long>>()
        
        return Middleware { _, action, next ->
            val actionName = action::class.simpleName ?: "Unknown"
            val startTime = System.nanoTime()
            
            next(action)
            
            val duration = (System.nanoTime() - startTime) / 1_000_000 // Convert to ms
            actionTimes.getOrPut(actionName) { mutableListOf() }.add(duration)
            
            // PED: Reportamos estadísticas cada 100 acciones
            val times = actionTimes[actionName]!!
            if (times.size % 100 == 0) {
                val avg = times.average()
                val max = times.maxOrNull() ?: 0L
                val min = times.minOrNull() ?: 0L
                println("📊 [$actionName] Avg: ${avg.toInt()}ms, Min: ${min}ms, Max: ${max}ms")
            }
        }
    }
}

/**
 * PED: EXTENSION FUNCTIONS PARA STORE
 * 
 * Funciones de conveniencia para trabajar con el store.
 */

/**
 * Extension function para suscribirse a cambios en el servidor
 */
fun Store<AppState, AppAction>.subscribeToServer(
    observer: (ServerState) -> Unit
): Subscription = subscribe({ it.serverState }, observer)

/**
 * Extension function para suscribirse a cambios en conexiones
 */
fun Store<AppState, AppAction>.subscribeToConnections(
    observer: (ConnectionState) -> Unit
): Subscription = subscribe({ it.connectionState }, observer)

/**
 * Extension function para suscribirse a cambios en plugins
 */
fun Store<AppState, AppAction>.subscribeToPlugins(
    observer: (PluginState) -> Unit
): Subscription = subscribe({ it.pluginState }, observer)

/**
 * Extension function para suscribirse a notificaciones
 */
fun Store<AppState, AppAction>.subscribeToNotifications(
    observer: (List<Notification>) -> Unit
): Subscription = subscribe({ it.uiState.notifications }, observer)

/**
 * Extension function para despachar acciones con DSL
 */
suspend fun Store<AppState, AppAction>.dispatch(
    builder: ActionCreators.() -> AppAction
) {
    val action = ActionCreators.builder()
    dispatch(action)
}

/**
 * PED: FACTORY FUNCTIONS PARA CREAR STORES
 */
object StoreFactory {
    
    /**
     * Crea un store para desarrollo con middleware de debugging
     */
    fun createDevelopmentStore(
        initialState: AppState = StateFactory.createDevelopmentState()
    ): AppStore {
        val store = AppStore(initialState)
        
        // PED: Agregamos middleware útil para desarrollo
        store.addMiddleware(Middlewares.logging("DevStore"))
        store.addMiddleware(Middlewares.performance())
        store.addMiddleware(Middlewares.validation { action ->
            if (action.requiresValidation()) {
                // PED: Validaciones específicas para desarrollo
                when (action) {
                    is AppAction.Server.Start -> {
                        if (action.port < 1024) {
                            listOf("Port ${action.port} requires root privileges")
                        } else emptyList()
                    }
                    else -> emptyList()
                }
            } else emptyList()
        })
        
        return store
    }
    
    /**
     * Crea un store para producción con middleware optimizado
     */
    fun createProductionStore(
        initialState: AppState = StateFactory.createProductionState()
    ): AppStore {
        val store = AppStore(initialState)
        
        // PED: Middleware optimizado para producción
        store.addMiddleware(Middlewares.throttling(windowMs = 5000, maxActions = 100))
        store.addMiddleware(Middlewares.validation { action ->
            // PED: Validaciones estrictas para producción
            when (action) {
                is AppAction.Server.Start -> {
                    buildList {
                        if (action.port !in 80..65535) add("Invalid port for production")
                        if (action.host == "localhost") add("Localhost not allowed in production")
                        if (action.protocol != ServerProtocol.HTTPS) add("HTTPS required in production")
                    }
                }
                else -> emptyList()
            }
        })
        
        return store
    }
    
    /**
     * Crea un store para testing con configuración mínima
     */
    fun createTestStore(
        initialState: AppState = StateFactory.createTestState()
    ): AppStore = AppStore(initialState)
}
