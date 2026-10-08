package com.axelliant.hris.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.axelliant.hris.R
import com.intuit.sdp.R as SdpR
import com.axelliant.hris.databinding.SheetWsDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

/**
 * Native Material bottom sheet used for every "details" popup: blue header band (title, subtitle,
 * close), an optional full-width status bar, label/value fields (two [Field.half] fields share a
 * row) and optional action buttons.
 *
 * Everything lives in the fragment arguments, so the sheet survives rotation. Action clicks are
 * delivered through the Fragment Result API:
 *
 * ```
 * WsDetailSheet.show(childFragmentManager, "Check-in", "Checkin details", "Approved", fields,
 *     actions = listOf(WsDetailSheet.Action("edit", "Edit request")), requestKey = "leaveSheet")
 * childFragmentManager.setFragmentResultListener("leaveSheet", viewLifecycleOwner) { _, b ->
 *     if (b.getString(WsDetailSheet.RESULT_ACTION) == "edit") { ... }
 * }
 * ```
 */
class WsDetailSheet : BottomSheetDialogFragment() {

    /** One label/value pair. [half] fields pair up side by side; [accent] draws the value in red. */
    data class Field(val label: String, val value: String?, val half: Boolean = false, val accent: Boolean = false)

    /** A button under the fields. [style] is one of [STYLE_PRIMARY], [STYLE_OUTLINE], [STYLE_DANGER]. */
    data class Action(val id: String, val label: String, val style: Int = STYLE_PRIMARY)

    private var _binding: SheetWsDetailBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.Ws_BottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetWsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        binding.wsSheetTitle.text = args.getString(ARG_TITLE)
        binding.wsSheetSubtitle.text = args.getString(ARG_SUBTITLE)
        binding.wsSheetSubtitle.visibility =
            if (binding.wsSheetSubtitle.text.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.wsSheetClose.setOnClickListener { dismiss() }

        args.getString(ARG_STATUS)?.takeIf { it.isNotBlank() }?.let {
            binding.wsSheetStatus.visibility = View.VISIBLE
            WsStatus.badge(binding.wsSheetStatus, it)
            (binding.wsSheetStatus.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                resources.getDimensionPixelSize(SdpR.dimen._6sdp)
        }

        bindFields(args)
        bindActions(args)
    }

    private fun bindFields(args: Bundle) {
        val labels = args.getStringArray(ARG_LABELS) ?: return
        val values = args.getStringArray(ARG_VALUES) ?: return
        val halves = args.getBooleanArray(ARG_HALVES) ?: BooleanArray(labels.size)
        val accents = args.getBooleanArray(ARG_ACCENTS) ?: BooleanArray(labels.size)
        val gap = resources.getDimensionPixelSize(SdpR.dimen._6sdp)

        var i = 0
        while (i < labels.size) {
            if (values[i].isBlank() || values[i] == "--") {
                i++
                continue
            }
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(fieldView(labels[i], values[i], accents[i]), weighted())
            var consumed = 1
            if (halves[i]) {
                val j = (i + 1 until labels.size).firstOrNull { values[it].isNotBlank() && values[it] != "--" && halves[it] }
                if (j != null && j == i + 1) {
                    row.addView(fieldView(labels[j], values[j], accents[j]), weighted(gap))
                    consumed = 2
                } else {
                    row.addView(View(requireContext()), weighted(gap))
                }
            }
            binding.wsSheetFields.addView(
                row,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = gap + gap / 2 }
            )
            i += consumed
        }
    }

