
package com.aptivist.kotlin.persistence.file

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.io.path.*

/**
 * PED: FILE STORAGE CON KOTLIN AVANZADO
 * 
 * Este archivo demuestra conceptos avanzados de Kotlin y persistencia de archivos:
 * 1. **File I/O Operations**: Operaciones de archivos asíncronas y thread-safe
 * 2. **JSON Serialization**: Serialización/deserialización con Jackson
 * 3. **Coroutine Safety**: Operaciones thread-safe con Mutex
 * 4. **Generic Storage**: Almacenamiento type-safe para cualquier tipo
 * 5. **Backup & Recovery**: Estrategias de backup y recuperación
 * 6. **Atomic Operations**: Escrituras atómicas para consistencia
 * 7. **Extension Functions**: Para operaciones domain-specific
 * 8. **Sealed Classes**: Para resultados type-safe
 * 9. **Resource Management**: Proper cleanup y manejo de recursos
 * 10. **Path API**: Uso moderno de java.nio.file.Path
 */

/**
 * PED: SEALED CLASS para resultados de operaciones de archivo
 * Proporciona type-safe error handling para I/O operations
 */
sealed class FileResult<out T> {
    data class Success<T>(val data: T) : FileResult<T>()
    data class Error(val exception: Throwable, val message: String = exception.message ?: "File operation failed") : FileResult<Nothing>()
    
    /**
     * PED: INLINE FUNCTIONS para transformaciones
     */
    inline fun <R> map(transform: (T) -> R): FileResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    inline fun <R> flatMap(transform: (T) -> FileResult<R>): FileResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): FileResult<T> = also {
        if (this is Success) action(data)
    }
    
    inline fun onError(action: (Throwable) -> Unit): FileResult<T> = also {
        if (this is Error) action(exception)
    }
    
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
 * PED: DATA CLASS para configuración de storage
 */
data class FileStorageConfig(
    val baseDirectory: Path,
    val enableBackups: Boolean = true,
    val maxBackups: Int = 5,
    val backupInterval: java.time.Duration = java.time.Duration.ofHours(1),
    val prettyPrint: Boolean = true,
    val atomicWrites: Boolean = true,
    val compressionEnabled: Boolean = false
) {
    companion object {
        fun default(baseDir: String = "./data"): FileStorageConfig =
            FileStorageConfig(baseDirectory = Path(baseDir))
    }
}

/**
 * PED: GENERIC FILE STORAGE INTERFACE
 * Abstracción type-safe para operaciones de persistencia
 */
interface FileStorage<T> {
    
    suspend fun save(key: String, data: T): FileResult<Unit>
    
    suspend fun load(key: String): FileResult<T?>
    
    suspend fun delete(key: String): FileResult<Boolean>
    
    suspend fun exists(key: String): FileResult<Boolean>
    
    suspend fun list(): FileResult<List<String>>
    
    suspend fun backup(key: String): FileResult<String>
    
    suspend fun restore(key: String, backupId: String): FileResult<T>
    
    suspend fun listBackups(key: String): FileResult<List<String>>
}

/**
 * PED: JSON FILE STORAGE IMPLEMENTATION
 * Implementación concreta usando Jackson para JSON serialization
 */
