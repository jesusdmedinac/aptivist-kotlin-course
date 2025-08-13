
package com.aptivist.kotlin.mcp.server

import com.aptivist.kotlin.mcp.protocol.McpMessage
import com.aptivist.kotlin.mcp.protocol.McpMessageHandler
import com.aptivist.kotlin.mcp.protocol.JsonRpcMessage
import kotlinx.coroutines.flow.Flow

/**
 * 🧑‍🏫: Este archivo demuestra INTERFACES avanzadas y COMPOSITION OVER INHERITANCE
 * 
 * En lugar de usar herencia pesada, usamos interfaces para definir contratos claros
 * y componemos funcionalidad a través de diferentes interfaces especializadas
 */

/**
 * 🧑‍🏫: INTERFACE principal que define el contrato del servidor MCP
 * 
 * Conceptos demostrados:
 * - Interface segregation: cada interface tiene una responsabilidad específica
 * - Suspend functions para operaciones asíncronas
 * - Flow para streams reactivos de datos
 * - Nullable returns para manejar casos donde no hay respuesta
 */
interface McpServer {
    
    /**
     * 🧑‍🏫: SUSPEND FUNCTION - puede pausar ejecución sin bloquear el hilo
     * Esencial para operaciones de red y I/O asíncrono
     */
    suspend fun start(port: Int = 8080)
    suspend fun stop()
    
    /**
     * 🧑‍🏫: Flow<T> - stream reactivo de datos asíncrono
     * Similar a Observable pero integrado con coroutines
     * - Flow es cold stream (no produce datos hasta que alguien los consume)
     * - Manejo automático de backpressure
     */
    fun messageFlow(): Flow<JsonRpcMessage>
    
    suspend fun sendMessage(message: JsonRpcMessage)
    
    /**
     * 🧑‍🏫: Propiedad read-only en interface
     * Las interfaces pueden declarar propiedades, pero las implementaciones
     * deben proporcionar el getter
     */
    val isRunning: Boolean
}

/**
 * 🧑‍🏫: INTERFACE para manejar conexiones específicas
 * Demuestra el principio de separación de responsabilidades
 */
interface McpConnection {
    
    suspend fun send(message: String)
    suspend fun receive(): String?
    fun close()
    
    /**
     * 🧑‍🏫: Propiedad nullable - la conexión puede estar cerrada
     */
    val isConnected: Boolean
}

/**
 * 🧑‍🏫: INTERFACE para configuración del servidor
 * Demuestra cómo usar interfaces para dependency injection y testabilidad
 */
interface McpServerConfig {
    val serverName: String
    val serverVersion: String
    val maxConnections: Int
    val timeoutMillis: Long
    
    /**
     * 🧑‍🏫: Método con implementación por defecto
     * Las implementaciones pueden override este comportamiento
     */
    fun validate(): Boolean = serverName.isNotBlank() && 
                             serverVersion.isNotBlank() && 
                             maxConnections > 0
}

/**
 * 🧑‍🏫: DATA CLASS que implementa interface
 * Combina la conveniencia de data class con el contrato de interface
 */
data class DefaultMcpServerConfig(
    override val serverName: String = "Kotlin MCP Server",
    override val serverVersion: String = "1.0.0",
    override val maxConnections: Int = 10,
    override val timeoutMillis: Long = 30_000L
) : McpServerConfig

/**
 * 🧑‍🏫: ABSTRACT CLASS que implementa parte de la funcionalidad
 * 
 * Diferencias entre abstract class y interface:
 * - Abstract class puede tener estado (properties con backing fields)
 * - Una clase solo puede extender una abstract class, pero implementar múltiples interfaces
 * - Abstract class puede tener constructores
 */
abstract class BaseMcpServer(
    protected val config: McpServerConfig,
    protected val messageHandler: McpMessageHandler
) : McpServer {
    
    /**
     * 🧑‍🏫: PROTECTED visibility - accesible por subclases pero no por clientes
     * MUTABLE property con backing field - solo abstract classes pueden tenerlas
     */
    protected var _isRunning: Boolean = false
    
    /**
     * 🧑‍🏫: Property override que delega a field privado
     * Implementa la propiedad abstracta definida en el interface
     */
    override val isRunning: Boolean get() = _isRunning
    
    /**
     * 🧑‍🏫: Template Method Pattern - método abstracto que deben implementar las subclases
     */
    protected abstract suspend fun startServer(port: Int)
    protected abstract suspend fun stopServer()
    
    /**
     * 🧑‍🏫: Implementación común que usa los métodos abstractos
     * Las subclases heredan este comportamiento y solo implementan los detalles específicos
     */
    override suspend fun start(port: Int) {
        if (!_isRunning) {
            startServer(port)
            _isRunning = true
        }
    }
    
    override suspend fun stop() {
        if (_isRunning) {
            stopServer()
            _isRunning = false
        }
    }
}