    private fun weighted(startMargin: Int = 0) =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = startMargin }

    private fun fieldView(label: String, value: String, accent: Boolean): View {
        val box = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val l = TextView(requireContext(), null, 0, R.style.Ws_Text_Label).apply { text = label }
        val v = TextView(requireContext(), null, 0, R.style.Ws_Text_Body).apply {
            text = value
            setTextColor(requireContext().getColor(if (accent) R.color.ws_red else R.color.black))
            setPadding(0, resources.getDimensionPixelSize(SdpR.dimen._2sdp), 0, 0)
        }
        box.addView(l)
        box.addView(v)
        return box
    }

    private fun bindActions(args: Bundle) {
        val ids = args.getStringArray(ARG_ACTION_IDS) ?: return
        val labels = args.getStringArray(ARG_ACTION_LABELS) ?: return
        val styles = args.getIntArray(ARG_ACTION_STYLES) ?: IntArray(ids.size)
        if (ids.isEmpty()) return
        binding.wsSheetActions.visibility = View.VISIBLE
        val requestKey = args.getString(ARG_REQUEST_KEY).orEmpty()
        val gap = resources.getDimensionPixelSize(SdpR.dimen._6sdp)
        ids.forEachIndexed { index, id ->
            val button = MaterialButton(requireContext()).apply {
                text = labels[index]
                isAllCaps = false
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.poppins_medium)
                cornerRadius = resources.getDimensionPixelSize(SdpR.dimen._6sdp)
                minimumHeight = resources.getDimensionPixelSize(SdpR.dimen._32sdp)
                when (styles[index]) {
                    STYLE_OUTLINE -> {
                        setTextColor(context.getColor(R.color.ws_primary))
                        backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.white))
                        strokeColor = android.content.res.ColorStateList.valueOf(context.getColor(R.color.ws_primary))
                        strokeWidth = resources.getDimensionPixelSize(SdpR.dimen._1sdp) * 2
                    }
                    STYLE_DANGER -> {
                        setTextColor(context.getColor(R.color.ws_red))
                        strokeColor = android.content.res.ColorStateList.valueOf(context.getColor(R.color.ws_red))
                        strokeWidth = resources.getDimensionPixelSize(SdpR.dimen._1sdp) * 2
                        backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.white))
                    }
                    else -> {
                        setTextColor(context.getColor(R.color.white))
                        backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.ws_primary))
                    }
                }
                setOnClickListener {
                    parentFragmentManager.setFragmentResult(requestKey, bundleOf(RESULT_ACTION to id))
                    dismiss()
                }
            }
            binding.wsSheetActions.addView(
                button,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { if (index > 0) marginStart = gap }
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_ACTION = "action"
        const val STYLE_PRIMARY = 0
        const val STYLE_OUTLINE = 1
        const val STYLE_DANGER = 2

        private const val TAG = "WsDetailSheet"
        private const val ARG_TITLE = "title"
        private const val ARG_SUBTITLE = "subtitle"
        private const val ARG_STATUS = "status"
        private const val ARG_LABELS = "labels"
        private const val ARG_VALUES = "values"
        private const val ARG_HALVES = "halves"
        private const val ARG_ACCENTS = "accents"
        private const val ARG_ACTION_IDS = "actionIds"
        private const val ARG_ACTION_LABELS = "actionLabels"
        private const val ARG_ACTION_STYLES = "actionStyles"
        private const val ARG_REQUEST_KEY = "requestKey"

        /** Shows the sheet. Pass the same [FragmentManager] you register the result listener on. */
        fun show(
            fm: FragmentManager,
            title: String,
            subtitle: String? = null,
            status: String? = null,
            fields: List<Field> = emptyList(),
            actions: List<Action> = emptyList(),
            requestKey: String = TAG
        ) {
            if (fm.findFragmentByTag(TAG) != null || fm.isStateSaved) return
            WsDetailSheet().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_SUBTITLE to subtitle,
                    ARG_STATUS to status,
                    ARG_LABELS to fields.map { it.label }.toTypedArray(),
                    ARG_VALUES to fields.map { it.value.orEmpty() }.toTypedArray(),
                    ARG_HALVES to fields.map { it.half }.toBooleanArray(),
                    ARG_ACCENTS to fields.map { it.accent }.toBooleanArray(),
                    ARG_ACTION_IDS to actions.map { it.id }.toTypedArray(),
                    ARG_ACTION_LABELS to actions.map { it.label }.toTypedArray(),
                    ARG_ACTION_STYLES to actions.map { it.style }.toIntArray(),
                    ARG_REQUEST_KEY to requestKey
                )
            }.show(fm, TAG)
        }
    }
}
