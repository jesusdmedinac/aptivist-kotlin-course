
package com.aptivist.kotlin.state

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 🎯 PHASE 3.1 - MANEJO DE ESTADO INMUTABLE CON DATA CLASSES
 * 
 * Este archivo demuestra conceptos avanzados de manejo de estado en Kotlin:
 * • Data classes para estado inmutable
 * • Sealed classes para acciones type-safe
 * • Copy functions para actualizaciones inmutables
 * • Nested data classes para estado complejo
 * • Serialización automática con kotlinx.serialization
 * 
 * PED: Las data classes en Kotlin proporcionan automáticamente:
 * - equals() y hashCode() basados en propiedades
 * - toString() con formato legible
 * - copy() para crear copias modificadas
 * - componentN() functions para destructuring
 * - Comparación estructural en lugar de referencial
 */

/**
 * PED: ESTADO PRINCIPAL DE LA APLICACIÓN
 * 
 * Esta data class representa el estado completo de nuestra aplicación MCP.
 * Al ser inmutable (todas las propiedades son val), garantizamos que el estado
 * no puede ser modificado directamente, solo reemplazado por nuevas instancias.
 * 
 * @property serverState Estado del servidor MCP
 * @property connectionState Estado de las conexiones
 * @property pluginState Estado del sistema de plugins
 * @property uiState Estado de la interfaz de usuario
 * @property metadata Metadatos del estado (timestamp, versión, etc.)
 */
@Serializable
data class AppState(
    val serverState: ServerState = ServerState(),
    val connectionState: ConnectionState = ConnectionState(),
    val pluginState: PluginState = PluginState(),
    val uiState: UiState = UiState(),
    val metadata: StateMetadata = StateMetadata()
) {
    /**
     * PED: COMPUTED PROPERTIES EN DATA CLASSES
     * 
     * Las computed properties nos permiten derivar información del estado
     * sin almacenarla redundantemente. Son recalculadas cada vez que se accede.
     */
    val isHealthy: Boolean
        get() = serverState.isRunning && 
                connectionState.activeConnections.isNotEmpty() &&
                pluginState.loadedPlugins.isNotEmpty()
    
    val totalConnections: Int
        get() = connectionState.activeConnections.size + connectionState.pendingConnections.size
    
    /**
     * PED: EXTENSION FUNCTIONS EN DATA CLASSES
     * 
     * Podemos agregar funcionalidad adicional a nuestras data classes
     * usando extension functions, manteniendo la separación de responsabilidades.
     */
    fun withUpdatedTimestamp(): AppState = copy(
        metadata = metadata.copy(lastUpdated = Instant.now().toEpochMilli())
    )
    
    /**
     * PED: VALIDATION FUNCTIONS
     * 
     * Funciones que validan la consistencia del estado.
     */
    fun validate(): List<String> = buildList {
        if (serverState.isRunning && serverState.port <= 0) {
            add("Server is running but port is invalid")
        }
        if (connectionState.activeConnections.size > serverState.maxConnections) {
            add("Active connections exceed maximum allowed")
        }
        if (pluginState.loadedPlugins.any { it.value.status == PluginStatus.ERROR }) {
            add("Some plugins are in error state")
        }
    }
}

/**
 * PED: ESTADO DEL SERVIDOR MCP
 * 
 * Data class que encapsula toda la información relacionada con el servidor.
 * Demuestra el uso de enums, default values, y propiedades opcionales.
 */
@Serializable
data class ServerState(
    val isRunning: Boolean = false,
    val port: Int = 8080,
    val host: String = "localhost",
    val protocol: ServerProtocol = ServerProtocol.HTTP,
    val maxConnections: Int = 100,
    val startTime: Long? = null,
    val lastError: String? = null,
    val capabilities: Set<String> = emptySet(),
    val statistics: ServerStatistics = ServerStatistics()
) {
    /**
     * PED: COMPUTED PROPERTY CON LÓGICA COMPLEJA
     */
    val uptime: Long?
        get() = startTime?.let { System.currentTimeMillis() - it }
    
    val address: String
        get() = "$protocol://$host:$port"
}

/**
 * PED: ENUM CLASS PARA PROTOCOLOS
 * 
 * Los enums en Kotlin pueden tener propiedades y métodos,
 * haciéndolos más poderosos que en otros lenguajes.
 */
@Serializable
enum class ServerProtocol(val displayName: String, val defaultPort: Int) {
    HTTP("HTTP", 8080),
    HTTPS("HTTPS", 8443),
    WEBSOCKET("WebSocket", 8080),
    TCP("TCP", 9090);
    