class JsonFileStorage<T>(
    private val config: FileStorageConfig,
    private val clazz: Class<T>
) : FileStorage<T> {
    
    private val logger = LoggerFactory.getLogger("FileStorage.${clazz.simpleName}")
    
    /**
     * PED: MUTEX para thread-safety
     * Garantiza operaciones atómicas en concurrent environment
     */
    private val mutex = Mutex()
    
    /**
     * PED: LAZY INITIALIZATION del ObjectMapper
     * Configuración costosa se hace solo una vez
     */
    private val objectMapper by lazy {
        ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
            if (config.prettyPrint) {
                writerWithDefaultPrettyPrinter()
            }
        }
    }
    
    /**
     * PED: COMPUTED PROPERTIES para paths
     */
    private val dataDirectory: Path get() = config.baseDirectory.resolve("data")
    private val backupDirectory: Path get() = config.baseDirectory.resolve("backups")
    
    init {
        // PED: Ensure directories exist
        runCatching {
            dataDirectory.createDirectories()
            if (config.enableBackups) {
                backupDirectory.createDirectories()
            }
        }.onFailure { 
            logger.error("Failed to create storage directories", it)
        }
    }
    
    /**
     * PED: SUSPEND FUNCTION implementations con error handling
     */
    override suspend fun save(key: String, data: T): FileResult<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val filePath = getDataPath(key)
                
                if (config.atomicWrites) {
                    // PED: Atomic write usando temporary file
                    saveAtomically(filePath, data)
                } else {
                    // PED: Direct write
                    saveDirect(filePath, data)
                }
                
                logger.debug("💾 Saved data to: $filePath")
                FileResult.Success(Unit)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to save data for key: $key", e)
                FileResult.Error(e)
            }
        }
    }
    
    override suspend fun load(key: String): FileResult<T?> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val filePath = getDataPath(key)
                
                if (!filePath.exists()) {
                    logger.debug("📂 File not found: $filePath")
                    return@withContext FileResult.Success(null)
                }
                
                val data = objectMapper.readValue<T>(filePath.toFile(), clazz)
                logger.debug("📖 Loaded data from: $filePath")
                FileResult.Success(data)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to load data for key: $key", e)
                FileResult.Error(e)
            }
        }
    }
    
    override suspend fun delete(key: String): FileResult<Boolean> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val filePath = getDataPath(key)
                val deleted = filePath.deleteIfExists()
                
                if (deleted) {
                    logger.debug("🗑️ Deleted file: $filePath")
                } else {
                    logger.debug("📂 File not found for deletion: $filePath")
                }
                
                FileResult.Success(deleted)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to delete data for key: $key", e)
                FileResult.Error(e)
            }
        }
    }
    
    override suspend fun exists(key: String): FileResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val exists = getDataPath(key).exists()
            FileResult.Success(exists)
        } catch (e: Exception) {
            FileResult.Error(e)
        }
    }
    
    override suspend fun list(): FileResult<List<String>> = withContext(Dispatchers.IO) {
        try {
            val files = dataDirectory.listDirectoryEntries("*.json")
                .map { it.nameWithoutExtension }
                .sorted()
            
            FileResult.Success(files)
        } catch (e: Exception) {
            logger.error("❌ Failed to list files", e)
            FileResult.Error(e)
        }
    }
    
    /**
     * PED: BACKUP OPERATIONS
     */
    override suspend fun backup(key: String): FileResult<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                if (!config.enableBackups) {
                    return@withContext FileResult.Error(IllegalStateException("Backups are disabled"))
                }
                
                val sourcePath = getDataPath(key)
                if (!sourcePath.exists()) {
                    return@withContext FileResult.Error(IllegalArgumentException("File not found: $key"))
                }
                
                val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                    .replace(":", "-") // Windows-safe filename
                val backupId = "${key}_$timestamp"
                val backupPath = getBackupPath(backupId)
                
                // PED: Copy file atomically
                Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING)
                
                // PED: Cleanup old backups
                cleanupOldBackups(key)
                
                logger.info("💾 Created backup: $backupId")
                FileResult.Success(backupId)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create backup for key: $key", e)
                FileResult.Error(e)
            }
        }
    }
    
    override suspend fun restore(key: String, backupId: String): FileResult<T> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val backupPath = getBackupPath(backupId)
                if (!backupPath.exists()) {
                    return@withContext FileResult.Error(IllegalArgumentException("Backup not found: $backupId"))
                }
                
                val data = objectMapper.readValue<T>(backupPath.toFile(), clazz)
                
                // PED: Restore to original location
                val originalPath = getDataPath(key)
                Files.copy(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING)
                
                logger.info("🔄 Restored from backup: $backupId")
                FileResult.Success(data)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to restore from backup: $backupId", e)
                FileResult.Error(e)
            }
        }
    }
    
    override suspend fun listBackups(key: String): FileResult<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (!config.enableBackups) {
                return@withContext FileResult.Success(emptyList())
            }
            
            val backups = backupDirectory.listDirectoryEntries("${key}_*.json")
                .map { it.nameWithoutExtension }
                .sortedDescending() // Most recent first
            
            FileResult.Success(backups)
        } catch (e: Exception) {
            logger.error("❌ Failed to list backups for key: $key", e)
            FileResult.Error(e)
        }
    }
    
    /**
     * PED: PRIVATE HELPER METHODS
     */
    private fun getDataPath(key: String): Path = dataDirectory.resolve("$key.json")
    
    private fun getBackupPath(backupId: String): Path = backupDirectory.resolve("$backupId.json")
    
    private suspend fun saveAtomically(filePath: Path, data: T) {
        val tempPath = filePath.resolveSibling("${filePath.fileName}.tmp")
        
        try {
            // PED: Write to temporary file first
            objectMapper.writeValue(tempPath.toFile(), data)
            
            // PED: Atomic move to final location
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING)
            
        } catch (e: Exception) {
            // PED: Cleanup temporary file on error
            tempPath.deleteIfExists()
            throw e
        }
    }
    
    private suspend fun saveDirect(filePath: Path, data: T) {
        objectMapper.writeValue(filePath.toFile(), data)
    }
    
    private suspend fun cleanupOldBackups(key: String) {
        try {
            val backups = backupDirectory.listDirectoryEntries("${key}_*.json")
                .sortedByDescending { it.getLastModifiedTime() }
            
            if (backups.size > config.maxBackups) {
                backups.drop(config.maxBackups).forEach { oldBackup ->
                    oldBackup.deleteIfExists()
                    logger.debug("🧹 Cleaned up old backup: ${oldBackup.fileName}")
                }
            }
        } catch (e: Exception) {
            logger.warn("⚠️ Failed to cleanup old backups for key: $key", e)
        }
    }
}

