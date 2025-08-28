

package com.aptivist.kotlin.http.api

import kotlinx.serialization.Serializable
import com.aptivist.kotlin.state.*

/**
 * 🎯 PHASE 3.2 - DATA TRANSFER OBJECTS (DTOs) PARA API REST
 * 
 * Este archivo demuestra conceptos avanzados de diseño de APIs REST:
 * • **DTOs (Data Transfer Objects)**: Separación entre modelo interno y API pública
 * • **Mapping Functions**: Conversión entre domain objects y DTOs
 * • **Extension Functions**: Para conversiones convenientes y type-safe
 * • **Sealed Classes**: Para responses polimórficas
 * • **Validation**: Validación de datos de entrada
 * • **Serialización Selectiva**: Control fino sobre qué datos se exponen
 * 
 * PED: Los DTOs son cruciales en APIs REST porque:
 * - Desacoplan la API pública del modelo interno
 * - Permiten evolución independiente de API y dominio
 * - Controlan exactamente qué datos se exponen
 * - Facilitan versionado de APIs
 * - Mejoran seguridad al no exponer datos sensibles
 */

/**
 * PED: DTO PARA ESTADO COMPLETO DE LA APLICACIÓN
 * 
 * Este DTO representa una vista simplificada del AppState para la API REST.
 * No incluye todos los campos internos, solo los relevantes para clientes externos.
 */
@Serializable
data class AppStateDto(
    val server: ServerStateDto,
    val connections: ConnectionSummaryDto,
    val plugins: PluginSummaryDto,
    val ui: UiStateDto,
    val metadata: StateMetadataDto,
    val health: HealthStatusDto
)

/**
 * PED: DTO PARA ESTADO DEL SERVIDOR
 * 
 * Versión simplificada del ServerState que expone solo información
 * relevante para clientes de la API.
 */
@Serializable
data class ServerStateDto(
    val isRunning: Boolean,
    val port: Int,
    val host: String,
    val protocol: String,
    val uptime: Long?,
    val address: String,
    val capabilities: List<String>,
    val statistics: ServerStatisticsDto
)

/**
 * PED: DTO PARA ESTADÍSTICAS DEL SERVIDOR
 * 
 * Incluye métricas calculadas y formateadas para consumo de la API.
 */
@Serializable
data class ServerStatisticsDto(
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val successRate: Double,
    val errorRate: Double,
    val averageResponseTime: Double,
    val peakConnections: Int,
    val bytesTransferred: Long
)

/**
 * PED: DTO PARA RESUMEN DE CONEXIONES
 * 
 * Proporciona un resumen de las conexiones sin exponer detalles sensibles
 * como direcciones IP completas o información de cliente detallada.
 */
@Serializable
data class ConnectionSummaryDto(
    val activeConnections: Int,
    val pendingConnections: Int,
    val totalConnections: Int,
    val maxConcurrentConnections: Int,
    val hasCapacity: Boolean,
    val connectionsByType: Map<String, Int>,
    val recentEvents: List<ConnectionEventDto>
)

/**
 * PED: DTO PARA EVENTOS DE CONEXIÓN
 * 
 * Versión sanitizada de ConnectionEvent que no expone información sensible.
 */
@Serializable
sealed class ConnectionEventDto {
    abstract val connectionId: String
    abstract val timestamp: Long
    abstract val type: String
    
    @Serializable
    data class Connected(
        override val connectionId: String,
        override val timestamp: Long,
        val clientType: String
    ) : ConnectionEventDto() {
        override val type: String = "connected"
    }
    
    @Serializable
    data class Disconnected(
        override val connectionId: String,
        override val timestamp: Long,
        val reason: String
    ) : ConnectionEventDto() {
        override val type: String = "disconnected"
    }
    
    @Serializable
    data class MessageReceived(
        override val connectionId: String,
        override val timestamp: Long,
        val messageType: String,
        val size: Int
    ) : ConnectionEventDto() {
        override val type: String = "message_received"
    }
    
    @Serializable
    data class Error(
        override val connectionId: String,
        override val timestamp: Long,
        val error: String,
        val severity: String
    ) : ConnectionEventDto() {
        override val type: String = "error"
    }
}

/**
 * PED: DTO PARA RESUMEN DE PLUGINS
 * 
 * Información agregada sobre el estado de los plugins sin exponer
 * detalles internos de implementación.
 */
