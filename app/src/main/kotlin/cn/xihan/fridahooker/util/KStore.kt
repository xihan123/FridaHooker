package cn.xihan.fridahooker.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import java.io.FileNotFoundException
import kotlinx.serialization.json.okio.decodeFromBufferedSource as decode
import kotlinx.serialization.json.okio.encodeToBufferedSink as encode

/**
 * @项目名 :
 * @作者 : MissYang
 * @创建时间 : 2023/3/25 1:30
 * @介绍 : https://github.com/xxfast/KStore 0.6.0
 */
val FILE_SYSTEM: FileSystem = FileSystem.SYSTEM

val DefaultJson: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal val StoreDispatcher: CoroutineDispatcher = Dispatchers.IO

class KStore<T : @Serializable Any>(
    private val default: T? = null,
    private val enableCache: Boolean = true,
    private val codec: Codec<T>,
) {
    private val lock: Mutex = Mutex()
    internal val cache: MutableStateFlow<T?> = MutableStateFlow(default)

    /** Observe store for updates */
    val updates: Flow<T?>
        get() = this.cache.onStart { read(fromCache = false) } // updates will always start with a fresh read

    private suspend fun write(value: T?): Unit = withContext(StoreDispatcher) {
        codec.encode(value)
        cache.emit(value)
    }

    private suspend fun read(fromCache: Boolean): T? = withContext(StoreDispatcher) {
        if (fromCache && cache.value != default) return@withContext cache.value
        val decoded: T? = codec.decode()
        val emitted: T? = decoded ?: default
        cache.emit(emitted)
        return@withContext emitted
    }

    /**
     * Set a value to the store
     *
     * @param value to set
     */
    suspend fun set(value: T?): Unit = lock.withLock { write(value) }

    /**
     * Get a value from the store
     *
     * @return value stored/cached (if enabled)
     */
    suspend fun get(): T? = lock.withLock { read(enableCache) }

    /**
     * Update a value in a store.
     * Note: This maintains a single mutex lock for both get and set
     *
     * @param operation lambda to update a given value of type [T]
     */
    suspend fun update(operation: (T?) -> T?): Unit = lock.withLock {
        val previous: T? = read(enableCache)
        val updated: T? = operation(previous)
        write(updated)
    }

    /**
     * Set the value of the store to null
     */
    suspend fun delete() {
        set(null)
        cache.emit(null)
    }

    /**
     * Set the value of the store to the default
     */
    suspend fun reset() {
        set(default)
        cache.emit(default)
    }
}

interface Codec<T : @Serializable Any> {
    suspend fun encode(value: T?)
    suspend fun decode(): T?
}

inline fun <reified T : @Serializable Any> FileCodec(
    filePath: String,
    json: Json = DefaultJson,
): FileCodec<T> = FileCodec(
    filePath = filePath,
    json = json,
    serializer = json.serializersModule.serializer(),
)

@OptIn(ExperimentalSerializationApi::class)
class FileCodec<T : @Serializable Any>(
    filePath: String,
    private val json: Json,
    private val serializer: KSerializer<T>,
) : Codec<T> {
    private val path: Path = filePath.toPath()

    override suspend fun decode(): T? = try {
        json.decode(serializer, FILE_SYSTEM.source(path).buffer())
    } catch (e: FileNotFoundException) {
        null
    }

    override suspend fun encode(value: T?) {
        val parentFolder: Path? = path.parent
        if (parentFolder != null && !FILE_SYSTEM.exists(parentFolder)) FILE_SYSTEM.createDirectories(
            parentFolder,
            mustCreate = false
        )
        if (value != null) FILE_SYSTEM.sink(path).buffer()
            .use { json.encode(serializer, value, it) }
        else FILE_SYSTEM.delete(path)
    }
}

inline fun <reified T : @Serializable Any> storeOf(
    filePath: String,
    default: T? = null,
    enableCache: Boolean = true,
    json: Json = DefaultJson,
): KStore<T> = KStore(
    default = default, enableCache = enableCache, codec = FileCodec(filePath, json)
)
