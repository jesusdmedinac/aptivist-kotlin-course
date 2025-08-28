
package com.aptivist.kotlin.state

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * 🧪 TESTS PARA MANEJO DE ESTADO INMUTABLE
 * 
 * Esta clase de test demuestra:
 * • Testing de data classes y sus propiedades
 * • Verificación de inmutabilidad
 * • Testing de computed properties
 * • Validación de extension functions
 * • Testing de factory functions
 */
class StateTest {
    
    @Nested
    @DisplayName("AppState Tests")
    inner class AppStateTests {
        
        @Test
        @DisplayName("Should create default AppState with correct initial values")
        fun shouldCreateDefaultAppState() {
            // Given & When
            val state = AppState()
            
            // Then
            assertFalse(state.serverState.isRunning)
            assertEquals(8080, state.serverState.port)
            assertTrue(state.connectionState.activeConnections.isEmpty())
            assertTrue(state.pluginState.loadedPlugins.isEmpty())
            assertEquals(Theme.DARK, state.uiState.theme)
            assertTrue(state.metadata.version > 0)
        }
        
        @Test
        @DisplayName("Should calculate isHealthy correctly")
        fun shouldCalculateIsHealthy() {
            // Given
            val unhealthyState = AppState()
            
            val healthyState = AppState(
                serverState = ServerState(isRunning = true),
                connectionState = ConnectionState(
                    activeConnections = mapOf(
                        "conn1" to Connection(
                            id = "conn1",
                            type = ConnectionType.HTTP,
                            clientInfo = ClientInfo(ipAddress = "127.0.0.1"),
                            establishedAt = System.currentTimeMillis(),
                            lastActivity = System.currentTimeMillis()
                        )
                    )
                ),
                pluginState = PluginState(
                    loadedPlugins = mapOf(
                        "plugin1" to PluginInfo(
                            id = "plugin1",
                            name = "Test Plugin",
                            version = "1.0.0",
                            status = PluginStatus.ACTIVE,
                            loadedAt = System.currentTimeMillis()
                        )
                    )
                )
            )
            
            // Then
            assertFalse(unhealthyState.isHealthy)
            assertTrue(healthyState.isHealthy)
        }
        
        @Test
        @DisplayName("Should maintain immutability when using copy")
        fun shouldMaintainImmutability() {
            // Given
            val originalState = AppState()
            
            // When
            val modifiedState = originalState.copy(
                serverState = originalState.serverState.copy(isRunning = true)
            )
            
            // Then
            assertFalse(originalState.serverState.isRunning)
            assertTrue(modifiedState.serverState.isRunning)
            assertNotSame(originalState, modifiedState)
            assertNotSame(originalState.serverState, modifiedState.serverState)
        }
        
        @Test
        @DisplayName("Should validate state correctly")
        fun shouldValidateState() {
            // Given
            val invalidState = AppState(
                serverState = ServerState(
                    isRunning = true,
                    port = -1, // Invalid port
                    maxConnections = 5
                ),
                connectionState = ConnectionState(
                    activeConnections = (1..10).associate { i ->
                        "conn$i" to Connection(
                            id = "conn$i",
                            type = ConnectionType.HTTP,
                            clientInfo = ClientInfo(ipAddress = "127.0.0.1"),
                            establishedAt = System.currentTimeMillis(),
                            lastActivity = System.currentTimeMillis()
                        )
                    }
                ),
                pluginState = PluginState(
                    loadedPlugins = mapOf(
                        "error-plugin" to PluginInfo(
                            id = "error-plugin",
                            name = "Error Plugin",
                            version = "1.0.0",
                            status = PluginStatus.ERROR,
                            loadedAt = System.currentTimeMillis()
                        )
                    )
                )
            )
            
            // When
            val validationErrors = invalidState.validate()
            
            // Then
            assertEquals(3, validationErrors.size)
            assertTrue(validationErrors.any { it.contains("port is invalid") })
            assertTrue(validationErrors.any { it.contains("exceed maximum") })
            assertTrue(validationErrors.any { it.contains("error state") })
        }
    }
    
    @Nested
    @DisplayName("ServerState Tests")
    inner class ServerStateTests {
        
        @Test
        @DisplayName("Should calculate uptime correctly")
        fun shouldCalculateUptime() {
            // Given
            val startTime = System.currentTimeMillis() - 5000 // 5 seconds ago
            val serverState = ServerState(
                isRunning = true,
                startTime = startTime
            )
            
            // When
            val uptime = serverState.uptime
            
            // Then
            assertNotNull(uptime)
            assertTrue(uptime!! >= 4000) // At least 4 seconds
            assertTrue(uptime <= 6000) // At most 6 seconds (accounting for test execution time)
        }
        
        @Test
        @DisplayName("Should return null uptime when not started")
        fun shouldReturnNullUptimeWhenNotStarted() {
            // Given
            val serverState = ServerState(isRunning = false)
            
            // When & Then
            assertNull(serverState.uptime)
        }
        
        @Test
        @DisplayName("Should format address correctly")
        fun shouldFormatAddress() {
            // Given
            val serverState = ServerState(
                host = "example.com",
                port = 9090,
                protocol = ServerProtocol.HTTPS
            )
            
            // When & Then
            assertEquals("HTTPS://example.com:9090", serverState.address)
        }
    }
    
