
package com.aptivist.kotlin.state

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 🎯 PHASE 3.1 - SISTEMA DE ACCIONES PARA STATE MANAGEMENT
 * 
 * Este archivo demuestra conceptos avanzados de arquitectura de estado:
 * • Sealed classes para acciones type-safe
 * • Pattern matching exhaustivo con when expressions
 * • Immutable updates usando data class copy()
 * • Action creators y factory functions
 * • Payload validation y error handling
 * • Hierarchical action organization
 * 
 * PED: Las sealed classes son perfectas para representar acciones porque:
 * - Garantizan type safety en compile time
 * - Permiten pattern matching exhaustivo
 * - Facilitan la adición de nuevos tipos de acción
 * - Proporcionan mejor IDE support y refactoring
 */

/**
 * PED: SEALED CLASS PRINCIPAL PARA TODAS LAS ACCIONES
 * 
 * Esta sealed class actúa como la raíz de nuestra jerarquía de acciones.
 * Cada acción representa una intención de cambiar el estado de la aplicación.
 * 
 * Al usar sealed classes, el compilador puede verificar que manejemos
 * todos los casos posibles en nuestros reducers.
 */
@Serializable
sealed class AppAction {
    
    /**
     * PED: ACCIONES DEL SERVIDOR
     * 
     * Nested sealed class que agrupa todas las acciones relacionadas
     * con el estado del servidor MCP.
     */
    @Serializable
    sealed class Server : AppAction() {
        
        @Serializable
        data class Start(
            val port: Int,
            val host: String = "localhost",
            val protocol: ServerProtocol = ServerProtocol.HTTP,
            val maxConnections: Int = 100
        ) : Server() {
            init {
                require(port in 1..65535) { "Port must be between 1 and 65535" }
                require(host.isNotBlank()) { "Host cannot be blank" }
                require(maxConnections > 0) { "Max connections must be positive" }
            }
        }
        
        @Serializable
        object Stop : Server()
        
        @Serializable
        data class UpdateCapabilities(
            val capabilities: Set<String>
        ) : Server() {
            init {
                require(capabilities.isNotEmpty()) { "Capabilities cannot be empty" }
            }
        }
        
        @Serializable
        data class UpdateStatistics(
            val requestsIncrement: Long = 0L,
            val successIncrement: Long = 0L,
            val failIncrement: Long = 0L,
            val responseTime: Double? = null,
            val bytesTransferred: Long = 0L
        ) : Server() {
            init {
                require(requestsIncrement >= 0) { "Requests increment cannot be negative" }
                require(successIncrement >= 0) { "Success increment cannot be negative" }
                require(failIncrement >= 0) { "Fail increment cannot be negative" }
                responseTime?.let { require(it >= 0) { "Response time cannot be negative" } }
                require(bytesTransferred >= 0) { "Bytes transferred cannot be negative" }
            }
        }
        
        @Serializable
        data class SetError(
            val error: String,
            val timestamp: Long = System.currentTimeMillis()
        ) : Server() {
            init {
                require(error.isNotBlank()) { "Error message cannot be blank" }
            }
        }
        
        @Serializable
        object ClearError : Server()
    }
    
    /**
     * PED: ACCIONES DE CONEXIONES
     * 
     * Maneja todas las operaciones relacionadas con conexiones de clientes.
     */
    @Serializable
    sealed class Connection : AppAction() {
        
        @Serializable
        data class Add(
            val connection: com.aptivist.kotlin.state.Connection
        ) : Connection()
        
        @Serializable
        data class Remove(
            val connectionId: String,
            val reason: String = "Client disconnected"
        ) : Connection() {
            init {
                require(connectionId.isNotBlank()) { "Connection ID cannot be blank" }
            }
        }
        
        @Serializable
        data class UpdateStatus(
            val connectionId: String,
            val status: ConnectionStatus
        ) : Connection() {
            init {
                require(connectionId.isNotBlank()) { "Connection ID cannot be blank" }
            }
        }
        
        @Serializable
        data class UpdateActivity(
            val connectionId: String,
            val messageCount: Int = 0,
            val bytesTransferred: Long = 0L
        ) : Connection() {
            init {
                require(connectionId.isNotBlank()) { "Connection ID cannot be blank" }
                require(messageCount >= 0) { "Message count cannot be negative" }
                require(bytesTransferred >= 0) { "Bytes transferred cannot be negative" }
            }
        }
        
