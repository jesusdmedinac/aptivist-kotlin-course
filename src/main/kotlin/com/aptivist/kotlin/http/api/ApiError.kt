

package com.aptivist.kotlin.http.api

import kotlinx.serialization.Serializable
import io.ktor.http.*

/**
 * 🎯 PHASE 3.2 - MANEJO DE ERRORES HTTP ESTRUCTURADO
 * 
 * Este archivo demuestra conceptos avanzados de manejo de errores en APIs REST:
 * • **Sealed Classes**: Para representar diferentes tipos de errores de manera type-safe
 * • **Data Classes**: Para estructurar información de errores de manera inmutable
 * • **Extension Functions**: Para convertir errores a responses HTTP
 * • **When Expressions**: Para mapear errores a códigos HTTP apropiados
 * • **Companion Objects**: Para crear errores comunes de manera conveniente
 * • **Serialización**: Para enviar errores como JSON estructurado al cliente
 * 
 * PED: Las sealed classes son perfectas para representar errores porque:
 * - Garantizan exhaustividad en when expressions
 * - Permiten diferentes tipos de datos para cada error
 * - Son type-safe y previenen errores en tiempo de compilación
 * - Facilitan el pattern matching y la transformación de errores
 */

/**
 * PED: SEALED CLASS PARA ERRORES DE API
 * 
 * Esta sealed class representa todos los posibles errores que puede devolver nuestra API.
 * Al ser sealed, el compilador puede verificar que manejemos todos los casos posibles.
 * 
 * Cada subclase representa un tipo específico de error con sus propios datos asociados.
 */
sealed class ApiError : Exception() {
    
    /**
     * PED: PROPIEDADES ABSTRACTAS
     * 
     * Estas propiedades deben ser implementadas por todas las subclases,
     * garantizando que cada error tenga la información necesaria para
     * generar una response HTTP apropiada.
     */
    abstract val message: String
    abstract val code: String
    abstract val httpStatus: HttpStatusCode
    
    /**
     * PED: COMPUTED PROPERTY PARA DETALLES ADICIONALES
     * 
     * Permite que cada subclase proporcione información adicional específica
     * del tipo de error, que será incluida en la response JSON.
     */
    open val details: Map<String, Any>? = null
    
    /**
     * PED: ERRORES DE VALIDACIÓN
     * 
     * Representa errores cuando los datos enviados por el cliente no son válidos.
     * Incluye información específica sobre qué campos son inválidos y por qué.
     */
    @Serializable
    data class ValidationError(
        override val message: String,
        val field: String,
        val rejectedValue: String? = null,
        val constraint: String? = null
    ) : ApiError() {
        override val code: String = "VALIDATION_ERROR"
        override val httpStatus: HttpStatusCode = HttpStatusCode.BadRequest
        
        override val details: Map<String, Any>
            get() = buildMap {
                put("field", field)
                rejectedValue?.let { put("rejectedValue", it) }
                constraint?.let { put("constraint", it) }
            }
    }
    
    /**
     * PED: ERRORES DE RECURSO NO ENCONTRADO
     * 
     * Representa errores cuando un recurso solicitado no existe.
     * Incluye información sobre el tipo de recurso y el identificador usado.
     */
    @Serializable
    data class NotFoundError(
        val resourceType: String,
        val resourceId: String,
        override val message: String = "Resource not found"
    ) : ApiError() {
        override val code: String = "RESOURCE_NOT_FOUND"
        override val httpStatus: HttpStatusCode = HttpStatusCode.NotFound
        
        override val details: Map<String, Any>
            get() = mapOf(
                "resourceType" to resourceType,
                "resourceId" to resourceId
            )
    }
    
    /**
     * PED: ERRORES DE CONFLICTO
     * 
     * Representa errores cuando una operación no puede completarse debido
     * a un conflicto con el estado actual del recurso.
     */
    @Serializable
    data class ConflictError(
        override val message: String,
        val conflictingResource: String? = null,
        val currentState: String? = null
    ) : ApiError() {
        override val code: String = "CONFLICT"
        override val httpStatus: HttpStatusCode = HttpStatusCode.Conflict
        
        override val details: Map<String, Any>?
            get() = if (conflictingResource != null || currentState != null) {
                buildMap {
                    conflictingResource?.let { put("conflictingResource", it) }
                    currentState?.let { put("currentState", it) }
                }
            } else null
    }
    
