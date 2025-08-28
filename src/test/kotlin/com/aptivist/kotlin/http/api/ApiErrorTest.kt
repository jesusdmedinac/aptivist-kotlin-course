

package com.aptivist.kotlin.http.api

import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 🎯 PHASE 3.2 - TESTS PARA MANEJO DE ERRORES DE API
 * 
 * Esta clase de test demuestra conceptos avanzados de testing en Kotlin:
 * • **Unit Testing**: Tests unitarios para sealed classes y error handling
 * • **Serialization Testing**: Verificación de serialización JSON de errores
 * • **Exception Testing**: Testing de validation functions y error mapping
 * • **Assertion Libraries**: Uso de kotlin.test para assertions expresivas
 * • **Test Organization**: Estructura clara de tests por funcionalidad
 * 
 * PED: Los tests son cruciales para APIs REST porque:
 * - Verifican que los errores se manejen correctamente
 * - Aseguran que la serialización funcione como se espera
 * - Documentan el comportamiento esperado de la API
 * - Previenen regresiones en el manejo de errores
 * - Facilitan refactoring seguro
 */
class ApiErrorTest {
    
    /**
     * PED: JSON CONFIGURATION PARA TESTS
     * 
     * Usamos la misma configuración JSON que la aplicación
     * para asegurar consistencia en los tests.
     */
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    /**
     * PED: TEST GROUP - CREACIÓN DE ERRORES
     * 
     * Estos tests verifican que los diferentes tipos de errores
     * se creen correctamente con los datos apropiados.
     */
    
    @Test
    fun `ValidationError should have correct properties`() {
        // PED: Arrange - Preparar datos de test
        val field = "email"
        val message = "Invalid email format"
        val rejectedValue = "invalid-email"
        val constraint = "must be valid email"
        
        // PED: Act - Ejecutar la operación a testear
        val error = ApiError.ValidationError(
            message = message,
            field = field,
            rejectedValue = rejectedValue,
            constraint = constraint
        )
        
        // PED: Assert - Verificar resultados
        assertEquals(message, error.message)
        assertEquals(field, error.field)
        assertEquals(rejectedValue, error.rejectedValue)
        assertEquals(constraint, error.constraint)
        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals(HttpStatusCode.BadRequest, error.httpStatus)
        
        // PED: Verificar details map
        val details = error.details
        assertNotNull(details)
        assertEquals(field, details["field"])
        assertEquals(rejectedValue, details["rejectedValue"])
        assertEquals(constraint, details["constraint"])
    }
    
    @Test
    fun `NotFoundError should have correct properties`() {
        val resourceType = "User"
        val resourceId = "123"
        val customMessage = "User not found"
        
        val error = ApiError.NotFoundError(
            resourceType = resourceType,
            resourceId = resourceId,
            message = customMessage
        )
        
        assertEquals(customMessage, error.message)
        assertEquals(resourceType, error.resourceType)
        assertEquals(resourceId, error.resourceId)
        assertEquals("RESOURCE_NOT_FOUND", error.code)
        assertEquals(HttpStatusCode.NotFound, error.httpStatus)
        
        val details = error.details
        assertNotNull(details)
        assertEquals(resourceType, details["resourceType"])
        assertEquals(resourceId, details["resourceId"])
    }
    
    @Test
    fun `ConflictError should have correct properties`() {
        val message = "Resource already exists"
        val conflictingResource = "user-123"
        val currentState = "active"
        
        val error = ApiError.ConflictError(
            message = message,
            conflictingResource = conflictingResource,
            currentState = currentState
        )
        
        assertEquals(message, error.message)
        assertEquals(conflictingResource, error.conflictingResource)
        assertEquals(currentState, error.currentState)
        assertEquals("CONFLICT", error.code)
        assertEquals(HttpStatusCode.Conflict, error.httpStatus)
        
        val details = error.details
        assertNotNull(details)
        assertEquals(conflictingResource, details["conflictingResource"])
        assertEquals(currentState, details["currentState"])
    }
    
    @Test
    fun `RateLimitError should have correct properties`() {
        val limit = 100
        val windowSeconds = 3600
        val retryAfterSeconds = 60
        
        val error = ApiError.RateLimitError(
            limit = limit,
            windowSeconds = windowSeconds,
            retryAfterSeconds = retryAfterSeconds
        )
        
        assertEquals("Rate limit exceeded", error.message)
        assertEquals(limit, error.limit)
        assertEquals(windowSeconds, error.windowSeconds)
        assertEquals(retryAfterSeconds, error.retryAfterSeconds)
        assertEquals("RATE_LIMIT_EXCEEDED", error.code)
        assertEquals(HttpStatusCode.TooManyRequests, error.httpStatus)
        
        val details = error.details
        assertNotNull(details)
        assertEquals(limit, details["limit"])
        assertEquals(windowSeconds, details["windowSeconds"])
        assertEquals(retryAfterSeconds, details["retryAfterSeconds"])
    }
    
