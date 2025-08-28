
package com.aptivist.kotlin.persistence.repository

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * PED: REPOSITORY PATTERN CON KOTLIN AVANZADO
 * 
 * Este archivo demuestra conceptos avanzados de Kotlin y patrones de diseño:
 * 1. **Repository Pattern**: Abstracción de acceso a datos
 * 2. **Generic Interfaces**: Para reutilización de código
 * 3. **Suspend Functions**: Para operaciones asíncronas no bloqueantes
 * 4. **Coroutine Context**: Dispatchers para I/O operations
 * 5. **Result<T>**: Functional error handling
 * 6. **Extension Functions**: Para operaciones domain-specific
 * 7. **Sealed Classes**: Para resultados type-safe
 * 8. **Higher-Order Functions**: Para queries flexibles
 * 9. **Inline Functions**: Para performance optimization
 * 10. **Reified Generics**: Para type-safe operations
 */

/**
 * PED: SEALED CLASS para resultados de operaciones
 * Proporciona type-safe error handling sin exceptions
 */
sealed class RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>()
    data class Error(val exception: Throwable, val message: String = exception.message ?: "Unknown error") : RepositoryResult<Nothing>()
    
    /**
     * PED: INLINE FUNCTIONS para transformaciones
     * inline evita overhead de lambdas en hot paths
     */
    inline fun <R> map(transform: (T) -> R): RepositoryResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    inline fun <R> flatMap(transform: (T) -> RepositoryResult<R>): RepositoryResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): RepositoryResult<T> = also {
        if (this is Success) action(data)
    }
    
    inline fun onError(action: (Throwable) -> Unit): RepositoryResult<T> = also {
        if (this is Error) action(exception)
    }
    
    /**
     * PED: EXTENSION PROPERTIES para convenience
     */
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
}

/**
 * PED: GENERIC REPOSITORY INTERFACE
 * Define contrato común para todas las operaciones CRUD
 */
interface Repository<T, ID> {
    
    /**
     * PED: SUSPEND FUNCTIONS para operaciones asíncronas
     * Todas las operaciones de DB deben ser suspend para no bloquear threads
     */
    suspend fun findById(id: ID): RepositoryResult<T?>
    
    suspend fun findAll(limit: Int = 100, offset: Int = 0): RepositoryResult<List<T>>
    
    suspend fun save(entity: T): RepositoryResult<T>
    
    suspend fun update(id: ID, entity: T): RepositoryResult<T>
    
    suspend fun delete(id: ID): RepositoryResult<Boolean>
    
    suspend fun exists(id: ID): RepositoryResult<Boolean>
    
    suspend fun count(): RepositoryResult<Long>
}

/**
 * PED: ABSTRACT BASE REPOSITORY
 * Implementa funcionalidad común y manejo de transacciones
 */
abstract class BaseRepository<T, ID> : Repository<T, ID> {
    
    protected val logger = LoggerFactory.getLogger(this::class.java)
    
    /**
     * PED: HIGHER-ORDER FUNCTION para manejo de transacciones
     * Encapsula el patrón de transaction + error handling
     */
    protected suspend inline fun <R> dbQuery(crossinline block: () -> R): RepositoryResult<R> {
        return try {
            // PED: newSuspendedTransaction para operaciones suspend
            val result = newSuspendedTransaction(Dispatchers.IO) {
                block()
            }
            RepositoryResult.Success(result)
        } catch (e: Exception) {
            logger.error("Database operation failed", e)
            RepositoryResult.Error(e)
        }
    }
    
    /**
     * PED: INLINE FUNCTION para transacciones síncronas
     * Para casos donde no necesitamos suspend
     */
    protected inline fun <R> syncDbQuery(crossinline block: () -> R): RepositoryResult<R> {
        return try {
            val result = transaction {
                block()
            }
            RepositoryResult.Success(result)
        } catch (e: Exception) {
            logger.error("Synchronous database operation failed", e)
            RepositoryResult.Error(e)
        }
    }
}

/**
 * PED: QUERY SPECIFICATION PATTERN
 * Para queries complejas y reutilizables
 */
interface Specification<T> {
    fun isSatisfiedBy(entity: T): Boolean
}

/**
 * PED: PAGINATION DATA CLASS
 * Para resultados paginados
 */
data class Page<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    val hasNext: Boolean get() = pageNumber < totalPages - 1
    val hasPrevious: Boolean get() = pageNumber > 0
    val isFirst: Boolean get() = pageNumber == 0
    val isLast: Boolean get() = pageNumber == totalPages - 1
    
    companion object {
        fun <T> empty(): Page<T> = Page(
            content = emptyList(),
            pageNumber = 0,
            pageSize = 0,
            totalElements = 0,
            totalPages = 0
        )
        
        fun <T> of(
            content: List<T>,
            pageNumber: Int,
            pageSize: Int,
            totalElements: Long
        ): Page<T> {
            val totalPages = if (pageSize > 0) ((totalElements + pageSize - 1) / pageSize).toInt() else 0
            return Page(content, pageNumber, pageSize, totalElements, totalPages)
        }
    }
}

