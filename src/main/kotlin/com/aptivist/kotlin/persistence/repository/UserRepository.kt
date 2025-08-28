
package com.aptivist.kotlin.persistence.repository

import com.aptivist.kotlin.persistence.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * PED: USER REPOSITORY IMPLEMENTATION
 * 
 * Demuestra implementación concreta del Repository Pattern con:
 * 1. **Concrete Repository**: Implementación específica para User entity
 * 2. **Custom Query Methods**: Métodos de búsqueda domain-specific
 * 3. **Complex Queries**: Joins, aggregations y subqueries
 * 4. **Batch Operations**: Operaciones en lote para performance
 * 5. **Caching Integration**: Preparado para integración con cache
 * 6. **Specification Pattern**: Queries flexibles y reutilizables
 * 7. **Transaction Management**: Manejo correcto de transacciones
 * 8. **Error Handling**: Manejo robusto de errores
 * 9. **Performance Optimization**: Queries optimizadas y lazy loading
 * 10. **Type Safety**: Operaciones type-safe con Exposed DSL
 */
class UserRepository : BaseRepository<User, UUID>(), ExtendedRepository<User, UUID> {
    
    /**
     * PED: BASIC CRUD OPERATIONS
     * Implementación de operaciones básicas del Repository interface
     */
    override suspend fun findById(id: UUID): RepositoryResult<User?> = dbQuery {
        User.findById(id)
    }
    
    override suspend fun findAll(limit: Int, offset: Int): RepositoryResult<List<User>> = dbQuery {
        User.all()
            .limit(limit, offset.toLong())
            .toList()
    }
    
    override suspend fun save(entity: User): RepositoryResult<User> = dbQuery {
        // PED: Para nuevas entidades, Exposed maneja automáticamente la creación
        entity.apply {
            // Actualizar timestamp si es necesario
            updatedAt = java.time.Instant.now()
        }
    }
    
    override suspend fun update(id: UUID, entity: User): RepositoryResult<User> = dbQuery {
        val existingUser = User.findById(id)
            ?: throw IllegalArgumentException("User with id $id not found")
        
        // PED: Update usando property assignment
        existingUser.apply {
            username = entity.username
            email = entity.email
            firstName = entity.firstName
            lastName = entity.lastName
            status = entity.status
            updatedAt = java.time.Instant.now()
        }
    }
    
    override suspend fun delete(id: UUID): RepositoryResult<Boolean> = dbQuery {
        val user = User.findById(id)
        if (user != null) {
            user.delete()
            true
        } else {
            false
        }
    }
    
    override suspend fun exists(id: UUID): RepositoryResult<Boolean> = dbQuery {
        User.findById(id) != null
    }
    
    override suspend fun count(): RepositoryResult<Long> = dbQuery {
        User.all().count()
    }
    
    /**
     * PED: EXTENDED REPOSITORY OPERATIONS
     * Implementación de operaciones avanzadas
     */
    override suspend fun findAll(pageable: Pageable): RepositoryResult<Page<User>> = dbQuery {
        val totalElements = User.all().count()
        
        val users = User.all()
            .limit(pageable.pageSize, pageable.offset.toLong())
            .toList()
        
        Page.of(users, pageable.pageNumber, pageable.pageSize, totalElements)
    }
    
    override suspend fun findBy(specification: Specification<User>): RepositoryResult<List<User>> = dbQuery {
        // PED: Para specifications complejas, necesitaríamos convertir a SQL
        // Por simplicidad, filtramos en memoria (no recomendado para production)
        User.all().filter { specification.isSatisfiedBy(it) }
    }
    
    override suspend fun findBy(
        specification: Specification<User>,
        pageable: Pageable
    ): RepositoryResult<Page<User>> = dbQuery {
        val allUsers = User.all().filter { specification.isSatisfiedBy(it) }
        val totalElements = allUsers.count().toLong()
        
        val users = allUsers
            .drop(pageable.offset)
            .take(pageable.pageSize)
        
        Page.of(users, pageable.pageNumber, pageable.pageSize, totalElements)
    }
    