    /**
     * PED: TEST GROUP - FACTORY FUNCTIONS
     * 
     * Estos tests verifican que las factory functions del companion object
     * creen errores con los valores por defecto correctos.
     */
    
    @Test
    fun `validationError factory should create correct error`() {
        val field = "username"
        val message = "is required"
        
        val error = ApiError.validationError(field, message)
        
        assertEquals("Validation failed for field '$field': $message", error.message)
        assertEquals(field, error.field)
        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals(HttpStatusCode.BadRequest, error.httpStatus)
    }
    
    @Test
    fun `notFound factory should create correct error`() {
        val resourceType = "Product"
        val id = "456"
        
        val error = ApiError.notFound(resourceType, id)
        
        assertEquals("$resourceType with id '$id' not found", error.message)
        assertEquals(resourceType, error.resourceType)
        assertEquals(id, error.resourceId)
        assertEquals("RESOURCE_NOT_FOUND", error.code)
        assertEquals(HttpStatusCode.NotFound, error.httpStatus)
    }
    
    @Test
    fun `internalError factory should create correct error`() {
        val errorId = "error-123"
        
        val error = ApiError.internalError(errorId)
        
        assertEquals("Internal server error", error.message)
        assertEquals(errorId, error.errorId)
        assertEquals("INTERNAL_SERVER_ERROR", error.code)
        assertEquals(HttpStatusCode.InternalServerError, error.httpStatus)
    }
    
    /**
     * PED: TEST GROUP - SERIALIZACIÓN
     * 
     * Estos tests verifican que los errores se serialicen correctamente
     * a JSON para enviar al cliente.
     */
    
    @Test
    fun `ErrorResponse should serialize correctly`() {
        val apiError = ApiError.ValidationError(
            message = "Invalid input",
            field = "name"
        )
        
        val errorResponse = apiError.toErrorResponse(
            path = "/api/v1/test",
            requestId = "req-123"
        )
        
        // PED: Serializar a JSON
        val jsonString = json.encodeToString(ErrorResponse.serializer(), errorResponse)
        
        // PED: Verificar que contiene los campos esperados
        assertTrue(jsonString.contains("\"code\":\"VALIDATION_ERROR\""))
        assertTrue(jsonString.contains("\"message\":\"Invalid input\""))
        assertTrue(jsonString.contains("\"path\":\"/api/v1/test\""))
        assertTrue(jsonString.contains("\"requestId\":\"req-123\""))
        assertTrue(jsonString.contains("\"field\":\"name\""))
        
        // PED: Deserializar de vuelta para verificar integridad
        val deserializedError = json.decodeFromString(ErrorResponse.serializer(), jsonString)
        assertEquals(errorResponse.error.code, deserializedError.error.code)
        assertEquals(errorResponse.error.message, deserializedError.error.message)
        assertEquals(errorResponse.path, deserializedError.path)
        assertEquals(errorResponse.requestId, deserializedError.requestId)
    }
    
    /**
     * PED: TEST GROUP - EXTENSION FUNCTIONS
     * 
     * Estos tests verifican que las extension functions para conversión
     * de errores funcionen correctamente.
     */
    
    @Test
    fun `toApiError should convert standard exceptions correctly`() {
        // PED: Test IllegalArgumentException
        val illegalArgException = IllegalArgumentException("Invalid argument")
        val apiError1 = illegalArgException.toApiError()
        
        assertTrue(apiError1 is ApiError.ValidationError)
        assertEquals("Invalid argument", apiError1.message)
        
        // PED: Test IllegalStateException
        val illegalStateException = IllegalStateException("Invalid state")
        val apiError2 = illegalStateException.toApiError()
        
        assertTrue(apiError2 is ApiError.ConflictError)
        assertEquals("Invalid state", apiError2.message)
        
        // PED: Test NoSuchElementException
        val noSuchElementException = NoSuchElementException("Element not found")
        val apiError3 = noSuchElementException.toApiError()
        
        assertTrue(apiError3 is ApiError.NotFoundError)
        assertEquals("Element not found", apiError3.message)
        
        // PED: Test generic Exception
        val genericException = RuntimeException("Unexpected error")
        val apiError4 = genericException.toApiError()
        
        assertTrue(apiError4 is ApiError.InternalServerError)
        assertEquals("An unexpected error occurred", apiError4.message)
        assertNotNull(apiError4.errorId)
    }
    
    /**
     * PED: TEST GROUP - VALIDATION FUNCTIONS
     * 
     * Estos tests verifican que las utility functions de validación
     * lancen los errores apropiados cuando los datos son inválidos.
     */
    
