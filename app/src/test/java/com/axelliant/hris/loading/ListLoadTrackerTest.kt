package com.axelliant.hris.loading

import org.junit.Assert.*
import org.junit.Test

class ListLoadTrackerTest {
    @Test fun overlappingInitialReadsRemainBusyUntilLastCompletion() {
        val tracker = ListLoadTracker {}
        val first = tracker.begin(ListLoadKind.INITIAL)
        val second = tracker.begin(ListLoadKind.INITIAL)
        assertTrue(ListLoadPresentation.from(tracker.state).skeleton)
        tracker.finish(first, itemCount = 3)
        assertTrue(tracker.state.initialLoading)
        val partial = ListLoadPresentation.from(tracker.state)
        assertTrue(partial.showContent)
        assertTrue(partial.inlineProgress)
        tracker.finish(second, itemCount = 5)
        assertFalse(tracker.state.isLoading)
        assertEquals(5, tracker.state.itemCount)
    }

    @Test fun firstLoadErrorIsNotAnEmptyResultAndRetryShowsSkeleton() {
        val tracker = ListLoadTracker {}
        val ticket = tracker.begin(ListLoadKind.INITIAL)
        tracker.finish(ticket, error = "Offline")
        assertFalse(tracker.state.hasLoaded)
        val failed = ListLoadPresentation.from(tracker.state)
        assertTrue(failed.fullError)
        assertFalse(failed.empty)
        tracker.begin(ListLoadKind.REFRESH)
        assertNull(tracker.state.error)
        assertTrue(ListLoadPresentation.from(tracker.state).skeleton)
    }

    @Test fun successfulZeroRowsShowsEmptyState() {
        val tracker = ListLoadTracker {}
        tracker.finish(tracker.begin(ListLoadKind.INITIAL), itemCount = 0)
        assertTrue(tracker.state.hasLoaded)
        assertTrue(ListLoadPresentation.from(tracker.state).empty)
        assertFalse(ListLoadPresentation.from(tracker.state).fullError)
    }

    @Test fun blankFailureStillShowsAnErrorRatherThanABlankList() {
        val tracker = ListLoadTracker {}
        tracker.finish(tracker.begin(ListLoadKind.INITIAL), error = "")
        assertTrue(ListLoadPresentation.from(tracker.state).fullError)
        assertFalse(ListLoadPresentation.from(tracker.state).empty)
    }

    @Test fun filteringAndFailureRetainContentWithInlineFeedback() {
        val tracker = loaded(7)
        val filter = tracker.begin(ListLoadKind.FILTER)
        assertTrue(tracker.state.refreshing)
        assertTrue(ListLoadPresentation.from(tracker.state).showContent)
        assertTrue(ListLoadPresentation.from(tracker.state).inlineProgress)
        tracker.finish(filter, error = "Try again")
        val failed = ListLoadPresentation.from(tracker.state)
        assertEquals(7, tracker.state.itemCount)
        assertTrue(failed.errorBanner)
        assertTrue(failed.showContent)
        assertFalse(failed.fullError)
    }

    @Test fun pullRefreshDoesNotAlsoShowInlineProgress() {
        val tracker = loaded(2)
        tracker.begin(ListLoadKind.PULL_REFRESH)
        assertTrue(tracker.state.pullRefreshing)
        assertTrue(tracker.state.refreshing)
        assertFalse(ListLoadPresentation.from(tracker.state).inlineProgress)
    }

    @Test fun paginationRetainsRowsAndUsesFooter() {
        val tracker = loaded(10)
        val ticket = tracker.begin(ListLoadKind.PAGE)
        val presentation = ListLoadPresentation.from(tracker.state)
        assertTrue(presentation.showContent)
        assertTrue(presentation.pageProgress)
        assertFalse(presentation.skeleton)
        tracker.finish(ticket, itemCount = 20)
        assertEquals(20, tracker.state.itemCount)
        assertFalse(tracker.state.loadingMore)
    }

    @Test fun oldGenerationCannotStopNewFilterOrReplaceItsCount() {
        val tracker = loaded(4)
        val old = tracker.begin(ListLoadKind.FILTER, replace = true)
        val current = tracker.begin(ListLoadKind.FILTER, replace = true)
        tracker.finish(old, itemCount = 99)
        assertTrue(tracker.state.refreshing)
        assertEquals(4, tracker.state.itemCount)
        tracker.finish(current, itemCount = 8)
        tracker.finish(old, error = "stale")
        assertEquals(8, tracker.state.itemCount)
        assertNull(tracker.state.error)
    }

    @Test fun cancellationAndRepeatedCompletionCannotUnderflow() {
        val tracker = loaded(3)
        val first = tracker.begin(ListLoadKind.REFRESH)
        val second = tracker.begin(ListLoadKind.REFRESH)
        tracker.cancel(first)
        assertTrue(tracker.state.refreshing)
        tracker.cancel(second)
        tracker.finish(second, itemCount = 100)
        tracker.cancel(second)
        assertFalse(tracker.state.isLoading)
        assertEquals(3, tracker.state.itemCount)
        assertNull(tracker.state.error)
    }

    @Test fun batchFailureIsNotHiddenByAnotherCompletion() {
        val tracker = ListLoadTracker {}
        val first = tracker.begin(ListLoadKind.INITIAL)
        val second = tracker.begin(ListLoadKind.INITIAL)
        tracker.finish(first, error = "One service failed")
        tracker.finish(second, itemCount = 4)
        assertTrue(ListLoadPresentation.from(tracker.state).errorBanner)
        assertFalse(tracker.state.isLoading)
    }

    @Test fun clearingRejectsAllPendingOperations() {
        val tracker = loaded(3)
        val pending = tracker.begin(ListLoadKind.REFRESH)
        tracker.clear()
        tracker.finish(pending, itemCount = 99)
        assertEquals(ListLoadState(), tracker.state)
    }

    private fun loaded(count: Int) = ListLoadTracker {}.apply {
        finish(begin(ListLoadKind.INITIAL), itemCount = count)
    }
}
