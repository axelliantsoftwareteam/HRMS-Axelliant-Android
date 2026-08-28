package com.axelliant.hris.features.inventory.products.presentation

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.hideKeyboard
import com.axelliant.hris.core.scanner.ProductCodeScannerResult
import com.axelliant.hris.databinding.FragmentAskAiProductSearchBinding
import com.axelliant.hris.features.inventory.products.ai.ParsedProductAiFilter
import com.axelliant.hris.features.inventory.products.ai.ProductAiCategorySpecSuggestion
import com.axelliant.hris.features.inventory.products.ai.ProductAiAvailability
import com.axelliant.hris.features.inventory.products.ai.ProductAiConfidenceLevel
import com.axelliant.hris.features.inventory.products.ai.ProductAiFilterOption
import com.axelliant.hris.features.inventory.products.ai.ProductAiFilterType
import com.axelliant.hris.features.inventory.products.ai.ProductAiStatus
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.SelectedCategorySpecs
import com.axelliant.hris.features.quotes.presentation.SmartQuoteProductFlow
import com.axelliant.hris.ui.designsystem.components.AppButtonView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.axelliant.hris.ui.designsystem.components.AppTextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.Reader
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class AskAiProductSearchFragment : Fragment() {
    private val viewModel: AskAiSearchViewModel by viewModels()
    private val productsViewModel: ProductsViewModel by viewModels()
    private var _binding: FragmentAskAiProductSearchBinding? = null
    private val binding get() = _binding!!

    private var speechRecognizer: SpeechRecognizer? = null
    private var selectedEditStatus: ProductAiStatus? = null
    private var selectedEditAvailability: ProductAiAvailability? = null
    private var ocrImageUri: Uri? = null
    private var selectedInputMode = AskAiInputMode.Voice
    private var suppressClarificationSearchWatcher = false
    private var clarificationOptionsRenderKey: String? = null
    private val clarificationOptionViews = mutableMapOf<String, View>()
    private var pendingClarificationAnimationDirection = CLARIFICATION_DIRECTION_NONE
    private var lastClarificationQuestionId: String? = null
    private var isClarificationTransitionRunning = false
    private var clarificationTouchStartX = 0f
    private var clarificationTouchStartY = 0f
    private var lastClarificationSelectedOptionIds = emptySet<String>()
    private var lastClarificationPeekHeight = 0
    private var peekShimmerAnimatorOne: ObjectAnimator? = null
    private var peekShimmerAnimatorTwo: ObjectAnimator? = null
    private var uploadedInputFileName: String? = null
    private var uploadInputProcessing = false

    private val ocrCameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { captured ->
        val uri = ocrImageUri
        if (captured && uri != null) {
            extractTextFromImage(uri)
        } else {
            viewModel.updateInputError(getString(R.string.ask_ai_ocr_cancelled))
        }
    }

    private val ocrDocumentScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK) {
            viewModel.updateInputError(getString(R.string.ask_ai_ocr_cancelled))
            return@registerForActivityResult
        }

        val scannedImageUri = GmsDocumentScanningResult
            .fromActivityResultIntent(activityResult.data)
            ?.getPages()
            ?.firstOrNull()
            ?.getImageUri()
        if (scannedImageUri == null) {
            viewModel.updateInputError(getString(R.string.ask_ai_ocr_error))
        } else {
            extractTextFromImage(scannedImageUri)
        }
    }

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceInput()
        } else {
            viewModel.updateVoiceError(getString(R.string.ask_ai_voice_permission_denied))
        }
    }

    private val uploadFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            setInputProcessing(processing = false)
            viewModel.updateInputError(getString(R.string.ask_ai_upload_cancelled))
            return@registerForActivityResult
        }
        extractTextFromUploadedFile(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAskAiProductSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupExamples()
        setupInteractions()
        observeState()
        observeScannerResult()
    }

    private fun setupInteractions() {
        binding.backButton.setOnClickListener {
            if (viewModel.uiState.value is AskAiSearchUiState.Clarifying) {
                moveClarificationPrevious()
            } else if (!viewModel.handleClarificationBack()) {
                findNavController().navigateUp()
            }
        }
        binding.retryHeaderButton.setOnClickListener { restartInputFromReview() }
        binding.resetEditButton.setOnClickListener { resetEditFieldsFromCurrentState() }
        binding.micButton.setOnClickListener { startSelectedInputMode() }
        binding.uploadFilePanel.setOnClickListener { startUploadFilePicker() }
        binding.inputMethodsMenuButton.setOnClickListener { showInputMethodsMenu() }
        binding.generateFiltersButton.setOnClickListener {
            submitQuery()
        }
        binding.queryInputLayout.isEndIconVisible = binding.queryEditText.text?.isNotEmpty() == true
        binding.queryInputLayout.setEndIconOnClickListener {
            binding.queryEditText.setText("")
        }
        binding.queryEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onQueryChanged(text?.toString().orEmpty())
            }
            override fun afterTextChanged(editable: Editable?) {
                binding.queryInputLayout.isEndIconVisible = editable?.isNotEmpty() == true
            }
        })
        binding.queryEditText.setOnEditorActionListener { _, actionId, event ->
            val isSubmitAction = actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE
            val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_UP
            if (isSubmitAction || isEnterKey) {
                submitQuery()
                true
            } else {
                false
            }
        }
        binding.editFiltersButton.setOnClickListener { showFilterSheetForCurrentReview() }
        binding.clarificationBackButton.setOnClickListener { moveClarificationPrevious() }
        binding.clarificationSkipButton.setOnClickListener {
            moveClarificationNext(skipIfEmpty = true, forceSkip = true)
        }
        binding.clarificationNextButton.setOnClickListener {
            moveClarificationNext(skipIfEmpty = false)
        }
        binding.clarificationSwipeBackHint.setOnClickListener { moveClarificationPrevious() }
        binding.clarificationSwipeNextHint.setOnClickListener {
            moveClarificationNext(skipIfEmpty = true)
        }
        binding.clarificationSearchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                if (!suppressClarificationSearchWatcher) {
                    viewModel.updateClarificationSearchQuery(text?.toString().orEmpty())
                }
            }
            override fun afterTextChanged(editable: Editable?) = Unit
        })
        binding.clarificationContainer.setOnTouchListener { _, event ->
            handleClarificationTouch(event)
        }
    }

    private fun submitQuery() {
        binding.queryEditText.hideKeyboard()
        binding.queryEditText.clearFocus()
        viewModel.generateFilters(binding.queryEditText.text?.toString().orEmpty())
    }

    private fun setupExamples() {
        val examples = listOf(
            R.string.ask_ai_example_cisco,
            R.string.ask_ai_example_dell,
            R.string.ask_ai_example_hp,
            R.string.ask_ai_example_active
        ).map(::getString)

        binding.exampleChipsContainer.removeAllViews()
        examples.forEach { example ->
            binding.exampleChipsContainer.addView(
                createTextChip(
                    text = example,
                    selected = false,
                    removable = false,
                    onClick = {
                        binding.queryEditText.setText(example)
                        binding.queryEditText.setSelection(example.length)
                        viewModel.useExample(example)
                    }
                )
            )
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderState)
            }
        }
    }

    private fun renderState(state: AskAiSearchUiState) {
        binding.inputContainer.isVisible = state is AskAiSearchUiState.Input
        binding.loadingContainer.isVisible = state is AskAiSearchUiState.Loading
        binding.reviewContainer.isVisible = state is AskAiSearchUiState.Review
        binding.clarificationOverlay.isVisible = state is AskAiSearchUiState.Clarifying
        binding.clarificationContainer.isVisible = state is AskAiSearchUiState.Clarifying
        binding.resetEditButton.isVisible = false
        binding.retryHeaderButton.isVisible = false
        binding.inputMethodsMenuButton.isVisible = state is AskAiSearchUiState.Input
        if (state !is AskAiSearchUiState.Clarifying) {
            clarificationOptionsRenderKey = null
            clarificationOptionViews.clear()
            lastClarificationSelectedOptionIds = emptySet()
            lastClarificationPeekHeight = 0
            setClarificationPeekLoading(false)
            lastClarificationQuestionId = null
            pendingClarificationAnimationDirection = CLARIFICATION_DIRECTION_NONE
            isClarificationTransitionRunning = false
            resetClarificationCardTransform()
        }

        when (state) {
            is AskAiSearchUiState.Input -> renderInputState(state)
            is AskAiSearchUiState.Loading -> renderLoadingState(state)
            is AskAiSearchUiState.Review -> renderReviewState(state)
            is AskAiSearchUiState.Clarifying -> renderClarificationState(state)
        }
    }

    private fun renderLoadingState(state: AskAiSearchUiState.Loading) {
        val isRefining = state.mode == AskAiLoadingMode.Refining
        binding.screenTitleText.text = getString(
            if (isRefining) R.string.ask_ai_refine_title else R.string.ask_ai
        )
        binding.loadingTitleText.text = getString(
            if (isRefining) R.string.ask_ai_refine_preparing else R.string.ask_ai_understanding
        )
        binding.loadingSubtitleText.text = getString(
            if (isRefining) R.string.ask_ai_refine_preparing_details else R.string.ask_ai_extracting_details
        )
    }

    private fun renderInputState(state: AskAiSearchUiState.Input) {
        binding.screenTitleText.text = getString(R.string.ask_ai)
        if (binding.queryEditText.text?.toString() != state.query) {
            binding.queryEditText.setText(state.query)
            binding.queryEditText.setSelection(state.query.length)
        }
        binding.inputErrorText.isVisible = state.errorMessage != null
        binding.inputErrorText.text = state.errorMessage.orEmpty()
        renderInputMode()
    }

    private fun renderReviewState(state: AskAiSearchUiState.Review) {
        val parsed = state.parsed
        selectedEditStatus = parsed.status
        selectedEditAvailability = parsed.availability
        binding.screenTitleText.text = getString(
            if (state.isEditing) R.string.ask_ai_edit_filters else R.string.ask_ai_review_title
        )
        binding.resetEditButton.isVisible = state.isEditing
        binding.retryHeaderButton.isVisible = !state.isEditing
        binding.confidenceLabel.text = getString(confidenceLabelRes(parsed.confidenceLevel))
        binding.confidenceLabel.setTextColor(confidenceColor(parsed.confidenceLevel))
        binding.confidenceIcon.setColorFilter(confidenceColor(parsed.confidenceLevel))
        binding.confidenceBadge.text = getString(
            R.string.ask_ai_refine_confidence_score,
            parsed.confidenceScore
        )
        binding.reviewHeaderRow.backgroundTintList = ColorStateList.valueOf(
            confidenceBackgroundColor(parsed.confidenceLevel)
        )
        binding.reviewHeaderRow.isVisible = !state.isEditing
        binding.detectedFiltersTitleRow.isVisible = !state.isEditing
        binding.detectedFiltersContainer.isVisible = !state.isEditing
        binding.reasonsContainer.isVisible = false
        binding.warningsContainer.isVisible = false
        binding.noDetectedFiltersText.isVisible = false
        binding.detectedFiltersContainer.removeAllViews()
        if (!state.isEditing) {
            addDetectedFilterRows(parsed = parsed, editing = false)
            binding.noDetectedFiltersText.isVisible = binding.detectedFiltersContainer.isEmpty()
            renderReasons(parsed.reasons)
            renderWarnings((parsed.warnings + state.warningsOverride).distinct())
        }
        binding.promptInfoButton.setOnClickListener {
            showOriginalPrompt(parsed.originalQuery)
        }
        renderEditControls(state)

        binding.primaryReviewButton.isVisible = state.isEditing
        binding.reviewActionRow.isVisible = !state.isEditing
        binding.tryAgainButton.isVisible = !state.isEditing
        binding.tryAgainButton.text = when {
            parsed.confidenceLevel == ProductAiConfidenceLevel.Medium || state.hasUserEdited ->
                getString(R.string.ask_ai_confirm_apply)
            else -> getString(R.string.ask_ai_apply_filters)
        }
        binding.tryAgainButton.setOnClickListener {
            applyParsedFilters(parsed)
        }
        binding.refineSearchButton.setOnClickListener {
            viewModel.startClarificationFlow()
        }
        binding.primaryReviewButton.setOnClickListener {
            when {
                state.isEditing -> {
                    saveEditValues()
                    (viewModel.uiState.value as? AskAiSearchUiState.Review)?.let { updatedState ->
                        applyParsedFilters(updatedState.parsed)
                    }
                }
                else -> applyParsedFilters(parsed)
            }
        }
        binding.editFiltersButton.isVisible = !state.isEditing
    }

    private fun renderClarificationState(state: AskAiSearchUiState.Clarifying) {
        val question = state.currentQuestion ?: return
        val questionChanged = lastClarificationQuestionId != question.id
        binding.screenTitleText.text = getString(R.string.ask_ai_refine_title)
        binding.inputMethodsMenuButton.isVisible = false
        binding.retryHeaderButton.isVisible = false
        binding.resetEditButton.isVisible = false

        binding.clarificationStepText.text = getString(
            R.string.ask_ai_refine_step_format,
            state.currentIndex + 1,
            state.questions.size
        )
        binding.clarificationProgress.progress = state.progressPercent
        renderClarificationPeekCard(state)
        val showEyebrow = question.type !in listOf(
            ProductAiClarificationType.Vendor,
            ProductAiClarificationType.Manufacturer,
            ProductAiClarificationType.Category
        ) && question.eyebrow.isNotBlank()
        binding.clarificationCategoryText.isVisible = showEyebrow
        binding.clarificationCategoryText.text = question.eyebrow
        binding.clarificationQuestionText.text = question.title
        binding.clarificationSubtitleText.text = question.subtitle
        binding.clarificationSubtitleText.isVisible = question.subtitle.isNotBlank()

        val showSearch = question.type == ProductAiClarificationType.CategorySpec &&
            question.options.size > CLARIFICATION_SEARCH_THRESHOLD
        binding.clarificationSearchInputLayout.isVisible = showSearch
        binding.clarificationSearchInputLayout.hint =
            question.searchHint ?: getString(R.string.ask_ai_refine_search_hint)
        if (binding.clarificationSearchEditText.text?.toString() != state.optionQuery) {
            suppressClarificationSearchWatcher = true
            binding.clarificationSearchEditText.setText(state.optionQuery)
            binding.clarificationSearchEditText.setSelection(state.optionQuery.length)
            suppressClarificationSearchWatcher = false
        }

        renderClarificationOptions(state)
        renderClarificationSelection(state)

        val hasSelection = state.currentAnswer.selectedOptionIds.isNotEmpty()
        binding.clarificationNextButton.isEnabled = hasSelection
        binding.clarificationNextButton.alpha = if (hasSelection) 1f else DISABLED_BUTTON_ALPHA
        binding.clarificationNextButton.text = getString(
            if (state.isLastQuestion) R.string.ask_ai_refine_apply else R.string.ask_ai_refine_next
        )
        binding.clarificationSkipButton.text = getString(
            if (state.isLastQuestion) R.string.ask_ai_refine_skip_apply else R.string.ask_ai_refine_skip
        )
        binding.clarificationSwipeBackHint.isVisible = state.currentIndex > 0
        binding.clarificationSwipeNextHint.isVisible = state.currentIndex < state.questions.lastIndex

        if (questionChanged) {
            lastClarificationQuestionId = question.id
            val direction = pendingClarificationAnimationDirection
            pendingClarificationAnimationDirection = CLARIFICATION_DIRECTION_NONE
            binding.clarificationContainer.post {
                if (direction == CLARIFICATION_DIRECTION_NONE) {
                    animateClarificationCardAppear()
                } else {
                    animateClarificationCardIn(direction)
                }
            }
        }
    }

    private fun renderClarificationOptions(state: AskAiSearchUiState.Clarifying) {
        val question = state.currentQuestion ?: return
        val selectedIds = state.currentAnswer.selectedOptionIds
        val options = state.visibleOptions()
        val renderKey = listOf(
            question.id,
            state.optionQuery,
            options.joinToString(separator = "|") { it.id }
        ).joinToString(separator = "::")
        if (renderKey == clarificationOptionsRenderKey) {
            updateClarificationOptionSelections(selectedIds)
            return
        }

        clarificationOptionsRenderKey = renderKey
        clarificationOptionViews.clear()
        lastClarificationSelectedOptionIds = emptySet()
        binding.clarificationOptionsContainer.removeAllViews()
        if (options.isEmpty()) {
            binding.clarificationOptionsContainer.addView(createClarificationMessageRow())
            return
        }
        binding.clarificationOptionsContainer.post {
            val latestState = viewModel.uiState.value as? AskAiSearchUiState.Clarifying ?: return@post
            if (latestState.currentQuestion?.id != question.id || latestState.optionQuery != state.optionQuery) {
                return@post
            }
            addClarificationOptionRows(
                options = latestState.visibleOptions(),
                selectedIds = latestState.currentAnswer.selectedOptionIds
            )
        }
    }

    private fun createClarificationOptionRow(
        option: ProductAiClarificationOption,
        selected: Boolean,
        itemWidth: Int
    ): View {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                itemWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
                bottomMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)
            }
            minimumHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._34sdp)
            setBackgroundResource(
                if (selected) R.drawable.bg_ask_ai_refine_option_selected else R.drawable.bg_ask_ai_refine_option
            )
            isClickable = true
            isFocusable = true
            foreground = android.util.TypedValue().let { typedValue ->
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                ContextCompat.getDrawable(context, typedValue.resourceId)
            }
            setOnClickListener { viewModel.selectClarificationOption(option.id) }
            gravity = android.view.Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)
            )
            addView(AppTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = option.label
                maxLines = CLARIFICATION_OPTION_LABEL_MAX_LINES
                ellipsize = TextUtils.TruncateAt.END
                gravity = android.view.Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, if (selected) R.color.ds_primary else R.color.ds_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._8ssp))
                typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
            })
            updateClarificationOptionView(this, selected)
        }
    }

    private fun updateClarificationOptionSelections(selectedIds: List<String>) {
        val nextSelectedIds = selectedIds.toSet()
        val changedIds = lastClarificationSelectedOptionIds + nextSelectedIds
        changedIds.forEach { optionId ->
            clarificationOptionViews[optionId]?.let { view ->
                updateClarificationOptionView(view, optionId in nextSelectedIds)
            }
        }
        lastClarificationSelectedOptionIds = nextSelectedIds
    }

    private fun updateClarificationOptionView(view: View, selected: Boolean) {
        view.setBackgroundResource(
            if (selected) R.drawable.bg_ask_ai_refine_option_selected else R.drawable.bg_ask_ai_refine_option
        )
        val textView = (view as? LinearLayout)?.getChildAt(0) as? AppTextView
        textView?.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.ds_primary else R.color.ds_text_primary
            )
        )
    }

    private fun addClarificationOptionRows(
        options: List<ProductAiClarificationOption>,
        selectedIds: List<String>
    ) {
        binding.clarificationOptionsContainer.removeAllViews()
        if (options.isEmpty()) {
            binding.clarificationOptionsContainer.addView(createClarificationMessageRow())
            return
        }

        val maxRowWidth = binding.clarificationOptionsContainer.width
            .takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels
        val gap = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
        val minItemWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._68sdp)
        val horizontalPadding = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
        val textPaint = android.text.TextPaint().apply {
            textSize = resources.getDimension(com.intuit.ssp.R.dimen._8ssp)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
        }

        var currentRow = createClarificationOptionRowContainer()
        var currentRowWidth = 0
        options.forEach { option ->
            val itemWidth = (textPaint.measureText(option.label).toInt() + horizontalPadding)
                .coerceAtLeast(minItemWidth)
                .coerceAtMost(maxRowWidth)
            val itemTotalWidth = itemWidth + gap
            val nextWidth = currentRowWidth + itemTotalWidth
            if (currentRow.childCount > 0 && nextWidth > maxRowWidth) {
                binding.clarificationOptionsContainer.addView(currentRow)
                currentRow = createClarificationOptionRowContainer()
                currentRowWidth = 0
            }
            currentRow.addView(
                createClarificationOptionRow(
                    option = option,
                    selected = option.id in selectedIds,
                    itemWidth = itemWidth
                ).also { view -> clarificationOptionViews[option.id] = view }
            )
            currentRowWidth += itemTotalWidth
        }
        if (currentRow.childCount > 0) {
            binding.clarificationOptionsContainer.addView(currentRow)
        }
        lastClarificationSelectedOptionIds = selectedIds.toSet()
        binding.clarificationContainer.post { syncClarificationPeekCardSize() }
    }

    private fun createClarificationOptionRowContainer(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }
    }

    private fun createClarificationMessageRow(): View {
        return AppTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.bg_ask_ai_warning)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._9sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._9sdp)
            )
            text = getString(R.string.ask_ai_refine_no_options)
            setTextColor(ContextCompat.getColor(context, R.color.ds_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
        }
    }

    private fun renderClarificationSelection(state: AskAiSearchUiState.Clarifying) {
        binding.clarificationSelectionSection.isVisible = false
        binding.clarificationSelectionChips.removeAllViews()
    }

    private fun renderClarificationPeekCard(state: AskAiSearchUiState.Clarifying) {
        val nextQuestion = state.questions.getOrNull(state.currentIndex + 1)
        binding.clarificationPeekCard.isVisible = nextQuestion != null
        binding.clarificationPeekStepText.text = getString(R.string.ask_ai_refine_next_preview)
        binding.clarificationPeekTitleText.text = nextQuestion?.title.orEmpty()
        setClarificationPeekLoading(false)
        binding.clarificationContainer.post { syncClarificationPeekCardSize() }
    }

    private fun syncClarificationPeekCardSize() {
        val safeBinding = _binding ?: return
        if (!safeBinding.clarificationPeekCard.isVisible) return
        val currentHeight = safeBinding.clarificationContainer.height
        if (currentHeight <= 0) return

        if (lastClarificationPeekHeight != currentHeight) {
            lastClarificationPeekHeight = currentHeight
            safeBinding.clarificationPeekCard.layoutParams = safeBinding.clarificationPeekCard.layoutParams.apply {
                height = currentHeight
            }
        }
        safeBinding.clarificationPeekCard.translationX = 0f
        safeBinding.clarificationPeekCard.translationY = 0f
        safeBinding.clarificationPeekCard.rotation = 0f
    }

    private fun setClarificationPeekLoading(loading: Boolean) {
        val safeBinding = _binding ?: return
        val showLoading = loading && safeBinding.clarificationPeekCard.isVisible
        safeBinding.clarificationPeekStepText.isVisible = !showLoading
        safeBinding.clarificationPeekTitleText.isVisible = !showLoading
        safeBinding.clarificationPeekShimmer.isVisible = showLoading
        if (showLoading) {
            startClarificationPeekShimmer()
        } else {
            stopClarificationPeekShimmer()
        }
    }

    private fun startClarificationPeekShimmer() {
        if (peekShimmerAnimatorOne?.isStarted == true) return
        val safeBinding = _binding ?: return
        peekShimmerAnimatorOne = ObjectAnimator.ofFloat(
            safeBinding.clarificationPeekShimmerBarOne,
            View.ALPHA,
            0.35f,
            1f
        ).apply {
            duration = CLARIFICATION_SHIMMER_DURATION_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
        peekShimmerAnimatorTwo = ObjectAnimator.ofFloat(
            safeBinding.clarificationPeekShimmerBarTwo,
            View.ALPHA,
            1f,
            0.35f
        ).apply {
            duration = CLARIFICATION_SHIMMER_DURATION_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopClarificationPeekShimmer() {
        peekShimmerAnimatorOne?.cancel()
        peekShimmerAnimatorTwo?.cancel()
        peekShimmerAnimatorOne = null
        peekShimmerAnimatorTwo = null
        _binding?.clarificationPeekShimmerBarOne?.alpha = 1f
        _binding?.clarificationPeekShimmerBarTwo?.alpha = 1f
    }

    private fun moveClarificationNext(skipIfEmpty: Boolean, forceSkip: Boolean = false) {
        val state = viewModel.uiState.value as? AskAiSearchUiState.Clarifying ?: return
        val hasSelection = state.currentAnswer.selectedOptionIds.isNotEmpty()
        if (!forceSkip && !skipIfEmpty && !hasSelection) return
        animateClarificationCardOut(CLARIFICATION_DIRECTION_NEXT) {
            val result = if (forceSkip || (!hasSelection && skipIfEmpty)) {
                viewModel.skipClarificationQuestion()
            } else {
                viewModel.onClarificationNext()
            }
            result?.let(::applyParsedFilters)
        }
    }

    private fun moveClarificationPrevious() {
        if (viewModel.uiState.value !is AskAiSearchUiState.Clarifying) return
        animateClarificationCardOut(CLARIFICATION_DIRECTION_PREVIOUS) {
            viewModel.handleClarificationBack()
        }
    }

    private fun handleClarificationTouch(
        event: MotionEvent,
        onTap: (() -> Unit)? = null
    ): Boolean {
        if (isClarificationTransitionRunning) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                clarificationTouchStartX = event.rawX
                clarificationTouchStartY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = event.rawX - clarificationTouchStartX
                val deltaY = event.rawY - clarificationTouchStartY
                val absX = abs(deltaX)
                val absY = abs(deltaY)
                val swipeDistance = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._42sdp).toFloat()
                val tapSlop = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp).toFloat()
                return when {
                    absX >= swipeDistance && absX > absY * CLARIFICATION_SWIPE_AXIS_RATIO -> {
                        if (deltaX < 0) {
                            moveClarificationNext(skipIfEmpty = true)
                        } else {
                            moveClarificationPrevious()
                        }
                        true
                    }
                    onTap != null && absX <= tapSlop && absY <= tapSlop -> {
                        onTap()
                        true
                    }
                    else -> onTap != null
                }
            }
            MotionEvent.ACTION_CANCEL -> return onTap != null
        }
        return onTap != null
    }

    private fun animateClarificationCardOut(direction: Int, endAction: () -> Unit) {
        if (isClarificationTransitionRunning) return
        isClarificationTransitionRunning = true
        setClarificationPeekLoading(true)
        val card = binding.clarificationContainer
        val width = card.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        card.animate().cancel()
        card.animate()
            .translationX(direction * width * CLARIFICATION_EXIT_DISTANCE_FACTOR)
            .rotation(direction * CLARIFICATION_CARD_ROTATION_DEGREES)
            .scaleX(CLARIFICATION_EXIT_SCALE)
            .scaleY(CLARIFICATION_EXIT_SCALE)
            .alpha(0f)
            .setDuration(CLARIFICATION_EXIT_DURATION_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                pendingClarificationAnimationDirection = direction
                endAction()
                if (viewModel.uiState.value !is AskAiSearchUiState.Clarifying) {
                    isClarificationTransitionRunning = false
                    resetClarificationCardTransform()
                }
            }
            .start()
    }

    private fun animateClarificationCardIn(direction: Int) {
        val card = binding.clarificationContainer
        val width = card.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        card.animate().cancel()
        card.translationX = -direction * width * CLARIFICATION_ENTER_DISTANCE_FACTOR
        card.translationY = 0f
        card.rotation = -direction * CLARIFICATION_CARD_ROTATION_DEGREES
        card.scaleX = CLARIFICATION_ENTER_SCALE
        card.scaleY = CLARIFICATION_ENTER_SCALE
        card.alpha = 0f
        card.animate()
            .translationX(0f)
            .rotation(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(CLARIFICATION_ENTER_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                resetClarificationCardTransform()
                isClarificationTransitionRunning = false
            }
            .start()
    }

    private fun animateClarificationCardAppear() {
        val card = binding.clarificationContainer
        card.animate().cancel()
        card.translationX = 0f
        card.translationY = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp).toFloat()
        card.rotation = 0f
        card.scaleX = CLARIFICATION_INITIAL_SCALE
        card.scaleY = CLARIFICATION_INITIAL_SCALE
        card.alpha = 0f
        card.animate()
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(CLARIFICATION_INITIAL_DURATION_MS)
            .setInterpolator(OvershootInterpolator(CLARIFICATION_OVERSHOOT_TENSION))
            .withEndAction { resetClarificationCardTransform() }
            .start()
    }

    private fun resetClarificationCardTransform() {
        binding.clarificationContainer.animate().cancel()
        binding.clarificationContainer.translationX = 0f
        binding.clarificationContainer.translationY = 0f
        binding.clarificationContainer.rotation = 0f
        binding.clarificationContainer.scaleX = 1f
        binding.clarificationContainer.scaleY = 1f
        binding.clarificationContainer.alpha = 1f
    }

    private fun addDetectedFilterRows(parsed: ParsedProductAiFilter, editing: Boolean) {
        val minDisplayPrice = parsed.minPrice ?: parsed.maxPrice ?: parsed.exactPrice
        val maxDisplayPrice = if (parsed.minPrice != null) {
            parsed.maxPrice ?: parsed.exactPrice
        } else {
            null
        }
        addFilterRow(
            label = getString(R.string.ask_ai_keyword),
            value = parsed.searchText.valueOrNotDetected(),
            iconRes = R.drawable.ia_ic_search,
            editing = editing,
            onRemove = { viewModel.removeFilter(ProductAiFilterType.SearchText) }
        )
        addFilterRow(
            label = getString(R.string.manufacturer),
            value = parsed.manufacturers.joinToStringOrNotDetected { it.name },
            iconRes = R.drawable.ic_detail_building,
            editing = editing,
            onRemove = { parsed.manufacturers.firstOrNull()?.let { viewModel.removeFilter(ProductAiFilterType.Manufacturer, it.id) } }
        )
        addFilterRow(
            label = getString(R.string.ia_vendor),
            value = parsed.vendors.joinToStringOrNotDetected { it.name },
            iconRes = R.drawable.ic_detail_building,
            editing = editing,
            onRemove = { parsed.vendors.firstOrNull()?.let { viewModel.removeFilter(ProductAiFilterType.Vendor, it.id) } }
        )
        addFilterRow(
            label = getString(R.string.ask_ai_min_price),
            value = minDisplayPrice?.formatPrice().orNotDetected(),
            iconRes = R.drawable.ic_info_outline,
            editing = editing,
            onRemove = { viewModel.removeFilter(ProductAiFilterType.MinPrice) }
        )
        addFilterRow(
            label = getString(R.string.ask_ai_max_price),
            value = maxDisplayPrice?.formatPrice().orNotDetected(),
            iconRes = R.drawable.ic_info_outline,
            editing = editing,
            onRemove = { viewModel.removeFilter(ProductAiFilterType.MaxPrice) }
        )
        addFilterRow(
            label = getString(R.string.ask_ai_availability),
            value = parsed.availability?.displayName.orNotDetected(),
            iconRes = R.drawable.ic_info_outline,
            editing = editing,
            onRemove = { viewModel.removeFilter(ProductAiFilterType.Availability) }
        )
        addFilterRow(
            label = getString(R.string.ask_ai_status),
            value = parsed.status?.displayName.orNotDetected(),
            iconRes = R.drawable.ic_info_outline,
            editing = editing,
            onRemove = { viewModel.removeFilter(ProductAiFilterType.Status) }
        )
        addFilterRow(
            label = getString(R.string.category),
            value = parsed.categoryReviewText(),
            iconRes = R.drawable.ic_detail_screen,
            editing = editing,
            onRemove = { viewModel.removeFilter(ProductAiFilterType.Category) }
        )
    }

    private fun showFilterSheetForCurrentReview() {
        val review = viewModel.uiState.value as? AskAiSearchUiState.Review ?: return
        ProductFilterSheetController(
            fragment = this,
            lifecycleOwner = viewLifecycleOwner,
            viewModel = productsViewModel,
            initialState = review.parsed.toFilterSheetState(),
            showSearchAndStatus = true,
            onApply = viewModel::applyEditedFilterState
        ).show()
    }

    private fun addFilterRow(
        label: String,
        value: String,
        iconRes: Int,
        editing: Boolean,
        onRemove: () -> Unit
    ) {
        val row = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._32sdp)
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)
            }
            gravity = android.view.Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        row.addView(ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._17sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._17sdp)
            ).apply {
                marginEnd = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._9sdp)
            }
            setImageResource(iconRes)
            setColorFilter(ContextCompat.getColor(requireContext(), R.color.ds_text_secondary))
            contentDescription = null
        })
        val labelView = AppTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
            text = label
            maxLines = 1
        }
        row.addView(labelView)
        row.addView(AppTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
            text = value
            maxLines = 1
        })
        val showRemove = editing || value != getString(R.string.ask_ai_not_detected)
        if (showRemove) {
            row.addView(ImageView(requireContext()).apply {
                val iconSize = resources.getDimensionPixelSize(R.dimen.ds_filter_chip_close_icon_size)
                layoutParams = LinearLayout.LayoutParams(
                    iconSize,
                    iconSize
                )
                setImageResource(R.drawable.ia_ic_filter_chip_close)
                contentDescription = getString(R.string.close)
                setOnClickListener { onRemove() }
            })
        }
        binding.detectedFiltersContainer.addView(row)
    }

    private fun renderWarnings(warnings: List<String>) {
        binding.warningsContainer.isVisible = warnings.isNotEmpty()
        binding.warningsContainer.removeAllViews()
        if (warnings.isEmpty()) return

        binding.warningsContainer.addView(AppTextView(requireContext()).apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._11ssp))
            text = getString(R.string.ask_ai_warnings)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
        })
        warnings.forEach { warning ->
            binding.warningsContainer.addView(TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
                }
                setBackgroundResource(R.drawable.bg_ask_ai_warning)
                setPadding(
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
                )
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
                text = warning
            })
        }
    }

    private fun renderReasons(reasons: List<String>) {
        binding.reasonsContainer.isVisible = reasons.isNotEmpty()
        binding.reasonsContainer.removeAllViews()
        if (reasons.isEmpty()) return

        binding.reasonsContainer.addView(AppTextView(requireContext()).apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._11ssp))
            text = getString(R.string.ask_ai_reason_title)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
        })
        reasons.take(MAX_VISIBLE_REASONS).forEach { reason ->
            binding.reasonsContainer.addView(TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)
                }
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
                text = reason
            })
        }
    }

    private fun renderEditControls(state: AskAiSearchUiState.Review) {
        val parsed = state.parsed
        binding.editContainer.isVisible = state.isEditing
        if (!state.isEditing) return

        binding.editSelectedFiltersContainer.removeAllViews()
        addEditableSelectedFilters(parsed)
        binding.editSearchEditText.setText(parsed.searchText)
        binding.editMinPriceEditText.setText(parsed.minPrice?.formatPlain().orEmpty())
        binding.editMaxPriceEditText.setText((parsed.maxPrice ?: parsed.exactPrice)?.formatPlain().orEmpty())
        renderChoiceButtons()
    }

    private fun addEditableSelectedFilters(parsed: ParsedProductAiFilter) {
        parsed.manufacturers.forEach { option ->
            addEditableFilterChip(
                label = getString(R.string.manufacturer),
                value = option.name,
                onRemove = { viewModel.removeFilter(ProductAiFilterType.Manufacturer, option.id) }
            )
        }
        parsed.vendors.forEach { option ->
            addEditableFilterChip(
                label = getString(R.string.ia_vendor),
                value = option.name,
                onRemove = { viewModel.removeFilter(ProductAiFilterType.Vendor, option.id) }
            )
        }
        if (parsed.dynamicCategorySpecs.hasCategory) {
            addEditableFilterChip(
                label = getString(R.string.category),
                value = parsed.categoryReviewText(),
                onRemove = { viewModel.removeFilter(ProductAiFilterType.Category) }
            )
        } else {
            parsed.categories.forEach { option ->
                addEditableFilterChip(
                    label = getString(R.string.category),
                    value = option.name,
                    onRemove = { viewModel.removeFilter(ProductAiFilterType.Category, option.id) }
                )
            }
        }
    }

    private fun addEditableFilterChip(
        label: String,
        value: String,
        onRemove: () -> Unit
    ) {
        binding.editSelectedFiltersContainer.addView(
            LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._42sdp)
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._7sdp)
                }
                gravity = android.view.Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_filter_field)
                setPadding(
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                    0,
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                    0
                )
                addView(AppTextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
                    maxLines = 1
                    text = getString(R.string.ask_ai_refine_chip_label, label, value)
                })
                addView(ImageView(requireContext()).apply {
                    val iconSize = resources.getDimensionPixelSize(R.dimen.ds_filter_chip_close_icon_size)
                    layoutParams = LinearLayout.LayoutParams(
                        iconSize,
                        iconSize
                    )
                    contentDescription = getString(R.string.close)
                    setImageResource(R.drawable.ia_ic_filter_chip_close)
                    setOnClickListener { onRemove() }
                })
            }
        )
    }

    private fun renderChoiceButtons() {
        binding.statusChoiceContainer.removeAllViews()
        listOf(null, ProductAiStatus.Active, ProductAiStatus.Inactive).forEach { status ->
            binding.statusChoiceContainer.addView(
                createTextChip(
                    text = status?.displayName ?: ProductAiStatus.All.displayName,
                    selected = selectedEditStatus == status,
                    removable = false,
                    onClick = {
                        selectedEditStatus = status
                        renderChoiceButtons()
                    }
                )
            )
        }

        binding.availabilityChoiceContainer.removeAllViews()
        listOf(null, ProductAiAvailability.InStock, ProductAiAvailability.OutOfStock).forEach { availability ->
            binding.availabilityChoiceContainer.addView(
                createTextChip(
                    text = availability?.displayName ?: getString(R.string.product_filter_all),
                    selected = selectedEditAvailability == availability,
                    removable = false,
                    onClick = {
                        selectedEditAvailability = availability
                        renderChoiceButtons()
                    }
                )
            )
        }
    }

    private fun createTextChip(
        text: String,
        selected: Boolean,
        removable: Boolean,
        onClick: () -> Unit
    ): AppTextView {
        return AppTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._32sdp)
            ).apply {
                marginEnd = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            }
            this.text = text
            setTextColor(ContextCompat.getColor(context, if (selected) R.color.ds_on_primary else R.color.ds_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._9ssp))
            gravity = android.view.Gravity.CENTER
            includeFontPadding = false
            minWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._48sdp)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0
            )
            setBackgroundResource(R.drawable.bg_filter_chip)
            backgroundTintList = ContextCompat.getColorStateList(
                context,
                if (selected) R.color.ds_primary else R.color.ds_primary_container
            )
            if (removable) {
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ia_ic_filter_chip_close,
                    0
                )
                compoundDrawablePadding = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun saveEditValues() {
        val minPrice = binding.editMinPriceEditText.text?.toString()?.trim()?.toDoubleOrNull()
        val maxPrice = binding.editMaxPriceEditText.text?.toString()?.trim()?.toDoubleOrNull()
        viewModel.updateEditableValues(
            searchText = binding.editSearchEditText.text?.toString().orEmpty(),
            minPrice = minPrice,
            maxPrice = maxPrice,
            status = selectedEditStatus,
            availability = selectedEditAvailability
        )
        viewModel.setEditing(false)
    }

    private fun resetEditFieldsFromCurrentState() {
        val review = viewModel.uiState.value as? AskAiSearchUiState.Review ?: return
        binding.editSearchEditText.setText(review.parsed.searchText)
        binding.editMinPriceEditText.setText(review.parsed.minPrice?.formatPlain().orEmpty())
        binding.editMaxPriceEditText.setText((review.parsed.maxPrice ?: review.parsed.exactPrice)?.formatPlain().orEmpty())
        selectedEditStatus = review.parsed.status
        selectedEditAvailability = review.parsed.availability
        renderChoiceButtons()
    }

    private fun showOriginalPrompt(prompt: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ask_ai_original_prompt)
            .setMessage(prompt.ifBlank { getString(R.string.ask_ai_not_detected) })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun applyParsedFilters(parsed: ParsedProductAiFilter) {
        val minPrice = parsed.minPrice ?: parsed.exactPrice
        val maxPrice = parsed.maxPrice ?: parsed.exactPrice
        val resultBundle = AskAiProductSearchResult.toBundle(
            searchText = parsed.searchText,
            status = parsed.status,
            availability = parsed.availability,
            minPrice = minPrice,
            maxPrice = maxPrice,
            categories = parsed.categories,
            manufacturers = parsed.manufacturers,
            vendors = parsed.vendors,
            dynamicCategorySpecs = parsed.dynamicCategorySpecs
        )
        val pickerFlow = arguments?.getString(SmartQuoteProductFlow.ARG_PRODUCT_PICKER_FLOW)
            ?: SmartQuoteProductFlow.FLOW_DEFAULT
        if (SmartQuoteProductFlow.isLineItemPickerFlow(pickerFlow) ||
            pickerFlow == ProductComparisonFlow.FLOW_PRODUCT_COMPARISON
        ) {
            val pickerArgs = bundleOf(
                SmartQuoteProductFlow.ARG_PRODUCT_PICKER_FLOW to pickerFlow,
                SmartQuoteProductFlow.ARG_SELECTION_MODE to true,
                SmartQuoteProductFlow.ARG_ASK_AI_RESULT to resultBundle,
                SmartQuoteProductFlow.ARG_PRESELECTED_PRODUCTS to
                    arguments?.getBundle(SmartQuoteProductFlow.ARG_PRESELECTED_PRODUCTS)
            )
            if (pickerFlow == ProductComparisonFlow.FLOW_PRODUCT_COMPARISON) {
                pickerArgs.putInt(
                    ProductComparisonFlow.ARG_COMPARE_SLOT,
                    arguments?.getInt(
                        ProductComparisonFlow.ARG_COMPARE_SLOT,
                        ProductComparisonFlow.SLOT_ONE
                    ) ?: ProductComparisonFlow.SLOT_ONE
                )
                pickerArgs.putBundle(
                    ProductComparisonFlow.ARG_PRESELECTED_PRODUCT,
                    arguments?.getBundle(ProductComparisonFlow.ARG_PRESELECTED_PRODUCT)
                )
            }
            findNavController().navigate(
                R.id.ia_action_askAiProductSearchFragment_to_productsFragment_selection,
                pickerArgs
            )
            return
        }
        findNavController().previousBackStackEntry?.savedStateHandle?.set(
            AskAiProductSearchResult.REQUEST_KEY,
            resultBundle
        )
        findNavController().popBackStack()
    }

    private fun requestVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            viewModel.updateVoiceError(getString(R.string.ask_ai_voice_unavailable))
            return
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceInput()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceInput() {
        setInputProcessing(
            processing = true,
            message = getString(R.string.ask_ai_listening),
            disableMicOnly = true
        )
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onError(error: Int) {
                    setInputProcessing(processing = false)
                    viewModel.updateVoiceError(getString(R.string.ask_ai_voice_error))
                }

                override fun onResults(results: Bundle?) {
                    setInputProcessing(processing = false)
                    val spokenText = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (spokenText.isBlank()) {
                        viewModel.updateVoiceError(getString(R.string.ask_ai_voice_error))
                        return
                    }
                    applyInputText(spokenText)
                }
            })
        }
        speechRecognizer?.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.ask_ai_voice_subtitle))
            }
        )
    }

    private fun showInputMethodsMenu() {
        PopupMenu(requireContext(), binding.inputMethodsMenuButton).apply {
            menu.add(MENU_GROUP_INPUT, MENU_ITEM_VOICE, 0, R.string.ask_ai_input_voice)
            menu.add(MENU_GROUP_INPUT, MENU_ITEM_OCR, 1, R.string.ask_ai_input_ocr)
            menu.add(MENU_GROUP_INPUT, MENU_ITEM_SCAN, 2, R.string.ask_ai_input_scan)
            menu.add(MENU_GROUP_INPUT, MENU_ITEM_UPLOAD_FILE, 3, R.string.ask_ai_input_upload_file)
            menu.setGroupCheckable(MENU_GROUP_INPUT, true, true)
            selectedInputModeMenuItemId(selectedInputMode)?.let { selectedMenuItemId ->
                menu.findItem(selectedMenuItemId)?.isChecked = true
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_ITEM_VOICE -> {
                        selectInputMode(AskAiInputMode.Voice)
                        true
                    }

                    MENU_ITEM_OCR -> {
                        selectInputMode(AskAiInputMode.Ocr)
                        true
                    }

                    MENU_ITEM_SCAN -> {
                        selectInputMode(AskAiInputMode.Scan)
                        true
                    }

                    MENU_ITEM_UPLOAD_FILE -> {
                        selectInputMode(AskAiInputMode.UploadFile)
                        true
                    }

                    else -> false
                }
            }
        }.show()
    }

    private fun restartInputFromReview() {
        selectedInputMode = AskAiInputMode.Voice
        viewModel.tryAgain()
    }

    private fun selectedInputModeMenuItemId(mode: AskAiInputMode): Int? {
        return when (mode) {
            AskAiInputMode.Default -> null
            AskAiInputMode.Voice -> MENU_ITEM_VOICE
            AskAiInputMode.Ocr -> MENU_ITEM_OCR
            AskAiInputMode.Scan -> MENU_ITEM_SCAN
            AskAiInputMode.UploadFile -> MENU_ITEM_UPLOAD_FILE
        }
    }

    private fun selectInputMode(mode: AskAiInputMode) {
        if (selectedInputMode != mode) {
            viewModel.clearInputError()
        }
        selectedInputMode = mode
        renderInputMode()
    }

    private fun startSelectedInputMode() {
        when (selectedInputMode) {
            AskAiInputMode.Voice -> requestVoiceInput()
            AskAiInputMode.Ocr -> startOcrCamera()
            AskAiInputMode.Scan -> startProductScan()
            AskAiInputMode.UploadFile -> startUploadFilePicker()
            AskAiInputMode.Default -> showInputMethodsMenu()
        }
    }

    private fun startOcrCamera() {
        startOcrDocumentScanner()
    }

    private fun startOcrDocumentScanner() {
        val scannerOptions = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        setInputProcessing(
            processing = true,
            message = getString(R.string.ask_ai_ocr_scanner_preparing)
        )
        GmsDocumentScanning.getClient(scannerOptions)
            .getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                if (_binding == null) return@addOnSuccessListener
                setInputProcessing(processing = false)
                runCatching {
                    ocrDocumentScannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }.onFailure {
                    startFallbackOcrCamera()
                }
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                setInputProcessing(processing = false)
                startFallbackOcrCamera()
            }
    }

    private fun startFallbackOcrCamera() {
        val uri = createInputImageUri(ASK_AI_OCR_FILE_PREFIX) ?: run {
            viewModel.updateInputError(getString(R.string.ask_ai_ocr_error))
            return
        }
        ocrImageUri = uri
        ocrCameraLauncher.launch(uri)
    }

    private fun extractTextFromImage(uri: Uri) {
        val image = runCatching {
            InputImage.fromFilePath(requireContext(), uri)
        }.getOrElse {
            viewModel.updateInputError(getString(R.string.ask_ai_ocr_error))
            return
        }

        setInputProcessing(
            processing = true,
            message = getString(R.string.ask_ai_ocr_processing)
        )
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
                setInputProcessing(processing = false)
                val extractedText = result.text
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .joinToString(separator = " ")
                    .trim()
                if (extractedText.isBlank()) {
                    viewModel.updateInputError(getString(R.string.ask_ai_ocr_empty))
                } else {
                    applyInputText(extractedText)
                }
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                setInputProcessing(processing = false)
                viewModel.updateInputError(getString(R.string.ask_ai_ocr_error))
            }
    }

    private fun startProductScan() {
        findNavController().navigate(R.id.iaProductCodeScannerFragment)
    }

    private fun startUploadFilePicker() {
        viewModel.clearInputError()
        uploadedInputFileName = null
        uploadInputProcessing = false
        renderUploadFilePanel()
        setInputProcessing(
            processing = false,
            message = getString(R.string.ask_ai_upload_file_pick_hint)
        )
        runCatching {
            uploadFileLauncher.launch(SUPPORTED_UPLOAD_MIME_TYPES)
        }.onFailure {
            viewModel.updateInputError(getString(R.string.ask_ai_upload_error))
        }
    }

    private fun extractTextFromUploadedFile(uri: Uri) {
        val resolver = requireContext().contentResolver
        val fileName = uploadedFileName(uri)
        val fileSize = uploadedFileSize(uri)
        if (fileSize != null && fileSize > MAX_UPLOAD_FILE_BYTES) {
            setInputProcessing(processing = false)
            viewModel.updateInputError(getString(R.string.ask_ai_upload_too_large))
            return
        }
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        uploadInputProcessing = true
        uploadedInputFileName = fileName
        renderUploadFilePanel()
        setInputProcessing(
            processing = true,
            message = getString(R.string.ask_ai_upload_processing, fileName)
        )
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val mimeType = resolver.getType(uri).orEmpty()
                    resolver.openInputStream(uri)?.use { input ->
                        extractUploadedText(input, fileName, mimeType)
                    }.orEmpty()
                }
            }
            if (_binding == null) return@launch
            uploadInputProcessing = false
            setInputProcessing(processing = false)
            result
                .onSuccess { extractedText ->
                    val cleanedText = extractedText.toUploadedInputText()
                    if (cleanedText.isBlank()) {
                        uploadedInputFileName = null
                        renderUploadFilePanel()
                        viewModel.updateInputError(getString(R.string.ask_ai_upload_empty))
                    } else {
                        applyInputText(cleanedText)
                        uploadedInputFileName = fileName
                        renderUploadFilePanel(uploaded = true)
                        setInputProcessing(
                            processing = false,
                            message = getString(R.string.ask_ai_upload_done, fileName)
                        )
                    }
                }
                .onFailure {
                    uploadedInputFileName = null
                    renderUploadFilePanel()
                    viewModel.updateInputError(getString(R.string.ask_ai_upload_unsupported))
                }
        }
    }

    private fun uploadedFileName(uri: Uri): String {
        val resolver = requireContext().contentResolver
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.ask_ai_upload_file_fallback_name)
    }

    private fun uploadedFileSize(uri: Uri): Long? {
        val resolver = requireContext().contentResolver
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst()) cursor.getLong(index) else null
            }
        }.getOrNull()?.takeIf { it > 0L }
    }

    private fun extractUploadedText(input: InputStream, fileName: String, mimeType: String): String {
        return when {
            fileName.endsWith(".docx", ignoreCase = true) ||
                mimeType == MIME_DOCX -> extractDocxText(input)

            fileName.endsWith(".xlsx", ignoreCase = true) ||
                mimeType == MIME_XLSX -> extractXlsxText(input)

            fileName.endsWith(".txt", ignoreCase = true) ||
                fileName.endsWith(".csv", ignoreCase = true) ||
                fileName.endsWith(".tsv", ignoreCase = true) ||
                mimeType.startsWith("text/") ||
                mimeType == MIME_CSV -> input.bufferedReader().use { reader ->
                    reader.readTextLimited(MAX_UPLOAD_RAW_TEXT_LENGTH)
                }

            else -> error("Unsupported upload type: $mimeType")
        }
    }

    private fun extractDocxText(input: InputStream): String {
        val parts = mutableListOf<String>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name.matches(DOCX_TEXT_ENTRY_REGEX)) {
                    parts += extractXmlTagText(zip.readCurrentEntryText(MAX_UPLOAD_ENTRY_BYTES), "w:t")
                    if (parts.totalTextLength() >= MAX_UPLOAD_TEXT_LENGTH) {
                        zip.closeEntry()
                        break
                    }
                }
                zip.closeEntry()
            }
        }
        return parts.joinToString(separator = " ")
    }

    private fun extractXlsxText(input: InputStream): String {
        val sharedStrings = mutableListOf<String>()
        val worksheetValues = mutableListOf<String>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    when {
                        entry.name == XLSX_SHARED_STRINGS_ENTRY ->
                            sharedStrings += extractXmlTagText(
                                zip.readCurrentEntryText(MAX_UPLOAD_ENTRY_BYTES),
                                "t"
                            ).take(MAX_UPLOAD_CELL_COUNT)
                        entry.name.startsWith(XLSX_WORKSHEET_ENTRY_PREFIX) &&
                            entry.name.endsWith(".xml") ->
                            worksheetValues += extractXlsxWorksheetValues(
                                zip.readCurrentEntryText(MAX_UPLOAD_ENTRY_BYTES),
                                sharedStrings
                            ).take((MAX_UPLOAD_CELL_COUNT - worksheetValues.size).coerceAtLeast(0))
                    }
                }
                zip.closeEntry()
                if (
                    worksheetValues.size >= MAX_UPLOAD_CELL_COUNT ||
                    worksheetValues.totalTextLength() >= MAX_UPLOAD_TEXT_LENGTH
                ) {
                    break
                }
            }
        }
        return (worksheetValues.ifEmpty { sharedStrings }).joinToString(separator = " ")
    }

    private fun extractXlsxWorksheetValues(xml: String, sharedStrings: List<String>): List<String> {
        return XLSX_CELL_REGEX.findAll(xml).mapNotNull { cellMatch ->
            val attributes = cellMatch.groupValues.getOrNull(1).orEmpty()
            val body = cellMatch.groupValues.getOrNull(2).orEmpty()
            val cellType = CELL_TYPE_REGEX.find(attributes)?.groupValues?.getOrNull(1).orEmpty()
            when (cellType) {
                "s" -> CELL_VALUE_REGEX.find(body)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                    ?.let(sharedStrings::getOrNull)
                "inlineStr" -> extractXmlTagText(body, "t").joinToString(separator = " ")
                else -> CELL_VALUE_REGEX.find(body)?.groupValues?.getOrNull(1)?.decodeXmlEntities()
            }
        }.map { it.trim() }.filter { it.isNotBlank() }.take(MAX_UPLOAD_CELL_COUNT).toList()
    }

    private fun extractXmlTagText(xml: String, tagName: String): List<String> {
        val regex = Regex("<$tagName(?:\\s[^>]*)?>(.*?)</$tagName>", RegexOption.DOT_MATCHES_ALL)
        return regex.findAll(xml)
            .map { match -> match.groupValues[1].decodeXmlEntities().trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun ZipInputStream.readCurrentEntryText(maxBytes: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalRead = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            val allowed = (maxBytes - totalRead).coerceAtMost(read)
            if (allowed <= 0) break
            output.write(buffer, 0, allowed)
            totalRead += allowed
            if (totalRead >= maxBytes) break
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private fun Reader.readTextLimited(maxChars: Int): String {
        val builder = StringBuilder()
        val buffer = CharArray(DEFAULT_BUFFER_SIZE)
        while (builder.length < maxChars) {
            val read = read(buffer, 0, (maxChars - builder.length).coerceAtMost(buffer.size))
            if (read == -1) break
            builder.append(buffer, 0, read)
        }
        return builder.toString()
    }

    private fun List<String>.totalTextLength(): Int {
        return sumOf { it.length }
    }

    private fun String.toUploadedInputText(): String {
        return lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_UPLOAD_TEXT_LENGTH)
    }

    private fun String.decodeXmlEntities(): String {
        return replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    private fun createInputImageUri(filePrefix: String): Uri? {
        return runCatching {
            val scanDirectory = File(requireContext().cacheDir, ASK_AI_SCAN_CACHE_DIR).apply {
                mkdirs()
            }
            val scanImage = File.createTempFile(filePrefix, ASK_AI_SCAN_FILE_SUFFIX, scanDirectory)
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                scanImage
            )
        }.getOrNull()
    }

    private fun observeScannerResult() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<String>(ProductCodeScannerResult.REQUEST_KEY)
            .observe(viewLifecycleOwner) { scannedText ->
                savedStateHandle.remove<String>(ProductCodeScannerResult.REQUEST_KEY)
                val cleanedText = scannedText.trim()
                if (cleanedText.isBlank()) return@observe
                selectInputMode(AskAiInputMode.Scan)
                applyInputText(cleanedText)
            }
    }

    private fun applyInputText(text: String) {
        binding.queryEditText.setText(text)
        binding.queryEditText.setSelection(text.length)
        viewModel.onQueryChanged(text)
    }

    private fun renderInputMode() {
        val subtitleRes = when (selectedInputMode) {
            AskAiInputMode.Default -> R.string.ask_ai_subtitle
            AskAiInputMode.Voice -> R.string.ask_ai_voice_subtitle
            AskAiInputMode.Ocr -> R.string.ask_ai_ocr_subtitle
            AskAiInputMode.Scan -> R.string.ask_ai_scan_subtitle
            AskAiInputMode.UploadFile -> R.string.ask_ai_upload_subtitle
        }
        val hintRes = when (selectedInputMode) {
            AskAiInputMode.Default -> R.string.ask_ai_input_hint_default
            AskAiInputMode.Voice -> R.string.ask_ai_input_hint_voice
            AskAiInputMode.Ocr -> R.string.ask_ai_input_hint_ocr
            AskAiInputMode.Scan -> R.string.ask_ai_input_hint_scan
            AskAiInputMode.UploadFile -> R.string.ask_ai_input_hint_upload
        }
        binding.inputSubtitleText.setText(subtitleRes)
        binding.queryEditText.setHint(hintRes)
        binding.voiceStatusText.setText(hintRes)
        binding.voiceStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_secondary))
        binding.selectedInputModeText.isVisible = selectedInputMode != AskAiInputMode.Default
        binding.selectedInputModeText.setText(
            when (selectedInputMode) {
                AskAiInputMode.Default -> R.string.ask_ai_input_methods
                AskAiInputMode.Voice -> R.string.ask_ai_selected_voice
                AskAiInputMode.Ocr -> R.string.ask_ai_selected_ocr
                AskAiInputMode.Scan -> R.string.ask_ai_selected_scan
                AskAiInputMode.UploadFile -> R.string.ask_ai_selected_upload
            }
        )
        binding.uploadFilePanel.isVisible = selectedInputMode == AskAiInputMode.UploadFile
        renderUploadFilePanel()
        binding.micButton.isVisible = selectedInputMode != AskAiInputMode.Default &&
            selectedInputMode != AskAiInputMode.UploadFile
        binding.micButton.icon = ContextCompat.getDrawable(
            requireContext(),
            when (selectedInputMode) {
                AskAiInputMode.Default,
                AskAiInputMode.Voice -> R.drawable.ia_ic_mic
                AskAiInputMode.Ocr -> R.drawable.ic_ocr_text
                AskAiInputMode.Scan -> R.drawable.ic_qr_scan
                AskAiInputMode.UploadFile -> R.drawable.ic_document
            }
        )
        binding.micButton.contentDescription = getString(
            when (selectedInputMode) {
                AskAiInputMode.Default,
                AskAiInputMode.Voice -> R.string.ask_ai_input_voice
                AskAiInputMode.Ocr -> R.string.ask_ai_input_ocr
                AskAiInputMode.Scan -> R.string.ask_ai_input_scan
                AskAiInputMode.UploadFile -> R.string.ask_ai_input_upload_file
            }
        )
    }

    private fun setInputProcessing(
        processing: Boolean,
        message: String? = null,
        disableMicOnly: Boolean = false
    ) {
        if (message != null) {
            binding.voiceStatusText.text = message
        } else {
            renderInputMode()
        }
        binding.voiceStatusText.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (processing) R.color.ds_primary else R.color.ds_text_secondary
            )
        )
        binding.inputMethodProgress.isVisible = processing && !disableMicOnly
        binding.micButton.alpha = if (processing) 0.82f else 1f
        binding.micButton.isEnabled = !processing
        binding.uploadFilePanel.alpha = if (processing) 0.82f else 1f
        binding.uploadFilePanel.isEnabled = !processing
        binding.inputMethodsMenuButton.isEnabled = !processing
        if (selectedInputMode == AskAiInputMode.UploadFile) {
            renderUploadFilePanel()
        }
    }

    private fun renderUploadFilePanel(uploaded: Boolean = false) {
        if (_binding == null) return
        val fileName = uploadedInputFileName
        val showUploaded = uploaded || (fileName != null && !uploadInputProcessing)
        when {
            uploadInputProcessing -> {
                binding.uploadFileIcon.setImageResource(R.drawable.ic_document)
                binding.uploadFileTitleText.setText(R.string.ask_ai_upload_reading_title)
                binding.uploadFileSubtitleText.text = getString(
                    R.string.ask_ai_upload_processing,
                    fileName ?: getString(R.string.ask_ai_upload_file_fallback_name)
                )
                binding.uploadFileProgress.isVisible = true
            }
            showUploaded && fileName != null -> {
                binding.uploadFileIcon.setImageResource(R.drawable.ia_ic_filter_check)
                binding.uploadFileTitleText.setText(R.string.ask_ai_upload_uploaded_title)
                binding.uploadFileSubtitleText.text = getString(R.string.ask_ai_upload_done, fileName)
                binding.uploadFileProgress.isVisible = false
            }
            else -> {
                binding.uploadFileIcon.setImageResource(R.drawable.ic_document)
                binding.uploadFileTitleText.setText(R.string.ask_ai_upload_file_title)
                binding.uploadFileSubtitleText.setText(R.string.ask_ai_upload_file_support)
                binding.uploadFileProgress.isVisible = false
            }
        }
    }

    private fun confidenceColor(level: ProductAiConfidenceLevel): Int {
        val colorRes = when (level) {
            ProductAiConfidenceLevel.High -> R.color.ds_success
            ProductAiConfidenceLevel.Medium -> R.color.ds_primary
            ProductAiConfidenceLevel.Low -> R.color.ds_warning
        }
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun confidenceLabelRes(level: ProductAiConfidenceLevel): Int {
        return when (level) {
            ProductAiConfidenceLevel.High -> R.string.ask_ai_high_confidence
            ProductAiConfidenceLevel.Medium -> R.string.ask_ai_medium_confidence
            ProductAiConfidenceLevel.Low -> R.string.ask_ai_low_confidence
        }
    }

    private fun confidenceBackgroundColor(level: ProductAiConfidenceLevel): Int {
        val color = when (level) {
            ProductAiConfidenceLevel.High -> 0xFFE8F8EF.toInt()
            ProductAiConfidenceLevel.Medium -> 0xFFFFF7ED.toInt()
            ProductAiConfidenceLevel.Low -> 0xFFFEF2F2.toInt()
        }
        return color
    }

    private fun Double.formatPrice(): String {
        val format = if (this % 1.0 == 0.0) "$%,.0f" else "$%,.2f"
        return String.format(Locale.US, format, this)
    }

    private fun String.valueOrNotDetected(): String {
        return takeIf { it.isNotBlank() } ?: getString(R.string.ask_ai_not_detected)
    }

    private fun String?.orNotDetected(): String {
        return this?.takeIf { it.isNotBlank() } ?: getString(R.string.ask_ai_not_detected)
    }

    private fun <T> List<T>.joinToStringOrNotDetected(transform: (T) -> String): String {
        return takeIf { it.isNotEmpty() }
            ?.joinToString { transform(it) }
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.ask_ai_not_detected)
    }

    private fun Double.formatPlain(): String {
        return if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
    }

    private fun ParsedProductAiFilter.toFilterSheetState(): ProductFilterSheetState {
        return ProductFilterSheetState(
            searchText = searchText,
            status = status ?: ProductAiStatus.All,
            manufacturers = manufacturers.toLinkedSelectionMap(),
            vendors = vendors.toLinkedSelectionMap(),
            minListPrice = minPrice ?: exactPrice,
            maxListPrice = maxPrice ?: exactPrice,
            dynamicCategorySpecs = dynamicCategorySpecs.toSelectedCategorySpecs(),
            availability = availability
        )
    }

    private fun List<ProductAiFilterOption>.toLinkedSelectionMap(): LinkedHashMap<String, String> {
        return LinkedHashMap<String, String>().apply {
            this@toLinkedSelectionMap.forEach { option -> put(option.id, option.name) }
        }
    }

    private fun ParsedProductAiFilter.categoryReviewText(): String {
        if (dynamicCategorySpecs.hasCategory) {
            val category = dynamicCategorySpecs.categoryName.orEmpty()
            val specs = dynamicCategorySpecs.selectedOptions
                .filterValues { it.isNotEmpty() }
                .map { (key, values) -> "${key.toDisplayLabel()}: ${values.joinToString()}" }
                .joinToString(separator = "; ")
            return if (specs.isBlank()) category else "$category ($specs)"
        }
        return categories.joinToStringOrNotDetected { it.name }
    }

    private fun ProductAiCategorySpecSuggestion.toSelectedCategorySpecs(): SelectedCategorySpecs {
        return SelectedCategorySpecs(
            categoryName = categoryName,
            selectedOptions = selectedOptions
        )
    }

    private fun String.toDisplayLabel(): String {
        return replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
            .ifBlank { this }
    }

    override fun onDestroyView() {
        stopClarificationPeekShimmer()
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MAX_VISIBLE_REASONS = 3
        const val ASK_AI_SCAN_CACHE_DIR = "ask_ai_scan"
        const val ASK_AI_OCR_FILE_PREFIX = "product_ocr_"
        const val ASK_AI_SCAN_FILE_SUFFIX = ".jpg"
        const val CLARIFICATION_DIRECTION_NONE = 0
        const val CLARIFICATION_DIRECTION_NEXT = -1
        const val CLARIFICATION_DIRECTION_PREVIOUS = 1
        const val CLARIFICATION_SEARCH_THRESHOLD = 8
        const val CLARIFICATION_OPTION_LABEL_MAX_LINES = 1
        const val CLARIFICATION_SWIPE_AXIS_RATIO = 1.25f
        const val CLARIFICATION_EXIT_DISTANCE_FACTOR = 0.58f
        const val CLARIFICATION_ENTER_DISTANCE_FACTOR = 0.38f
        const val CLARIFICATION_CARD_ROTATION_DEGREES = 7f
        const val CLARIFICATION_EXIT_SCALE = 0.94f
        const val CLARIFICATION_ENTER_SCALE = 0.96f
        const val CLARIFICATION_INITIAL_SCALE = 0.94f
        const val CLARIFICATION_OVERSHOOT_TENSION = 1.05f
        const val CLARIFICATION_EXIT_DURATION_MS = 150L
        const val CLARIFICATION_ENTER_DURATION_MS = 220L
        const val CLARIFICATION_INITIAL_DURATION_MS = 240L
        const val CLARIFICATION_SHIMMER_DURATION_MS = 720L
        const val DISABLED_BUTTON_ALPHA = 0.55f
        const val MENU_GROUP_INPUT = 100
        const val MENU_ITEM_VOICE = 101
        const val MENU_ITEM_OCR = 102
        const val MENU_ITEM_SCAN = 103
        const val MENU_ITEM_UPLOAD_FILE = 104
        const val MAX_UPLOAD_TEXT_LENGTH = 100
        const val MAX_UPLOAD_RAW_TEXT_LENGTH = 512
        const val MAX_UPLOAD_ENTRY_BYTES = 64 * 1024
        const val MAX_UPLOAD_CELL_COUNT = 140
        const val MAX_UPLOAD_FILE_BYTES = 2L * 1024L * 1024L
        const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        const val MIME_CSV = "text/csv"
        const val XLSX_SHARED_STRINGS_ENTRY = "xl/sharedStrings.xml"
        const val XLSX_WORKSHEET_ENTRY_PREFIX = "xl/worksheets/"
        val SUPPORTED_UPLOAD_MIME_TYPES = arrayOf(
            "text/plain",
            "text/csv",
            "text/tab-separated-values",
            MIME_DOCX,
            MIME_XLSX
        )
        val DOCX_TEXT_ENTRY_REGEX = Regex("word/(document|header\\d*|footer\\d*)\\.xml")
        val XLSX_CELL_REGEX = Regex("<c([^>]*)>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)
        val CELL_TYPE_REGEX = Regex("t=\"([^\"]+)\"")
        val CELL_VALUE_REGEX = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL)
    }
}

private enum class AskAiInputMode {
    Default,
    Voice,
    Ocr,
    Scan,
    UploadFile
}



