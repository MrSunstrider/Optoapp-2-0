package com.example.optoapp.util

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackgroundErrorCollectorTest {

    private val collector = BackgroundErrorCollector()

    @Test
    fun `starts with empty errors list`() = runTest {
        assertEquals(0, collector.errors.first().size)
    }

    @Test
    fun `record adds entry to errors`() = runTest {
        collector.record("sync", "connection timeout")
        val errors = collector.errors.first()
        assertEquals(1, errors.size)
        assertEquals("sync", errors[0].source)
        assertEquals("connection timeout", errors[0].message)
    }

    @Test
    fun `record preserves insertion order`() = runTest {
        collector.record("auth", "first")
        collector.record("sync", "second")
        collector.record("refresh", "third")
        val errors = collector.errors.first()
        assertEquals(3, errors.size)
        assertEquals("auth", errors[0].source)
        assertEquals("sync", errors[1].source)
        assertEquals("refresh", errors[2].source)
    }

    @Test
    fun `clear empties the error list`() = runTest {
        collector.record("sync", "error 1")
        collector.record("auth", "error 2")
        assertEquals(2, collector.errors.first().size)

        collector.clear()
        assertEquals(0, collector.errors.first().size)
    }

    @Test
    fun `enforces max of 50 entries discarding oldest`() = runTest {
        for (i in 1..55) {
            collector.record("src", "message $i")
        }
        val errors = collector.errors.first()
        assertEquals(50, errors.size)
        assertEquals("message 6", errors.first().message)
    }

    @Test
    fun `max 50 entries keeps latest when exactly at limit`() = runTest {
        for (i in 1..50) {
            collector.record("src", "msg$i")
        }
        val errors = collector.errors.first()
        assertEquals(50, errors.size)
        assertEquals("msg1", errors.first().message)
        assertEquals("msg50", errors.last().message)
    }

    @Test
    fun `clear after overflow resets`() = runTest {
        for (i in 1..60) {
            collector.record("src", "overflow $i")
        }
        assertEquals(50, collector.errors.first().size)
        collector.clear()
        assertEquals(0, collector.errors.first().size)
    }

    @Test
    fun `record with empty strings`() = runTest {
        collector.record("", "")
        val errors = collector.errors.first()
        assertEquals(1, errors.size)
        assertEquals("", errors[0].source)
        assertEquals("", errors[0].message)
    }

    @Test
    fun `background error has timestamp`() = runTest {
        collector.record("test", "has timestamp")
        val error = collector.errors.first().first()
        assertTrue(error.timestampMs > 0)
    }
}