    /**
     * PED: MÉTODOS EN ENUMS
     */
    fun isSecure(): Boolean = this in listOf(HTTPS)
    
    companion object {
        fun fromString(value: String): ServerProtocol? = 
            values().find { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * PED: NESTED DATA CLASSES PARA ESTADÍSTICAS
 * 
 * Las data classes anidadas nos permiten organizar información relacionada
 * manteniendo la inmutabilidad y la estructura clara.
 */
@Serializable
data class ServerStatistics(
    val totalRequests: Long = 0L,
    val successfulRequests: Long = 0L,
    val failedRequests: Long = 0L,
    val averageResponseTime: Double = 0.0,
    val peakConnections: Int = 0,
    val bytesTransferred: Long = 0L
) {
    val successRate: Double
        get() = if (totalRequests > 0) {
            (successfulRequests.toDouble() / totalRequests) * 100
        } else 0.0
    
    val errorRate: Double
        get() = if (totalRequests > 0) {
            (failedRequests.toDouble() / totalRequests) * 100
        } else 0.0
}

/**
 * PED: ESTADO DE CONEXIONES CON COLLECTIONS INMUTABLES
 * 
 * Demuestra el uso de Maps y Lists inmutables para mantener
 * colecciones de objetos en el estado.
 */
@Serializable
data class ConnectionState(
    val activeConnections: Map<String, Connection> = emptyMap(),
    val pendingConnections: List<String> = emptyList(),
    val connectionHistory: List<ConnectionEvent> = emptyList(),
    val maxConcurrentConnections: Int = 50
) {
    /**
     * PED: FUNCIONES DE UTILIDAD PARA COLLECTIONS
     */
    fun getConnectionById(id: String): Connection? = activeConnections[id]
    
    fun getConnectionsByType(type: ConnectionType): List<Connection> = 
        activeConnections.values.filter { it.type == type }
    
    fun hasCapacity(): Boolean = activeConnections.size < maxConcurrentConnections
}

/**
 * PED: DATA CLASS PARA CONEXIONES INDIVIDUALES
 */
@Serializable
data class Connection(
    val id: String,
    val type: ConnectionType,
    val clientInfo: ClientInfo,
    val establishedAt: Long,
    val lastActivity: Long,
    val status: ConnectionStatus = ConnectionStatus.ACTIVE,
    val messageCount: Int = 0,
    val bytesTransferred: Long = 0L
) {
    val isActive: Boolean
        get() = status == ConnectionStatus.ACTIVE
    
    val duration: Long
        get() = System.currentTimeMillis() - establishedAt
}

@Serializable
enum class ConnectionType {
    HTTP, WEBSOCKET, TCP, UDP
}

@Serializable
enum class ConnectionStatus {
    CONNECTING, ACTIVE, IDLE, DISCONNECTING, DISCONNECTED, ERROR
}

@Serializable
data class ClientInfo(
    val userAgent: String? = null,
    val ipAddress: String,
    val version: String? = null,
    val capabilities: Set<String> = emptySet()
)

/**
 * PED: EVENTOS DE CONEXIÓN PARA HISTORIAL
 * 
 * Sealed class que representa diferentes tipos de eventos
 * que pueden ocurrir en las conexiones.
 */
@Serializable
sealed class ConnectionEvent {
    abstract val connectionId: String
    abstract val timestamp: Long
    
    @Serializable
    data class Connected(
        override val connectionId: String,
        override val timestamp: Long,
        val clientInfo: ClientInfo
    ) : ConnectionEvent()
    
    @Serializable
    data class Disconnected(
        override val connectionId: String,
        override val timestamp: Long,
        val reason: String
    ) : ConnectionEvent()
    
    @Serializable
    data class MessageReceived(
        override val connectionId: String,
        override val timestamp: Long,
        val messageType: String,
        val size: Int
    ) : ConnectionEvent()
    
    @Serializable
    data class Error(
        override val connectionId: String,
        override val timestamp: Long,
        val error: String,
        val severity: ErrorSeverity
    ) : ConnectionEvent()
}

@Serializable
enum class ErrorSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * PED: ESTADO DEL SISTEMA DE PLUGINS
 * 
 * Demuestra el manejo de estado para sistemas complejos con
 * múltiples componentes y estados anidados.
 */
@Serializable
data class PluginState(
    val loadedPlugins: Map<String, PluginInfo> = emptyMap(),
    val availablePlugins: List<PluginDescriptor> = emptyList(),
    val pluginRegistry: PluginRegistry = PluginRegistry(),
    val loadingErrors: List<PluginError> = emptyList()
) {
    val activePlugins: List<PluginInfo>
        get() = loadedPlugins.values.filter { it.status == PluginStatus.ACTIVE }
    
    val errorPlugins: List<PluginInfo>
        get() = loadedPlugins.values.filter { it.status == PluginStatus.ERROR }
}

@Serializable
data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val status: PluginStatus,
    val loadedAt: Long,
    val lastActivity: Long? = null,
    val commandCount: Int = 0,
    val executionCount: Long = 0L,
    val errorCount: Long = 0L
)

@Serializable
enum class PluginStatus {
    LOADING, ACTIVE, INACTIVE, ERROR, UNLOADING
}

@Serializable
data class PluginDescriptor(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val dependencies: List<String> = emptyList(),
    val capabilities: Set<String> = emptySet()
)

@Serializable
data class PluginRegistry(
    val registeredCommands: Map<String, String> = emptyMap(), // command -> pluginId
    val eventHandlers: Map<String, List<String>> = emptyMap(), // event -> pluginIds
    val capabilities: Map<String, Set<String>> = emptyMap() // pluginId -> capabilities
)

@Serializable
data class PluginError(
    val pluginId: String,
    val error: String,
    val timestamp: Long,
    val severity: ErrorSeverity,
    val stackTrace: String? = null
)

/**
 * PED: ESTADO DE LA INTERFAZ DE USUARIO
 * 
 * Aunque nuestro servidor es principalmente backend,
 * podemos tener estado relacionado con interfaces de administración.
 */
@Serializable
data class UiState(
    val theme: Theme = Theme.DARK,
    val language: String = "en",
    val notifications: List<Notification> = emptyList(),
    val activeView: String = "dashboard",
    val preferences: UserPreferences = UserPreferences()
)

@Serializable
enum class Theme {
    LIGHT, DARK, AUTO
}

@Serializable
data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val read: Boolean = false,
    val actions: List<NotificationAction> = emptyList()
)

@Serializable
enum class NotificationType {
    INFO, WARNING, ERROR, SUCCESS
}

@Serializable
data class NotificationAction(
    val id: String,
    val label: String,
    val action: String
)

@Serializable
data class UserPreferences(
    val autoRefresh: Boolean = true,
    val refreshInterval: Int = 5000, // milliseconds
    val showDebugInfo: Boolean = false,
    val maxLogEntries: Int = 1000,
    val enableSounds: Boolean = true
)

/**
 * PED: METADATOS DEL ESTADO
 * 
 * Información sobre el estado mismo, útil para debugging,
 * sincronización, y auditoría.
 */
@Serializable
data class StateMetadata(
    val version: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis(),
    val source: String = "local",
    val checksum: String? = null,
    val tags: Set<String> = emptySet()
) {
    /**
     * PED: COMPUTED PROPERTY PARA EDAD DEL ESTADO
     */
    val ageInMillis: Long
        get() = System.currentTimeMillis() - lastUpdated
    
    val isStale: Boolean
        get() = ageInMillis > 30_000 // 30 seconds
}

/**
 * PED: EXTENSION FUNCTIONS PARA OPERACIONES COMUNES
 * 
 * Estas funciones extienden nuestras data classes con operaciones
 * útiles manteniendo la inmutabilidad.
 */

/**
 * Extension function para agregar una conexión al estado
 */
fun AppState.addConnection(connection: Connection): AppState = copy(
    connectionState = connectionState.copy(
        activeConnections = connectionState.activeConnections + (connection.id to connection)
    ),
    metadata = metadata.copy(lastUpdated = System.currentTimeMillis())
)

/**
 * Extension function para remover una conexión
 */
fun AppState.removeConnection(connectionId: String): AppState = copy(
    connectionState = connectionState.copy(
        activeConnections = connectionState.activeConnections - connectionId
    ),
    metadata = metadata.copy(lastUpdated = System.currentTimeMillis())
)

/**
 * Extension function para actualizar estadísticas del servidor
 */
fun AppState.updateServerStats(
    requestsIncrement: Long = 0L,
    successIncrement: Long = 0L,
    failIncrement: Long = 0L,
    responseTime: Double? = null
): AppState {
    val currentStats = serverState.statistics
    val newStats = currentStats.copy(
        totalRequests = currentStats.totalRequests + requestsIncrement,
        successfulRequests = currentStats.successfulRequests + successIncrement,
        failedRequests = currentStats.failedRequests + failIncrement,
        averageResponseTime = responseTime ?: currentStats.averageResponseTime
    )
    
    return copy(
        serverState = serverState.copy(statistics = newStats),
        metadata = metadata.copy(lastUpdated = System.currentTimeMillis())
    )
}

/**
 * Extension function para agregar una notificación
 */
fun AppState.addNotification(notification: Notification): AppState = copy(
    uiState = uiState.copy(
        notifications = uiState.notifications + notification
    ),
    metadata = metadata.copy(lastUpdated = System.currentTimeMillis())
)

/**
 * Extension function para marcar notificaciones como leídas
 */
fun AppState.markNotificationsAsRead(notificationIds: List<String>): AppState = copy(
    uiState = uiState.copy(
        notifications = uiState.notifications.map { notification ->
            if (notification.id in notificationIds) {
                notification.copy(read = true)
            } else {
                notification
            }
        }
    ),
    metadata = metadata.copy(lastUpdated = System.currentTimeMillis())
)

/**
 * PED: FACTORY FUNCTIONS PARA CREAR ESTADOS INICIALES
 * 
 * Estas funciones proporcionan formas convenientes de crear
 * instancias de estado con configuraciones específicas.
 */
object StateFactory {
    