/**
 * PED: FILE STORAGE FACTORY
 * Factory pattern para crear diferentes tipos de storage
 */
object FileStorageFactory {
    
    private val logger = LoggerFactory.getLogger(FileStorageFactory::class.java)
    
    /**
     * PED: REIFIED GENERIC FUNCTION para type-safe creation
     */
    inline fun <reified T> createJsonStorage(
        config: FileStorageConfig = FileStorageConfig.default()
    ): JsonFileStorage<T> {
        logger.info("🔧 Creating JSON storage for type: ${T::class.simpleName}")
        return JsonFileStorage(config, T::class.java)
    }
    
    /**
     * PED: FACTORY METHOD con class parameter
     */
    fun <T> createJsonStorage(
        clazz: Class<T>,
        config: FileStorageConfig = FileStorageConfig.default()
    ): JsonFileStorage<T> {
        logger.info("🔧 Creating JSON storage for class: ${clazz.simpleName}")
        return JsonFileStorage(config, clazz)
    }
}

/**
 * PED: EXTENSION FUNCTIONS para operaciones comunes
 */
suspend inline fun <T> FileStorage<T>.saveOrThrow(key: String, data: T) {
    save(key, data).getOrThrow()
}

suspend inline fun <T> FileStorage<T>.loadOrThrow(key: String): T? {
    return load(key).getOrThrow()
}

suspend inline fun <T> FileStorage<T>.loadOrDefault(key: String, default: T): T {
    return load(key).getOrNull() ?: default
}

/**
 * PED: DSL FUNCTION para configuración fluida
 */
fun fileStorageConfig(baseDir: String, configure: FileStorageConfigBuilder.() -> Unit): FileStorageConfig =
    FileStorageConfigBuilder(baseDir).apply(configure).build()

/**
 * PED: BUILDER para FileStorageConfig
 */
class FileStorageConfigBuilder(private val baseDir: String) {
    private var enableBackups: Boolean = true
    private var maxBackups: Int = 5
    private var backupInterval: java.time.Duration = java.time.Duration.ofHours(1)
    private var prettyPrint: Boolean = true
    private var atomicWrites: Boolean = true
    private var compressionEnabled: Boolean = false
    
    fun enableBackups(enable: Boolean = true): FileStorageConfigBuilder = apply {
        enableBackups = enable
    }
    
    fun maxBackups(max: Int): FileStorageConfigBuilder = apply {
        maxBackups = max
    }
    
    fun backupInterval(interval: java.time.Duration): FileStorageConfigBuilder = apply {
        backupInterval = interval
    }
    
    fun prettyPrint(enable: Boolean = true): FileStorageConfigBuilder = apply {
        prettyPrint = enable
    }
    
    fun atomicWrites(enable: Boolean = true): FileStorageConfigBuilder = apply {
        atomicWrites = enable
    }
    
    fun compression(enable: Boolean = true): FileStorageConfigBuilder = apply {
        compressionEnabled = enable
    }
    
    fun build(): FileStorageConfig = FileStorageConfig(
        baseDirectory = Path(baseDir),
        enableBackups = enableBackups,
        maxBackups = maxBackups,
        backupInterval = backupInterval,
        prettyPrint = prettyPrint,
        atomicWrites = atomicWrites,
        compressionEnabled = compressionEnabled
    )
}

/**
 * PED: TYPE ALIASES para common storage types
 */
typealias StringStorage = FileStorage<String>
typealias MapStorage<K, V> = FileStorage<Map<K, V>>
typealias ListStorage<T> = FileStorage<List<T>>

