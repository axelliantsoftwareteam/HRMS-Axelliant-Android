package com.axelliant.hris.cache

import com.axelliant.hris.network.ApiResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keeps one successful value per login key with bounded freshness and shared concurrent loads. */
class SessionValueCache<T : Any>(
    private val ttlMillis: Long,
    private val now: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    init { require(ttlMillis > 0) { "Cache lifetime must be positive." } }
    private data class Entry<T>(val key: String, val value: T, val loadedAt: Long)
    private var entry: Entry<T>? = null
    private var generation = 0L
    private val mutex = Mutex()

    /** Returns cached content for [key], even when stale; never returns another login's value. */
    @Synchronized
    fun peek(key: String): T? = entry?.takeIf { it.key == key }?.value

    /** Returns whether [key]'s successful value is still inside the cache lifetime. */
    @Synchronized
    fun isFresh(key: String): Boolean = entry?.let {
        it.key == key && (now() - it.loadedAt) in 0 until ttlMillis
    } ?: false

    /** Removes cached content and rejects in-flight writes started before this invalidation. */
    @Synchronized
    fun clear() {
        entry = null
        generation++
    }

    /**
     * Loads [key] once when stale, or always when [force] is true. [isCurrent] rejects ended logins.
     * Successful current results are cached; errors are returned uncached and cancellation propagates.
     */
    suspend fun load(
        key: String,
        isCurrent: () -> Boolean,
        force: Boolean = false,
        fetch: suspend () -> ApiResult<T>
    ): ApiResult<T> = mutex.withLock {
        if (!isCurrent()) return@withLock ApiResult.Failure("Session changed. Please try again.")
        val cached = synchronized(this) { if (!force && isFresh(key)) peek(key) else null }
        if (cached != null) return@withLock ApiResult.Success(cached)
        val started = synchronized(this) { generation }
        val result = fetch()
        currentCoroutineContext().ensureActive()
        synchronized(this) {
            if (generation != started || !isCurrent()) return@withLock ApiResult.Failure("Session changed. Please try again.")
            if (result is ApiResult.Success) entry = Entry(key, result.data, now())
        }
        result
    }
}
