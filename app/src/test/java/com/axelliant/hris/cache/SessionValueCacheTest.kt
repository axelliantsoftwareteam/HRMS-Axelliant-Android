package com.axelliant.hris.cache

import com.axelliant.hris.network.ApiResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class SessionValueCacheTest {
    @Test fun expiryAndForceRefresh() = runBlocking {
        var now = 0L
        var loads = 0
        val cache = SessionValueCache<String>(30) { now }
        suspend fun read(force: Boolean = false) = cache.load("login/day", { true }, force) {
            ApiResult.Success("value-${++loads}")
        }
        read(); now = 29; read()
        assertEquals(1, loads)
        now = 30; read()
        assertEquals(2, loads)
        read(true)
        assertEquals(3, loads)
    }

    @Test fun concurrentReadsShareSuccessfulFetch() = runBlocking {
        val cache = SessionValueCache<String>(30_000)
        val response = CompletableDeferred<ApiResult<String>>()
        var loads = 0
        val first = async { cache.load("user", { true }) { loads++; response.await() } }
        yield()
        val second = async { cache.load("user", { true }) { loads++; ApiResult.Success("wrong") } }
        yield()
        response.complete(ApiResult.Success("shared"))
        assertEquals(first.await(), second.await())
        assertEquals(1, loads)
    }

    @Test fun loginAndDayKeysCannotReuseOtherContent() = runBlocking {
        val cache = SessionValueCache<String>(30_000)
        cache.load("user-a/day-1", { true }) { ApiResult.Success("private") }
        assertNull(cache.peek("user-b/day-1"))
        assertNull(cache.peek("user-a/day-2"))
        assertFalse(cache.isFresh("user-b/day-1"))
    }

    @Test fun invalidationRejectsAlreadyRunningWrite() = runBlocking {
        val cache = SessionValueCache<String>(30_000)
        val response = CompletableDeferred<ApiResult<String>>()
        val job = async { cache.load("user", { true }) { response.await() } }
        yield(); cache.clear(); response.complete(ApiResult.Success("old"))
        assertTrue(job.await() is ApiResult.Failure)
        assertNull(cache.peek("user"))
    }

    @Test fun changedSessionRejectsRunningResponse() = runBlocking {
        val cache = SessionValueCache<String>(30_000)
        var current = true
        val response = CompletableDeferred<ApiResult<String>>()
        val job = async { cache.load("user", { current }) { response.await() } }
        yield(); current = false; response.complete(ApiResult.Success("old-login"))
        assertTrue(job.await() is ApiResult.Failure)
        assertNull(cache.peek("user"))
    }

    @Test fun failureRetainsStaleContentAndRetries() = runBlocking {
        var now = 0L
        val cache = SessionValueCache<String>(30) { now }
        cache.load("user", { true }) { ApiResult.Success("previous") }
        now = 30
        cache.load("user", { true }) { ApiResult.Failure("offline") }
        assertEquals("previous", cache.peek("user"))
        assertFalse(cache.isFresh("user"))
        cache.load("user", { true }) { ApiResult.Success("new") }
        assertEquals("new", cache.peek("user"))
    }

    @Test fun cancellationDoesNotFillCache() = runBlocking {
        val cache = SessionValueCache<String>(30_000)
        val response = CompletableDeferred<ApiResult<String>>()
        val job = async { cache.load("user", { true }) { response.await() } }
        yield(); job.cancelAndJoin()
        assertNull(cache.peek("user"))
        cache.load("user", { true }) { ApiResult.Success("retry") }
        assertEquals("retry", cache.peek("user"))
    }
}
