package com.axelliant.hris.network

import java.util.concurrent.Executor
import org.junit.Assert.*
import org.junit.Test

class BackgroundResponseParserTest {
    private class Queue : Executor {
        val tasks = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { tasks.add(command) }
        fun run() = tasks.removeFirst().run()
    }
    private data class Payload(val value: String? = null)

    @Test fun bodyIsReadOnWorkerAndDeliveredOnlyOnMain() {
        val worker = Queue(); val main = Queue()
        val parser = BackgroundResponseParser(worker, main) { fail("Unexpected unauthorized") }
        var read = false
        var result: ApiResult<Payload>? = null
        parser.parse({ read = true; """{"message":{"value":"ready"}}""" }, Payload::class.java,
            { false }, { true }) { result = it }
        assertFalse(read); assertNull(result)
        worker.run()
        assertTrue(read); assertNull(result)
        main.run()
        assertEquals("ready", (result as ApiResult.Success).data.value)
    }

    @Test fun cancellationAndSessionChangesSuppressDelivery() {
        for (cancelled in listOf(false, true)) {
            val worker = Queue(); val main = Queue()
            val parser = BackgroundResponseParser(worker, main) { fail("Stale unauthorized") }
            parser.parse<Payload>({ """{"message":{"meta":{"status_code":401}}}""" }, Payload::class.java,
                { cancelled }, { cancelled }) { fail("Stale delivery") }
            worker.run(); main.run()
        }
    }

    @Test fun unauthorizedEventRunsOnMainBeforeFailureDelivery() {
        val worker = Queue(); val main = Queue()
        val events = mutableListOf<String>()
        val parser = BackgroundResponseParser(worker, main) { events += "401" }
        parser.parse<Payload>({ """{"message":{"meta":{"status_code":401}}}""" }, Payload::class.java,
            { false }, { true }) { assertTrue(it is ApiResult.Failure); events += "delivery" }
        worker.run(); assertTrue(events.isEmpty())
        main.run(); assertEquals(listOf("401", "delivery"), events)
    }

    @Test fun bodyReadOrMalformedEnvelopeErrorsBecomeFailures() {
        val worker = Queue(); val main = Queue()
        val parser = BackgroundResponseParser(worker, main) {}
        val readers: List<() -> String?> = listOf({ throw IllegalStateException("read failed") },
            { """{"message":{"meta":[]}}""" }, { "not-json" }, { null })
        readers.forEach { read ->
            var result: ApiResult<Payload>? = null
            parser.parse(read, Payload::class.java, { false }, { true }) { result = it }
            worker.run(); main.run()
            assertTrue(result is ApiResult.Failure)
        }
    }
}
