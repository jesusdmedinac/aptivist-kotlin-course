
package com.aptivist.kotlin.persistence.db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

/**
 * PED: ENTITY DEFINITIONS CON EXPOSED ORM
 * 
 * Este archivo demuestra conceptos avanzados de Kotlin y Exposed:
 * 1. **Table Definitions**: DSL type-safe para definir schemas
 * 2. **Entity Classes**: ORM-style entities con lazy loading
 * 3. **Relationships**: One-to-Many, Many-to-One con referential integrity
 * 4. **Custom Column Types**: JSON, enums, y tipos personalizados
 * 5. **Companion Objects**: Para factory methods y queries
 * 6. **Extension Functions**: Para operaciones domain-specific
 * 7. **Sealed Classes**: Para type-safe status y categorías
 * 8. **Data Classes**: Para DTOs y value objects
 * 9. **Nullable Types**: Para campos opcionales con null safety
 * 10. **Java Time API**: Para manejo moderno de fechas y timestamps
 */

/**
 * PED: SEALED CLASS para estados type-safe
 * Garantiza exhaustividad en when expressions y type safety
 */
sealed class EntityStatus(val value: String) {
    object Active : EntityStatus("ACTIVE")
    object Inactive : EntityStatus("INACTIVE")
    object Pending : EntityStatus("PENDING")
    object Archived : EntityStatus("ARCHIVED")
    
    companion object {
        fun fromString(value: String): EntityStatus = when (value.uppercase()) {
            "ACTIVE" -> Active
            "INACTIVE" -> Inactive
            "PENDING" -> Pending
            "ARCHIVED" -> Archived
            else -> throw IllegalArgumentException("Unknown status: $value")
        }
        
        fun values(): List<EntityStatus> = listOf(Active, Inactive, Pending, Archived)
    }
}

/**
 * PED: ENUM CLASS para prioridades
 * Enums con propiedades y métodos personalizados
 */
enum class Priority(val level: Int, val displayName: String) {
    LOW(1, "Baja"),
    MEDIUM(2, "Media"),
    HIGH(3, "Alta"),
    CRITICAL(4, "Crítica");
    
    /**
     * PED: COMPANION OBJECT en enum para factory methods
     */
    companion object {
        fun fromLevel(level: Int): Priority = values().find { it.level == level }
            ?: throw IllegalArgumentException("Invalid priority level: $level")
    }
}

/**
 * PED: TABLE DEFINITION - USERS
 * 
 * Demuestra:
 * - UUIDTable para primary keys UUID
 * - Column constraints y defaults
 * - Index definitions para performance
 * - Custom column types
 */
object Users : UUIDTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val firstName = varchar("first_name", 50).nullable()
    val lastName = varchar("last_name", 50).nullable()
    val status = enumerationByName("status", 20, EntityStatus::class).default(EntityStatus.Active)
    val createdAt = timestamp("created_at").defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp())
    val lastLoginAt = timestamp("last_login_at").nullable()
    val metadata = text("metadata").nullable() // JSON storage
    
    /**
     * PED: INIT BLOCK para configuración adicional de tabla
     */
    init {
        // PED: Composite index para queries frecuentes
        index(false, username, status)
        index(false, email, status)
    }
}

/**
 * PED: ENTITY CLASS - USER
 * 
 * Demuestra:
 * - Entity inheritance de UUIDEntity
 * - Property delegation con 'by' keyword
 * - Lazy relationships
 * - Custom properties y computed fields
 */
class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users) {
        
        /**
         * PED: FACTORY METHOD con named parameters
         */
        fun create(
            username: String,
            email: String,
            passwordHash: String,
            firstName: String? = null,
            lastName: String? = null,
            status: EntityStatus = EntityStatus.Active
        ): User = new {
            this.username = username
            this.email = email
            this.passwordHash = passwordHash
            this.firstName = firstName
            this.lastName = lastName
            this.status = status
        }
        
        /**
         * PED: QUERY METHODS con DSL
         */
        fun findByUsername(username: String): User? = find { Users.username eq username }.singleOrNull()
        
        fun findByEmail(email: String): User? = find { Users.email eq email }.singleOrNull()
        
