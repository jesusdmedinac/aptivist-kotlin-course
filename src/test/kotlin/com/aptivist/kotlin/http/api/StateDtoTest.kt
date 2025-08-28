

package com.aptivist.kotlin.http.api

import com.aptivist.kotlin.state.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 🎯 PHASE 3.2 - TESTS PARA DTOs Y MAPPING
 * 
 * Esta clase de test demuestra conceptos avanzados de testing para DTOs:
 * • **DTO Testing**: Verificación de conversiones entre domain objects y DTOs
 * • **Serialization Testing**: Testing de serialización/deserialización JSON
 * • **Mapping Testing**: Verificación de que los mappings preserven datos correctamente
 * • **Factory Testing**: Testing de factory functions para crear estados de prueba
 * • **Extension Function Testing**: Verificación de extension functions de conversión
 * 
 * PED: Los tests de DTOs son importantes porque:
 * - Verifican que no se pierdan datos en las conversiones
 * - Aseguran que la serialización JSON funcione correctamente
 * - Documentan la estructura esperada de la API
 * - Previenen breaking changes accidentales en la API
 * - Facilitan evolución segura de DTOs
 */
class StateDtoTest {
    
    /**
     * PED: JSON CONFIGURATION PARA TESTS
     */
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    /**
     * PED: TEST GROUP - CONVERSIÓN DE ESTADO PRINCIPAL
     * 
     * Estos tests verifican que el AppState se convierta correctamente
     * a AppStateDto manteniendo toda la información relevante.
     */
    
    @Test
    fun `AppState should convert to AppStateDto correctly`() {
        // PED: Arrange - Crear estado de prueba usando factory
        val appState = StateFactory.createTestState()
        
        // PED: Act - Convertir a DTO
        val dto = appState.toDto()
        
        // PED: Assert - Verificar conversión
        assertNotNull(dto)
        assertEquals(appState.serverState.isRunning, dto.server.isRunning)
        assertEquals(appState.serverState.port, dto.server.port)
        assertEquals(appState.serverState.host, dto.server.host)
        assertEquals(appState.serverState.protocol.name, dto.server.protocol)
        assertEquals(appState.serverState.address, dto.server.address)
        
        // PED: Verificar estadísticas del servidor
        assertEquals(appState.serverState.statistics.totalRequests, dto.server.statistics.totalRequests)
        assertEquals(appState.serverState.statistics.successRate, dto.server.statistics.successRate)
        assertEquals(appState.serverState.statistics.errorRate, dto.server.statistics.errorRate)
        
        // PED: Verificar resumen de conexiones
        assertEquals(appState.connectionState.activeConnections.size, dto.connections.activeConnections)
        assertEquals(appState.connectionState.pendingConnections.size, dto.connections.pendingConnections)
        assertEquals(appState.connectionState.hasCapacity(), dto.connections.hasCapacity)
        
        // PED: Verificar resumen de plugins
        assertEquals(appState.pluginState.loadedPlugins.size, dto.plugins.totalPlugins)
        assertEquals(appState.pluginState.activePlugins.size, dto.plugins.activePlugins)
        assertEquals(appState.pluginState.errorPlugins.size, dto.plugins.errorPlugins)
        
        // PED: Verificar metadatos
        assertEquals(appState.metadata.version, dto.metadata.version)
        assertEquals(appState.metadata.lastUpdated, dto.metadata.lastUpdated)
        assertEquals(appState.metadata.source, dto.metadata.source)
        assertEquals(appState.metadata.isStale, dto.metadata.isStale)
        
        // PED: Verificar health status
        assertEquals(appState.isHealthy, dto.health.isHealthy)
        assertTrue(dto.health.checks.isNotEmpty())
    }
    