        @Serializable
        data class AddEvent(
            val event: ConnectionEvent
        ) : Connection()
        
        @Serializable
        data class ClearHistory(
            val olderThan: Long? = null
        ) : Connection()
    }
    
    /**
     * PED: ACCIONES DE PLUGINS
     * 
     * Gestiona el ciclo de vida y estado de los plugins.
     */
    @Serializable
    sealed class Plugin : AppAction() {
        
        @Serializable
        data class Load(
            val pluginInfo: PluginInfo
        ) : Plugin()
        
        @Serializable
        data class Unload(
            val pluginId: String
        ) : Plugin() {
            init {
                require(pluginId.isNotBlank()) { "Plugin ID cannot be blank" }
            }
        }
        
        @Serializable
        data class UpdateStatus(
            val pluginId: String,
            val status: PluginStatus
        ) : Plugin() {
            init {
                require(pluginId.isNotBlank()) { "Plugin ID cannot be blank" }
            }
        }
        
        @Serializable
        data class UpdateActivity(
            val pluginId: String,
            val executionIncrement: Long = 0L,
            val errorIncrement: Long = 0L
        ) : Plugin() {
            init {
                require(pluginId.isNotBlank()) { "Plugin ID cannot be blank" }
                require(executionIncrement >= 0) { "Execution increment cannot be negative" }
                require(errorIncrement >= 0) { "Error increment cannot be negative" }
            }
        }
        
        @Serializable
        data class RegisterCommand(
            val pluginId: String,
            val commandName: String
        ) : Plugin() {
            init {
                require(pluginId.isNotBlank()) { "Plugin ID cannot be blank" }
                require(commandName.isNotBlank()) { "Command name cannot be blank" }
            }
        }
        
        @Serializable
        data class UnregisterCommand(
            val commandName: String
        ) : Plugin() {
            init {
                require(commandName.isNotBlank()) { "Command name cannot be blank" }
            }
        }
        
        @Serializable
        data class AddError(
            val error: PluginError
        ) : Plugin()
        
        @Serializable
        data class ClearErrors(
            val pluginId: String? = null
        ) : Plugin()
    }
    
    /**
     * PED: ACCIONES DE UI
     * 
     * Maneja el estado de la interfaz de usuario y preferencias.
     */
    @Serializable
    sealed class UI : AppAction() {
        
        @Serializable
        data class SetTheme(
            val theme: Theme
        ) : UI()
        
        @Serializable
        data class SetLanguage(
            val language: String
        ) : UI() {
            init {
                require(language.isNotBlank()) { "Language cannot be blank" }
            }
        }
        
        @Serializable
        data class SetActiveView(
            val view: String
        ) : UI() {
            init {
                require(view.isNotBlank()) { "View cannot be blank" }
            }
        }
        
        @Serializable
        data class AddNotification(
            val notification: Notification
        ) : UI()
        
        @Serializable
        data class RemoveNotification(
            val notificationId: String
        ) : UI() {
            init {
                require(notificationId.isNotBlank()) { "Notification ID cannot be blank" }
            }
        }
        
        @Serializable
        data class MarkNotificationsAsRead(
            val notificationIds: List<String>
        ) : UI() {
            init {
                require(notificationIds.isNotEmpty()) { "Notification IDs cannot be empty" }
                require(notificationIds.all { it.isNotBlank() }) { "All notification IDs must be non-blank" }
            }
        }
        
        @Serializable
        object ClearAllNotifications : UI()
        
        @Serializable
        data class UpdatePreferences(
            val preferences: UserPreferences
        ) : UI()
    }
    
    /**
     * PED: ACCIONES DE SISTEMA
     * 
     * Acciones que afectan el estado general del sistema.
     */
    @Serializable
    sealed class System : AppAction() {
        
        @Serializable
        object Reset : System()
        
        @Serializable
        data class LoadState(
            val state: AppState
        ) : System()
        
        @Serializable
        data class UpdateMetadata(
            val source: String? = null,
            val tags: Set<String>? = null,
            val version: Int? = null
        ) : System()
        
        @Serializable
        data class Batch(
            val actions: List<AppAction>
        ) : System() {
            init {
                require(actions.isNotEmpty()) { "Batch actions cannot be empty" }
                require(actions.none { it is Batch }) { "Nested batch actions are not allowed" }
            }
        }
    }
}

