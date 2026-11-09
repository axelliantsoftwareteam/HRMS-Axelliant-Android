package com.axelliant.hris.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ViewWsListLoadingBinding
import com.axelliant.hris.loading.ListLoadPresentation
import com.axelliant.hris.loading.ListLoadState

/**
 * Installs skeleton/progress/error/empty presentation inside a list FrameLayout BELOW its toolbar.
 * Keeps existing children/scroll state, reserves space for refresh errors, and never opens a dialog.
 * Bind a ViewModel's persistent list state; the caller owns retry and any pull-refresh spinner.
 */
class WsListLoading(
    owner: LifecycleOwner,
    host: FrameLayout,
    onRetry: () -> Unit,
    skeletonRows: Int = 6,
    private val emptyTitle: CharSequence = host.context.getString(R.string.ws_list_empty_title),
    private val emptyMessage: CharSequence = host.context.getString(R.string.ws_list_empty_message)
) : DefaultLifecycleObserver {
    private var owner: LifecycleOwner? = owner
    private var host: FrameLayout? = host
    private var retry: (() -> Unit)? = onRetry
    private val originals = host.children.toMutableList()
    private var binding: ViewWsListLoadingBinding? = null
    private var source: LiveData<ListLoadState>? = null
    private val observer = Observer<ListLoadState> { render(it) }
    private var lastState = ListLoadState()

    init {
        require(skeletonRows in 1..12)
        require(originals.none { it.tag is WsListLoading }) { "List host already has a loading controller" }
        val inflater = LayoutInflater.from(host.context)
        val b = ViewWsListLoadingBinding.inflate(inflater, host, false)
        b.root.tag = this
        binding = b
        originals.forEach { host.removeView(it); b.wsListContent.addView(it) }
        host.addView(b.root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        repeat(skeletonRows) { inflater.inflate(R.layout.ws_item_skeleton, b.wsListSkeleton, true) }
        val retryClick = View.OnClickListener { if (!lastState.isLoading) retry?.invoke() }
        b.wsListRetry.setOnClickListener(retryClick)
        b.wsListBannerRetry.setOnClickListener(retryClick)
        owner.lifecycle.addObserver(this)
    }

    fun bind(state: LiveData<ListLoadState>) {
        val lifecycleOwner = owner ?: return
        source?.removeObserver(observer)
        source = state
        state.observe(lifecycleOwner, observer)
    }

    fun render(state: ListLoadState) {
        lastState = state
        val b = binding ?: return
        val presentation = ListLoadPresentation.from(state)
        val visible = owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
        b.wsListSkeleton.isVisible = presentation.skeleton
        b.wsListSkeleton.setAnimating(visible && presentation.skeleton)
        b.wsListContent.isVisible = presentation.showContent
        b.wsListProgress.isVisible = visible && presentation.inlineProgress
        b.wsListPageProgress.isVisible = visible && presentation.pageProgress
        b.wsListErrorBanner.isVisible = presentation.errorBanner
        b.wsListErrorText.text = state.error?.takeIf { it.isNotBlank() }
            ?: b.root.context.getString(R.string.ws_list_error_message)
        b.wsListBannerRetry.isEnabled = !state.isLoading
        b.wsListMessage.isVisible = presentation.fullError || presentation.empty
        if (presentation.fullError) b.wsListMessageCard.show(
            title = b.root.context.getString(R.string.ws_list_error_title),
            message = state.error?.takeIf { it.isNotBlank() } ?: b.root.context.getString(R.string.ws_list_error_message))
        else if (presentation.empty) b.wsListMessageCard.show(title = emptyTitle, message = emptyMessage)
        b.wsListRetry.isVisible = presentation.fullError
        b.wsListRetry.isEnabled = !state.isLoading
    }

    override fun onStart(owner: LifecycleOwner) { render(lastState) }
    override fun onStop(owner: LifecycleOwner) {
        binding?.wsListSkeleton?.setAnimating(false)
        binding?.wsListProgress?.isVisible = false
        binding?.wsListPageProgress?.isVisible = false
    }
    override fun onDestroy(owner: LifecycleOwner) { release() }

    /** Idempotent; releases view/lifecycle references and restores the original host children. */
    fun release() {
        source?.removeObserver(observer)
        source = null
        owner?.lifecycle?.removeObserver(this)
        val b = binding
        val container = host
        if (b != null && container != null) {
            b.wsListSkeleton.setAnimating(false)
            container.removeView(b.root)
            originals.forEach { b.wsListContent.removeView(it); container.addView(it) }
        }
        originals.clear()
        binding = null
        host = null
        owner = null
        retry = null
    }
}