    /**
     * PED: ERRORES DE AUTORIZACIÓN
     * 
     * Representa errores cuando el cliente no tiene permisos para realizar
     * la operación solicitada.
     */
    @Serializable
    data class UnauthorizedError(
        override val message: String = "Authentication required",
        val realm: String? = null
    ) : ApiError() {
        override val code: String = "UNAUTHORIZED"
        override val httpStatus: HttpStatusCode = HttpStatusCode.Unauthorized
        
        override val details: Map<String, Any>?
            get() = realm?.let { mapOf("realm" to it) }
    }
    
    /**
     * PED: ERRORES DE PERMISOS INSUFICIENTES
     * 
     * Representa errores cuando el cliente está autenticado pero no tiene
     * permisos suficientes para realizar la operación.
     */
    @Serializable
    data class ForbiddenError(
        override val message: String = "Insufficient permissions",
        val requiredPermission: String? = null,
        val userPermissions: List<String> = emptyList()
    ) : ApiError() {
        override val code: String = "FORBIDDEN"
        override val httpStatus: HttpStatusCode = HttpStatusCode.Forbidden
        
        override val details: Map<String, Any>?
            get() = if (requiredPermission != null || userPermissions.isNotEmpty()) {
                buildMap {
                    requiredPermission?.let { put("requiredPermission", it) }
                    if (userPermissions.isNotEmpty()) {
                        put("userPermissions", userPermissions)
                    }
                }
            } else null
    }
    
    /**
     * PED: ERRORES DE LÍMITE DE TASA
     * 
     * Representa errores cuando el cliente ha excedido los límites de tasa
     * de la API (rate limiting).
     */
    @Serializable
    data class RateLimitError(
        override val message: String = "Rate limit exceeded",
        val limit: Int,
        val windowSeconds: Int,
        val retryAfterSeconds: Int
    ) : ApiError() {
        override val code: String = "RATE_LIMIT_EXCEEDED"
        override val httpStatus: HttpStatusCode = HttpStatusCode.TooManyRequests
        
        override val details: Map<String, Any>
            get() = mapOf(
                "limit" to limit,
                "windowSeconds" to windowSeconds,
                "retryAfterSeconds" to retryAfterSeconds
            )
    }
    
