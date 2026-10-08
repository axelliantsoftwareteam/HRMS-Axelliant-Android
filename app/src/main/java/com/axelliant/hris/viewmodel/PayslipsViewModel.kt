package com.axelliant.hris.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.payslip.PayslipFormat
import com.axelliant.hris.model.payslip.PayslipPreview
import com.axelliant.hris.model.payslip.SalarySlip
import com.axelliant.hris.model.payslip.SalarySlipPage
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.BaseCallBack
import com.axelliant.hris.network.enqueueResult
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** What went wrong, so the screen can show the right copy. */
enum class PayslipError { LOAD_FAILED, PREVIEW_FAILED }

/** Pay slips list (12 per page, loaded as the user scrolls) and the PDF download behind "View". */
class PayslipsViewModel(private val api: ApiInterface) : BaseViewModel() {

    data class UiState(
        val items: List<SalarySlip> = emptyList(),
        val total: Int = 0,
        val hasLoaded: Boolean = false,
        val loadFailed: Boolean = false,
        val loadingMore: Boolean = false,
        val refreshing: Boolean = false,
        /** Name of the slip whose PDF is downloading; every View button is disabled meanwhile. */
        val openingName: String? = null
    )

    private val _state = MutableLiveData(UiState())
    val state: LiveData<UiState> get() = _state

    private val _errors = MutableLiveData<Event<PayslipError>>()
    val errors: LiveData<Event<PayslipError>> get() = _errors

    private val _preview = MutableLiveData<Event<PayslipPreview>>()
    val preview: LiveData<Event<PayslipPreview>> get() = _preview

    private val main = Handler(Looper.getMainLooper())
    private var io: ExecutorService? = null

    /** Bumped on every reload so a slow older page can never overwrite newer data. */
    private var generation = 0
    private var reloading = false
    private var exhausted = false
    private var loadMoreFailed = false

    private fun current() = _state.value ?: UiState()

    /** Loads the first page again, replacing the list. [pull] is true for pull-to-refresh. */
    fun reload(pull: Boolean = false) {
        val before = current()
        if (reloading) {
            if (pull) _state.value = before.copy(refreshing = true)
            return
        }
        reloading = true
        generation++
        val myGeneration = generation
        val showOverlay = !pull && before.items.isEmpty()
        if (showOverlay) isLoading.value = Event(true)
        _state.value = before.copy(refreshing = pull, loadingMore = false)
        api.getSalarySlips(AppConst.authHeader(), PAGE_SIZE, 0)
            .enqueueResult<SalarySlipPage>(SalarySlipPage::class.java) { result ->
                if (myGeneration != generation) return@enqueueResult
                reloading = false
                if (showOverlay) isLoading.value = Event(false)
                val latest = current()
                when (result) {
                    is ApiResult.Success -> {
                        val slips = result.data.slips().distinctBy { it.name }
                        exhausted = false
                        loadMoreFailed = false
                        _state.value = latest.copy(
                            items = slips,
                            total = result.data.totalCount(),
                            hasLoaded = true,
                            loadFailed = false,
                            refreshing = false
                        )
                    }
                    is ApiResult.Failure -> {
                        _state.value = latest.copy(
                            hasLoaded = true,
                            loadFailed = latest.items.isEmpty(),
                            refreshing = false
                        )
                        _errors.value = Event(PayslipError.LOAD_FAILED)
                    }
                }
            }
    }

    /** Loads the next page; call when the last slip is on screen. */
    fun loadMore() {
        val s = current()
        if (!s.hasLoaded || s.loadFailed || s.loadingMore || s.refreshing || reloading) return
        if (exhausted || loadMoreFailed || s.items.size >= s.total) return
        val myGeneration = generation
        _state.value = s.copy(loadingMore = true)
        api.getSalarySlips(AppConst.authHeader(), PAGE_SIZE, s.items.size)
            .enqueueResult<SalarySlipPage>(SalarySlipPage::class.java) { result ->
                if (myGeneration != generation) return@enqueueResult
                val latest = current()
                when (result) {
                    is ApiResult.Success -> {
                        val known = latest.items.mapNotNull { it.name }.toHashSet()
                        val fresh = result.data.slips().filter { known.add(it.name.orEmpty()) }
                        if (fresh.isEmpty()) exhausted = true
                        _state.value = latest.copy(
                            items = latest.items + fresh,
                            total = result.data.totalCount(),
                            loadingMore = false
                        )
                    }
                    is ApiResult.Failure -> {
                        loadMoreFailed = true
                        _state.value = latest.copy(loadingMore = false)
                        _errors.value = Event(PayslipError.LOAD_FAILED)
                    }
                }
            }
    }

    /** Downloads the slip's PDF into [cacheDir] and posts it to [preview]. */
    fun open(slip: SalarySlip, cacheDir: File) {
        val name = slip.name ?: return
        if (current().openingName != null) return
        _state.value = current().copy(openingName = name)
        val title = PayslipFormat.period(slip)
        val call = api.getSalarySlipPdf(AppConst.authHeader(), name)
        call.enqueue(object : BaseCallBack<ResponseBody>(call) {
            override fun onFinalSuccess(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val body = response.body()
                executor().execute {
                    val file = savePdf(body, name, cacheDir)
                    main.post { finishOpening(if (file != null) PayslipPreview(title, file.absolutePath) else null) }
                }
            }

            override fun onFinalFailure(errorString: String?) {
                finishOpening(null)
            }
        })
    }

    private fun finishOpening(preview: PayslipPreview?) {
        _state.value = current().copy(openingName = null)
        if (preview != null) _preview.value = Event(preview) else _errors.value = Event(PayslipError.PREVIEW_FAILED)
    }

    private fun executor(): ExecutorService =
        io ?: Executors.newSingleThreadExecutor().also { io = it }

    /** Writes the body to `payslips/Payslip-<name>.pdf`; null when it is not a PDF or cannot be saved. */
    private fun savePdf(body: ResponseBody?, name: String, cacheDir: File): File? {
        if (body == null) return null
        return try {
            body.use { responseBody ->
                val dir = File(cacheDir, "payslips")
                if (!dir.exists() && !dir.mkdirs()) return null
                val target = File(dir, "Payslip-${PayslipFormat.safeFileName(name)}.pdf")
                val partial = File(dir, target.name + ".part")
                responseBody.byteStream().use { input ->
                    FileOutputStream(partial).use { output -> input.copyTo(output) }
                }
                if (!looksLikePdf(partial)) {
                    partial.delete()
                    return null
                }
                if (target.exists()) target.delete()
                if (partial.renameTo(target)) target else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** A PDF starts with "%PDF"; anything else (an error page, an empty body) is not a slip. */
    private fun looksLikePdf(file: File): Boolean {
        if (file.length() <= 4) return false
        return try {
            file.inputStream().use { stream ->
                val head = ByteArray(4)
                stream.read(head) == 4 && String(head, Charsets.ISO_8859_1) == "%PDF"
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun onCleared() {
        io?.shutdown()
        super.onCleared()
    }

    companion object {
        const val PAGE_SIZE = 12
    }
}
