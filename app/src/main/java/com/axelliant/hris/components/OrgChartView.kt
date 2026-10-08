package com.axelliant.hris.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.axelliant.hris.R
import com.axelliant.hris.databinding.OrgItemNodeBinding
import com.axelliant.hris.model.team.OrgLayout
import com.axelliant.hris.model.team.OrgNode
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.setAvatar
import com.intuit.sdp.R as SdpR
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Zoomable, pannable organogram. The tree is built out of real child views (a card per person) laid
 * out absolutely inside [content] with a connector layer underneath; pinch (or [zoomIn]/[zoomOut])
 * scales [content] and a drag/fling pans it in both axes. A chart narrower than the view is centred.
 */
class OrgChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** Called whenever the zoom changes (pinch, buttons or fit). */
    var onZoomChanged: ((Float) -> Unit)? = null

    var zoom = 1f
        private set

    /** Pixels per design unit of [OrgLayout]; sdp-proportional so the chart scales with the screen. */
    private val unit = context.resources.getDimension(SdpR.dimen._1sdp) * UNIT_SCALE
    private val pad = CHART_PADDING * unit

    private val content = FrameLayout(context)
    private var contentW = 0
    private var contentH = 0
    private var panX = 0f
    private var panY = 0f
    private var fitPending = false
    private var currentNodes: List<OrgNode>? = null

    private val scroller = OverScroller(context)
    private val flingStep = object : Runnable {
        override fun run() {
            if (scroller.computeScrollOffset()) {
                panX = scroller.currX.toFloat()
                panY = scroller.currY.toFloat()
                applyTransform()
                postOnAnimation(this)
            }
        }
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            setZoomInternal(zoom * detector.scaleFactor, detector.focusX, detector.focusY)
            return true
        }
    })

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            panX -= distanceX
            panY -= distanceY
            applyTransform()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            fling(velocityX, velocityY)
            return true
        }
    })

    init {
        content.pivotX = 0f
        content.pivotY = 0f
        addView(content, FrameLayout.LayoutParams(1, 1))
    }

    /** True when [nodes] is the tree already shown (so the caller can skip a rebuild and keep the zoom). */
    fun isShowing(nodes: List<OrgNode>): Boolean = currentNodes == nodes

    /** Builds the chart for [roots] and fits it to the view width. */
    fun setChart(roots: List<OrgNode>) {
        currentNodes = roots
        val result = OrgLayout.layout(roots)
        contentW = (result.width * unit + 2 * pad).roundToInt()
        contentH = (result.height * unit + 2 * pad).roundToInt()
        content.removeAllViews()
        content.layoutParams = FrameLayout.LayoutParams(contentW, contentH)

        val connectors = ConnectorView(context)
        connectors.setLinks(result.links, unit, pad)
        content.addView(connectors, FrameLayout.LayoutParams(contentW, contentH))

        val inflater = LayoutInflater.from(context)
        val nodeW = (OrgLayout.NODE_WIDTH * unit).roundToInt()
        val nodeH = (OrgLayout.NODE_HEIGHT * unit).roundToInt()
        val radius = 14f * unit
        val grey = ContextCompat.getColor(context, R.color.ws_grey)
        val primary = ContextCompat.getColor(context, R.color.ws_primary)
        val rootFill = ColorUtils.compositeColors(WsStatus.tint(primary, 10), Color.WHITE)
        val hairline = max(1, context.resources.displayMetrics.density.toInt())
        result.placed.forEach { p ->
            val b = OrgItemNodeBinding.inflate(inflater, content, false)
            val name = p.node.name.orEmpty()
            b.orgNodeAvatar.setAvatar(p.node.image, name.ifEmpty { "?" })
            b.orgNodeName.text = name
            val title = p.node.title.orEmpty()
            b.orgNodeTitle.text = title
            b.orgNodeTitle.visibility = if (title.isEmpty()) GONE else VISIBLE
            b.root.radius = radius
            b.root.setCardBackgroundColor(if (p.depth == 0) rootFill else Color.WHITE)
            b.root.strokeColor = WsStatus.tint(grey, 35)
            b.root.strokeWidth = hairline
            val lp = FrameLayout.LayoutParams(nodeW, nodeH).apply {
                leftMargin = (pad + p.x * unit).roundToInt()
                topMargin = (pad + p.y * unit).roundToInt()
            }
            content.addView(b.root, lp)
        }
        fitPending = true
        if (width > 0) fit()
    }

    /** Shrinks the chart to the view width when it is wider, and shows it at full size when it is not. */
    fun fit() {
        if (width == 0 || contentW == 0) {
            fitPending = true
            return
        }
        fitPending = false
        panX = 0f
        panY = 0f
        zoom = 1f
        setZoomInternal(min(1f, width.toFloat() / contentW), 0f, 0f)
    }

    fun zoomIn() = setZoomInternal(zoom + ZOOM_STEP, width / 2f, height / 2f, roundToHundredths = true)

    fun zoomOut() = setZoomInternal(zoom - ZOOM_STEP, width / 2f, height / 2f, roundToHundredths = true)

    val canZoomIn: Boolean get() = zoom < MAX_ZOOM - 0.001f
    val canZoomOut: Boolean get() = zoom > MIN_ZOOM + 0.001f

    /** Zoom as a percentage ("85"). */
    val zoomPercent: Int get() = (zoom * 100).roundToInt()

    private fun setZoomInternal(target: Float, focusX: Float, focusY: Float, roundToHundredths: Boolean = false) {
        var next = target.coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (roundToHundredths) next = (next * 100).roundToInt() / 100f
        val ratio = if (zoom == 0f) 1f else next / zoom
        panX = focusX - (focusX - panX) * ratio
        panY = focusY - (focusY - panY) * ratio
        zoom = next
        applyTransform()
        onZoomChanged?.invoke(zoom)
    }

    /** Keeps the chart inside the view (centred horizontally when narrower, top aligned when shorter). */
    private fun applyTransform() {
        val sw = contentW * zoom
        val sh = contentH * zoom
        panX = if (sw <= width) (width - sw) / 2f else panX.coerceIn(width - sw, 0f)
        panY = if (sh <= height) 0f else panY.coerceIn(height - sh, 0f)
        content.scaleX = zoom
        content.scaleY = zoom
        content.translationX = panX
        content.translationY = panY
    }

    private fun fling(vx: Float, vy: Float) {
        val sw = contentW * zoom
        val sh = contentH * zoom
        val minX = if (sw <= width) panX.roundToInt() else (width - sw).roundToInt()
        val maxX = if (sw <= width) panX.roundToInt() else 0
        val minY = if (sh <= height) 0 else (height - sh).roundToInt()
        val maxY = 0
        scroller.fling(panX.roundToInt(), panY.roundToInt(), vx.roundToInt(), vy.roundToInt(), minX, maxX, minY, maxY)
        postOnAnimation(flingStep)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (fitPending && w > 0) fit() else applyTransform()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                removeCallbacks(flingStep)
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        scaleDetector.onTouchEvent(event)
        if (!scaleDetector.isInProgress) gestures.onTouchEvent(event)
        return true
    }

    /** Draws the manager-to-report lines (1dp right-angle polylines) and the arrowheads. */
    private class ConnectorView(context: Context) : View(context) {
        private val color = WsStatus.tint(ContextCompat.getColor(context, R.color.ws_grey), 70)
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = this@ConnectorView.color
            strokeWidth = max(1f, context.resources.displayMetrics.density)
        }
        private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = this@ConnectorView.color
        }
        private val linePath = Path()
        private val arrowPath = Path()

        fun setLinks(links: List<OrgLayout.Link>, unit: Float, pad: Float) {
            linePath.reset()
            arrowPath.reset()
            links.forEach { l ->
                val x1 = pad + l.fromX * unit
                val y1 = pad + l.fromY * unit
                val x2 = pad + l.toX * unit
                val y2 = pad + l.toY * unit
                val midY = (y1 + y2) / 2f
                linePath.moveTo(x1, y1)
                linePath.lineTo(x1, midY)
                linePath.lineTo(x2, midY)
                linePath.lineTo(x2, y2)
                arrowPath.moveTo(x2 - 5f * unit, y2 - 6f * unit)
                arrowPath.lineTo(x2 + 5f * unit, y2 - 6f * unit)
                arrowPath.lineTo(x2, y2)
                arrowPath.close()
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawPath(linePath, linePaint)
            canvas.drawPath(arrowPath, arrowPaint)
        }
    }

    companion object {
        const val MIN_ZOOM = 0.3f
        const val MAX_ZOOM = 2f
        const val ZOOM_STEP = 0.15f

        /** Design units are the iOS points; this maps them to sdp (0.82 matches the rest of the redesign). */
        private const val UNIT_SCALE = 0.7f
        private const val CHART_PADDING = 20f
    }
}