    override suspend fun saveAll(entities: List<User>): RepositoryResult<List<User>> = dbQuery {
        entities.map { user ->
            user.apply {
                updatedAt = java.time.Instant.now()
            }
        }
    }
    
    override suspend fun deleteAll(ids: List<UUID>): RepositoryResult<Int> = dbQuery {
        var deletedCount = 0
        ids.forEach { id ->
            User.findById(id)?.let {
                it.delete()
                deletedCount++
            }
        }
        deletedCount
    }
    
    override suspend fun existsBy(specification: Specification<User>): RepositoryResult<Boolean> = dbQuery {
        User.all().any { specification.isSatisfiedBy(it) }
    }
    
    override suspend fun countBy(specification: Specification<User>): RepositoryResult<Long> = dbQuery {
        User.all().count { specification.isSatisfiedBy(it) }.toLong()
    }
    
    /**
     * PED: DOMAIN-SPECIFIC QUERY METHODS
     * Métodos de búsqueda específicos para User entity
     */
    suspend fun findByUsername(username: String): RepositoryResult<User?> = dbQuery {
        User.find { Users.username eq username }.singleOrNull()
    }
    
    suspend fun findByEmail(email: String): RepositoryResult<User?> = dbQuery {
        User.find { Users.email eq email }.singleOrNull()
    }
    
    suspend fun findByStatus(status: EntityStatus): RepositoryResult<List<User>> = dbQuery {
        User.find { Users.status eq status }.toList()
    }
    
    suspend fun findActiveUsers(): RepositoryResult<List<User>> = dbQuery {
        User.find { Users.status eq EntityStatus.Active }.toList()
    }
    
    suspend fun findUsersWithProjects(): RepositoryResult<List<User>> = dbQuery {
        // PED: Query con JOIN usando Exposed DSL
        User.find {
            Users.id inSubQuery (
                Projects.slice(Projects.owner).selectAll()
            )
        }.toList()
    }
    
    suspend fun findUsersByProjectCount(minProjects: Int): RepositoryResult<List<User>> = dbQuery {
        // PED: Complex query con aggregation
        val userIds = Projects
            .slice(Projects.owner, Projects.owner.count())
            .selectAll()
            .groupBy(Projects.owner)
            .having { Projects.owner.count() greaterEq minProjects }
            .map { it[Projects.owner] }
        
        User.find { Users.id inList userIds }.toList()
    }
    
    /**
     * PED: SEARCH METHODS
     * Métodos de búsqueda flexible
     */
    suspend fun searchUsers(
        query: String,
        status: EntityStatus? = null,
        limit: Int = 50
    ): RepositoryResult<List<User>> = dbQuery {
        val baseQuery = Users.selectAll()
        
        // PED: Dynamic query building
        val conditions = mutableListOf<Op<Boolean>>()
        
        // Text search en múltiples campos
        if (query.isNotBlank()) {
            conditions.add(
                (Users.username like "%$query%") or
                (Users.email like "%$query%") or
                (Users.firstName like "%$query%") or
                (Users.lastName like "%$query%")
            )
        }
        
        // Filter por status
        status?.let { conditions.add(Users.status eq it) }
        
        // Combine conditions
        val finalQuery = if (conditions.isNotEmpty()) {
            baseQuery.where { conditions.reduce { acc, condition -> acc and condition } }
        } else {
            baseQuery
        }
        
        User.wrapRows(finalQuery.limit(limit)).toList()
    }
    
    /**
     * PED: BATCH OPERATIONS
     * Operaciones optimizadas para múltiples registros
     */
    suspend fun createUsers(userRequests: List<CreateUserRequest>): RepositoryResult<List<User>> = dbQuery {
        userRequests.map { request ->
            User.create(
                username = request.username,
                email = request.email,
                passwordHash = request.passwordHash,
                firstName = request.firstName,
                lastName = request.lastName,
                status = request.status
            )
        }
    }
    
    suspend fun updateUserStatuses(
        userIds: List<UUID>,
        newStatus: EntityStatus
    ): RepositoryResult<Int> = dbQuery {
        Users.update({ Users.id inList userIds }) {
            it[status] = newStatus
            it[updatedAt] = java.time.Instant.now()
        }
    }
    