    @Test
    fun `AppStateDto should serialize to JSON correctly`() {
        val appState = StateFactory.createTestState()
        val dto = appState.toDto()
        
        // PED: Serializar a JSON
        val jsonString = json.encodeToString(AppStateDto.serializer(), dto)
        
        // PED: Verificar que contiene campos esperados
        assertTrue(jsonString.contains("\"server\""))
        assertTrue(jsonString.contains("\"connections\""))
        assertTrue(jsonString.contains("\"plugins\""))
        assertTrue(jsonString.contains("\"ui\""))
        assertTrue(jsonString.contains("\"metadata\""))
        assertTrue(jsonString.contains("\"health\""))
        
        // PED: Deserializar de vuelta
        val deserializedDto = json.decodeFromString(AppStateDto.serializer(), jsonString)
        
        // PED: Verificar integridad
        assertEquals(dto.server.isRunning, deserializedDto.server.isRunning)
        assertEquals(dto.server.port, deserializedDto.server.port)
        assertEquals(dto.connections.activeConnections, deserializedDto.connections.activeConnections)
        assertEquals(dto.plugins.totalPlugins, deserializedDto.plugins.totalPlugins)
        assertEquals(dto.metadata.version, deserializedDto.metadata.version)
        assertEquals(dto.health.isHealthy, deserializedDto.health.isHealthy)
    }
    
    /**
     * PED: TEST GROUP - CONVERSIÓN DE ESTADÍSTICAS DEL SERVIDOR
     */
    
    @Test
    fun `ServerStatistics should convert to ServerStatisticsDto correctly`() {
        val stats = ServerStatistics(
            totalRequests = 1000L,
            successfulRequests = 950L,
            failedRequests = 50L,
            averageResponseTime = 125.5,
            peakConnections = 25,
            bytesTransferred = 1024000L
        )
        
        val dto = stats.toDto()
        
        assertEquals(stats.totalRequests, dto.totalRequests)
        assertEquals(stats.successfulRequests, dto.successfulRequests)
        assertEquals(stats.failedRequests, dto.failedRequests)
        assertEquals(stats.successRate, dto.successRate)
        assertEquals(stats.errorRate, dto.errorRate)
        assertEquals(stats.averageResponseTime, dto.averageResponseTime)
        assertEquals(stats.peakConnections, dto.peakConnections)
        assertEquals(stats.bytesTransferred, dto.bytesTransferred)
    }
    
    /**
     * PED: TEST GROUP - CONVERSIÓN DE EVENTOS DE CONEXIÓN
     */
    
    @Test
    fun `ConnectionEvent Connected should convert to ConnectionEventDto correctly`() {
        val clientInfo = ClientInfo(
            userAgent = "Test Client 1.0",
            ipAddress = "127.0.0.1",
            version = "1.0.0",
            capabilities = setOf("test")
        )
        
        val event = ConnectionEvent.Connected(
            connectionId = "conn-123",
            timestamp = System.currentTimeMillis(),
            clientInfo = clientInfo
        )
        
        val dto = event.toDto()
        
        assertTrue(dto is ConnectionEventDto.Connected)
        assertEquals(event.connectionId, dto.connectionId)
        assertEquals(event.timestamp, dto.timestamp)
        assertEquals("connected", dto.type)
        assertEquals(clientInfo.userAgent, (dto as ConnectionEventDto.Connected).clientType)
    }
    
    @Test
    fun `ConnectionEvent Disconnected should convert to ConnectionEventDto correctly`() {
        val event = ConnectionEvent.Disconnected(
            connectionId = "conn-123",
            timestamp = System.currentTimeMillis(),
            reason = "Client closed connection"
        )
        
        val dto = event.toDto()
        
        assertTrue(dto is ConnectionEventDto.Disconnected)
        assertEquals(event.connectionId, dto.connectionId)
        assertEquals(event.timestamp, dto.timestamp)
        assertEquals("disconnected", dto.type)
        assertEquals(event.reason, (dto as ConnectionEventDto.Disconnected).reason)
    }
    
    @Test
    fun `ConnectionEvent MessageReceived should convert to ConnectionEventDto correctly`() {
        val event = ConnectionEvent.MessageReceived(
            connectionId = "conn-123",
            timestamp = System.currentTimeMillis(),
            messageType = "ping",
            size = 1024
        )
        
        val dto = event.toDto()
        
        assertTrue(dto is ConnectionEventDto.MessageReceived)
        assertEquals(event.connectionId, dto.connectionId)
        assertEquals(event.timestamp, dto.timestamp)
        assertEquals("message_received", dto.type)
        assertEquals(event.messageType, (dto as ConnectionEventDto.MessageReceived).messageType)
        assertEquals(event.size, dto.size)
    }
    
