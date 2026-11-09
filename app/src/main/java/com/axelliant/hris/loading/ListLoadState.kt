package com.axelliant.hris.loading

/** Read intent controls presentation; pull refresh must not also show an inline spinner. */
enum class ListLoadKind { INITIAL, REFRESH, FILTER, PULL_REFRESH, PAGE }

/** Persistent presentation state, independent of a Fragment's view and one-shot result events. */
data class ListLoadState(
    val hasLoaded: Boolean = false,
    val itemCount: Int = 0,
    val initialLoading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val pullRefreshing: Boolean = false,
    val inlineRefreshing: Boolean = false,
    val error: String? = null
) {
    val hasContent: Boolean get() = itemCount > 0
    val isLoading: Boolean get() = initialLoading || refreshing || loadingMore
    val isEmpty: Boolean get() = hasLoaded && !hasContent
}

/** An opaque operation identity; replaced/finished work cannot change current presentation. */
data class ListLoadTicket internal constructor(internal val generation: Long, internal val id: Long)

/** Main-thread reducer. Use one ticket for a multi-API batch, finishing after its aggregate result. */
class ListLoadTracker(private val changed: (ListLoadState) -> Unit) {
    var state = ListLoadState()
        private set
    private var generation = 0L
    private var nextId = 0L
    private val pending = mutableMapOf<ListLoadTicket, ListLoadKind>()

    fun begin(kind: ListLoadKind, replace: Boolean = false): ListLoadTicket {
        if (replace) { generation++; pending.clear() }
        if (pending.isEmpty()) state = state.copy(error = null)
        val ticket = ListLoadTicket(generation, ++nextId)
        val presentation = if (!state.hasLoaded) ListLoadKind.INITIAL
            else if (state.hasLoaded && kind == ListLoadKind.INITIAL) ListLoadKind.REFRESH else kind
        pending[ticket] = presentation
        publish()
        return ticket
    }

    fun isCurrent(ticket: ListLoadTicket): Boolean = pending.containsKey(ticket)

    /** [itemCount] is the total displayed count after merging, including on page loads. */
    fun finish(ticket: ListLoadTicket, itemCount: Int? = null, error: String? = null) {
        if (!pending.containsKey(ticket)) return
        require(itemCount == null || itemCount >= 0)
        pending.remove(ticket)
        state = state.copy(
            hasLoaded = state.hasLoaded || itemCount != null,
            itemCount = itemCount ?: state.itemCount,
            error = error ?: state.error
        )
        publish()
    }

    fun cancel(ticket: ListLoadTicket) {
        if (pending.remove(ticket) != null) publish()
    }

    fun clear() {
        generation++
        pending.clear()
        state = ListLoadState()
        changed(state)
    }

    private fun publish() {
        val kinds = pending.values
        state = state.copy(
            initialLoading = ListLoadKind.INITIAL in kinds,
            refreshing = kinds.any { it == ListLoadKind.REFRESH || it == ListLoadKind.FILTER || it == ListLoadKind.PULL_REFRESH },
            loadingMore = ListLoadKind.PAGE in kinds,
            pullRefreshing = ListLoadKind.PULL_REFRESH in kinds,
            inlineRefreshing = kinds.any { it == ListLoadKind.REFRESH || it == ListLoadKind.FILTER }
        )
        changed(state)
    }
}
