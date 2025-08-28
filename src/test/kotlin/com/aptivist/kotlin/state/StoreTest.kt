
package com.aptivist.kotlin.state

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach

/**
 * 🧪 TESTS PARA STORE Y REACTIVE STATE MANAGEMENT
 * 
 * Esta clase de test demuestra:
 * • Testing de coroutines y async operations
 * • Testing de reactive streams con StateFlow
 * • Testing de observers y subscriptions
 * • Testing de middleware y interceptors
 * • Testing de thread-safety y concurrency
 * • Testing de time-travel debugging
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest {
    
    private lateinit var testScope: TestScope
    private lateinit var store: AppStore
    
    @BeforeEach
    fun setup() {
        testScope = TestScope()
        store = AppStore(
            initialState = AppState(),
            scope = testScope
        )
    }
    
    @AfterEach
    fun cleanup() {
        store.close()
    }
    
    @Nested
    @DisplayName("Basic Store Operations")
    inner class BasicStoreOperations {
        
        @Test
        @DisplayName("Should have initial state")
        fun shouldHaveInitialState() {
            // Given & When
            val currentState = store.state.value
            
            // Then
            assertFalse(currentState.serverState.isRunning)
            assertTrue(currentState.connectionState.activeConnections.isEmpty())
            assertTrue(currentState.pluginState.loadedPlugins.isEmpty())
        }
        
        @Test
        @DisplayName("Should dispatch actions and update state")
        fun shouldDispatchActionsAndUpdateState() = testScope.runTest {
            // Given
            val startAction = AppAction.Server.Start(port = 8080)
            
            // When
            store.dispatch(startAction)
            advanceUntilIdle() // Wait for async processing
            
            // Then
            val newState = store.state.value
            assertTrue(newState.serverState.isRunning)
            assertEquals(8080, newState.serverState.port)
            assertNotNull(newState.serverState.startTime)
        }
        
        @Test
        @DisplayName("Should handle batch actions")
        fun shouldHandleBatchActions() = testScope.runTest {
            // Given
            val actions = listOf(
                AppAction.Server.Start(port = 8080),
                AppAction.Server.UpdateCapabilities(setOf("tools", "resources")),
                AppAction.UI.SetTheme(Theme.LIGHT)
            )
            
            // When
            store.dispatchBatch(actions)
            advanceUntilIdle()
            
            // Then
            val state = store.state.value
            assertTrue(state.serverState.isRunning)
            assertEquals(setOf("tools", "resources"), state.serverState.capabilities)
            assertEquals(Theme.LIGHT, state.uiState.theme)
        }
        
        @Test
        @DisplayName("Should maintain state immutability")
        fun shouldMaintainStateImmutability() = testScope.runTest {
            // Given
            val initialState = store.state.value
            val startAction = AppAction.Server.Start(port = 8080)
            
            // When
            store.dispatch(startAction)
            advanceUntilIdle()
            
            // Then
            val newState = store.state.value
            assertNotSame(initialState, newState)
            assertNotSame(initialState.serverState, newState.serverState)
            assertFalse(initialState.serverState.isRunning) // Original unchanged
            assertTrue(newState.serverState.isRunning) // New state updated
        }
    }
    
    @Nested
    @DisplayName("State Subscriptions")
    inner class StateSubscriptions {
        
        @Test
        @DisplayName("Should notify observers on state changes")
        fun shouldNotifyObserversOnStateChanges() = testScope.runTest {
            // Given
            var notificationCount = 0
            var lastOldState: AppState? = null
            var lastNewState: AppState? = null
            
            val observer = StateObserver<AppState> { oldState, newState ->
                notificationCount++
                lastOldState = oldState
                lastNewState = newState
            }
            
            val subscription = store.subscribe(observer)
            
            // When
            store.dispatch(AppAction.Server.Start(port = 8080))
            advanceUntilIdle()
            
            // Then
            assertEquals(1, notificationCount)
            assertNotNull(lastOldState)
            assertNotNull(lastNewState)
            assertFalse(lastOldState!!.serverState.isRunning)
            assertTrue(lastNewState!!.serverState.isRunning)
            
            // Cleanup
            subscription.cancel()
        }
        
        @Test
        @DisplayName("Should support selector-based subscriptions")
        fun shouldSupportSelectorBasedSubscriptions() = testScope.runTest {
            // Given
            var serverStateChanges = 0
            var lastServerState: ServerState? = null
            
            val subscription = store.subscribe(
                selector = { it.serverState },
                observer = { serverState ->
                    serverStateChanges++
                    lastServerState = serverState
                }
            )
            
            // When - Change server state
            store.dispatch(AppAction.Server.Start(port = 8080))
            advanceUntilIdle()
            
            // When - Change UI state (should not trigger server observer)
            store.dispatch(AppAction.UI.SetTheme(Theme.LIGHT))
            advanceUntilIdle()
            
            // Then
            assertEquals(1, serverStateChanges) // Only server change should trigger
            assertNotNull(lastServerState)
            assertTrue(lastServerState!!.isRunning)
            
            // Cleanup
            subscription.cancel()
        }
        
        @Test
        @DisplayName("Should cancel subscriptions properly")
        fun shouldCancelSubscriptionsProperly() = testScope.runTest {
            // Given
            var notificationCount = 0
            val observer = StateObserver<AppState> { _, _ ->
                notificationCount++
            }
            
            val subscription = store.subscribe(observer)
            assertTrue(subscription.isActive)
            
            // When - First change (should notify)
            store.dispatch(AppAction.Server.Start(port = 8080))
            advanceUntilIdle()
            assertEquals(1, notificationCount)
            
            // When - Cancel subscription
            subscription.cancel()
            assertFalse(subscription.isActive)
            
            // When - Second change (should not notify)
            store.dispatch(AppAction.Server.Stop)
            advanceUntilIdle()
            
            // Then
            assertEquals(1, notificationCount) // No additional notifications
        }
        
        @Test
        @DisplayName("Should handle multiple concurrent subscriptions")
        fun shouldHandleMultipleConcurrentSubscriptions() = testScope.runTest {
            // Given
            var observer1Count = 0
            var observer2Count = 0
            var observer3Count = 0
            
            val subscription1 = store.subscribe { _, _ -> observer1Count++ }
            val subscription2 = store.subscribeToServer { _ -> observer2Count++ }
            val subscription3 = store.subscribeToConnections { _ -> observer3Count++ }
            
            // When
            store.dispatch(AppAction.Server.Start(port = 8080))
            advanceUntilIdle()
            
            // Then
            assertEquals(1, observer1Count) // General observer
            assertEquals(1, observer2Count) // Server observer
            assertEquals(0, observer3Count) // Connection observer (no connection changes)
            
            // Cleanup
            subscription1.cancel()
            subscription2.cancel()
            subscription3.cancel()
        }
    }
    
    @Nested
    @DisplayName("Middleware System")
    inner class MiddlewareSystem {
        
        @Test
        @DisplayName("Should execute middleware in order")
        fun shouldExecuteMiddlewareInOrder() = testScope.runTest {
            // Given
            val executionOrder = mutableListOf<String>()
            
            val middleware1 = Middleware<AppState, AppAction> { _, action, next ->
                executionOrder.add("middleware1-before")
                next(action)
                executionOrder.add("middleware1-after")
            }
            
            val middleware2 = Middleware<AppState, AppAction> { _, action, next ->
                executionOrder.add("middleware2-before")
                next(action)
                executionOrder.add("middleware2-after")
            }
            
            store.addMiddleware(middleware1)
            store.addMiddleware(middleware2)
            
            // When
            store.dispatch(AppAction.Server.Start(port = 8080))
            advanceUntilIdle()
            
            // Then
            assertEquals(
                listOf(
                    "middleware1-before",
                    "middleware2-before",
                    "middleware2-after",
                    "middleware1-after"
                ),
                executionOrder
            )
        }
        
        @Test
        @DisplayName("Should allow middleware to intercept actions")
        fun shouldAllowMiddlewareToInterceptActions() = testScope.runTest {
            // Given
            val interceptingMiddleware = Middleware<AppState, AppAction> { _, action, next ->
                if (action is AppAction.Server.Start && action.port < 1024) {
                    // Don't call next() - intercept the action
                    println("Intercepted privileged port action")
                } else {
                    next(action)
                }
            }
            
            store.addMiddleware(interceptingMiddleware)
            
            // When - Try to start on privileged port
            store.dispatch(AppAction.Server.Start(port = 80))
            advanceUntilIdle()
            
            // Then - Server should not be running
            assertFalse(store.state.value.serverState.isRunning)
            
            // When - Start on regular port
            store.dispatch(AppAction.Server.Start(port = 8080))
            advanceUntilIdle()
            
            // Then - Server should be running
            assertTrue(store.state.value.serverState.isRunning)
        }
        
        @Test
        @DisplayName("Should handle middleware errors gracefully")
        fun shouldHandleMiddlewareErrorsGracefully() = testScope.runTest {
            // Given
            val errorMiddleware = Middleware<AppState, AppAction> { _, _, _ ->
                throw RuntimeException("Middleware error")
            }
            
            store.addMiddleware(errorMiddleware)
            
            // When & Then - Should not crash the store
            assertDoesNotThrow {
                runBlocking {
                    store.dispatch(AppAction.Server.Start(port = 8080))
                    advanceUntilIdle()
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Performance and Debugging")
    inner class PerformanceAndDebugging {
        
        @Test
        @DisplayName("Should track action count")
        fun shouldTrackActionCount() = testScope.runTest {
            // Given
            val initialCount = store.getActionCount()
            
            // When
            store.dispatch(AppAction.Server.Start(port = 8080))
            store.dispatch(AppAction.Server.Stop)
            store.dispatch(AppAction.UI.SetTheme(Theme.LIGHT))
            advanceUntilIdle()
            
            // Then
            assertEquals(initialCount + 3, store.getActionCount())
        }
        
        @Test
        @DisplayName("Should maintain state history")
        fun shouldMaintainStateHistory() = testScope.runTest {
            // Given & When
            store.dispatch(AppAction.Server.Start(port = 8080))
            store.dispatch(AppAction.UI.SetTheme(Theme.LIGHT))
            advanceUntilIdle()
            
            // Then
            val history = store.getStateHistory()
            assertEquals(2, history.size)
            
            val firstSnapshot = history[0]
            assertTrue(firstSnapshot.action is AppAction.Server.Start)
            assertFalse(firstSnapshot.stateBefore.serverState.isRunning)
            assertTrue(firstSnapshot.stateAfter.serverState.isRunning)
            
            val secondSnapshot = history[1]
            assertTrue(secondSnapshot.action is AppAction.UI.SetTheme)
            assertEquals(Theme.DARK, secondSnapshot.stateBefore.uiState.theme)
            assertEquals(Theme.LIGHT, secondSnapshot.stateAfter.uiState.theme)
        }
        
        @Test
        @DisplayName("Should support time travel debugging")
        fun shouldSupportTimeTravelDebugging() = testScope.runTest {
            // Given - Dispatch some actions
            store.dispatch(AppAction.Server.Start(port = 8080))
            store.dispatch(AppAction.UI.SetTheme(Theme.LIGHT))
            advanceUntilIdle()
            
            val history = store.getStateHistory()
            val firstSnapshotId = history[0].id
            
            // When - Time travel to first snapshot
            store.timeTravel(firstSnapshotId)
            advanceUntilIdle()
            
            // Then - State should be reverted
            val currentState = store.state.value
            assertFalse(currentState.serverState.isRunning) // Back to initial state
            assertEquals(Theme.DARK, currentState.uiState.theme) // Back to initial theme
        }
    }
    
    @Nested
    @DisplayName("Thread Safety")
    inner class ThreadSafety {
        
        @Test
        @DisplayName("Should handle concurrent dispatches safely")
        fun shouldHandleConcurrentDispatchesSafely() = testScope.runTest {
            // Given
            val numberOfActions = 100
            val jobs = mutableListOf<Job>()
            
            // When - Dispatch many actions concurrently
            repeat(numberOfActions) { i ->
                val job = launch {
                    store.dispatch(AppAction.Server.UpdateStatistics(requestsIncrement = 1))
                }
                jobs.add(job)
            }
            
            // Wait for all jobs to complete
            jobs.joinAll()
            advanceUntilIdle()
            
            // Then - All actions should be processed
            val finalState = store.state.value
            assertEquals(numberOfActions.toLong(), finalState.serverState.statistics.totalRequests)
        }
        
        @Test
        @DisplayName("Should handle concurrent subscriptions safely")
        fun shouldHandleConcurrentSubscriptionsSafely() = testScope.runTest {
            // Given
            val numberOfObservers = 50
            val subscriptions = mutableListOf<Subscription>()
            val notificationCounts = mutableMapOf<Int, Int>()
            
            // When - Create many observers concurrently
            repeat(numberOfObservers) { i ->
                val subscription = store.subscribe { _, _ ->
                    notificationCounts[i] = notificationCounts.getOrDefault(i, 0) + 1
                }
                subscriptions.add(subscription)
            }
            
            // Dispatch an action
            store.dispatch(AppAction.Server.Start(port = 8080))
            advanceUntilIdle()
            
            // Then - All observers should be notified
            assertEquals(numberOfObservers, notificationCounts.size)
            notificationCounts.values.forEach { count ->
                assertEquals(1, count)
            }
            
            // Cleanup
            subscriptions.forEach { it.cancel() }
        }
    }
    
    @Nested
    @DisplayName("Store Factory")
    inner class StoreFactoryTests {
        
        @Test
        @DisplayName("Should create development store with correct configuration")
        fun shouldCreateDevelopmentStoreWithCorrectConfiguration() {
            // When
            val devStore = StoreFactory.createDevelopmentStore()
            
            // Then
            assertTrue(devStore.state.value.serverState.isRunning)
            assertEquals("localhost", devStore.state.value.serverState.host)
            assertEquals(Theme.DARK, devStore.state.value.uiState.theme)
            assertTrue(devStore.state.value.uiState.preferences.showDebugInfo)
            assertTrue(devStore.getMiddlewareCount() > 0) // Should have middleware
            
            // Cleanup
            devStore.close()
        }
        
        @Test
        @DisplayName("Should create production store with correct configuration")
        fun shouldCreateProductionStoreWithCorrectConfiguration() {
            // When
            val prodStore = StoreFactory.createProductionStore()
            
            // Then
            assertFalse(prodStore.state.value.serverState.isRunning)
            assertEquals("0.0.0.0", prodStore.state.value.serverState.host)
            assertEquals(443, prodStore.state.value.serverState.port)
            assertEquals(ServerProtocol.HTTPS, prodStore.state.value.serverState.protocol)
            assertEquals(Theme.AUTO, prodStore.state.value.uiState.theme)
            assertFalse(prodStore.state.value.uiState.preferences.showDebugInfo)
            assertTrue(prodStore.getMiddlewareCount() > 0) // Should have middleware
            
            // Cleanup
            prodStore.close()
        }
        
        @Test
        @DisplayName("Should create test store with minimal configuration")
        fun shouldCreateTestStoreWithMinimalConfiguration() {
            // When
            val testStore = StoreFactory.createTestStore()
            
            // Then
            assertTrue(testStore.state.value.serverState.isRunning)
            assertEquals(1, testStore.state.value.connectionState.activeConnections.size)
            assertEquals(1, testStore.state.value.pluginState.loadedPlugins.size)
            assertEquals("test", testStore.state.value.metadata.source)
            assertEquals(0, testStore.getMiddlewareCount()) // No middleware for testing
            
            // Cleanup
            testStore.close()
        }
    }
}
