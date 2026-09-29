package com.axelliant.hris.features.quotes.presentation

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.InputFilter
import android.text.InputType
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentSmartQuoteBinding
import com.axelliant.hris.databinding.ItemSmartQuoteLineItemBinding
import com.axelliant.hris.databinding.ItemSmartQuoteReviewProductBinding
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.presentation.QuoteProductSelectionBundles.toQuoteCreationProducts
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.axelliant.hris.ui.designsystem.components.AppTextView
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmartQuoteFragment : Fragment() {

    private val viewModel: SmartQuoteViewModel by viewModels()
    private var _binding: FragmentSmartQuoteBinding? = null
    private val binding get() = _binding!!
    private var speechRecognizer: SpeechRecognizer? = null
    private var suppressInputWatcher = false
    private var lastStep: SmartQuoteStep? = null
    private var previousSoftInputMode: Int? = null
    private lateinit var gestureDetector: GestureDetector
    private val optionShimmerAnimators = mutableListOf<ObjectAnimator>()
    private val nextPreviewShimmerAnimators = mutableListOf<ObjectAnimator>()
    private var pendingCardAnimationDirection = CARD_DIRECTION_NONE
    private var isCardTransitionRunning = false
    private var lastNextPreviewHeight = 0

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceInput()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.smart_quote_voice_permission_denied,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmartQuoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previousSoftInputMode = requireActivity().window.attributes.softInputMode
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        setupGestureDetector()
        setupInteractions()
        observeState()
        observeProductSelectionResult()
    }

    private fun observeProductSelectionResult() {
        val savedStateHandle = runCatching {
            findNavController().getBackStackEntry(R.id.iaSmartQuoteFragment).savedStateHandle
        }.getOrNull() ?: return
        savedStateHandle
            .getLiveData<Bundle>(SmartQuoteViewModel.RESULT_PRODUCTS)
            .observe(viewLifecycleOwner) { bundle ->
                savedStateHandle.remove<Bundle>(SmartQuoteViewModel.RESULT_PRODUCTS)
                val searchQuery = savedStateHandle.get<String>(SmartQuoteViewModel.RESULT_SEARCH_QUERY)
                savedStateHandle.remove<String>(SmartQuoteViewModel.RESULT_SEARCH_QUERY)
                viewModel.applySelectedProducts(
                    products = bundle.toQuoteCreationProducts(),
                    searchQuery = searchQuery
                )
            }
    }

    private fun openProductSearchViaAskAi() {
        val state = viewModel.uiState.value
        findNavController().navigate(
            R.id.ia_action_smartQuoteFragment_to_askAiProductSearchFragment,
            bundleOf(
                SmartQuoteProductFlow.ARG_PRODUCT_PICKER_FLOW to SmartQuoteProductFlow.FLOW_SMART_QUOTE,
                SmartQuoteProductFlow.ARG_PRESELECTED_PRODUCTS to
                    QuoteProductSelectionBundles.fromProducts(state.selectedProducts)
            )
        )
    }

    private fun openProductSearchWithQuery(query: String) {
        val state = viewModel.uiState.value
        findNavController().navigate(
            R.id.ia_action_smartQuoteFragment_to_productsFragment_selection,
            bundleOf(
                SmartQuoteProductFlow.ARG_SELECTION_MODE to true,
                SmartQuoteProductFlow.ARG_INITIAL_SEARCH_QUERY to query,
                SmartQuoteProductFlow.ARG_PRESELECTED_PRODUCTS to
                    QuoteProductSelectionBundles.fromProducts(state.selectedProducts)
            )
        )
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener {
            if (viewModel.uiState.value.canGoBack) {
                movePrevious()
            } else {
                findNavController().navigateUp()
            }
        }
        binding.appTopBar.setOnActionClickListener { showSavePlaceholder() }
        binding.quoteStepBackButton.setOnClickListener { movePrevious() }
        binding.nextButton.setOnClickListener { moveNext() }
        binding.micButton.setOnClickListener {
            if (viewModel.uiState.value.step == SmartQuoteStep.LineItems) {
                openProductSearchViaAskAi()
            } else {
                requestVoiceInput()
            }
        }
        binding.swipeBackHint.setOnClickListener { movePrevious() }
        binding.swipeNextHint.setOnClickListener { moveNext() }
        binding.activeCard.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
        binding.answerInput.addTextChangedListener { editable ->
            if (!suppressInputWatcher) {
                val input = editable?.toString().orEmpty()
                if (viewModel.uiState.value.step == SmartQuoteStep.DeliveryDate) {
                    applyDeliveryDateEndIcon()
                    val formatted = formatTypedDeliveryDate(input)
                    if (formatted != input) {
                        suppressInputWatcher = true
                        binding.answerInput.setText(formatted)
                        binding.answerInput.setSelection(formatted.length)
                        suppressInputWatcher = false
                    }
                    viewModel.onInputChanged(formatted)
                } else {
                    viewModel.onInputChanged(input)
                }
            }
        }
        binding.answerInput.setOnFocusChangeListener { _, _ ->
            if (viewModel.uiState.value.step == SmartQuoteStep.DeliveryDate) {
                applyDeliveryDateEndIcon()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: SmartQuoteUiState) {
        val stepChanged = lastStep != null && lastStep != state.step
        lastStep = state.step

        binding.progressText.text = state.progressText
        binding.stepProgressIndicator.progress = state.progressPercent
        val isReview = state.step == SmartQuoteStep.Review
        binding.appTopBar.setTitle(
            if (state.step == SmartQuoteStep.Review) {
                getString(R.string.quote_preview_title)
            } else {
                getString(R.string.smart_quote_title)
            }
        )
        binding.appTopBar.setActionVisible(true)
        binding.progressContainer.isVisible = true
        binding.stepTitleText.isVisible = true
        binding.nextStepText.isVisible = true
        binding.insightLabelRow.isVisible = false
        binding.insightText.isVisible = false
        binding.cardStackContainer.isVisible = true
        binding.questionContent.isVisible = !isReview
        binding.reviewScroll.isVisible = isReview
        binding.stepTitleText.text = state.step.title
        binding.nextStepText.text = if (state.step == SmartQuoteStep.Review) {
            reviewStatusText(state)
        } else {
            getString(R.string.smart_quote_next_format, state.step.nextLabel)
        }
        binding.insightText.text = state.step.insight
        binding.categoryText.isVisible = false
        binding.questionText.text = state.step.question
        binding.helperText.isVisible = false
        binding.nextButton.text = if (state.step == SmartQuoteStep.Review) {
            getString(R.string.smart_quote_review_finalize)
        } else {
            state.nextButtonText
        }
        binding.micButton.isVisible = false
        binding.quoteStepBackButton.isEnabled = state.canGoBack
        binding.quoteStepBackButton.alpha = if (state.canGoBack) 1f else DISABLED_BUTTON_ALPHA
        renderNextStepPreview(state)
        binding.swipeBackHint.isVisible = !isReview && state.canGoBack
        binding.swipeNextHint.isVisible = !isReview && state.step.next() != null
        updateActionSpacing()
        val showAnswerInput = state.step.showsAnswerInput() && !isReview
        binding.answerInputLayout.isVisible = showAnswerInput
        binding.answerInputLayout.error = state.errorMessage.takeIf { showAnswerInput }
        binding.answerInputLayout.endIconMode = when {
            state.step == SmartQuoteStep.DeliveryDate ->
                com.axelliant.hris.ui.designsystem.components.AppTextFieldLayout.END_ICON_CUSTOM
            showAnswerInput ->
                com.axelliant.hris.ui.designsystem.components.AppTextFieldLayout.END_ICON_CLEAR_TEXT
            else ->
                com.axelliant.hris.ui.designsystem.components.AppTextFieldLayout.END_ICON_NONE
        }
        configureAnswerInputForStep(state.step)
        if (state.step == SmartQuoteStep.DeliveryDate) {
            applyDeliveryDateEndIcon()
        } else if (showAnswerInput) {
            binding.answerInputLayout.endIconDrawable =
                ContextCompat.getDrawable(requireContext(), R.drawable.ia_ic_filter_close)
        }

        if (binding.answerInput.text?.toString() != state.input) {
            suppressInputWatcher = true
            binding.answerInput.setText(state.input)
            binding.answerInput.setSelection(state.input.length)
            suppressInputWatcher = false
        }
        binding.answerInput.hint = state.step.hint

        if (isReview) {
            stopOptionShimmer()
            renderReview(state)
        } else {
            renderOptions(state)
        }
        syncNextPreviewCardSize()
        if (stepChanged) {
            val direction = pendingCardAnimationDirection
            pendingCardAnimationDirection = CARD_DIRECTION_NONE
            binding.activeCard.post {
                if (direction == CARD_DIRECTION_NONE) {
                    animateCardAppear()
                } else {
                    animateCardIn(direction)
                }
            }
        }
    }

    private fun updateActionSpacing() {
        val params = binding.nextButton.layoutParams as LinearLayout.LayoutParams
        params.marginStart = 0
        binding.nextButton.layoutParams = params
    }

    private fun renderNextStepPreview(state: SmartQuoteUiState) {
        val nextStep = state.step.next()
        val showPreview = state.step != SmartQuoteStep.Review && nextStep != null
        binding.nextPreviewCard.isVisible = showPreview
        binding.nextPreviewTitleText.text = nextStep?.question.orEmpty()
        if (!showPreview) {
            lastNextPreviewHeight = 0
            setNextPreviewShimmerLoading(false)
            return
        }
        setNextPreviewShimmerLoading(isCardTransitionRunning)
        syncNextPreviewCardSize()
    }

    private fun syncNextPreviewCardSize() {
        val safeBinding = _binding ?: return
        if (!safeBinding.nextPreviewCard.isVisible) return
        safeBinding.activeCard.post {
            val currentHeight = safeBinding.activeCard.height
            if (currentHeight <= 0) return@post
            if (lastNextPreviewHeight != currentHeight) {
                lastNextPreviewHeight = currentHeight
                safeBinding.nextPreviewCard.layoutParams = safeBinding.nextPreviewCard.layoutParams.apply {
                    height = currentHeight
                }
            }
            safeBinding.nextPreviewCard.translationX = 0f
            safeBinding.nextPreviewCard.translationY = 0f
            safeBinding.nextPreviewCard.rotation = 0f
        }
    }

    private fun SmartQuoteStep.showsAnswerInput(): Boolean {
        return this in setOf(
            SmartQuoteStep.QuoteTitle,
            SmartQuoteStep.Customer,
            SmartQuoteStep.DeliveryDate
        )
    }

    private fun configureAnswerInputForStep(step: SmartQuoteStep) {
        if (step == SmartQuoteStep.DeliveryDate) {
            binding.answerInput.inputType = InputType.TYPE_CLASS_NUMBER
            binding.answerInput.filters = arrayOf(InputFilter.LengthFilter(DELIVERY_DATE_MAX_LENGTH))
        } else {
            binding.answerInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            binding.answerInput.filters = emptyArray()
        }
    }

    private fun applyDeliveryDateEndIcon() {
        binding.answerInputLayout.endIconMode =
            com.axelliant.hris.ui.designsystem.components.AppTextFieldLayout.END_ICON_CUSTOM
        binding.answerInputLayout.endIconDrawable =
            ContextCompat.getDrawable(requireContext(), R.drawable.ia_ic_filter_calendar)
        binding.answerInputLayout.isEndIconCheckable = false
        binding.answerInputLayout.isEndIconVisible = true
        binding.answerInputLayout.setEndIconOnClickListener {
            showDeliveryDatePicker()
        }
    }

    private fun formatTypedDeliveryDate(value: String): String {
        val digits = value.filter(Char::isDigit).take(DELIVERY_DATE_DIGIT_COUNT)
        return buildString {
            append(digits.take(2))
            if (digits.length > 2) {
                append("-")
                append(digits.drop(2).take(2))
            }
            if (digits.length > 4) {
                append("-")
                append(digits.drop(4))
            }
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (isCardTransitionRunning) return true
                    val startX = e1?.x ?: return false
                    val deltaX = e2.x - startX
                    if (kotlin.math.abs(deltaX) < SWIPE_DISTANCE_THRESHOLD) return false
                    if (deltaX < 0) {
                        moveNext()
                    } else {
                        movePrevious()
                    }
                    return true
                }
            }
        )
    }

    private fun moveNext() {
        if (isCardTransitionRunning) return
        if (viewModel.uiState.value.step == SmartQuoteStep.Review) {
            val advanced = viewModel.onNext()
            if (advanced && viewModel.uiState.value.isFinished) {
                showSavePlaceholder()
            }
            return
        }
        animateCardOut(CARD_DIRECTION_NEXT) {
            val advanced = viewModel.onNext()
            if (advanced && viewModel.uiState.value.isFinished) {
                showSavePlaceholder()
            }
            if (!advanced) {
                setNextPreviewShimmerLoading(false)
                isCardTransitionRunning = false
                resetCardTransform()
            }
        }
    }

    private fun movePrevious() {
        if (isCardTransitionRunning || !viewModel.uiState.value.canGoBack) return
        animateCardOut(CARD_DIRECTION_PREVIOUS) {
            if (!viewModel.onBack()) {
                setNextPreviewShimmerLoading(false)
                isCardTransitionRunning = false
                resetCardTransform()
            }
        }
    }

    private fun renderOptions(state: SmartQuoteUiState) {
        stopOptionShimmer()
        binding.optionsContainer.removeAllViews()
        if (state.isCurrentStepLoading()) {
            renderOptionLoadingShimmer()
            return
        }
        val options = state.visibleOptions()
        if (options.isEmpty()) {
            state.emptyOptionsMessageRes()?.let { messageRes ->
                binding.optionsContainer.addView(createOptionStatusView(getString(messageRes)))
                return
            }
        }
        if (state.step == SmartQuoteStep.ShippingAddress) {
            renderShippingAddressOptions(state)
            return
        }
        if (state.step == SmartQuoteStep.LineItems) {
            renderLineItemsOptions(state)
            return
        }
        renderWrappedOptions(options, state)
    }

    private fun renderShippingAddressOptions(state: SmartQuoteUiState) {
        renderWrappedOptions(
            state.selectedCustomer?.shippingAddresses.orEmpty().map { it.smartQuoteOptionLabel() },
            state
        )
        binding.optionsContainer.addView(createAddNewAddressButton())
    }

    private fun renderLineItemsOptions(state: SmartQuoteUiState) {
        if (state.selectedProducts.isEmpty()) {
            binding.optionsContainer.addView(createLineItemsEmptyHint())
        } else {
            state.selectedProducts.forEach { product ->
                binding.optionsContainer.addView(createSelectedProductRow(product))
            }
        }
        binding.optionsContainer.addView(createAddMoreProductsCard())
        if (state.frequentSearchQueries.isNotEmpty()) {
            binding.optionsContainer.addView(
                createLineItemsSectionLabel(getString(R.string.smart_quote_line_items_frequent_searches))
            )
            binding.optionsContainer.addView(createFrequentSearchChipRow(state.frequentSearchQueries))
        }
    }

    private fun createLineItemsSectionLabel(text: String): View {
        return AppTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.ds_space_8)
                bottomMargin = resources.getDimensionPixelSize(R.dimen.ds_space_4)
            }
            this.text = text.uppercase(Locale.US)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._7ssp))
            typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
        }
    }

    private fun createLineItemsEmptyHint(): View {
        return AppTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.ds_space_4)
            }
            text = getString(R.string.smart_quote_line_items_empty)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._8ssp))
        }
    }

    private fun createSelectedProductRow(product: QuoteCreationProductUi): View {
        val itemBinding = ItemSmartQuoteLineItemBinding.inflate(
            layoutInflater,
            binding.optionsContainer,
            false
        )
        val thumbBackground = if (product.brandThumbnail) {
            R.drawable.bg_product_thumb_cisco
        } else {
            R.drawable.bg_product_thumb_placeholder
        }
        itemBinding.thumbnailFrame.setBackgroundResource(thumbBackground)
        itemBinding.thumbnailLabel.text = product.thumbnailLabel
        itemBinding.thumbnailLabel.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (product.brandThumbnail) R.color.ds_on_primary else R.color.ds_text_muted
            )
        )
        itemBinding.productNameText.text = product.name
        itemBinding.productPriceText.text = NumberFormat.getCurrencyInstance(Locale.US).format(product.unitPrice)
        itemBinding.productMenuButton.setOnClickListener { anchor ->
            showLineItemProductMenu(anchor, product)
        }
        itemBinding.root.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = resources.getDimensionPixelSize(R.dimen.ds_space_8)
        }
        return itemBinding.root
    }

    private fun showLineItemProductMenu(anchor: View, product: QuoteCreationProductUi) {
        PopupMenu(requireContext(), anchor).apply {
            inflate(R.menu.menu_smart_quote_line_item)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_line_item -> {
                        showProductActionsBottomSheet(product)
                        true
                    }
                    R.id.action_delete_line_item -> {
                        confirmRemoveLineItemProduct(product)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun confirmRemoveLineItemProduct(product: QuoteCreationProductUi) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_quote_delete_product_confirm_title)
            .setMessage(getString(R.string.add_quote_delete_product_confirm_message, product.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.add_quote_delete_product_confirm_action) { _, _ ->
                viewModel.removeSelectedProduct(product.id)
            }
            .show()
    }

    private fun showProductActionsBottomSheet(product: QuoteCreationProductUi) {
        val state = viewModel.uiState.value
        val shippingAddresses = state.selectedCustomer?.shippingAddresses.orEmpty()
        QuoteProductActionsBottomSheet(
            fragment = this,
            initialProduct = product,
            shippingAddresses = shippingAddresses,
            defaultShippingAddress = state.selectedShippingAddress ?: shippingAddresses.firstOrNull(),
            quoteDeliveryDate = state.deliveryDate,
            formatCurrency = { amount -> NumberFormat.getCurrencyInstance(Locale.US).format(amount) },
            onApply = viewModel::updateSelectedProduct,
            onDelete = { viewModel.removeSelectedProduct(product.id) }
        ).show()
    }

    private fun createFrequentSearchChipRow(queries: List<String>): View {
        val row = createOptionRowContainer()
        queries.forEach { query ->
            row.addView(
                createCompactOptionView(
                    label = query,
                    selected = false,
                    itemWidth = LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setOnClickListener { openProductSearchWithQuery(query) }
                }
            )
        }
        return row
    }

    private fun createAddMoreProductsCard(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.ds_space_4)
            }
            setBackgroundResource(R.drawable.bg_smart_quote_add_more_products)
            isClickable = true
            isFocusable = true
            foreground = android.util.TypedValue().let { typedValue ->
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                ContextCompat.getDrawable(context, typedValue.resourceId)
            }
            setPadding(
                resources.getDimensionPixelSize(R.dimen.ds_space_16),
                resources.getDimensionPixelSize(R.dimen.ds_space_20),
                resources.getDimensionPixelSize(R.dimen.ds_space_16),
                resources.getDimensionPixelSize(R.dimen.ds_space_20)
            )
            setOnClickListener { openProductSearchViaAskAi() }
            addView(ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp),
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp)
                )
                setBackgroundResource(R.drawable.bg_smart_quote_add_more_icon)
                scaleType = ImageView.ScaleType.CENTER
                setImageResource(R.drawable.ia_ic_add)
                imageTintList = ContextCompat.getColorStateList(context, R.color.ds_primary)
            })
            addView(AppTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.ds_space_8)
                }
                text = getString(R.string.smart_quote_line_items_add_more)
                setTextColor(ContextCompat.getColor(context, R.color.ds_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._8ssp))
                typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
            })
        }
    }

    private fun renderWrappedOptions(options: List<String>, state: SmartQuoteUiState) {
        val maxRowWidth = binding.optionsContainer.width
            .takeIf { it > 0 }
            ?: (resources.displayMetrics.widthPixels - resources.getDimensionPixelSize(R.dimen.ds_space_32))
        val gap = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
        val minItemWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._68sdp)
        val horizontalPadding = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
        val textPaint = TextPaint().apply {
            textSize = resources.getDimension(com.intuit.ssp.R.dimen._8ssp)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
        }

        var currentRow = createOptionRowContainer()
        var currentRowWidth = 0
        options.forEach { option ->
            val label = if (state.step == SmartQuoteStep.LineItems) {
                option.toSmartQuoteProductName()
            } else {
                option.toSmartQuoteOptionLabel()
            }
            val itemWidth = (textPaint.measureText(label).toInt() + horizontalPadding)
                .coerceAtLeast(minItemWidth)
                .coerceAtMost(maxRowWidth)
            val itemTotalWidth = itemWidth + gap
            val nextWidth = currentRowWidth + itemTotalWidth
            if (currentRow.childCount > 0 && nextWidth > maxRowWidth) {
                binding.optionsContainer.addView(currentRow)
                currentRow = createOptionRowContainer()
                currentRowWidth = 0
            }
            currentRow.addView(
                createCompactOptionView(
                    label = label,
                    selected = isSelected(option, state),
                    itemWidth = itemWidth
                ).apply {
                    setOnClickListener { viewModel.onOptionSelected(option) }
                }
            )
            currentRowWidth += itemTotalWidth
        }
        if (currentRow.childCount > 0) {
            binding.optionsContainer.addView(currentRow)
        }
    }

    private fun createOptionRowContainer(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }
    }

    private fun createCompactOptionView(
        label: String,
        selected: Boolean,
        itemWidth: Int
    ): AppTextView {
        return AppTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                itemWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
                bottomMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)
            }
            minHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._34sdp)
            setBackgroundResource(
                if (selected) R.drawable.bg_ask_ai_refine_option_selected else R.drawable.bg_ask_ai_refine_option
            )
            foreground = android.util.TypedValue().let { typedValue ->
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                ContextCompat.getDrawable(context, typedValue.resourceId)
            }
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)
            )
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            maxLines = OPTION_LABEL_MAX_LINES
            text = label
            setTextColor(ContextCompat.getColor(requireContext(), if (selected) R.color.ds_primary else R.color.ds_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._8ssp))
            typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
        }
    }

    private fun renderOptionLoadingShimmer() {
        stopOptionShimmer()
        val widths = listOf(
            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._92sdp),
            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._124sdp),
            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._78sdp)
        )
        val row = createOptionRowContainer()
        widths.forEachIndexed { index, width ->
            val chip = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    width,
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._34sdp)
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
                    bottomMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)
                }
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_ask_ai_refine_option)
                addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (width * SHIMMER_BAR_WIDTH_FACTOR).toInt(),
                        resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
                    )
                    setBackgroundResource(R.drawable.bg_ask_ai_refine_shimmer_bar)
                    startOptionShimmer(this, index)
                })
            }
            row.addView(chip)
        }
        binding.optionsContainer.addView(row)
    }

    private fun startOptionShimmer(view: View, index: Int) {
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, SHIMMER_MIN_ALPHA, 1f).apply {
            duration = SHIMMER_DURATION_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            startDelay = index * SHIMMER_STAGGER_MS
            start()
        }
        optionShimmerAnimators.add(animator)
    }

    private fun stopOptionShimmer() {
        optionShimmerAnimators.forEach { it.cancel() }
        optionShimmerAnimators.clear()
    }

    private fun setNextPreviewShimmerLoading(isLoading: Boolean) {
        val safeBinding = _binding ?: return
        val shouldShowShimmer = isLoading && safeBinding.nextPreviewCard.isVisible
        safeBinding.nextPreviewStepText.isVisible = !shouldShowShimmer
        safeBinding.nextPreviewTitleText.isVisible = !shouldShowShimmer
        safeBinding.nextPreviewShimmer.isVisible = shouldShowShimmer
        if (shouldShowShimmer) {
            startNextPreviewShimmer()
        } else {
            stopNextPreviewShimmer()
        }
    }

    private fun startNextPreviewShimmer() {
        if (nextPreviewShimmerAnimators.isNotEmpty()) return
        collectNextPreviewShimmerViews().forEachIndexed { index, view ->
            val animator = ObjectAnimator.ofFloat(view, View.ALPHA, SHIMMER_MIN_ALPHA, 1f).apply {
                duration = SHIMMER_DURATION_MS
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                startDelay = index * SHIMMER_STAGGER_MS
                start()
            }
            nextPreviewShimmerAnimators.add(animator)
        }
    }

    private fun stopNextPreviewShimmer() {
        nextPreviewShimmerAnimators.forEach { it.cancel() }
        nextPreviewShimmerAnimators.clear()
        collectNextPreviewShimmerViews().forEach { it.alpha = 1f }
    }

    private fun collectNextPreviewShimmerViews(): List<View> {
        val safeBinding = _binding ?: return emptyList()
        val shimmerViews = mutableListOf<View>()
        fun collect(view: View) {
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    collect(view.getChildAt(index))
                }
            } else if (view.background != null) {
                shimmerViews.add(view)
            }
        }
        collect(safeBinding.nextPreviewShimmer)
        return shimmerViews
    }

    private fun SmartQuoteUiState.isCurrentStepLoading(): Boolean {
        return when (step) {
            SmartQuoteStep.Customer -> isCustomerSearchLoading
            SmartQuoteStep.ShippingAddress,
            SmartQuoteStep.BillingAddress -> isCustomerDetailsLoading
            SmartQuoteStep.PaymentTerms -> isPaymentTermsLoading
            else -> false
        }
    }

    private fun SmartQuoteUiState.emptyOptionsMessageRes(): Int? {
        return when (step) {
            SmartQuoteStep.Customer -> R.string.smart_quote_no_customer_results
            SmartQuoteStep.ShippingAddress -> if (selectedCustomer == null) {
                R.string.smart_quote_select_customer_first
            } else {
                R.string.smart_quote_no_shipping_addresses
            }
            SmartQuoteStep.BillingAddress -> if (selectedCustomer == null) {
                R.string.smart_quote_select_customer_first
            } else {
                R.string.smart_quote_no_billing_addresses
            }
            SmartQuoteStep.PaymentTerms -> if (selectedCustomer == null) {
                R.string.smart_quote_select_customer_first
            } else {
                R.string.smart_quote_no_payment_terms
            }
            else -> null
        }
    }

    private fun renderReview(state: SmartQuoteUiState) {
        binding.reviewContent.removeAllViews()
        val missingFields = state.missingFields.ifEmpty { state.missingFields() }
        binding.reviewContent.addView(createReadinessCard(missingFields.size))
        binding.reviewContent.addView(
            createReviewRow(ReviewRow("Project Name", state.quoteTitle, SmartQuoteStep.QuoteTitle), missingFields)
        )
        binding.reviewContent.addView(
            createReviewRow(ReviewRow("Customer", state.customer, SmartQuoteStep.Customer), missingFields)
        )
        binding.reviewContent.addView(
            createReviewRow(ReviewRow("Billing Address", state.billingAddress, SmartQuoteStep.BillingAddress), missingFields)
        )
        binding.reviewContent.addView(
            createReviewRow(ReviewRow("Shipping Address", state.shippingAddress, SmartQuoteStep.ShippingAddress), missingFields)
        )
        binding.reviewContent.addView(createPaymentLogisticsCard(state, missingFields))
        binding.reviewContent.addView(createProductsCard(state, missingFields))
        binding.reviewContent.addView(createTotalCard(state))
    }

    private fun reviewStatusText(state: SmartQuoteUiState): String {
        val missingCount = state.missingFields().size
        return if (missingCount == 0) {
            getString(R.string.smart_quote_review_complete)
        } else {
            getString(R.string.smart_quote_review_missing_format, missingCount)
        }
    }

    private fun createReadinessCard(missingCount: Int): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.ds_space_10)
            }
            setBackgroundResource(R.drawable.bg_ask_ai_refine_card)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.ds_space_12),
                resources.getDimensionPixelSize(R.dimen.ds_space_12),
                resources.getDimensionPixelSize(R.dimen.ds_space_12),
                resources.getDimensionPixelSize(R.dimen.ds_space_12)
            )
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(AppTextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    text = SmartQuoteStep.Review.question
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
                    textSize = 14f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
                addView(AppTextView(requireContext()).apply {
                    text = if (missingCount == 0) "100%" else "${7 - missingCount} of 7"
                    gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.bg_ask_ai_refine_badge)
                    setPadding(
                        resources.getDimensionPixelSize(R.dimen.ds_space_8),
                        resources.getDimensionPixelSize(R.dimen.ds_space_4),
                        resources.getDimensionPixelSize(R.dimen.ds_space_8),
                        resources.getDimensionPixelSize(R.dimen.ds_space_4)
                    )
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.quotes_status_draft_text))
                    textSize = 10f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
            })
            addView(AppTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.ds_space_4)
                }
                text = if (missingCount == 0) {
                    getString(R.string.smart_quote_review_complete)
                } else {
                    getString(R.string.smart_quote_review_missing_instruction)
                }
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_secondary))
                textSize = 11f
            })
        }
    }

    private fun createAddNewAddressButton(): View {
        return AppTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._34sdp)
            ).apply {
                topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._3sdp)
            }
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_ask_ai_refine_option)
            foreground = android.util.TypedValue().let { typedValue ->
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                ContextCompat.getDrawable(context, typedValue.resourceId)
            }
            text = "+ Add New Address"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._8ssp))
            typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                0,
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp),
                0
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createOptionStatusView(message: String): AppTextView {
        return AppTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.bg_ask_ai_refine_option)
            setPadding(
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._9sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._9sdp)
            )
            text = message
            setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._10ssp))
        }
    }

    private fun createReviewRow(row: ReviewRow, missingFields: List<SmartQuoteMissingField>): View {
        val missing = missingFields.any { it.step == row.step }
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.ds_space_8)
            }
            setBackgroundResource(R.drawable.bg_ask_ai_refine_option)
            isClickable = true
            isFocusable = true
            setOnClickListener { viewModel.navigateToStep(row.step) }
            setPadding(
                resources.getDimensionPixelSize(R.dimen.ds_space_14),
                resources.getDimensionPixelSize(R.dimen.ds_space_12),
                resources.getDimensionPixelSize(R.dimen.ds_space_14),
                resources.getDimensionPixelSize(R.dimen.ds_space_12)
            )
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                addView(AppTextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    text = row.label.uppercase(Locale.US)
                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (missing) R.color.ds_error else R.color.ds_text_muted
                        )
                    )
                    textSize = 9f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
                addView(AppTextView(requireContext()).apply {
                    text = getString(R.string.smart_quote_review_edit)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_primary))
                    textSize = 10f
                })
            })
            addView(AppTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.ds_space_4)
                }
                text = row.value.ifBlank { getString(R.string.smart_quote_review_missing_row) }
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (missing) R.color.ds_error else R.color.ds_text_primary
                    )
                )
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
    }

    private fun createPaymentLogisticsCard(
        state: SmartQuoteUiState,
        missingFields: List<SmartQuoteMissingField>
    ): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = reviewCardLayoutParams()
            setBackgroundResource(R.drawable.bg_ask_ai_refine_option)
            isClickable = true
            isFocusable = true
            setOnClickListener { viewModel.navigateToStep(SmartQuoteStep.PaymentTerms) }
            setPadding(
                resources.getDimensionPixelSize(R.dimen.ds_space_14),
                resources.getDimensionPixelSize(R.dimen.ds_space_12),
                resources.getDimensionPixelSize(R.dimen.ds_space_14),
                resources.getDimensionPixelSize(R.dimen.ds_space_12)
            )
            addView(createReviewHeader("Logistics & Payment", SmartQuoteStep.PaymentTerms, missingFields))
            addView(createInlineReviewLine("Payment Terms", state.paymentTerm.ifBlank { "Missing" }))
            addView(createInlineReviewLine("Delivery Date", state.deliveryDate.ifBlank { "Missing" }))
        }
    }

    private fun createProductsCard(
        state: SmartQuoteUiState,
        missingFields: List<SmartQuoteMissingField>
    ): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = reviewCardLayoutParams()
            setBackgroundResource(R.drawable.bg_smart_quote_option)
            isClickable = true
            isFocusable = true
            setOnClickListener { viewModel.navigateToStep(SmartQuoteStep.LineItems) }
            setPadding(
                resources.getDimensionPixelSize(R.dimen.ds_space_14),
                resources.getDimensionPixelSize(R.dimen.ds_space_12),
                resources.getDimensionPixelSize(R.dimen.ds_space_14),
                resources.getDimensionPixelSize(R.dimen.ds_space_12)
            )
            addView(createReviewHeader("Product Line Items", SmartQuoteStep.LineItems, missingFields))
            if (state.selectedProducts.isEmpty()) {
                addView(AppTextView(requireContext()).apply {
                    text = getString(R.string.smart_quote_review_missing_row)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_error))
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
            } else {
                state.selectedProducts.forEach { product ->
                    addView(createProductPreviewLine(product))
                }
            }
        }
    }

    private fun createReviewHeader(
        label: String,
        step: SmartQuoteStep,
        missingFields: List<SmartQuoteMissingField>
    ): View {
        val missing = missingFields.any { it.step == step }
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(AppTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = label.uppercase(Locale.US)
                setTextColor(ContextCompat.getColor(requireContext(), if (missing) R.color.ds_error else R.color.ds_text_muted))
                textSize = 8f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            addView(AppTextView(requireContext()).apply {
                text = getString(R.string.smart_quote_review_edit)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_primary))
                textSize = 10f
            })
        }
    }

    private fun createInlineReviewLine(label: String, value: String): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, resources.getDimensionPixelSize(R.dimen.ds_space_6), 0, 0)
            addView(AppTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = label
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_secondary))
                textSize = 11f
            })
            addView(AppTextView(requireContext()).apply {
                text = value
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_text_primary))
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
    }

    private fun createProductPreviewLine(product: QuoteCreationProductUi): View {
        val formattedPrice = NumberFormat.getCurrencyInstance(Locale.US).format(product.lineTotal)
        val subtitle = buildList {
            if (product.quantity > 1) add("Qty: ${product.quantity}")
            val meta = listOf(product.sku, product.category).filter { it.isNotBlank() }.joinToString(" - ")
            if (meta.isNotBlank()) add(meta)
        }.joinToString(" - ")
        val itemBinding = ItemSmartQuoteReviewProductBinding.inflate(
            layoutInflater,
            binding.reviewContent,
            false
        )
        val thumbBackground = if (product.brandThumbnail) {
            R.drawable.bg_product_thumb_cisco
        } else {
            R.drawable.bg_product_thumb_placeholder
        }
        itemBinding.productIconFrame.setBackgroundResource(thumbBackground)
        itemBinding.productIconLabel.text = product.thumbnailLabel
        itemBinding.productIconLabel.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (product.brandThumbnail) R.color.ds_on_primary else R.color.ds_text_muted
            )
        )
        itemBinding.productNameText.text = product.name
        itemBinding.productSubtitleText.isVisible = subtitle.isNotBlank()
        itemBinding.productSubtitleText.text = subtitle
        itemBinding.productPriceText.text = formattedPrice
        return itemBinding.root
    }

    private fun reviewCardLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = resources.getDimensionPixelSize(R.dimen.ds_space_8)
        }
    }

    private fun createTotalCard(state: SmartQuoteUiState): View {
        val subtotal = state.selectedProducts.sumOf { it.lineTotal }
        val tax = subtotal * TAX_RATE
        val total = subtotal + tax
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.ds_space_2)
                bottomMargin = resources.getDimensionPixelSize(R.dimen.ds_space_12)
            }
            setBackgroundResource(R.drawable.bg_smart_quote_review_total)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.ds_space_14),
                resources.getDimensionPixelSize(R.dimen.ds_space_12),
                resources.getDimensionPixelSize(R.dimen.ds_space_14),
                resources.getDimensionPixelSize(R.dimen.ds_space_12)
            )
            addView(createTotalLine(getString(R.string.smart_quote_review_subtotal), subtotal))
            addView(createTotalLine(getString(R.string.smart_quote_review_tax), tax))
            addView(createTotalLine(getString(R.string.smart_quote_review_total), total, emphasize = true))
        }
    }

    private fun createTotalLine(label: String, amount: Double, emphasize: Boolean = false): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(AppTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = label
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ia_white))
                textSize = if (emphasize) 14f else 10f
                typeface = if (emphasize) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            })
            addView(AppTextView(requireContext()).apply {
                text = NumberFormat.getCurrencyInstance(Locale.US).format(amount)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.ia_white))
                textSize = if (emphasize) 16f else 10f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
    }

    private fun isSelected(option: String, state: SmartQuoteUiState): Boolean {
        return when (state.step) {
            SmartQuoteStep.Customer -> state.customer == option
            SmartQuoteStep.LineItems -> false
            SmartQuoteStep.ShippingAddress -> state.shippingAddress == option
            SmartQuoteStep.BillingAddress -> state.billingAddress == option ||
                (option == SmartQuoteViewModel.SAME_AS_SHIPPING && state.billingAddress == state.shippingAddress)
            SmartQuoteStep.PaymentTerms -> state.selectedPaymentTerm?.let { term ->
                option == term.name || option.startsWith("${term.name}\n")
            } ?: (state.paymentTerm == option)
            else -> false
        }
    }

    private fun animateCardOut(direction: Int, endAction: () -> Unit) {
        if (isCardTransitionRunning) return
        isCardTransitionRunning = true
        setNextPreviewShimmerLoading(true)
        val card = binding.activeCard
        val width = card.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        card.animate().cancel()
        card.animate()
            .translationX(direction * width * CARD_EXIT_DISTANCE_FACTOR)
            .rotation(direction * CARD_ROTATION_DEGREES)
            .scaleX(CARD_EXIT_SCALE)
            .scaleY(CARD_EXIT_SCALE)
            .alpha(0f)
            .setDuration(CARD_EXIT_DURATION_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                pendingCardAnimationDirection = direction
                endAction()
            }
            .start()
    }

    private fun animateCardIn(direction: Int) {
        val card = binding.activeCard
        val width = card.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        card.animate().cancel()
        card.translationX = -direction * width * CARD_ENTER_DISTANCE_FACTOR
        card.translationY = 0f
        card.rotation = -direction * CARD_ROTATION_DEGREES
        card.scaleX = CARD_ENTER_SCALE
        card.scaleY = CARD_ENTER_SCALE
        card.alpha = 0f
        card.animate()
            .translationX(0f)
            .rotation(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(CARD_ENTER_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                resetCardTransform()
                setNextPreviewShimmerLoading(false)
                isCardTransitionRunning = false
            }
            .start()
    }

    private fun animateCardAppear() {
        if (isCardTransitionRunning) return
        setNextPreviewShimmerLoading(false)
        binding.activeCard.animate().cancel()
        binding.activeCard.translationX = 0f
        binding.activeCard.translationY = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp).toFloat()
        binding.activeCard.rotation = 0f
        binding.activeCard.scaleX = CARD_INITIAL_SCALE
        binding.activeCard.scaleY = CARD_INITIAL_SCALE
        binding.activeCard.alpha = 0f
        binding.activeCard.animate()
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(CARD_INITIAL_DURATION_MS)
            .setInterpolator(OvershootInterpolator(CARD_OVERSHOOT_TENSION))
            .withEndAction { resetCardTransform() }
            .start()
    }

    private fun resetCardTransform() {
        binding.activeCard.animate().cancel()
        binding.activeCard.translationX = 0f
        binding.activeCard.translationY = 0f
        binding.activeCard.rotation = 0f
        binding.activeCard.scaleX = 1f
        binding.activeCard.scaleY = 1f
        binding.activeCard.alpha = 1f
    }

    private fun showDeliveryDatePicker() {
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(SmartQuoteStep.DeliveryDate.question)
            .build()
            .apply {
                addOnPositiveButtonClickListener { utcMillis ->
                    val formatted = SimpleDateFormat("MM-dd-yyyy", Locale.US).format(utcMillis)
                    viewModel.onDateSelected(formatted)
                }
            }
            .show(parentFragmentManager, DATE_PICKER_TAG)
    }

    private fun requestVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), R.string.smart_quote_voice_unavailable, Toast.LENGTH_SHORT).show()
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
        setVoiceListening(true)
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
                    setVoiceListening(false)
                    Toast.makeText(requireContext(), R.string.smart_quote_voice_error, Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    setVoiceListening(false)
                    val spokenText = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (spokenText.isBlank()) {
                        Toast.makeText(requireContext(), R.string.smart_quote_voice_error, Toast.LENGTH_SHORT).show()
                        return
                    }
                    viewModel.onVoiceAnswer(spokenText)
                }
            })
        }
        speechRecognizer?.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, viewModel.uiState.value.step.question)
            }
        )
    }

    private fun setVoiceListening(listening: Boolean) {
        binding.micButton.isEnabled = !listening
        binding.micButton.alpha = if (listening) 0.7f else 1f
        if (listening) {
            Toast.makeText(requireContext(), R.string.smart_quote_listening, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSavePlaceholder() {
        Toast.makeText(requireContext(), R.string.smart_quote_saved, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        stopOptionShimmer()
        stopNextPreviewShimmer()
        speechRecognizer?.destroy()
        speechRecognizer = null
        previousSoftInputMode?.let { requireActivity().window.setSoftInputMode(it) }
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val CARD_DIRECTION_NONE = 0
        private const val CARD_DIRECTION_NEXT = -1
        private const val CARD_DIRECTION_PREVIOUS = 1
        private const val CARD_EXIT_DISTANCE_FACTOR = 0.58f
        private const val CARD_ENTER_DISTANCE_FACTOR = 0.38f
        private const val CARD_ROTATION_DEGREES = 7f
        private const val CARD_EXIT_SCALE = 0.94f
        private const val CARD_ENTER_SCALE = 0.96f
        private const val CARD_INITIAL_SCALE = 0.94f
        private const val CARD_OVERSHOOT_TENSION = 1.05f
        private const val CARD_EXIT_DURATION_MS = 240L
        private const val CARD_ENTER_DURATION_MS = 360L
        private const val CARD_INITIAL_DURATION_MS = 380L
        private const val DATE_PICKER_TAG = "smart_quote_delivery_date_picker"
        private const val DELIVERY_DATE_DIGIT_COUNT = 8
        private const val DELIVERY_DATE_MAX_LENGTH = 10
        private const val OPTION_LABEL_MAX_LINES = 1
        private const val SWIPE_DISTANCE_THRESHOLD = 80
        private const val DISABLED_BUTTON_ALPHA = 0.55f
        private const val SHIMMER_BAR_WIDTH_FACTOR = 0.58f
        private const val SHIMMER_MIN_ALPHA = 0.38f
        private const val SHIMMER_DURATION_MS = 760L
        private const val SHIMMER_STAGGER_MS = 120L
        private const val TAX_RATE = 0.0825
    }
}

private data class ReviewRow(
    val label: String,
    val value: String,
    val step: SmartQuoteStep
)

private fun com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi.smartQuoteOptionLabel(): String {
    return displayTextWithLocation.ifBlank { displayText }
}

private fun String.toSmartQuoteOptionLabel(): String {
    return replace("\n", " - ").trim()
}