/**
 * PED: SORT SPECIFICATION
 * Para ordenamiento flexible
 */
data class Sort(
    val property: String,
    val direction: Direction = Direction.ASC
) {
    enum class Direction { ASC, DESC }
    
    companion object {
        fun by(property: String, direction: Direction = Direction.ASC): Sort = Sort(property, direction)
        fun asc(property: String): Sort = Sort(property, Direction.ASC)
        fun desc(property: String): Sort = Sort(property, Direction.DESC)
    }
}

/**
 * PED: PAGEABLE INTERFACE
 * Para especificaciones de paginación
 */
data class Pageable(
    val pageNumber: Int = 0,
    val pageSize: Int = 20,
    val sort: List<Sort> = emptyList()
) {
    val offset: Int get() = pageNumber * pageSize
    
    companion object {
        fun of(pageNumber: Int, pageSize: Int, vararg sorts: Sort): Pageable =
            Pageable(pageNumber, pageSize, sorts.toList())
        
        fun unpaged(): Pageable = Pageable(0, Int.MAX_VALUE)
    }
}

/**
 * PED: EXTENDED REPOSITORY INTERFACE
 * Para operaciones avanzadas como paginación y búsqueda
 */
interface ExtendedRepository<T, ID> : Repository<T, ID> {
    
    suspend fun findAll(pageable: Pageable): RepositoryResult<Page<T>>
    
    suspend fun findBy(specification: Specification<T>): RepositoryResult<List<T>>
    
    suspend fun findBy(specification: Specification<T>, pageable: Pageable): RepositoryResult<Page<T>>
    
    suspend fun saveAll(entities: List<T>): RepositoryResult<List<T>>
    
    suspend fun deleteAll(ids: List<ID>): RepositoryResult<Int>
    
    suspend fun existsBy(specification: Specification<T>): RepositoryResult<Boolean>
    
    suspend fun countBy(specification: Specification<T>): RepositoryResult<Long>
}

/**
 * PED: REPOSITORY FACTORY INTERFACE
 * Para dependency injection y testing
 */
interface RepositoryFactory {
    fun <T, ID> create(entityClass: Class<T>): Repository<T, ID>
}

/**
 * PED: EXTENSION FUNCTIONS para Result handling
 * Proporcionan API fluida para manejo de resultados
 */
suspend inline fun <T, R> RepositoryResult<T>.mapSuspend(
    crossinline transform: suspend (T) -> R
): RepositoryResult<R> = when (this) {
    is RepositoryResult.Success -> try {
        RepositoryResult.Success(transform(data))
    } catch (e: Exception) {
        RepositoryResult.Error(e)
    }
    is RepositoryResult.Error -> this
}

suspend inline fun <T, R> RepositoryResult<T>.flatMapSuspend(
    crossinline transform: suspend (T) -> RepositoryResult<R>
): RepositoryResult<R> = when (this) {
    is RepositoryResult.Success -> try {
        transform(data)
    } catch (e: Exception) {
        RepositoryResult.Error(e)
    }
    is RepositoryResult.Error -> this
}

/**
 * PED: UTILITY FUNCTIONS para conversión
 */
inline fun <T> Result<T>.toRepositoryResult(): RepositoryResult<T> = fold(
    onSuccess = { RepositoryResult.Success(it) },
    onFailure = { RepositoryResult.Error(it) }
)

inline fun <T> RepositoryResult<T>.toResult(): Result<T> = when (this) {
    is RepositoryResult.Success -> Result.success(data)
    is RepositoryResult.Error -> Result.failure(exception)
}

/**
 * PED: DSL BUILDER para Specifications
 * Permite crear specifications de forma fluida
 */
class SpecificationBuilder<T> {
    private val specifications = mutableListOf<Specification<T>>()
    
    fun add(spec: Specification<T>) {
        specifications.add(spec)
    }
    
    fun build(): Specification<T> = CompositeSpecification(specifications)
}

/**
 * PED: COMPOSITE SPECIFICATION
 * Implementa patrón Composite para combinar specifications
 */
class CompositeSpecification<T>(
    private val specifications: List<Specification<T>>
) : Specification<T> {
    
    override fun isSatisfiedBy(entity: T): Boolean =
        specifications.all { it.isSatisfiedBy(entity) }
}

/**
 * PED: TOP-LEVEL FUNCTION para DSL
 */
inline fun <T> specification(builder: SpecificationBuilder<T>.() -> Unit): Specification<T> =
    SpecificationBuilder<T>().apply(builder).build()

