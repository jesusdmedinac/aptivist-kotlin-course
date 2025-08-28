

package com.aptivist.kotlin.http.api

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import com.aptivist.kotlin.state.*
import java.util.*

/**
 * 🎯 PHASE 3.2 - RUTAS DE API REST CON INTEGRACIÓN DE ESTADO
 * 
 * Este archivo demuestra conceptos avanzados de APIs REST en Kotlin:
 * • **Routing DSL**: Definición declarativa de endpoints usando DSL de Ktor
 * • **Suspend Functions**: Handlers asíncronos que no bloquean threads
 * • **State Management**: Integración con el sistema de estado inmutable de Phase 3.1
 * • **Error Handling**: Manejo estructurado de errores con tipos específicos
 * • **Content Negotiation**: Serialización/deserialización automática de JSON
 * • **Thread Safety**: Uso de Mutex para operaciones thread-safe en estado compartido
 * • **Extension Functions**: Para organizar y reutilizar lógica de routing
 * 
 * PED: Las APIs REST siguen principios específicos:
 * - Recursos identificados por URLs
 * - Operaciones CRUD mapeadas a métodos HTTP (GET, POST, PUT, DELETE)
 * - Stateless: cada request contiene toda la información necesaria
 * - Representaciones uniformes usando JSON
 * - Códigos de estado HTTP semánticamente correctos
 */

/**
 * PED: CLASE PARA MANEJAR ESTADO COMPARTIDO THREAD-SAFE
 * 
 * Esta clase encapsula el estado de la aplicación y proporciona
 * operaciones thread-safe usando Mutex para sincronización.
 */
class StateManager {
    companion object {
        private val logger = LoggerFactory.getLogger(StateManager::class.java)
    }
    
    // PED: Mutex para sincronización thread-safe
    private val mutex = Mutex()
    
    // PED: Estado mutable protegido por mutex
    private var currentState: AppState = StateFactory.createDevelopmentState()
    
    /**
     * PED: SUSPEND FUNCTION PARA LEER ESTADO
     * 
     * Proporciona acceso thread-safe de solo lectura al estado actual.
     */
    suspend fun getState(): AppState = mutex.withLock {
        currentState
    }
    
    /**
     * PED: SUSPEND FUNCTION PARA ACTUALIZAR ESTADO
     * 
     * Permite actualizaciones thread-safe del estado usando una función de transformación.
     */
    suspend fun updateState(transform: (AppState) -> AppState): AppState = mutex.withLock {
        val newState = transform(currentState).withUpdatedTimestamp()
        currentState = newState
        logger.debug("State updated: ${newState.metadata.lastUpdated}")
        newState
    }
    
    /**
     * PED: FUNCIÓN PARA AGREGAR CONEXIÓN
     */
    suspend fun addConnection(connection: Connection): AppState = updateState { state ->
        state.addConnection(connection)
    }
    
    /**
     * PED: FUNCIÓN PARA REMOVER CONEXIÓN
     */
    suspend fun removeConnection(connectionId: String): AppState = updateState { state ->
        state.removeConnection(connectionId)
    }
    
    /**
     * PED: FUNCIÓN PARA ACTUALIZAR ESTADÍSTICAS
     */
    suspend fun updateServerStats(
        requestsIncrement: Long = 1L,
        successIncrement: Long = 0L,
        failIncrement: Long = 0L,
        responseTime: Double? = null
    ): AppState = updateState { state ->
        state.updateServerStats(requestsIncrement, successIncrement, failIncrement, responseTime)
    }
}

/**
 * PED: EXTENSION FUNCTION PARA CONFIGURAR RUTAS DE API
 * 
 * Esta extension function organiza todas las rutas de la API REST
 * de manera modular y reutilizable.
 */