        fun findActive(): SizedIterable<User> = find { Users.status eq EntityStatus.Active }
    }
    
    // PED: Property delegation - automáticamente mapea a columnas de tabla
    var username by Users.username
    var email by Users.email
    var passwordHash by Users.passwordHash
    var firstName by Users.firstName
    var lastName by Users.lastName
    var status by Users.status
    var createdAt by Users.createdAt
    var updatedAt by Users.updatedAt
    var lastLoginAt by Users.lastLoginAt
    var metadata by Users.metadata
    
    /**
     * PED: COMPUTED PROPERTY usando custom getter
     */
    val fullName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").takeIf { it.isNotBlank() } ?: username
    
    /**
     * PED: LAZY RELATIONSHIP - One-to-Many
     * Lazy loading evita N+1 queries
     */
    val projects by Project referrersOn Projects.owner
    val tasks by Task referrersOn Tasks.assignee
    
    /**
     * PED: EXTENSION FUNCTION para operaciones domain-specific
     */
    fun updateLastLogin() {
        lastLoginAt = Instant.now()
    }
    
    /**
     * PED: METHOD con validation
     */
    fun updateProfile(firstName: String?, lastName: String?) {
        this.firstName = firstName?.takeIf { it.isNotBlank() }
        this.lastName = lastName?.takeIf { it.isNotBlank() }
        this.updatedAt = Instant.now()
    }
}

/**
 * PED: TABLE DEFINITION - PROJECTS
 */
object Projects : LongIdTable("projects") {
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val owner = reference("owner_id", Users)
    val status = enumerationByName("status", 20, EntityStatus::class).default(EntityStatus.Active)
    val priority = enumerationByName("priority", 20, Priority::class).default(Priority.MEDIUM)
    val startDate = datetime("start_date").nullable()
    val endDate = datetime("end_date").nullable()
    val createdAt = timestamp("created_at").defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp())
    
    init {
        index(false, owner, status)
        index(false, status, priority)
    }
}

/**
 * PED: ENTITY CLASS - PROJECT
 */
class Project(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Project>(Projects) {
        
        fun create(
            name: String,
            description: String? = null,
            owner: User,
            priority: Priority = Priority.MEDIUM,
            startDate: LocalDateTime? = null,
            endDate: LocalDateTime? = null
        ): Project = new {
            this.name = name
            this.description = description
            this.owner = owner
            this.priority = priority
            this.startDate = startDate
            this.endDate = endDate
        }
        
        fun findByOwner(owner: User): SizedIterable<Project> = find { Projects.owner eq owner.id }
        
        fun findActive(): SizedIterable<Project> = find { Projects.status eq EntityStatus.Active }
    }
    
    var name by Projects.name
    var description by Projects.description
    var owner by User referencedOn Projects.owner
    var status by Projects.status
    var priority by Projects.priority
    var startDate by Projects.startDate
    var endDate by Projects.endDate
    var createdAt by Projects.createdAt
    var updatedAt by Projects.updatedAt
    
    /**
     * PED: LAZY RELATIONSHIPS
     */
    val tasks by Task referrersOn Tasks.project
    
    /**
     * PED: COMPUTED PROPERTIES
     */
    val isOverdue: Boolean
        get() = endDate?.let { it.isBefore(LocalDateTime.now()) } ?: false
    
    val taskCount: Long
        get() = tasks.count()
    
    val completedTaskCount: Long
        get() = tasks.count { Tasks.status eq EntityStatus.Active }
    
    /**
     * PED: BUSINESS LOGIC METHODS
     */
    fun addTask(
        title: String,
        description: String? = null,
        assignee: User? = null,
        priority: Priority = Priority.MEDIUM
    ): Task = Task.create(
        title = title,
        description = description,
        project = this,
        assignee = assignee,
        priority = priority
    )
    
    fun archive() {
        status = EntityStatus.Archived
        updatedAt = Instant.now()
    }
}

/**
 * PED: TABLE DEFINITION - TASKS
 */
object Tasks : LongIdTable("tasks") {
    val title = varchar("title", 200)
    val description = text("description").nullable()
    val project = reference("project_id", Projects)
    val assignee = reference("assignee_id", Users).nullable()
    val status = enumerationByName("status", 20, EntityStatus::class).default(EntityStatus.Pending)
    val priority = enumerationByName("priority", 20, Priority::class).default(Priority.MEDIUM)
    val dueDate = datetime("due_date").nullable()
    val completedAt = timestamp("completed_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp())
    val updatedAt = timestamp("updated_at").defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp())
    val estimatedHours = decimal("estimated_hours", 5, 2).nullable()
    val actualHours = decimal("actual_hours", 5, 2).nullable()
    