    @Nested
    @DisplayName("ServerProtocol Tests")
    inner class ServerProtocolTests {
        
        @Test
        @DisplayName("Should identify secure protocols correctly")
        fun shouldIdentifySecureProtocols() {
            assertTrue(ServerProtocol.HTTPS.isSecure())
            assertFalse(ServerProtocol.HTTP.isSecure())
            assertFalse(ServerProtocol.WEBSOCKET.isSecure())
            assertFalse(ServerProtocol.TCP.isSecure())
        }
        
        @Test
        @DisplayName("Should parse protocol from string")
        fun shouldParseProtocolFromString() {
            assertEquals(ServerProtocol.HTTP, ServerProtocol.fromString("http"))
            assertEquals(ServerProtocol.HTTPS, ServerProtocol.fromString("HTTPS"))
            assertEquals(ServerProtocol.WEBSOCKET, ServerProtocol.fromString("websocket"))
            assertNull(ServerProtocol.fromString("invalid"))
        }
    }
    
    @Nested
    @DisplayName("ServerStatistics Tests")
    inner class ServerStatisticsTests {
        
        @Test
        @DisplayName("Should calculate success rate correctly")
        fun shouldCalculateSuccessRate() {
            // Given
            val stats = ServerStatistics(
                totalRequests = 100L,
                successfulRequests = 85L,
                failedRequests = 15L
            )
            
            // When & Then
            assertEquals(85.0, stats.successRate, 0.01)
            assertEquals(15.0, stats.errorRate, 0.01)
        }
        
        @Test
        @DisplayName("Should handle zero requests correctly")
        fun shouldHandleZeroRequests() {
            // Given
            val stats = ServerStatistics()
            
            // When & Then
            assertEquals(0.0, stats.successRate)
            assertEquals(0.0, stats.errorRate)
        }
    }
    
    @Nested
    @DisplayName("Extension Functions Tests")
    inner class ExtensionFunctionsTests {
        
        @Test
        @DisplayName("Should add connection correctly")
        fun shouldAddConnection() {
            // Given
            val initialState = AppState()
            val connection = Connection(
                id = "test-conn",
                type = ConnectionType.WEBSOCKET,
                clientInfo = ClientInfo(ipAddress = "192.168.1.1"),
                establishedAt = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis()
            )
            
            // When
            val newState = initialState.addConnection(connection)
            
            // Then
            assertTrue(initialState.connectionState.activeConnections.isEmpty())
            assertEquals(1, newState.connectionState.activeConnections.size)
            assertEquals(connection, newState.connectionState.activeConnections["test-conn"])
            assertTrue(newState.metadata.lastUpdated > initialState.metadata.lastUpdated)
        }
        
        @Test
        @DisplayName("Should remove connection correctly")
        fun shouldRemoveConnection() {
            // Given
            val connection = Connection(
                id = "test-conn",
                type = ConnectionType.HTTP,
                clientInfo = ClientInfo(ipAddress = "192.168.1.1"),
                establishedAt = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis()
            )
            val initialState = AppState().addConnection(connection)
            
            // When
            val newState = initialState.removeConnection("test-conn")
            
            // Then
            assertEquals(1, initialState.connectionState.activeConnections.size)
            assertTrue(newState.connectionState.activeConnections.isEmpty())
        }
        
        @Test
        @DisplayName("Should update server stats correctly")
        fun shouldUpdateServerStats() {
            // Given
            val initialState = AppState()
            
            // When
            val newState = initialState.updateServerStats(
                requestsIncrement = 10L,
                successIncrement = 8L,
                failIncrement = 2L,
                responseTime = 150.0
            )
            
            // Then
            val stats = newState.serverState.statistics
            assertEquals(10L, stats.totalRequests)
            assertEquals(8L, stats.successfulRequests)
            assertEquals(2L, stats.failedRequests)
            assertEquals(150.0, stats.averageResponseTime)
            assertEquals(80.0, stats.successRate, 0.01)
        }
        
        @Test
        @DisplayName("Should add notification correctly")
        fun shouldAddNotification() {
            // Given
            val initialState = AppState()
            val notification = Notification(
                id = "notif-1",
                type = NotificationType.INFO,
                title = "Test Notification",
                message = "This is a test",
                timestamp = System.currentTimeMillis()
            )
            
            // When
            val newState = initialState.addNotification(notification)
            
            // Then
            assertTrue(initialState.uiState.notifications.isEmpty())
            assertEquals(1, newState.uiState.notifications.size)
            assertEquals(notification, newState.uiState.notifications.first())
        }
        
        @Test
        @DisplayName("Should mark notifications as read")
        fun shouldMarkNotificationsAsRead() {
            // Given
            val notification1 = Notification(
                id = "notif-1",
                type = NotificationType.INFO,
                title = "Test 1",
                message = "Message 1",
                timestamp = System.currentTimeMillis(),
                read = false
            )
            val notification2 = Notification(
                id = "notif-2",
                type = NotificationType.WARNING,
                title = "Test 2",
                message = "Message 2",
                timestamp = System.currentTimeMillis(),
                read = false
            )
            
            val initialState = AppState()
                .addNotification(notification1)
                .addNotification(notification2)
            
            // When
            val newState = initialState.markNotificationsAsRead(listOf("notif-1"))
            
            // Then
            val notifications = newState.uiState.notifications
            assertTrue(notifications.find { it.id == "notif-1" }?.read == true)
            assertTrue(notifications.find { it.id == "notif-2" }?.read == false)
        }
    }
    