/**
 * PED: ACTION CREATORS - FACTORY FUNCTIONS PARA CREAR ACCIONES
 * 
 * Estas funciones proporcionan una API más conveniente para crear acciones,
 * con validación adicional y valores por defecto inteligentes.
 */
object ActionCreators {
    
    /**
     * PED: SERVER ACTION CREATORS
     */
    object Server {
        
        fun start(
            port: Int = 8080,
            host: String = "localhost",
            protocol: ServerProtocol = ServerProtocol.HTTP,
            maxConnections: Int = 100
        ): AppAction.Server.Start = AppAction.Server.Start(port, host, protocol, maxConnections)
        
        fun stop(): AppAction.Server.Stop = AppAction.Server.Stop
        
        fun updateCapabilities(vararg capabilities: String): AppAction.Server.UpdateCapabilities =
            AppAction.Server.UpdateCapabilities(capabilities.toSet())
        
        fun recordRequest(
            success: Boolean = true,
            responseTime: Double? = null,
            bytesTransferred: Long = 0L
        ): AppAction.Server.UpdateStatistics = AppAction.Server.UpdateStatistics(
            requestsIncrement = 1L,
            successIncrement = if (success) 1L else 0L,
            failIncrement = if (!success) 1L else 0L,
            responseTime = responseTime,
            bytesTransferred = bytesTransferred
        )
        
        fun setError(error: String): AppAction.Server.SetError = 
            AppAction.Server.SetError(error)
        
        fun clearError(): AppAction.Server.ClearError = AppAction.Server.ClearError
    }
    
    /**
     * PED: CONNECTION ACTION CREATORS
     */
    object Connection {
        
        fun connect(
            id: String,
            type: ConnectionType,
            clientInfo: ClientInfo
        ): AppAction.Connection.Add {
            val connection = com.aptivist.kotlin.state.Connection(
                id = id,
                type = type,
                clientInfo = clientInfo,
                establishedAt = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis(),
                status = ConnectionStatus.ACTIVE
            )
            return AppAction.Connection.Add(connection)
        }
        
        fun disconnect(
            connectionId: String,
            reason: String = "Client disconnected"
        ): AppAction.Connection.Remove = AppAction.Connection.Remove(connectionId, reason)
        
        fun updateActivity(
            connectionId: String,
            messageIncrement: Int = 1,
            bytesTransferred: Long = 0L
        ): AppAction.Connection.UpdateActivity = AppAction.Connection.UpdateActivity(
            connectionId = connectionId,
            messageCount = messageIncrement,
            bytesTransferred = bytesTransferred
        )
        
        fun recordEvent(event: ConnectionEvent): AppAction.Connection.AddEvent =
            AppAction.Connection.AddEvent(event)
        
        fun clearOldHistory(olderThanHours: Int = 24): AppAction.Connection.ClearHistory {
            val cutoffTime = System.currentTimeMillis() - (olderThanHours * 60 * 60 * 1000L)
            return AppAction.Connection.ClearHistory(cutoffTime)
        }
    }
    
    /**
     * PED: PLUGIN ACTION CREATORS
     */
    object Plugin {
        
        fun load(
            id: String,
            name: String,
            version: String,
            status: PluginStatus = PluginStatus.LOADING
        ): AppAction.Plugin.Load {
            val pluginInfo = PluginInfo(
                id = id,
                name = name,
                version = version,
                status = status,
                loadedAt = Instant.now().toEpochMilli()
            )
            return AppAction.Plugin.Load(pluginInfo)
        }
        
        fun activate(pluginId: String): AppAction.Plugin.UpdateStatus =
            AppAction.Plugin.UpdateStatus(pluginId, PluginStatus.ACTIVE)
        
        fun deactivate(pluginId: String): AppAction.Plugin.UpdateStatus =
            AppAction.Plugin.UpdateStatus(pluginId, PluginStatus.INACTIVE)
        
        fun error(pluginId: String): AppAction.Plugin.UpdateStatus =
            AppAction.Plugin.UpdateStatus(pluginId, PluginStatus.ERROR)
        