    init {
        index(false, project, status)
        index(false, assignee, status)
        index(false, status, priority)
        index(false, dueDate)
    }
}

/**
 * PED: ENTITY CLASS - TASK
 */
class Task(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Task>(Tasks) {
        
        fun create(
            title: String,
            description: String? = null,
            project: Project,
            assignee: User? = null,
            priority: Priority = Priority.MEDIUM,
            dueDate: LocalDateTime? = null
        ): Task = new {
            this.title = title
            this.description = description
            this.project = project
            this.assignee = assignee
            this.priority = priority
            this.dueDate = dueDate
        }
        
        fun findByProject(project: Project): SizedIterable<Task> = find { Tasks.project eq project.id }
        
        fun findByAssignee(assignee: User): SizedIterable<Task> = find { Tasks.assignee eq assignee.id }
        
        fun findOverdue(): SizedIterable<Task> = find { 
            (Tasks.dueDate less LocalDateTime.now()) and (Tasks.status neq EntityStatus.Active)
        }
    }
    
    var title by Tasks.title
    var description by Tasks.description
    var project by Project referencedOn Tasks.project
    var assignee by User optionalReferencedOn Tasks.assignee
    var status by Tasks.status
    var priority by Tasks.priority
    var dueDate by Tasks.dueDate
    var completedAt by Tasks.completedAt
    var createdAt by Tasks.createdAt
    var updatedAt by Tasks.updatedAt
    var estimatedHours by Tasks.estimatedHours
    var actualHours by Tasks.actualHours
    
    /**
     * PED: COMPUTED PROPERTIES con business logic
     */
    val isOverdue: Boolean
        get() = dueDate?.let { it.isBefore(LocalDateTime.now()) && status != EntityStatus.Active } ?: false
    
    val isCompleted: Boolean
        get() = status == EntityStatus.Active && completedAt != null
    
    val hoursVariance: Double?
        get() = if (estimatedHours != null && actualHours != null) {
            actualHours!!.toDouble() - estimatedHours!!.toDouble()
        } else null
    
    /**
     * PED: BUSINESS METHODS con state transitions
     */
    fun complete() {
        status = EntityStatus.Active
        completedAt = Instant.now()
        updatedAt = Instant.now()
    }
    
    fun assign(user: User) {
        assignee = user
        updatedAt = Instant.now()
    }
    
    fun updateProgress(actualHours: Double) {
        this.actualHours = actualHours.toBigDecimal()
        updatedAt = Instant.now()
    }
}

/**
 * PED: DATA CLASSES para DTOs y Value Objects
 * 
 * Estas clases demuestran:
 * - Immutable data structures
 * - Automatic equals/hashCode/toString
 * - Copy functions para updates
 * - Serialization support
 */
data class UserSummary(
    val id: UUID,
    val username: String,
    val email: String,
    val fullName: String,
    val status: EntityStatus,
    val projectCount: Int,
    val taskCount: Int,
    val createdAt: Instant
)

data class ProjectSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val ownerName: String,
    val status: EntityStatus,
    val priority: Priority,
    val taskCount: Long,
    val completedTaskCount: Long,
    val isOverdue: Boolean,
    val createdAt: Instant
)

data class TaskSummary(
    val id: Long,
    val title: String,
    val projectName: String,
    val assigneeName: String?,
    val status: EntityStatus,
    val priority: Priority,
    val isOverdue: Boolean,
    val dueDate: LocalDateTime?,
    val createdAt: Instant
)

/**
 * PED: EXTENSION FUNCTIONS para conversión Entity -> DTO
 * Demuestra separation of concerns y clean architecture
 */
fun User.toSummary(): UserSummary = UserSummary(
    id = id.value,
    username = username,
    email = email,
    fullName = fullName,
    status = status,
    projectCount = projects.count().toInt(),
    taskCount = tasks.count().toInt(),
    createdAt = createdAt
)

fun Project.toSummary(): ProjectSummary = ProjectSummary(
    id = id.value,
    name = name,
    description = description,
    ownerName = owner.fullName,
    status = status,
    priority = priority,
    taskCount = taskCount,
    completedTaskCount = completedTaskCount,
    isOverdue = isOverdue,
    createdAt = createdAt
)

fun Task.toSummary(): TaskSummary = TaskSummary(
    id = id.value,
    title = title,
    projectName = project.name,
    assigneeName = assignee?.fullName,
    status = status,
    priority = priority,
    isOverdue = isOverdue,
    dueDate = dueDate,
    createdAt = createdAt
)

