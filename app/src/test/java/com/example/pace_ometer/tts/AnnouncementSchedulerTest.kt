package com.example.pace_ometer.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementSchedulerTest {

    @Test
    fun `does not trigger before the first interval`() {
        val scheduler = AnnouncementScheduler(intervalMeters = 1000.0)
        assertFalse(scheduler.checkAndAdvance(500.0))
    }

    @Test
    fun `triggers once distance reaches the interval`() {
        val scheduler = AnnouncementScheduler(intervalMeters = 1000.0)
        assertTrue(scheduler.checkAndAdvance(1000.0))
        assertFalse(scheduler.checkAndAdvance(1500.0))
        assertTrue(scheduler.checkAndAdvance(2000.0))
    }

    @Test
    fun `catches up when distance jumps past multiple intervals at once`() {
        val scheduler = AnnouncementScheduler(intervalMeters = 1000.0)
        assertTrue(scheduler.checkAndAdvance(3200.0))
        assertEquals(4000.0, scheduler.nextTriggerDistanceMeters(), 0.001)
    }

    @Test
    fun `reset returns to the first interval`() {
        val scheduler = AnnouncementScheduler(intervalMeters = 1000.0)
        scheduler.checkAndAdvance(2500.0)
        scheduler.reset()
        assertEquals(1000.0, scheduler.nextTriggerDistanceMeters(), 0.001)
    }
}