fun Application.configureApiRoutes() {
    // PED: Instancia compartida del StateManager
    val stateManager = StateManager()
    
    routing {
        // PED: Grupo de rutas bajo /api/v1
        route("/api/v1") {
            // PED: Configurar rutas de estado
            configureStateRoutes(stateManager)
            
            // PED: Configurar rutas de conexiones
            configureConnectionRoutes(stateManager)
            
            // PED: Configurar rutas de plugins
            configurePluginRoutes(stateManager)
            
            // PED: Configurar rutas de servidor
            configureServerRoutes(stateManager)
            
            // PED: Configurar rutas de salud
            configureHealthRoutes(stateManager)
        }
    }
}

/**
 * PED: EXTENSION FUNCTION PARA RUTAS DE ESTADO
 * 
 * Agrupa todas las operaciones relacionadas con el estado de la aplicación.
 */
fun Route.configureStateRoutes(stateManager: StateManager) {
    route("/state") {
        
        /**
         * PED: GET /api/v1/state - Obtener estado completo
         * 
         * Endpoint que devuelve el estado completo de la aplicación
         * convertido a DTO para exposición externa.
         */
        get {
            try {
                val startTime = System.currentTimeMillis()
                val state = stateManager.getState()
                val responseTime = System.currentTimeMillis() - startTime
                
                // Actualizar estadísticas
                stateManager.updateServerStats(
                    successIncrement = 1L,
                    responseTime = responseTime.toDouble()
                )
                
                call.respond(HttpStatusCode.OK, state.toDto())
                
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
        
        /**
         * PED: GET /api/v1/state/summary - Obtener resumen de estado
         * 
         * Endpoint optimizado que devuelve solo información esencial
         * para dashboards o monitoreo ligero.
         */
        get("/summary") {
            try {
                val state = stateManager.getState()
                
                val summary = mapOf(
                    "isHealthy" to state.isHealthy,
                    "serverRunning" to state.serverState.isRunning,
                    "activeConnections" to state.connectionState.activeConnections.size,
                    "activePlugins" to state.pluginState.activePlugins.size,
                    "uptime" to state.serverState.uptime,
                    "lastUpdated" to state.metadata.lastUpdated
                )
                
                stateManager.updateServerStats(successIncrement = 1L)
                call.respond(HttpStatusCode.OK, summary)
                
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
    }
}

/**
 * PED: EXTENSION FUNCTION PARA RUTAS DE CONEXIONES
 * 
 * Maneja operaciones CRUD para conexiones del servidor.
 */
fun Route.configureConnectionRoutes(stateManager: StateManager) {
    route("/connections") {
        
        /**
         * PED: GET /api/v1/connections - Listar conexiones con paginación
         */
        get {
            try {
                val paginationRequest = call.request.queryParameters.toPaginationRequest()
                paginationRequest.validate()
                
                val state = stateManager.getState()
                val connections = state.connectionState.activeConnections.values.toList()
                val connectionsDto = connections.map { connection ->
                    mapOf(
                        "id" to connection.id,
                        "type" to connection.type.name,
                        "status" to connection.status.name,
                        "establishedAt" to connection.establishedAt,
                        "lastActivity" to connection.lastActivity,
                        "messageCount" to connection.messageCount,
                        "bytesTransferred" to connection.bytesTransferred,
                        "duration" to connection.duration,
                        "isActive" to connection.isActive
                    )
                }
                
                val pagedResponse = connectionsDto.toPagedResponse(paginationRequest)
                
                stateManager.updateServerStats(successIncrement = 1L)
                call.respond(HttpStatusCode.OK, pagedResponse)
                
            } catch (e: ApiError) {
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(e.httpStatus, e.toErrorResponse(call.request.path()))
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
        
        /**
         * PED: GET /api/v1/connections/{id} - Obtener conexión específica
         */
        get("/{id}") {
            try {
                val connectionId = call.parameters["id"]
                    ?: throw ApiError.validationError("id", "Connection ID is required")
                
                val state = stateManager.getState()
                val connection = state.connectionState.getConnectionById(connectionId)
                    ?: throw ApiError.notFound("Connection", connectionId)
                
                val connectionDto = mapOf(
                    "id" to connection.id,
                    "type" to connection.type.name,
                    "status" to connection.status.name,
                    "clientInfo" to mapOf(
                        "userAgent" to connection.clientInfo.userAgent,
                        "version" to connection.clientInfo.version,
                        "capabilities" to connection.clientInfo.capabilities.toList()
                    ),
                    "establishedAt" to connection.establishedAt,
                    "lastActivity" to connection.lastActivity,
                    "messageCount" to connection.messageCount,
                    "bytesTransferred" to connection.bytesTransferred,
                    "duration" to connection.duration,
                    "isActive" to connection.isActive
                )
                
                stateManager.updateServerStats(successIncrement = 1L)
                call.respond(HttpStatusCode.OK, connectionDto)
                
            } catch (e: ApiError) {
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(e.httpStatus, e.toErrorResponse(call.request.path()))
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
        
        /**
         * PED: POST /api/v1/connections - Crear nueva conexión (simulada)
         */
        post {
            try {
                val request = call.receive<CreateConnectionRequest>()
                request.validate()
                
                // PED: Crear nueva conexión simulada
                val newConnection = Connection(
                    id = UUID.randomUUID().toString(),
                    type = ConnectionType.valueOf(request.type),
                    clientInfo = ClientInfo(
                        userAgent = request.clientInfo.userAgent,
                        ipAddress = "127.0.0.1", // Simulado
                        version = request.clientInfo.version,
                        capabilities = request.clientInfo.capabilities.toSet()
                    ),
                    establishedAt = System.currentTimeMillis(),
                    lastActivity = System.currentTimeMillis()
                )
                
                val updatedState = stateManager.addConnection(newConnection)
                
                stateManager.updateServerStats(successIncrement = 1L)
                call.respond(HttpStatusCode.Created, mapOf(
                    "id" to newConnection.id,
                    "message" to "Connection created successfully",
                    "connection" to mapOf(
                        "id" to newConnection.id,
                        "type" to newConnection.type.name,
                        "establishedAt" to newConnection.establishedAt
                    )
                ))
                
            } catch (e: ApiError) {
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(e.httpStatus, e.toErrorResponse(call.request.path()))
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
        
        /**
         * PED: DELETE /api/v1/connections/{id} - Cerrar conexión
         */
        delete("/{id}") {
            try {
                val connectionId = call.parameters["id"]
                    ?: throw ApiError.validationError("id", "Connection ID is required")
                
                val state = stateManager.getState()
                val connection = state.connectionState.getConnectionById(connectionId)
                    ?: throw ApiError.notFound("Connection", connectionId)
                
                stateManager.removeConnection(connectionId)
                
                stateManager.updateServerStats(successIncrement = 1L)
                call.respond(HttpStatusCode.OK, mapOf(
                    "message" to "Connection closed successfully",
                    "connectionId" to connectionId
                ))
                
            } catch (e: ApiError) {
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(e.httpStatus, e.toErrorResponse(call.request.path()))
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
    }
}

/**
 * PED: EXTENSION FUNCTION PARA RUTAS DE PLUGINS
 */
fun Route.configurePluginRoutes(stateManager: StateManager) {
    route("/plugins") {
        
        /**
         * PED: GET /api/v1/plugins - Listar plugins
         */
        get {
            try {
                val state = stateManager.getState()
                val pluginsDto = state.pluginState.toSummaryDto()
                
                stateManager.updateServerStats(successIncrement = 1L)
                call.respond(HttpStatusCode.OK, pluginsDto)
                
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
        
        /**
         * PED: POST /api/v1/plugins - Cargar plugin (simulado)
         */
        post {
            try {
                val request = call.receive<LoadPluginRequest>()
                request.validate()
                
                // TODO: Implementar carga real de plugins en futuras fases
                stateManager.updateServerStats(successIncrement = 1L)
                call.respond(HttpStatusCode.Accepted, mapOf(
                    "message" to "Plugin load request accepted",
                    "pluginId" to request.id,
                    "status" to "loading"
                ))
                
            } catch (e: ApiError) {
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(e.httpStatus, e.toErrorResponse(call.request.path()))
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
    }
}

/**
 * PED: EXTENSION FUNCTION PARA RUTAS DE SERVIDOR
 */
fun Route.configureServerRoutes(stateManager: StateManager) {
    route("/server") {
        
        /**
         * PED: GET /api/v1/server - Información del servidor
         */
        get {
            try {
                val state = stateManager.getState()
                val serverDto = state.serverState.toDto()
                
                stateManager.updateServerStats(successIncrement = 1L)
                call.respond(HttpStatusCode.OK, serverDto)
                
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
        
        /**
         * PED: PUT /api/v1/server/config - Actualizar configuración
         */
        put("/config") {
            try {
                val request = call.receive<UpdateServerConfigRequest>()
                request.validate()
                
                val updatedState = stateManager.updateState { state ->
                    state.copy(
                        serverState = state.serverState.copy(
                            maxConnections = request.maxConnections ?: state.serverState.maxConnections,
                            capabilities = request.capabilities?.toSet() ?: state.serverState.capabilities
                        )
                    )
                }
                
                stateManager.updateServerStats(successIncrement = 1L)
                call.respond(HttpStatusCode.OK, mapOf(
                    "message" to "Server configuration updated successfully",
                    "config" to updatedState.serverState.toDto()
                ))
                
            } catch (e: ApiError) {
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(e.httpStatus, e.toErrorResponse(call.request.path()))
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
    }
}

/**
 * PED: EXTENSION FUNCTION PARA RUTAS DE SALUD
 */
fun Route.configureHealthRoutes(stateManager: StateManager) {
    route("/health") {
        
        /**
         * PED: GET /api/v1/health - Health check completo
         */
        get {
            try {
                val state = stateManager.getState()
                val healthDto = state.toHealthStatusDto()
                
                val statusCode = when (healthDto.status) {
                    "healthy" -> HttpStatusCode.OK
                    "degraded" -> HttpStatusCode.OK // Aún funcional
                    "unhealthy" -> HttpStatusCode.ServiceUnavailable
                    else -> HttpStatusCode.OK
                }
                
                stateManager.updateServerStats(successIncrement = 1L)
                call.respond(statusCode, healthDto)
                
            } catch (e: Exception) {
                val error = e.toApiError()
                stateManager.updateServerStats(failIncrement = 1L)
                call.respond(error.httpStatus, error.toErrorResponse(call.request.path()))
            }
        }
        
        /**
         * PED: GET /api/v1/health/live - Liveness probe
         */
        get("/live") {
            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "alive",
                "timestamp" to System.currentTimeMillis()
            ))
        }
        
        /**
         * PED: GET /api/v1/health/ready - Readiness probe
         */
        get("/ready") {
            try {
                val state = stateManager.getState()
                val isReady = state.serverState.isRunning
                
                val statusCode = if (isReady) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
                
                call.respond(statusCode, mapOf(
                    "status" to if (isReady) "ready" else "not_ready",
                    "timestamp" to System.currentTimeMillis(),
                    "details" to mapOf(
                        "serverRunning" to state.serverState.isRunning
                    )
                ))
                
            } catch (e: Exception) {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf(
                    "status" to "not_ready",
                    "error" to e.message,
                    "timestamp" to System.currentTimeMillis()
                ))
            }
        }
    }
}

/**
 * PED: EXTENSION FUNCTIONS PARA UTILIDADES DE REQUEST
 */

/**
 * Convierte query parameters a PaginationRequest
 */
fun Parameters.toPaginationRequest(): PaginationRequest = PaginationRequest(
    page = this["page"]?.toIntOrNull() ?: 0,
    size = this["size"]?.toIntOrNull() ?: 20,
    sort = this["sort"],
    direction = this["direction"] ?: "asc"
)