    @Test
    fun `ConnectionEvent Error should convert to ConnectionEventDto correctly`() {
        val event = ConnectionEvent.Error(
            connectionId = "conn-123",
            timestamp = System.currentTimeMillis(),
            error = "Connection timeout",
            severity = ErrorSeverity.HIGH
        )
        
        val dto = event.toDto()
        
        assertTrue(dto is ConnectionEventDto.Error)
        assertEquals(event.connectionId, dto.connectionId)
        assertEquals(event.timestamp, dto.timestamp)
        assertEquals("error", dto.type)
        assertEquals(event.error, (dto as ConnectionEventDto.Error).error)
        assertEquals(event.severity.name, dto.severity)
    }
    
    /**
     * PED: TEST GROUP - CONVERSIÓN DE INFORMACIÓN DE PLUGINS
     */
    
    @Test
    fun `PluginInfo should convert to PluginStatsDto correctly`() {
        val pluginInfo = PluginInfo(
            id = "test-plugin",
            name = "Test Plugin",
            version = "1.0.0",
            status = PluginStatus.ACTIVE,
            loadedAt = System.currentTimeMillis() - 120000,
            lastActivity = System.currentTimeMillis() - 5000,
            commandCount = 5,
            executionCount = 100L,
            errorCount = 2L
        )
        
        val dto = pluginInfo.toStatsDto()
        
        assertEquals(pluginInfo.id, dto.id)
        assertEquals(pluginInfo.name, dto.name)
        assertEquals(pluginInfo.version, dto.version)
        assertEquals(pluginInfo.status.name, dto.status)
        assertEquals(pluginInfo.commandCount, dto.commandCount)
        assertEquals(pluginInfo.executionCount, dto.executionCount)
        assertEquals(pluginInfo.errorCount, dto.errorCount)
        assertEquals(pluginInfo.lastActivity, dto.lastActivity)
        
        // PED: Verificar cálculo de success rate
        val expectedSuccessRate = ((pluginInfo.executionCount - pluginInfo.errorCount).toDouble() / pluginInfo.executionCount) * 100
        assertEquals(expectedSuccessRate, dto.successRate)
    }
    
    @Test
    fun `PluginInfo with zero executions should have zero success rate`() {
        val pluginInfo = PluginInfo(
            id = "new-plugin",
            name = "New Plugin",
            version = "1.0.0",
            status = PluginStatus.LOADING,
            loadedAt = System.currentTimeMillis(),
            executionCount = 0L,
            errorCount = 0L
        )
        
        val dto = pluginInfo.toStatsDto()
        
        assertEquals(0.0, dto.successRate)
    }
    
    /**
     * PED: TEST GROUP - HEALTH STATUS GENERATION
     */
    
    @Test
    fun `healthy AppState should generate healthy HealthStatusDto`() {
        val appState = StateFactory.createDevelopmentState()
        val healthDto = appState.toHealthStatusDto()
        
        assertEquals(appState.isHealthy, healthDto.isHealthy)
        assertTrue(healthDto.checks.isNotEmpty())
        
        // PED: Verificar que hay checks para componentes principales
        val checkNames = healthDto.checks.map { it.name }
        assertTrue(checkNames.contains("server"))
        assertTrue(checkNames.contains("connections"))
        assertTrue(checkNames.contains("plugins"))
        
        // PED: Verificar que el servidor está marcado como pass
        val serverCheck = healthDto.checks.find { it.name == "server" }
        assertNotNull(serverCheck)
        assertEquals("pass", serverCheck.status)
    }
    
    @Test
    fun `unhealthy AppState should generate appropriate HealthStatusDto`() {
        val appState = StateFactory.createDevelopmentState().copy(
            serverState = StateFactory.createDevelopmentState().serverState.copy(
                isRunning = false
            )
        )
        
        val healthDto = appState.toHealthStatusDto()
        
        assertEquals("unhealthy", healthDto.status)
        
        // PED: Verificar que el servidor está marcado como fail
        val serverCheck = healthDto.checks.find { it.name == "server" }
        assertNotNull(serverCheck)
        assertEquals("fail", serverCheck.status)
    }
    
    /**
     * PED: TEST GROUP - REQUEST DTOs VALIDATION
     */
    
    @Test
    fun `CreateConnectionRequest should validate correctly`() {
        val validRequest = CreateConnectionRequest(
            type = "WEBSOCKET",
            clientInfo = ClientInfoDto(
                userAgent = "Test Client",
                version = "1.0.0",
                capabilities = listOf("test")
            )
        )
        
        // PED: No debería lanzar excepción
        validRequest.validate()
    }
    