    @Test
    fun `requireNotBlank should throw ValidationError for blank string`() {
        val exception = assertThrows<ApiError.ValidationError> {
            "".requireNotBlank("username")
        }
        
        assertEquals("username", exception.field)
        assertTrue(exception.message.contains("must not be blank"))
    }
    
    @Test
    fun `requireNotBlank should return string for valid input`() {
        val result = "valid-username".requireNotBlank("username")
        assertEquals("valid-username", result)
    }
    
    @Test
    fun `requireInRange should throw ValidationError for out of range value`() {
        val exception = assertThrows<ApiError.ValidationError> {
            150.requireInRange(1, 100, "age")
        }
        
        assertEquals("age", exception.field)
        assertTrue(exception.message.contains("must be between 1 and 100"))
        assertTrue(exception.message.contains("but was 150"))
    }
    
    @Test
    fun `requireInRange should return value for valid input`() {
        val result = 50.requireInRange(1, 100, "age")
        assertEquals(50, result)
    }
    
    @Test
    fun `requireNotEmpty should throw ValidationError for empty list`() {
        val exception = assertThrows<ApiError.ValidationError> {
            emptyList<String>().requireNotEmpty("tags")
        }
        
        assertEquals("tags", exception.field)
        assertTrue(exception.message.contains("must not be empty"))
    }
    
    @Test
    fun `requireNotEmpty should return list for valid input`() {
        val list = listOf("tag1", "tag2")
        val result = list.requireNotEmpty("tags")
        assertEquals(list, result)
    }
    
    /**
     * PED: TEST GROUP - INLINE VALIDATION FUNCTIONS
     * 
     * Estos tests verifican las inline functions de validación
     * que usan lambdas para condiciones personalizadas.
     */
    
    @Test
    fun `require should throw ValidationError for false condition`() {
        val exception = assertThrows<ApiError.ValidationError> {
            require(condition = false, fieldName = "email") {
                "must be valid email format"
            }
        }
        
        assertEquals("email", exception.field)
        assertEquals("must be valid email format", exception.message)
    }
    
    @Test
    fun `require should not throw for true condition`() {
        // PED: No debería lanzar excepción
        require(condition = true, fieldName = "email") {
            "must be valid email format"
        }
    }
    
    @Test
    fun `validateAndTransform should work correctly`() {
        val input = "test@example.com"
        
        val result = input.validateAndTransform(
            fieldName = "email",
            validator = { it.contains("@") },
            transformer = { it.lowercase() },
            lazyMessage = { "must contain @ symbol" }
        )
        
        assertEquals("test@example.com", result)
    }
    
    @Test
    fun `validateAndTransform should throw for invalid input`() {
        val input = "invalid-email"
        
        val exception = assertThrows<ApiError.ValidationError> {
            input.validateAndTransform(
                fieldName = "email",
                validator = { it.contains("@") },
                transformer = { it.lowercase() },
                lazyMessage = { "must contain @ symbol" }
            )
        }
        
        assertEquals("email", exception.field)
        assertEquals("must contain @ symbol", exception.message)
    }
    
    /**
     * PED: TEST GROUP - EDGE CASES
     * 
     * Estos tests verifican comportamientos en casos límite
     * y situaciones especiales.
     */
    
    @Test
    fun `ConflictError with null optional fields should have null details`() {
        val error = ApiError.ConflictError(
            message = "Conflict occurred",
            conflictingResource = null,
            currentState = null
        )
        
        assertEquals(null, error.details)
    }
    
    @Test
    fun `UnauthorizedError with realm should include realm in details`() {
        val realm = "api"
        val error = ApiError.UnauthorizedError(
            realm = realm
        )
        
        val details = error.details
        assertNotNull(details)
        assertEquals(realm, details["realm"])
    }
    
    @Test
    fun `ForbiddenError with permissions should include them in details`() {
        val requiredPermission = "admin"
        val userPermissions = listOf("user", "read")
        
        val error = ApiError.ForbiddenError(
            requiredPermission = requiredPermission,
            userPermissions = userPermissions
        )
        
        val details = error.details
        assertNotNull(details)
        assertEquals(requiredPermission, details["requiredPermission"])
        assertEquals(userPermissions, details["userPermissions"])
    }
    
    @Test
    fun `ServiceUnavailableError should handle optional fields correctly`() {
        // PED: Test con todos los campos opcionales
        val error1 = ApiError.ServiceUnavailableError()
        assertEquals(null, error1.details)
        
        // PED: Test con algunos campos opcionales
        val error2 = ApiError.ServiceUnavailableError(
            retryAfterSeconds = 300,
            maintenanceWindow = "2024-01-01 02:00-04:00"
        )
        
        val details = error2.details
        assertNotNull(details)
        assertEquals(300, details["retryAfterSeconds"])
        assertEquals("2024-01-01 02:00-04:00", details["maintenanceWindow"])
    }
}

