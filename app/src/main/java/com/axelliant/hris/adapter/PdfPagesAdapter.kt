package com.axelliant.hris.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Shows the pages of a PDF with the platform [PdfRenderer] (no third-party library). Every page is an
 * image scaled to [pageWidthPx]; bitmaps are rendered off the main thread, one page at a time (the
 * renderer allows only one open page), and recycled as rows scroll away.
 */
class PdfPagesAdapter(
    private val file: File,
    private val pageWidthPx: Int,
    private val pageDescription: (Int) -> String
) : RecyclerView.Adapter<PdfPagesAdapter.PageHolder>() {

    /** Called on the main thread once the document is open. */
    var onReady: (() -> Unit)? = null

    /** Called on the main thread when the file cannot be opened as a PDF. */
    var onError: (() -> Unit)? = null

    private val main = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var pageSizes: List<Pair<Int, Int>> = emptyList()

    @Volatile
    private var closed = false

    /** Opens the document and measures the pages in the background. */
    fun open() {
        executor.execute {
            try {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                descriptor = fd
                val pdf = PdfRenderer(fd)
                renderer = pdf
                val sizes = ArrayList<Pair<Int, Int>>(pdf.pageCount)
                for (index in 0 until pdf.pageCount) {
                    val page = pdf.openPage(index)
                    try {
                        sizes.add(page.width to page.height)
                    } finally {
                        page.close()
                    }
                }
                main.post {
                    if (closed) return@post
                    if (sizes.isEmpty()) {
                        onError?.invoke()
                    } else {
                        pageSizes = sizes
                        notifyDataSetChanged()
                        onReady?.invoke()
                    }
                }
            } catch (e: Exception) {
                main.post { if (!closed) onError?.invoke() }
            }
        }
    }

    /** Releases the renderer; the adapter must not be used afterwards. */
    fun close() {
        closed = true
        executor.execute {
            try {
                renderer?.close()
                descriptor?.close()
            } catch (ignored: Exception) {
            }
            renderer = null
            descriptor = null
        }
        executor.shutdown()
    }

    override fun getItemCount(): Int = pageSizes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val image = LayoutInflater.from(parent.context).inflate(R.layout.pay_item_pdf_page, parent, false) as ImageView
        return PageHolder(image)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val (width, height) = pageSizes[position]
        val pageHeightPx = (pageWidthPx.toFloat() * height / width).roundToInt().coerceAtLeast(1)
        holder.image.layoutParams.height = pageHeightPx
        holder.image.contentDescription = pageDescription(position + 1)
        holder.release()
        val ticket = holder.ticket
        render(position, pageHeightPx) { bitmap ->
            if (bitmap == null) return@render
            if (holder.ticket != ticket) {
                bitmap.recycle()
            } else {
                holder.bitmap = bitmap
                holder.image.setImageBitmap(bitmap)
            }
        }
    }

    override fun onViewRecycled(holder: PageHolder) {
        holder.release()
        super.onViewRecycled(holder)
    }

    private fun render(position: Int, heightPx: Int, done: (Bitmap?) -> Unit) {
        if (closed) return
        try {
            executor.execute {
                var bitmap: Bitmap? = null
                try {
                    val pdf = renderer
                    if (!closed && pdf != null) {
                        bitmap = Bitmap.createBitmap(pageWidthPx, heightPx, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        val page = pdf.openPage(position)
                        try {
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        } finally {
                            page.close()
                        }
                    }
                } catch (e: Throwable) {
                    bitmap?.recycle()
                    bitmap = null
                }
                val result = bitmap
                main.post {
                    if (closed) result?.recycle() else done(result)
                }
            }
        } catch (e: Exception) {
            // The executor was shut down while the dialog was closing.
        }
    }

    class PageHolder(val image: ImageView) : RecyclerView.ViewHolder(image) {
        var bitmap: Bitmap? = null

        /** Bumped whenever the holder is rebound or recycled, so a late render is dropped. */
        var ticket = 0

        fun release() {
            ticket++
            image.setImageDrawable(null)
            bitmap?.recycle()
            bitmap = null
        }
    }
}