        fun recordExecution(
            pluginId: String,
            success: Boolean = true
        ): AppAction.Plugin.UpdateActivity = AppAction.Plugin.UpdateActivity(
            pluginId = pluginId,
            executionIncrement = 1L,
            errorIncrement = if (!success) 1L else 0L
        )
        
        fun registerCommand(
            pluginId: String,
            commandName: String
        ): AppAction.Plugin.RegisterCommand = AppAction.Plugin.RegisterCommand(pluginId, commandName)
        
        fun reportError(
            pluginId: String,
            error: String,
            severity: ErrorSeverity = ErrorSeverity.MEDIUM,
            stackTrace: String? = null
        ): AppAction.Plugin.AddError {
            val pluginError = PluginError(
                pluginId = pluginId,
                error = error,
                timestamp = Instant.now().toEpochMilli(),
                severity = severity,
                stackTrace = stackTrace
            )
            return AppAction.Plugin.AddError(pluginError)
        }
    }
    
    /**
     * PED: UI ACTION CREATORS
     */
    object UI {
        
        fun setTheme(theme: Theme): AppAction.UI.SetTheme = AppAction.UI.SetTheme(theme)
        
        fun setLanguage(language: String): AppAction.UI.SetLanguage = AppAction.UI.SetLanguage(language)
        
        fun navigateTo(view: String): AppAction.UI.SetActiveView = AppAction.UI.SetActiveView(view)
        
        fun notify(
            type: NotificationType,
            title: String,
            message: String,
            actions: List<NotificationAction> = emptyList()
        ): AppAction.UI.AddNotification {
            val notification = Notification(
                id = "notif-${Instant.now().toEpochMilli()}-${(0..999).random()}",
                type = type,
                title = title,
                message = message,
                timestamp = Instant.now().toEpochMilli(),
                actions = actions
            )
            return AppAction.UI.AddNotification(notification)
        }
        
        fun info(title: String, message: String): AppAction.UI.AddNotification =
            notify(NotificationType.INFO, title, message)
        
        fun warning(title: String, message: String): AppAction.UI.AddNotification =
            notify(NotificationType.WARNING, title, message)
        
        fun error(title: String, message: String): AppAction.UI.AddNotification =
            notify(NotificationType.ERROR, title, message)
        
        fun success(title: String, message: String): AppAction.UI.AddNotification =
            notify(NotificationType.SUCCESS, title, message)
        
        fun dismissNotification(notificationId: String): AppAction.UI.RemoveNotification =
            AppAction.UI.RemoveNotification(notificationId)
        
        fun markAsRead(vararg notificationIds: String): AppAction.UI.MarkNotificationsAsRead =
            AppAction.UI.MarkNotificationsAsRead(notificationIds.toList())
        
        fun updatePreferences(
            autoRefresh: Boolean? = null,
            refreshInterval: Int? = null,
            showDebugInfo: Boolean? = null,
            maxLogEntries: Int? = null,
            enableSounds: Boolean? = null
        ): AppAction.UI.UpdatePreferences {
            // PED: Usamos el patrón builder con copy() para actualizar solo los campos especificados
            val currentPreferences = UserPreferences() // Default preferences
            val updatedPreferences = currentPreferences.copy(
                autoRefresh = autoRefresh ?: currentPreferences.autoRefresh,
                refreshInterval = refreshInterval ?: currentPreferences.refreshInterval,
                showDebugInfo = showDebugInfo ?: currentPreferences.showDebugInfo,
                maxLogEntries = maxLogEntries ?: currentPreferences.maxLogEntries,
                enableSounds = enableSounds ?: currentPreferences.enableSounds
            )
            return AppAction.UI.UpdatePreferences(updatedPreferences)
        }
    }
    
    /**
     * PED: SYSTEM ACTION CREATORS
     */
    object System {
        
        fun reset(): AppAction.System.Reset = AppAction.System.Reset
        
        fun loadState(state: AppState): AppAction.System.LoadState = AppAction.System.LoadState(state)
        
        fun updateMetadata(
            source: String? = null,
            tags: Set<String>? = null,
            version: Int? = null
        ): AppAction.System.UpdateMetadata = AppAction.System.UpdateMetadata(source, tags, version)
        
        fun batch(vararg actions: AppAction): AppAction.System.Batch =
            AppAction.System.Batch(actions.toList())
        
        fun batch(actions: List<AppAction>): AppAction.System.Batch =
            AppAction.System.Batch(actions)
    }
}