    @Test
    fun `CreateConnectionRequest should throw for invalid type`() {
        val invalidRequest = CreateConnectionRequest(
            type = "INVALID_TYPE",
            clientInfo = ClientInfoDto(
                userAgent = "Test Client",
                capabilities = listOf("test")
            )
        )
        
        val exception = org.junit.jupiter.api.assertThrows<ApiError.ValidationError> {
            invalidRequest.validate()
        }
        
        assertEquals("type", exception.field)
        assertTrue(exception.message.contains("must be one of"))
    }
    
    @Test
    fun `ClientInfoDto should validate version format`() {
        val invalidClientInfo = ClientInfoDto(
            userAgent = "Test Client",
            version = "invalid-version",
            capabilities = listOf("test")
        )
        
        val exception = org.junit.jupiter.api.assertThrows<ApiError.ValidationError> {
            invalidClientInfo.validate()
        }
        
        assertEquals("version", exception.field)
        assertTrue(exception.message.contains("semantic versioning"))
    }
    
    @Test
    fun `UpdateServerConfigRequest should validate max connections range`() {
        val invalidRequest = UpdateServerConfigRequest(
            maxConnections = -1,
            capabilities = listOf("tools")
        )
        
        val exception = org.junit.jupiter.api.assertThrows<ApiError.ValidationError> {
            invalidRequest.validate()
        }
        
        assertEquals("maxConnections", exception.field)
        assertTrue(exception.message.contains("must be between"))
    }
    
    /**
     * PED: TEST GROUP - PAGINACIÓN
     */
    
    @Test
    fun `PaginationRequest should validate correctly`() {
        val validRequest = PaginationRequest(
            page = 0,
            size = 20,
            sort = "id",
            direction = "asc"
        )
        
        validRequest.validate()
    }
    
    @Test
    fun `PaginationRequest should throw for invalid direction`() {
        val invalidRequest = PaginationRequest(
            page = 0,
            size = 20,
            direction = "invalid"
        )
        
        val exception = org.junit.jupiter.api.assertThrows<ApiError.ValidationError> {
            invalidRequest.validate()
        }
        
        assertEquals("direction", exception.field)
        assertTrue(exception.message.contains("must be 'asc' or 'desc'"))
    }
    
    @Test
    fun `List should convert to PagedResponse correctly`() {
        val items = (1..25).map { "item-$it" }
        val request = PaginationRequest(page = 1, size = 10)
        
        val pagedResponse = items.toPagedResponse(request)
        
        assertEquals(10, pagedResponse.data.size)
        assertEquals("item-11", pagedResponse.data.first())
        assertEquals("item-20", pagedResponse.data.last())
        
        assertEquals(1, pagedResponse.pagination.page)
        assertEquals(10, pagedResponse.pagination.size)
        assertEquals(25L, pagedResponse.pagination.totalElements)
        assertEquals(3, pagedResponse.pagination.totalPages)
        assertTrue(pagedResponse.pagination.hasPrevious)
        assertTrue(pagedResponse.pagination.hasNext)
    }
    
    @Test
    fun `empty List should convert to empty PagedResponse`() {
        val items = emptyList<String>()
        val request = PaginationRequest(page = 0, size = 10)
        
        val pagedResponse = items.toPagedResponse(request)
        
        assertTrue(pagedResponse.data.isEmpty())
        assertEquals(0L, pagedResponse.pagination.totalElements)
        assertEquals(0, pagedResponse.pagination.totalPages)
        assertEquals(false, pagedResponse.pagination.hasPrevious)
        assertEquals(false, pagedResponse.pagination.hasNext)
    }
    
    @Test
    fun `PagedResponse should handle out of bounds page correctly`() {
        val items = listOf("item-1", "item-2", "item-3")
        val request = PaginationRequest(page = 10, size = 10) // Página que no existe
        
        val pagedResponse = items.toPagedResponse(request)
        
        assertTrue(pagedResponse.data.isEmpty())
        assertEquals(3L, pagedResponse.pagination.totalElements)
        assertEquals(1, pagedResponse.pagination.totalPages)
        assertTrue(pagedResponse.pagination.hasPrevious)
        assertEquals(false, pagedResponse.pagination.hasNext)
    }
}