@Serializable
data class PluginSummaryDto(
    val totalPlugins: Int,
    val activePlugins: Int,
    val errorPlugins: Int,
    val loadingPlugins: Int,
    val availablePlugins: Int,
    val pluginStats: List<PluginStatsDto>
)

/**
 * PED: DTO PARA ESTADÍSTICAS DE PLUGIN INDIVIDUAL
 */
@Serializable
data class PluginStatsDto(
    val id: String,
    val name: String,
    val version: String,
    val status: String,
    val commandCount: Int,
    val executionCount: Long,
    val errorCount: Long,
    val successRate: Double,
    val lastActivity: Long?
)

/**
 * PED: DTO PARA ESTADO DE UI
 * 
 * Configuración de interfaz de usuario relevante para clientes de la API.
 */
@Serializable
data class UiStateDto(
    val theme: String,
    val language: String,
    val unreadNotifications: Int,
    val activeView: String,
    val preferences: UserPreferencesDto
)

/**
 * PED: DTO PARA PREFERENCIAS DE USUARIO
 */
@Serializable
data class UserPreferencesDto(
    val autoRefresh: Boolean,
    val refreshInterval: Int,
    val showDebugInfo: Boolean,
    val enableSounds: Boolean
)

/**
 * PED: DTO PARA METADATOS DE ESTADO
 */
@Serializable
data class StateMetadataDto(
    val version: Int,
    val lastUpdated: Long,
    val source: String,
    val ageInMillis: Long,
    val isStale: Boolean,
    val tags: List<String>
)

/**
 * PED: DTO PARA ESTADO DE SALUD
 * 
 * Información agregada sobre la salud general del sistema.
 */
@Serializable
data class HealthStatusDto(
    val isHealthy: Boolean,
    val status: String, // "healthy", "degraded", "unhealthy"
    val checks: List<HealthCheckDto>,
    val lastCheck: Long
)

/**
 * PED: DTO PARA CHECKS DE SALUD INDIVIDUALES
 */
@Serializable
data class HealthCheckDto(
    val name: String,
    val status: String, // "pass", "fail", "warn"
    val message: String?,
    val duration: Long?, // tiempo en ms que tomó el check
    val timestamp: Long
)

/**
 * PED: DTOs PARA REQUESTS DE CREACIÓN Y ACTUALIZACIÓN
 * 
 * Estos DTOs representan los datos que los clientes envían para
 * crear o actualizar recursos.
 */

/**
 * DTO para crear una nueva conexión
 */
@Serializable
data class CreateConnectionRequest(
    val type: String,
    val clientInfo: ClientInfoDto
) {
    /**
     * PED: VALIDATION FUNCTION EN DTO
     * 
     * Los DTOs pueden incluir funciones de validación para
     * verificar que los datos recibidos sean válidos.
     */
    fun validate() {
        type.requireNotBlank("type")
        require(
            condition = type in listOf("HTTP", "WEBSOCKET", "TCP", "UDP"),
            fieldName = "type"
        ) { "must be one of: HTTP, WEBSOCKET, TCP, UDP" }
        
        clientInfo.validate()
    }
}

/**
 * DTO para información de cliente
 */
@Serializable
data class ClientInfoDto(
    val userAgent: String?,
    val version: String?,
    val capabilities: List<String> = emptyList()
) {
    fun validate() {
        version?.let { v ->
            require(
                condition = v.matches(Regex("""\d+\.\d+\.\d+""")),
                fieldName = "version"
            ) { "must follow semantic versioning format (x.y.z)" }
        }
        
        capabilities.requireNotEmpty("capabilities")
    }
}

/**
 * DTO para actualizar configuración del servidor
 */
@Serializable
data class UpdateServerConfigRequest(
    val maxConnections: Int?,
    val capabilities: List<String>?
) {
    fun validate() {
        maxConnections?.requireInRange(1, 10000, "maxConnections")
        capabilities?.requireNotEmpty("capabilities")
    }
}

/**
 * DTO para cargar un plugin
 */
@Serializable
data class LoadPluginRequest(
    val id: String,
    val version: String? = null,
    val config: Map<String, String> = emptyMap()
) {
    fun validate() {
        id.requireNotBlank("id")
        version?.let { v ->
            require(
                condition = v.matches(Regex("""\d+\.\d+\.\d+""")),
                fieldName = "version"
            ) { "must follow semantic versioning format (x.y.z)" }
        }
    }
}