    /**
     * Crea un estado inicial para desarrollo
     */
    fun createDevelopmentState(): AppState = AppState(
        serverState = ServerState(
            isRunning = true,
            port = 8080,
            host = "localhost",
            protocol = ServerProtocol.HTTP,
            maxConnections = 10,
            startTime = System.currentTimeMillis(),
            capabilities = setOf("tools", "resources", "prompts")
        ),
        uiState = UiState(
            theme = Theme.DARK,
            preferences = UserPreferences(
                showDebugInfo = true,
                autoRefresh = true,
                refreshInterval = 1000
            )
        ),
        metadata = StateMetadata(
            source = "development",
            tags = setOf("dev", "local")
        )
    )
    
    /**
     * Crea un estado inicial para producción
     */
    fun createProductionState(): AppState = AppState(
        serverState = ServerState(
            isRunning = false,
            port = 443,
            host = "0.0.0.0",
            protocol = ServerProtocol.HTTPS,
            maxConnections = 1000,
            capabilities = setOf("tools", "resources", "prompts", "sampling")
        ),
        uiState = UiState(
            theme = Theme.AUTO,
            preferences = UserPreferences(
                showDebugInfo = false,
                autoRefresh = true,
                refreshInterval = 5000
            )
        ),
        metadata = StateMetadata(
            source = "production",
            tags = setOf("prod", "secure")
        )
    )
    
