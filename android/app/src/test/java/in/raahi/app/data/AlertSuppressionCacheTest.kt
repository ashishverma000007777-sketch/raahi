package `in`.raahi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlertSuppressionCacheTest {

    private lateinit var cache: AlertSuppressionCache

    @Before
    fun setUp() {
        // Cache with max 3 items and 1000ms TTL for testing
        cache = AlertSuppressionCache(maxSize = 3, ttlMs = 1000L)
    }

    @Test
    fun testDismissedJobIsSuppressedWithinWindow() {
        val jobId = "job-101"
        assertFalse("Job should not be dismissed initially", cache.isDismissed(jobId))

        val now = 10000L
        cache.markDismissed(jobId, timestamp = now)

        assertTrue("Job should be dismissed within TTL window", cache.isDismissed(jobId, now = now + 500L))
    }

    @Test
    fun testSuppressionExpiresAfterTtl() {
        val jobId = "job-202"
        val start = 10000L
        cache.markDismissed(jobId, timestamp = start)

        assertTrue(cache.isDismissed(jobId, now = start + 999L))
        assertFalse("Job suppression should expire past TTL", cache.isDismissed(jobId, now = start + 1001L))
    }

    @Test
    fun testLruEvictionWhenCapacityExceeded() {
        val t0 = 1000L
        cache.markDismissed("job-1", timestamp = t0)
        cache.markDismissed("job-2", timestamp = t0)
        cache.markDismissed("job-3", timestamp = t0)
        assertEquals(3, cache.size())

        // Adding 4th item evicts the oldest entry ("job-1")
        cache.markDismissed("job-4", timestamp = t0)
        assertEquals(3, cache.size())

        assertFalse("Oldest entry job-1 should have been evicted", cache.isDismissed("job-1", now = t0 + 100))
        assertTrue("job-2 should still be present", cache.isDismissed("job-2", now = t0 + 100))
        assertTrue("job-3 should still be present", cache.isDismissed("job-3", now = t0 + 100))
        assertTrue("job-4 should still be present", cache.isDismissed("job-4", now = t0 + 100))
    }

    @Test
    fun testClearEmptiesCache() {
        cache.markDismissed("job-a")
        cache.markDismissed("job-b")
        assertEquals(2, cache.size())

        cache.clear()
        assertEquals(0, cache.size())
        assertFalse(cache.isDismissed("job-a"))
        assertFalse(cache.isDismissed("job-b"))
    }
}
