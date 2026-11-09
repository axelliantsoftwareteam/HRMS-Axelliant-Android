package com.axelliant.hris.loading

/** Shared rendering policy, including retained-content errors and single-indicator pull refresh. */
data class ListLoadPresentation(
    val skeleton: Boolean,
    val fullError: Boolean,
    val empty: Boolean,
    val inlineProgress: Boolean,
    val pageProgress: Boolean,
    val errorBanner: Boolean
) {
    val showContent: Boolean get() = !skeleton && !fullError && !empty

    companion object {
        fun from(state: ListLoadState): ListLoadPresentation {
            val skeleton = state.initialLoading && !state.hasLoaded
            val failed = state.error != null && !state.hasContent && !skeleton
            return ListLoadPresentation(
                skeleton = skeleton,
                fullError = failed,
                empty = state.isEmpty && !skeleton && !failed,
                inlineProgress = (state.inlineRefreshing || state.initialLoading && state.hasLoaded) && !skeleton,
                pageProgress = state.loadingMore && !skeleton,
                errorBanner = state.hasContent && state.error != null
            )
        }
    }
}