    @Nested
    @DisplayName("StateFactory Tests")
    inner class StateFactoryTests {
        
        @Test
        @DisplayName("Should create development state correctly")
        fun shouldCreateDevelopmentState() {
            // When
            val state = StateFactory.createDevelopmentState()
            
            // Then
            assertTrue(state.serverState.isRunning)
            assertEquals(8080, state.serverState.port)
            assertEquals("localhost", state.serverState.host)
            assertEquals(ServerProtocol.HTTP, state.serverState.protocol)
            assertEquals(Theme.DARK, state.uiState.theme)
            assertTrue(state.uiState.preferences.showDebugInfo)
            assertEquals("development", state.metadata.source)
            assertTrue(state.metadata.tags.contains("dev"))
        }
        
        @Test
        @DisplayName("Should create production state correctly")
        fun shouldCreateProductionState() {
            // When
            val state = StateFactory.createProductionState()
            
            // Then
            assertFalse(state.serverState.isRunning)
            assertEquals(443, state.serverState.port)
            assertEquals("0.0.0.0", state.serverState.host)
            assertEquals(ServerProtocol.HTTPS, state.serverState.protocol)
            assertEquals(Theme.AUTO, state.uiState.theme)
            assertFalse(state.uiState.preferences.showDebugInfo)
            assertEquals("production", state.metadata.source)
            assertTrue(state.metadata.tags.contains("prod"))
        }
        
        @Test
        @DisplayName("Should create test state with mock data")
        fun shouldCreateTestStateWithMockData() {
            // When
            val state = StateFactory.createTestState()
            
            // Then
            assertTrue(state.serverState.isRunning)
            assertEquals(1, state.connectionState.activeConnections.size)
            assertEquals(1, state.pluginState.loadedPlugins.size)
            assertEquals(1, state.connectionState.connectionHistory.size)
            
            val stats = state.serverState.statistics
            assertEquals(1000L, stats.totalRequests)
            assertEquals(950L, stats.successfulRequests)
            assertEquals(50L, stats.failedRequests)
            assertEquals(95.0, stats.successRate, 0.01)
            
            assertEquals("test", state.metadata.source)
            assertTrue(state.metadata.tags.contains("test"))
        }
    }
    
    @Nested
    @DisplayName("Data Class Properties Tests")
    inner class DataClassPropertiesTests {
        
        @Test
        @DisplayName("Should support destructuring")
        fun shouldSupportDestructuring() {
            // Given
            val clientInfo = ClientInfo(
                userAgent = "Test Agent",
                ipAddress = "192.168.1.100",
                version = "2.0.0",
                capabilities = setOf("test", "debug")
            )
            
            // When - Destructuring (uses componentN functions)
            val (userAgent, ipAddress, version, capabilities) = clientInfo
            
            // Then
            assertEquals("Test Agent", userAgent)
            assertEquals("192.168.1.100", ipAddress)
            assertEquals("2.0.0", version)
            assertEquals(setOf("test", "debug"), capabilities)
        }
        
        @Test
        @DisplayName("Should provide structural equality")
        fun shouldProvideStructuralEquality() {
            // Given
            val state1 = AppState(
                serverState = ServerState(port = 8080, isRunning = true)
            )
            val state2 = AppState(
                serverState = ServerState(port = 8080, isRunning = true)
            )
            val state3 = AppState(
                serverState = ServerState(port = 8081, isRunning = true)
            )
            
            // Then
            assertEquals(state1, state2) // Structural equality
            assertNotEquals(state1, state3)
            assertEquals(state1.hashCode(), state2.hashCode())
            assertNotEquals(state1.hashCode(), state3.hashCode())
        }
        
        @Test
        @DisplayName("Should provide meaningful toString")
        fun shouldProvideMeaningfulToString() {
            // Given
            val connection = Connection(
                id = "test-123",
                type = ConnectionType.WEBSOCKET,
                clientInfo = ClientInfo(ipAddress = "127.0.0.1"),
                establishedAt = 1234567890L,
                lastActivity = 1234567890L
            )
            
            // When
            val toString = connection.toString()
            
            // Then
            assertTrue(toString.contains("Connection"))
            assertTrue(toString.contains("test-123"))
            assertTrue(toString.contains("WEBSOCKET"))
            assertTrue(toString.contains("127.0.0.1"))
        }
    }
}
