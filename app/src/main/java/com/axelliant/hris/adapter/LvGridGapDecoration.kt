package com.axelliant.hris.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** Equal [gap] between grid cells (and between rows) without any outer margin. */
class LvGridGapDecoration(private val gap: Int, private val spanCount: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val column = position % spanCount
        outRect.left = column * gap / spanCount
        outRect.right = gap - (column + 1) * gap / spanCount
        outRect.top = if (position < spanCount) 0 else gap
    }
}
