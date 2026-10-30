package com.notaflotante.app

import org.junit.Assert.*
import org.junit.Test

class ForegroundTrackerTest {
    @Test fun unknownOrPausedHomeDoesNotShowNotes() {
        val tracker = ForegroundTracker()
        assertFalse(tracker.isHome("home", "notes"))
        tracker.event(1, "home", "Home", true)
        assertTrue(tracker.isHome("home", "notes"))
        tracker.event(2, "home", "Home", false)
        assertFalse(tracker.isHome("home", "notes"))
    }

    @Test fun switchingAppsHidesNotesAndReturningRestoresThem() {
        val tracker = ForegroundTracker()
        tracker.event(1, "home", "Home", true)
        tracker.event(2, "clock", "Clock", true)
        tracker.event(3, "home", "Home", false)
        assertFalse(tracker.isHome("home", "notes"))
        tracker.event(4, "home", "Home", true)
        assertTrue(tracker.isHome("home", "notes"))
    }

    @Test fun delayedEventsCannotReplaceANewerApp() {
        val tracker = ForegroundTracker()
        tracker.event(20, "clock", "Clock", true)
        tracker.event(10, "home", "Home", true)
        assertFalse(tracker.isHome("home", "notes"))
    }

    @Test fun pausingOldActivityDoesNotClearANewerActivityInSamePackage() {
        val tracker = ForegroundTracker()
        tracker.event(1, "home", "Old", true)
        tracker.event(2, "home", "New", true)
        tracker.event(3, "home", "Old", false)
        assertTrue(tracker.isHome("home", "notes"))
    }

    @Test fun missingHomeAndOwnAppAreNeverAcceptedAsHome() {
        val tracker = ForegroundTracker()
        tracker.event(1, "notes", "Editor", true)
        assertFalse(tracker.isHome(null, "notes"))
        assertFalse(tracker.isHome("notes", "notes"))
    }
}