/**
 * PED: EXTENSION FUNCTIONS PARA ACCIONES
 * 
 * Estas extension functions proporcionan funcionalidad adicional
 * para trabajar con acciones de manera más conveniente.
 */

/**
 * Extension function para verificar si una acción afecta el servidor
 */
fun AppAction.affectsServer(): Boolean = this is AppAction.Server

/**
 * Extension function para verificar si una acción afecta las conexiones
 */
fun AppAction.affectsConnections(): Boolean = this is AppAction.Connection

/**
 * Extension function para verificar si una acción afecta los plugins
 */
fun AppAction.affectsPlugins(): Boolean = this is AppAction.Plugin

/**
 * Extension function para verificar si una acción afecta la UI
 */
fun AppAction.affectsUI(): Boolean = this is AppAction.UI

/**
 * Extension function para obtener una descripción legible de la acción
 */
fun AppAction.describe(): String = when (this) {
    is AppAction.Server.Start -> "Start server on $host:$port"
    is AppAction.Server.Stop -> "Stop server"
    is AppAction.Server.UpdateCapabilities -> "Update server capabilities: ${capabilities.joinToString()}"
    is AppAction.Server.UpdateStatistics -> "Update server statistics"
    is AppAction.Server.SetError -> "Set server error: $error"
    is AppAction.Server.ClearError -> "Clear server error"
    
    is AppAction.Connection.Add -> "Add connection: ${connection.id}"
    is AppAction.Connection.Remove -> "Remove connection: $connectionId ($reason)"
    is AppAction.Connection.UpdateStatus -> "Update connection $connectionId status to $status"
    is AppAction.Connection.UpdateActivity -> "Update connection $connectionId activity"
    is AppAction.Connection.AddEvent -> "Add connection event for ${event.connectionId}"
    is AppAction.Connection.ClearHistory -> "Clear connection history"
    
    is AppAction.Plugin.Load -> "Load plugin: ${pluginInfo.name}"
    is AppAction.Plugin.Unload -> "Unload plugin: $pluginId"
    is AppAction.Plugin.UpdateStatus -> "Update plugin $pluginId status to $status"
    is AppAction.Plugin.UpdateActivity -> "Update plugin $pluginId activity"
    is AppAction.Plugin.RegisterCommand -> "Register command $commandName for plugin $pluginId"
    is AppAction.Plugin.UnregisterCommand -> "Unregister command $commandName"
    is AppAction.Plugin.AddError -> "Add plugin error for ${error.pluginId}"
    is AppAction.Plugin.ClearErrors -> "Clear plugin errors${pluginId?.let { " for $it" } ?: ""}"
    
    is AppAction.UI.SetTheme -> "Set theme to $theme"
    is AppAction.UI.SetLanguage -> "Set language to $language"
    is AppAction.UI.SetActiveView -> "Navigate to $view"
    is AppAction.UI.AddNotification -> "Add ${notification.type} notification: ${notification.title}"
    is AppAction.UI.RemoveNotification -> "Remove notification: $notificationId"
    is AppAction.UI.MarkNotificationsAsRead -> "Mark ${notificationIds.size} notifications as read"
    is AppAction.UI.ClearAllNotifications -> "Clear all notifications"
    is AppAction.UI.UpdatePreferences -> "Update user preferences"
    
    is AppAction.System.Reset -> "Reset application state"
    is AppAction.System.LoadState -> "Load application state"
    is AppAction.System.UpdateMetadata -> "Update state metadata"
    is AppAction.System.Batch -> "Batch ${actions.size} actions"
}

/**
 * Extension function para obtener la prioridad de una acción
 * (útil para ordenar acciones en colas de procesamiento)
 */
fun AppAction.priority(): Int = when (this) {
    is AppAction.System -> 100 // Highest priority
    is AppAction.Server -> 80
    is AppAction.Connection -> 60
    is AppAction.Plugin -> 40
    is AppAction.UI -> 20 // Lowest priority
}

/**
 * Extension function para verificar si una acción requiere validación especial
 */
fun AppAction.requiresValidation(): Boolean = when (this) {
    is AppAction.Server.Start -> true
    is AppAction.Connection.Add -> true
    is AppAction.Plugin.Load -> true
    is AppAction.System.LoadState -> true
    else -> false
}