    /**
     * PED: ERRORES INTERNOS DEL SERVIDOR
     * 
     * Representa errores inesperados del servidor que no deben exponer
     * detalles internos al cliente.
     */
    @Serializable
    data class InternalServerError(
        override val message: String = "Internal server error",
        val errorId: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : ApiError() {
        override val code: String = "INTERNAL_SERVER_ERROR"
        override val httpStatus: HttpStatusCode = HttpStatusCode.InternalServerError
        
        override val details: Map<String, Any>?
            get() = errorId?.let { 
                mapOf(
                    "errorId" to it,
                    "timestamp" to timestamp
                )
            }
    }
    
    /**
     * PED: ERRORES DE SERVICIO NO DISPONIBLE
     * 
     * Representa errores cuando el servicio está temporalmente no disponible,
     * por ejemplo durante mantenimiento.
     */
    @Serializable
    data class ServiceUnavailableError(
        override val message: String = "Service temporarily unavailable",
        val retryAfterSeconds: Int? = null,
        val maintenanceWindow: String? = null
    ) : ApiError() {
        override val code: String = "SERVICE_UNAVAILABLE"
        override val httpStatus: HttpStatusCode = HttpStatusCode.ServiceUnavailable
        
        override val details: Map<String, Any>?
            get() = buildMap {
                retryAfterSeconds?.let { put("retryAfterSeconds", it) }
                maintenanceWindow?.let { put("maintenanceWindow", it) }
            }.takeIf { it.isNotEmpty() }
    }
    
    /**
     * PED: COMPANION OBJECT CON FACTORY FUNCTIONS
     * 
     * Proporciona métodos convenientes para crear errores comunes
     * sin tener que especificar todos los parámetros.
     */
    companion object {
        
        /**
         * PED: FACTORY FUNCTION PARA ERRORES DE VALIDACIÓN SIMPLES
         */
        fun validationError(field: String, message: String): ValidationError =
            ValidationError(
                message = "Validation failed for field '$field': $message",
                field = field
            )
        
        /**
         * PED: FACTORY FUNCTION PARA RECURSOS NO ENCONTRADOS
         */
        fun notFound(resourceType: String, id: String): NotFoundError =
            NotFoundError(
                resourceType = resourceType,
                resourceId = id,
                message = "$resourceType with id '$id' not found"
            )
        
        /**
         * PED: FACTORY FUNCTION PARA ERRORES DE CONFLICTO SIMPLES
         */
        fun conflict(message: String): ConflictError =
            ConflictError(message = message)
        
        /**
         * PED: FACTORY FUNCTION PARA ERRORES INTERNOS CON ID DE TRACKING
         */
        fun internalError(errorId: String? = null): InternalServerError =
            InternalServerError(errorId = errorId)
        
        /**
         * PED: FACTORY FUNCTION PARA ERRORES DE RATE LIMITING
         */
        fun rateLimitExceeded(
            limit: Int = 100,
            windowSeconds: Int = 3600,
            retryAfterSeconds: Int = 60
        ): RateLimitError = RateLimitError(
            limit = limit,
            windowSeconds = windowSeconds,
            retryAfterSeconds = retryAfterSeconds
        )
    }
}

/**
 * PED: DATA CLASS PARA RESPONSE DE ERROR ESTRUCTURADA
 * 
 * Esta clase representa la estructura JSON que se envía al cliente
 * cuando ocurre un error. Proporciona información consistente y útil
 * para que los clientes puedan manejar errores apropiadamente.
 */
@Serializable
data class ErrorResponse(
    val error: ErrorInfo,
    val timestamp: Long = System.currentTimeMillis(),
    val path: String? = null,
    val requestId: String? = null
) {
    @Serializable
    data class ErrorInfo(
        val code: String,
        val message: String,
        val details: Map<String, Any>? = null
    )
}

/**
 * PED: EXTENSION FUNCTIONS PARA CONVERSIÓN DE ERRORES
 * 
 * Estas extension functions proporcionan una forma conveniente de
 * convertir ApiError instances a ErrorResponse objects que pueden
 * ser serializados y enviados al cliente.
 */

/**
 * Extension function para convertir ApiError a ErrorResponse
 */
fun ApiError.toErrorResponse(path: String? = null, requestId: String? = null): ErrorResponse =
    ErrorResponse(
        error = ErrorResponse.ErrorInfo(
            code = this.code,
            message = this.message,
            details = this.details
        ),
        path = path,
        requestId = requestId
    )

/**
 * PED: EXTENSION FUNCTION PARA CREAR ERRORES DESDE EXCEPTIONS
 * 
 * Permite convertir exceptions estándar de Kotlin/Java a nuestros
 * ApiError types de manera conveniente.
 */
fun Throwable.toApiError(): ApiError = when (this) {
    is ApiError -> this
    is IllegalArgumentException -> ApiError.ValidationError(
        message = this.message ?: "Invalid argument",
        field = "unknown"
    )
    is IllegalStateException -> ApiError.ConflictError(
        message = this.message ?: "Invalid state"
    )
    is NoSuchElementException -> ApiError.NotFoundError(
        resourceType = "resource",
        resourceId = "unknown",
        message = this.message ?: "Resource not found"
    )
    else -> ApiError.InternalServerError(
        message = "An unexpected error occurred",
        errorId = System.currentTimeMillis().toString()
    )
}

/**
 * PED: UTILITY FUNCTIONS PARA VALIDACIÓN
 * 
 * Estas funciones proporcionan una forma conveniente de realizar
 * validaciones comunes y lanzar errores apropiados.
 */

/**
 * Valida que un string no esté vacío
 */
fun String?.requireNotBlank(fieldName: String): String {
    if (this.isNullOrBlank()) {
        throw ApiError.validationError(fieldName, "must not be blank")
    }
    return this
}

/**
 * Valida que un número esté en un rango específico
 */
fun Int.requireInRange(min: Int, max: Int, fieldName: String): Int {
    if (this < min || this > max) {
        throw ApiError.validationError(
            fieldName, 
            "must be between $min and $max, but was $this"
        )
    }
    return this
}

/**
 * Valida que una lista no esté vacía
 */
fun <T> List<T>?.requireNotEmpty(fieldName: String): List<T> {
    if (this.isNullOrEmpty()) {
        throw ApiError.validationError(fieldName, "must not be empty")
    }
    return this
}

/**
 * PED: INLINE FUNCTIONS PARA VALIDACIÓN CON LAMBDAS
 * 
 * Estas inline functions permiten validaciones más complejas
 * usando lambdas, manteniendo el performance gracias a la inlining.
 */

/**
 * Valida una condición personalizada
 */
inline fun require(condition: Boolean, fieldName: String, lazyMessage: () -> String) {
    if (!condition) {
        throw ApiError.validationError(fieldName, lazyMessage())
    }
}

/**
 * Valida y transforma un valor
 */
inline fun <T, R> T.validateAndTransform(
    fieldName: String,
    validator: (T) -> Boolean,
    transformer: (T) -> R,
    lazyMessage: () -> String
): R {
    if (!validator(this)) {
        throw ApiError.validationError(fieldName, lazyMessage())
    }
    return transformer(this)
}