/**
 * PED: DTOs PARA RESPONSES PAGINADAS
 * 
 * Estos DTOs manejan responses que pueden contener grandes cantidades de datos
 * y necesitan ser paginadas.
 */

/**
 * DTO genérico para responses paginadas
 */
@Serializable
data class PagedResponse<T>(
    val data: List<T>,
    val pagination: PaginationInfo
)

/**
 * DTO para información de paginación
 */
@Serializable
data class PaginationInfo(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * DTO para parámetros de paginación en requests
 */
@Serializable
data class PaginationRequest(
    val page: Int = 0,
    val size: Int = 20,
    val sort: String? = null,
    val direction: String = "asc"
) {
    fun validate() {
        page.requireInRange(0, Int.MAX_VALUE, "page")
        size.requireInRange(1, 100, "size")
        
        if (direction !in listOf("asc", "desc")) {
            throw ApiError.validationError("direction", "must be 'asc' or 'desc'")
        }
    }
}

/**
 * PED: EXTENSION FUNCTIONS PARA MAPPING
 * 
 * Estas extension functions proporcionan conversiones convenientes
 * entre domain objects y DTOs.
 */

/**
 * Convierte AppState a AppStateDto
 */
fun AppState.toDto(): AppStateDto = AppStateDto(
    server = serverState.toDto(),
    connections = connectionState.toSummaryDto(),
    plugins = pluginState.toSummaryDto(),
    ui = uiState.toDto(),
    metadata = metadata.toDto(),
    health = toHealthStatusDto()
)

/**
 * Convierte ServerState a ServerStateDto
 */
fun ServerState.toDto(): ServerStateDto = ServerStateDto(
    isRunning = isRunning,
    port = port,
    host = host,
    protocol = protocol.name,
    uptime = uptime,
    address = address,
    capabilities = capabilities.toList(),
    statistics = statistics.toDto()
)

/**
 * Convierte ServerStatistics a ServerStatisticsDto
 */
fun ServerStatistics.toDto(): ServerStatisticsDto = ServerStatisticsDto(
    totalRequests = totalRequests,
    successfulRequests = successfulRequests,
    failedRequests = failedRequests,
    successRate = successRate,
    errorRate = errorRate,
    averageResponseTime = averageResponseTime,
    peakConnections = peakConnections,
    bytesTransferred = bytesTransferred
)

/**
 * Convierte ConnectionState a ConnectionSummaryDto
 */
fun ConnectionState.toSummaryDto(): ConnectionSummaryDto = ConnectionSummaryDto(
    activeConnections = activeConnections.size,
    pendingConnections = pendingConnections.size,
    totalConnections = activeConnections.size + pendingConnections.size,
    maxConcurrentConnections = maxConcurrentConnections,
    hasCapacity = hasCapacity(),
    connectionsByType = activeConnections.values
        .groupBy { it.type.name }
        .mapValues { it.value.size },
    recentEvents = connectionHistory.takeLast(10).map { it.toDto() }
)

/**
 * Convierte ConnectionEvent a ConnectionEventDto
 */
fun ConnectionEvent.toDto(): ConnectionEventDto = when (this) {
    is ConnectionEvent.Connected -> ConnectionEventDto.Connected(
        connectionId = connectionId,
        timestamp = timestamp,
        clientType = clientInfo.userAgent ?: "Unknown"
    )
    is ConnectionEvent.Disconnected -> ConnectionEventDto.Disconnected(
        connectionId = connectionId,
        timestamp = timestamp,
        reason = reason
    )
    is ConnectionEvent.MessageReceived -> ConnectionEventDto.MessageReceived(
        connectionId = connectionId,
        timestamp = timestamp,
        messageType = messageType,
        size = size
    )
    is ConnectionEvent.Error -> ConnectionEventDto.Error(
        connectionId = connectionId,
        timestamp = timestamp,
        error = error,
        severity = severity.name
    )
}

/**
 * Convierte PluginState a PluginSummaryDto
 */
fun PluginState.toSummaryDto(): PluginSummaryDto = PluginSummaryDto(
    totalPlugins = loadedPlugins.size,
    activePlugins = activePlugins.size,
    errorPlugins = errorPlugins.size,
    loadingPlugins = loadedPlugins.values.count { it.status == PluginStatus.LOADING },
    availablePlugins = availablePlugins.size,
    pluginStats = loadedPlugins.values.map { it.toStatsDto() }
)

/**
 * Convierte PluginInfo a PluginStatsDto
 */
fun PluginInfo.toStatsDto(): PluginStatsDto = PluginStatsDto(
    id = id,
    name = name,
    version = version,
    status = status.name,
    commandCount = commandCount,
    executionCount = executionCount,
    errorCount = errorCount,
    successRate = if (executionCount > 0) {
        ((executionCount - errorCount).toDouble() / executionCount) * 100
    } else 0.0,
    lastActivity = lastActivity
)

/**
 * Convierte UiState a UiStateDto
 */
fun UiState.toDto(): UiStateDto = UiStateDto(
    theme = theme.name,
    language = language,
    unreadNotifications = notifications.count { !it.read },
    activeView = activeView,
    preferences = preferences.toDto()
)

/**
 * Convierte UserPreferences a UserPreferencesDto
 */
fun UserPreferences.toDto(): UserPreferencesDto = UserPreferencesDto(
    autoRefresh = autoRefresh,
    refreshInterval = refreshInterval,
    showDebugInfo = showDebugInfo,
    enableSounds = enableSounds
)

/**
 * Convierte StateMetadata a StateMetadataDto
 */
fun StateMetadata.toDto(): StateMetadataDto = StateMetadataDto(
    version = version,
    lastUpdated = lastUpdated,
    source = source,
    ageInMillis = ageInMillis,
    isStale = isStale,
    tags = tags.toList()
)

/**
 * PED: EXTENSION FUNCTION PARA HEALTH STATUS
 * 
 * Genera un DTO de estado de salud basado en el estado actual de la aplicación.
 */
fun AppState.toHealthStatusDto(): HealthStatusDto {
    val checks = mutableListOf<HealthCheckDto>()
    val now = System.currentTimeMillis()
    
    // Check del servidor
    checks.add(
        HealthCheckDto(
            name = "server",
            status = if (serverState.isRunning) "pass" else "fail",
            message = if (serverState.isRunning) "Server is running" else "Server is not running",
            duration = null,
            timestamp = now
        )
    )
    
    // Check de conexiones
    val hasConnections = connectionState.activeConnections.isNotEmpty()
    checks.add(
        HealthCheckDto(
            name = "connections",
            status = if (hasConnections) "pass" else "warn",
            message = if (hasConnections) 
                "${connectionState.activeConnections.size} active connections" 
            else "No active connections",
            duration = null,
            timestamp = now
        )
    )
    
    // Check de plugins
    val hasActivePlugins = pluginState.activePlugins.isNotEmpty()
    val hasErrorPlugins = pluginState.errorPlugins.isNotEmpty()
    checks.add(
        HealthCheckDto(
            name = "plugins",
            status = when {
                hasErrorPlugins -> "fail"
                hasActivePlugins -> "pass"
                else -> "warn"
            },
            message = "${pluginState.activePlugins.size} active, ${pluginState.errorPlugins.size} errors",
            duration = null,
            timestamp = now
        )
    )
    
    // Determinar estado general
    val overallStatus = when {
        checks.any { it.status == "fail" } -> "unhealthy"
        checks.any { it.status == "warn" } -> "degraded"
        else -> "healthy"
    }
    
    return HealthStatusDto(
        isHealthy = isHealthy,
        status = overallStatus,
        checks = checks,
        lastCheck = now
    )
}

/**
 * PED: UTILITY FUNCTIONS PARA PAGINACIÓN
 * 
 * Estas funciones ayudan a crear responses paginadas de manera conveniente.
 */

/**
 * Crea una response paginada a partir de una lista y parámetros de paginación
 */
fun <T> List<T>.toPagedResponse(request: PaginationRequest): PagedResponse<T> {
    val totalElements = this.size.toLong()
    val totalPages = (totalElements + request.size - 1) / request.size
    val startIndex = request.page * request.size
    val endIndex = minOf(startIndex + request.size, this.size)
    
    val pageData = if (startIndex < this.size) {
        this.subList(startIndex, endIndex)
    } else {
        emptyList()
    }
    
    return PagedResponse(
        data = pageData,
        pagination = PaginationInfo(
            page = request.page,
            size = request.size,
            totalElements = totalElements,
            totalPages = totalPages.toInt(),
            hasNext = request.page < totalPages - 1,
            hasPrevious = request.page > 0
        )
    )
}