    /**
     * PED: STATISTICS METHODS
     * Métodos para obtener estadísticas y métricas
     */
    suspend fun getUserStatistics(): RepositoryResult<UserStatistics> = dbQuery {
        val totalUsers = User.all().count()
        val activeUsers = User.find { Users.status eq EntityStatus.Active }.count()
        val inactiveUsers = User.find { Users.status eq EntityStatus.Inactive }.count()
        val pendingUsers = User.find { Users.status eq EntityStatus.Pending }.count()
        
        // PED: Query con JOIN para obtener usuarios con proyectos
        val usersWithProjects = Users
            .innerJoin(Projects)
            .slice(Users.id)
            .selectAll()
            .withDistinct()
            .count()
        
        UserStatistics(
            totalUsers = totalUsers,
            activeUsers = activeUsers,
            inactiveUsers = inactiveUsers,
            pendingUsers = pendingUsers,
            usersWithProjects = usersWithProjects.toLong()
        )
    }
    
    /**
     * PED: VALIDATION METHODS
     * Métodos para validación de business rules
     */
    suspend fun isUsernameAvailable(username: String, excludeUserId: UUID? = null): RepositoryResult<Boolean> = dbQuery {
        val query = Users.select { Users.username eq username }
        
        val finalQuery = excludeUserId?.let {
            query.andWhere { Users.id neq it }
        } ?: query
        
        finalQuery.empty()
    }
    
    suspend fun isEmailAvailable(email: String, excludeUserId: UUID? = null): RepositoryResult<Boolean> = dbQuery {
        val query = Users.select { Users.email eq email }
        
        val finalQuery = excludeUserId?.let {
            query.andWhere { Users.id neq it }
        } ?: query
        
        finalQuery.empty()
    }
}

/**
 * PED: DATA CLASSES para requests y responses
 * Separación entre API layer y domain layer
 */
data class CreateUserRequest(
    val username: String,
    val email: String,
    val passwordHash: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val status: EntityStatus = EntityStatus.Pending
)

data class UpdateUserRequest(
    val username: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val status: EntityStatus? = null
)

data class UserStatistics(
    val totalUsers: Long,
    val activeUsers: Long,
    val inactiveUsers: Long,
    val pendingUsers: Long,
    val usersWithProjects: Long
) {
    val activePercentage: Double get() = if (totalUsers > 0) (activeUsers.toDouble() / totalUsers) * 100 else 0.0
    val usersWithProjectsPercentage: Double get() = if (totalUsers > 0) (usersWithProjects.toDouble() / totalUsers) * 100 else 0.0
}

/**
 * PED: SPECIFICATION IMPLEMENTATIONS
 * Specifications concretas para User entity
 */
class UserByStatusSpecification(private val status: EntityStatus) : Specification<User> {
    override fun isSatisfiedBy(entity: User): Boolean = entity.status == status
}

class UserWithProjectsSpecification : Specification<User> {
    override fun isSatisfiedBy(entity: User): Boolean = entity.projects.count() > 0
}

class UserCreatedAfterSpecification(private val date: java.time.Instant) : Specification<User> {
    override fun isSatisfiedBy(entity: User): Boolean = entity.createdAt.isAfter(date)
}

/**
 * PED: EXTENSION FUNCTIONS para User entity
 * Operaciones domain-specific como extension functions
 */
suspend fun UserRepository.findOrCreateByEmail(
    email: String,
    defaultUsername: String,
    passwordHash: String
): RepositoryResult<User> {
    return findByEmail(email).flatMapSuspend { existingUser ->
        if (existingUser != null) {
            RepositoryResult.Success(existingUser)
        } else {
            dbQuery {
                User.create(
                    username = defaultUsername,
                    email = email,
                    passwordHash = passwordHash
                )
            }
        }
    }
}

suspend fun UserRepository.activateUser(userId: UUID): RepositoryResult<User> {
    return findById(userId).flatMapSuspend { user ->
        if (user != null) {
            dbQuery {
                user.apply {
                    status = EntityStatus.Active
                    updatedAt = java.time.Instant.now()
                }
            }
        } else {
            RepositoryResult.Error(IllegalArgumentException("User not found: $userId"))
        }
    }
}