    /**
     * Crea un estado de prueba con datos mock
     */
    fun createTestState(): AppState {
        val mockConnection = Connection(
            id = "test-conn-1",
            type = ConnectionType.WEBSOCKET,
            clientInfo = ClientInfo(
                userAgent = "Test Client 1.0",
                ipAddress = "127.0.0.1",
                version = "1.0.0",
                capabilities = setOf("test")
            ),
            establishedAt = System.currentTimeMillis() - 60000, // 1 minute ago
            lastActivity = System.currentTimeMillis() - 5000, // 5 seconds ago
            messageCount = 42,
            bytesTransferred = 1024L
        )
        
        val mockPlugin = PluginInfo(
            id = "test-plugin",
            name = "Test Plugin",
            version = "1.0.0",
            status = PluginStatus.ACTIVE,
            loadedAt = System.currentTimeMillis() - 120000, // 2 minutes ago
            commandCount = 5,
            executionCount = 100L
        )
        
        return AppState(
            serverState = ServerState(
                isRunning = true,
                port = 8080,
                startTime = System.currentTimeMillis() - 300000, // 5 minutes ago
                statistics = ServerStatistics(
                    totalRequests = 1000L,
                    successfulRequests = 950L,
                    failedRequests = 50L,
                    averageResponseTime = 125.5,
                    peakConnections = 25,
                    bytesTransferred = 1024000L
                )
            ),
            connectionState = ConnectionState(
                activeConnections = mapOf(mockConnection.id to mockConnection),
                connectionHistory = listOf(
                    ConnectionEvent.Connected(
                        connectionId = mockConnection.id,
                        timestamp = mockConnection.establishedAt,
                        clientInfo = mockConnection.clientInfo
                    )
                )
            ),
            pluginState = PluginState(
                loadedPlugins = mapOf(mockPlugin.id to mockPlugin)
            ),
            metadata = StateMetadata(
                source = "test",
                tags = setOf("test", "mock")
            )
        )
    }
}
