package com.axelliant.hris.features.inventory.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.features.inventory.products.ai.ParsedProductAiFilter
import com.axelliant.hris.features.inventory.products.ai.ProductAiAvailability
import com.axelliant.hris.features.inventory.products.ai.ProductAiCategoryContext
import com.axelliant.hris.features.inventory.products.ai.ProductAiCategorySpecSuggestion
import com.axelliant.hris.features.inventory.products.ai.ProductAiContextFilter
import com.axelliant.hris.features.inventory.products.ai.ProductAiFilterOption
import com.axelliant.hris.features.inventory.products.ai.ProductAiFilterType
import com.axelliant.hris.features.inventory.products.ai.ProductAiQueryIntent
import com.axelliant.hris.features.inventory.products.ai.ProductAiSearchContext
import com.axelliant.hris.features.inventory.products.ai.ProductAiSearchEngine
import com.axelliant.hris.features.inventory.products.ai.ProductAiStatus
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.DynamicCategoryFilter
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.DynamicSpecFilter
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.StaticCategoryFilterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

@HiltViewModel
class AskAiSearchViewModel @Inject constructor(
    private val staticCategoryFilterRepository: StaticCategoryFilterRepository,
    private val searchEngine: ProductAiSearchEngine
) : ViewModel() {
    private val _uiState = MutableStateFlow<AskAiSearchUiState>(AskAiSearchUiState.Input())
    val uiState = _uiState.asStateFlow()

    private var lookupContext = ProductAiSearchContext()

    fun onQueryChanged(query: String) {
        val current = _uiState.value
        if (current is AskAiSearchUiState.Input) {
            _uiState.value = current.copy(query = query, errorMessage = null)
        }
    }

    fun useExample(example: String) {
        _uiState.value = AskAiSearchUiState.Input(query = example)
    }

    fun updateVoiceError(message: String) {
        updateInputError(message)
    }

    fun updateInputError(message: String) {
        val current = _uiState.value
        _uiState.value = when (current) {
            is AskAiSearchUiState.Input -> current.copy(errorMessage = message)
            is AskAiSearchUiState.Review -> current.copy(warningsOverride = current.warningsOverride + message)
            else -> AskAiSearchUiState.Input(errorMessage = message)
        }
    }

    fun clearInputError() {
        val current = _uiState.value
        if (current is AskAiSearchUiState.Input) {
            _uiState.value = current.copy(errorMessage = null)
        }
    }

    fun generateFilters(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            _uiState.value = AskAiSearchUiState.Input(
                query = query,
                errorMessage = "Please enter or speak a search request."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AskAiSearchUiState.Loading()
            yield()
            val lookupWarnings = withContext(Dispatchers.Default) {
                ensureLookupsLoaded()
            }
            val parsed = withContext(Dispatchers.Default) {
                searchEngine.parse(cleanQuery, lookupContext)
            }
            val parsedWithWarnings = parsed.copy(warnings = (parsed.warnings + lookupWarnings).distinct())

            _uiState.value = if (parsedWithWarnings.hasMeaningfulFilters) {
                AskAiSearchUiState.Review(parsed = parsedWithWarnings)
            } else {
                AskAiSearchUiState.Input(
                    query = cleanQuery,
                    errorMessage = "I could not confidently detect filters. Please edit manually or try again."
                )
            }
        }
    }

    fun removeFilter(type: ProductAiFilterType, id: String? = null) {
        val review = _uiState.value as? AskAiSearchUiState.Review ?: return
        _uiState.value = review.copy(
            parsed = review.parsed.withRemovedFilter(type, id),
            isEditing = review.isEditing,
            hasUserEdited = true
        )
    }

    fun updateEditableValues(
        searchText: String,
        minPrice: Double?,
        maxPrice: Double?,
        status: ProductAiStatus?,
        availability: ProductAiAvailability?
    ) {
        val review = _uiState.value as? AskAiSearchUiState.Review ?: return
        _uiState.value = review.copy(
            parsed = review.parsed.copy(
                searchText = searchText.trim(),
                minPrice = minPrice,
                maxPrice = maxPrice,
                exactPrice = null,
                status = status,
                availability = availability
            ),
            hasUserEdited = true
        )
    }

    fun applyEditedFilterState(filterState: ProductFilterSheetState) {
        val review = _uiState.value as? AskAiSearchUiState.Review ?: return
        _uiState.value = review.copy(
            parsed = review.parsed.copy(
                searchText = filterState.searchText,
                status = filterState.status,
                manufacturers = filterState.manufacturers.toAiFilterOptions(),
                vendors = filterState.vendors.toAiFilterOptions(),
                categories = emptyList(),
                minPrice = filterState.minListPrice,
                maxPrice = filterState.maxListPrice,
                exactPrice = null,
                dynamicCategorySpecs = ProductAiCategorySpecSuggestion(
                    categoryName = filterState.dynamicCategorySpecs.categoryName,
                    selectedOptions = filterState.dynamicCategorySpecs.selectedOptions
                ),
                availability = filterState.availability
            ),
            isEditing = false,
            hasUserEdited = true
        )
    }

    fun startClarificationFlow(): Boolean {
        val review = _uiState.value as? AskAiSearchUiState.Review ?: return false
        _uiState.value = AskAiSearchUiState.Loading(
            mode = AskAiLoadingMode.Refining,
            review = review
        )
        viewModelScope.launch {
            delay(REFINEMENT_LOADING_DELAY_MS)
            val loadingState = _uiState.value as? AskAiSearchUiState.Loading ?: return@launch
            if (loadingState.mode != AskAiLoadingMode.Refining) return@launch
            val sourceReview = loadingState.review ?: return@launch
            val questions = buildClarificationQuestions(sourceReview.parsed, emptyMap())
            _uiState.value = if (questions.isEmpty()) {
                sourceReview.copy(
                    warningsOverride = (sourceReview.warningsOverride + NO_REFINEMENT_QUESTIONS_MESSAGE).distinct()
                )
            } else {
                AskAiSearchUiState.Clarifying(
                    parsed = sourceReview.parsed,
                    questions = questions
                )
            }
        }
        return true
    }

    fun updateClarificationSearchQuery(query: String) {
        val current = _uiState.value as? AskAiSearchUiState.Clarifying ?: return
        _uiState.value = current.copy(optionQuery = query)
    }

    fun selectClarificationOption(optionId: String) {
        val current = _uiState.value as? AskAiSearchUiState.Clarifying ?: return
        val question = current.currentQuestion ?: return
        val existingSelections = current.currentAnswer.selectedOptionIds
        val nextSelections = if (question.allowsMultiple) {
            if (optionId in existingSelections) {
                existingSelections - optionId
            } else {
                existingSelections + optionId
            }
        } else {
            if (existingSelections.singleOrNull() == optionId) emptyList() else listOf(optionId)
        }

        val answers = current.answers.toMutableMap()
        if (nextSelections.isEmpty()) {
            answers.remove(question.id)
        } else {
            answers[question.id] = ProductAiClarificationAnswer(nextSelections)
        }
        if (question.type == ProductAiClarificationType.Category) {
            answers.keys
                .filter { it.startsWith(CATEGORY_SPEC_QUESTION_PREFIX) }
                .forEach(answers::remove)
        }

        _uiState.value = current.copy(
            answers = answers,
            optionQuery = ""
        )
    }

    fun onClarificationNext(): ParsedProductAiFilter? {
        val current = _uiState.value as? AskAiSearchUiState.Clarifying ?: return null
        if (current.currentAnswer.selectedOptionIds.isEmpty()) return null
        return advanceClarification(current)
    }

    fun skipClarificationQuestion(): ParsedProductAiFilter? {
        val current = _uiState.value as? AskAiSearchUiState.Clarifying ?: return null
        return advanceClarification(current)
    }

    fun handleClarificationBack(): Boolean {
        val loading = _uiState.value as? AskAiSearchUiState.Loading
        if (loading?.mode == AskAiLoadingMode.Refining && loading.review != null) {
            _uiState.value = loading.review
            return true
        }
        val current = _uiState.value as? AskAiSearchUiState.Clarifying ?: return false
        if (current.currentIndex > 0) {
            _uiState.value = current.copy(
                currentIndex = current.currentIndex - 1,
                optionQuery = ""
            )
        } else {
            _uiState.value = AskAiSearchUiState.Review(
                parsed = current.toMergedParsed(),
                hasUserEdited = current.hasSelectedAnswers
            )
        }
        return true
    }

    fun setEditing(editing: Boolean) {
        val review = _uiState.value as? AskAiSearchUiState.Review ?: return
        _uiState.value = review.copy(isEditing = editing)
    }

    fun tryAgain() {
        _uiState.value = AskAiSearchUiState.Input()
    }

    private fun advanceClarification(current: AskAiSearchUiState.Clarifying): ParsedProductAiFilter? {
        if (current.isLastQuestion) return current.toMergedParsed()
        _uiState.value = current.copy(
            currentIndex = current.currentIndex + 1,
            optionQuery = ""
        )
        return null
    }

    private fun AskAiSearchUiState.Clarifying.toMergedParsed(): ParsedProductAiFilter {
        val categoryAnswer = answers[QUESTION_CATEGORY]?.selectedOptionIds?.firstOrNull()
        val priceIntentAnswer = answers[QUESTION_PRICE_INTENT]?.selectedOptionIds?.firstOrNull()

        val manufacturerOptions = toAiOptionsForQuestion(QUESTION_MANUFACTURER)
        val vendorOptions = toAiOptionsForQuestion(QUESTION_VENDOR)
        val categoryName = categoryAnswer
            ?.let(::categoryNameForOptionId)
            ?: parsed.dynamicCategorySpecs.categoryName
        val selectedSpecs = mergedCategorySpecAnswers(categoryName)
        val dynamicSpecs = if (categoryName.isNullOrBlank()) {
            parsed.dynamicCategorySpecs
        } else {
            ProductAiCategorySpecSuggestion(
                categoryName = categoryName,
                selectedOptions = selectedSpecs
            )
        }
        val priceValue = parsed.exactPrice
        val updatedPrice = when (priceIntentAnswer) {
            PRICE_INTENT_MIN -> PriceSelection(minPrice = priceValue, maxPrice = null, exactPrice = null)
            PRICE_INTENT_MAX -> PriceSelection(minPrice = null, maxPrice = priceValue, exactPrice = null)
            PRICE_INTENT_EXACT -> PriceSelection(minPrice = priceValue, maxPrice = priceValue, exactPrice = null)
            else -> PriceSelection(parsed.minPrice, parsed.maxPrice, parsed.exactPrice)
        }
        val userConfirmed = hasSelectedAnswers
        val reasons = if (userConfirmed) {
            (parsed.reasons + "Confirmed additional filters from refinement cards.").distinct()
        } else {
            parsed.reasons
        }

        return parsed.copy(
            manufacturers = manufacturerOptions.ifEmpty { parsed.manufacturers },
            vendors = vendorOptions.ifEmpty { parsed.vendors },
            minPrice = updatedPrice.minPrice,
            maxPrice = updatedPrice.maxPrice,
            exactPrice = updatedPrice.exactPrice,
            dynamicCategorySpecs = dynamicSpecs,
            confidenceScore = if (userConfirmed) maxOf(parsed.confidenceScore, REFINED_CONFIDENCE_SCORE) else parsed.confidenceScore,
            reasons = reasons
        )
    }

    private fun AskAiSearchUiState.Clarifying.mergedCategorySpecAnswers(
        categoryName: String?
    ): Map<String, List<String>> {
        val keepExisting = parsed.dynamicCategorySpecs.categoryName.equals(categoryName, ignoreCase = true)
        val selectedSpecs = if (keepExisting) {
            parsed.dynamicCategorySpecs.selectedOptions.toMutableMap()
        } else {
            mutableMapOf()
        }
        questions
            .filter { it.type == ProductAiClarificationType.CategorySpec }
            .forEach { question ->
                val specKey = question.specKey ?: return@forEach
                val selectedLabels = answers[question.id]
                    ?.selectedOptionIds
                    .orEmpty()
                    .mapNotNull { id -> question.options.firstOrNull { it.id == id }?.label }
                if (selectedLabels.isNotEmpty()) selectedSpecs[specKey] = selectedLabels
            }
        return selectedSpecs.filterValues { it.isNotEmpty() }
    }

    private fun AskAiSearchUiState.Clarifying.toAiOptionsForQuestion(
        questionId: String
    ): List<ProductAiFilterOption> {
        val question = questions.firstOrNull { it.id == questionId } ?: return emptyList()
        val selectedIds = answers[questionId]?.selectedOptionIds.orEmpty()
        return selectedIds.mapNotNull { selectedId ->
            question.options.firstOrNull { it.id == selectedId }?.let { option ->
                ProductAiFilterOption(id = option.id, name = option.label)
            }
        }
    }

    private fun buildClarificationQuestions(
        parsed: ParsedProductAiFilter,
        answers: Map<String, ProductAiClarificationAnswer>
    ): List<ProductAiClarificationQuestion> {
        val leadingQuestions = mutableListOf<ProductAiClarificationQuestion>()
        val selectedCategoryName = selectedCategoryName(parsed, answers)
        val categoryQuestion = if (shouldAskCategory(parsed)) {
            buildCategoryQuestion(parsed)
        } else {
            null
        }

        if (shouldAskVendor(parsed)) {
            buildVendorQuestion(parsed)?.let(leadingQuestions::add)
        }
        if (shouldAskManufacturer(parsed)) {
            buildManufacturerQuestion(parsed)?.let(leadingQuestions::add)
        }
        if (shouldAskPriceIntent(parsed)) {
            buildPriceIntentQuestion(parsed)?.let(leadingQuestions::add)
        }
        selectedCategoryName
            ?.takeUnless { answers.containsKey(QUESTION_CATEGORY) }
            ?.let { buildCategorySpecQuestion(categoryName = it, parsed = parsed, answers = answers) }
            ?.let(leadingQuestions::add)

        val distinctLeading = leadingQuestions
            .distinctBy { it.id }
        return if (categoryQuestion != null) {
            distinctLeading
                .take(MAX_CLARIFICATION_QUESTIONS - 1)
                .plus(categoryQuestion)
                .distinctBy { it.id }
        } else {
            distinctLeading.take(MAX_CLARIFICATION_QUESTIONS)
        }
    }

    private fun shouldAskCategory(parsed: ParsedProductAiFilter): Boolean {
        if (parsed.dynamicCategorySpecs.hasCategory || lookupContext.dynamicCategories.isEmpty()) return false
        if (parsed.warnings.any { it.contains("category", ignoreCase = true) }) return true
        if (parsed.queryIntent == ProductAiQueryIntent.FILTER_ONLY) return true
        return parsed.isBroadSearch() || parsed.confidenceScore < HIGH_CONFIDENCE_SCORE
    }

    private fun shouldAskManufacturer(parsed: ParsedProductAiFilter): Boolean {
        val options = lookupContext.optionsForCommonKey(COMMON_MANUFACTURER_KEY).ifEmpty {
            lookupContext.manufacturers
        }
        if (parsed.manufacturers.isNotEmpty() || options.isEmpty()) return false
        return parsed.isBroadSearch() || parsed.queryIntent != ProductAiQueryIntent.PRODUCT_SEARCH
    }

    private fun shouldAskVendor(parsed: ParsedProductAiFilter): Boolean {
        val options = lookupContext.optionsForCommonKey(COMMON_VENDOR_KEY).ifEmpty { lookupContext.vendors }
        if (parsed.vendors.isNotEmpty() || options.isEmpty()) return false
        val normalized = parsed.originalQuery.normalizedForClarification()
        val hasVendorCue = VENDOR_CUES.any { normalized.containsWordOrPhrase(it) }
        return hasVendorCue || parsed.queryIntent != ProductAiQueryIntent.PRODUCT_SEARCH || parsed.confidenceScore < HIGH_CONFIDENCE_SCORE
    }

    private fun shouldAskPriceIntent(parsed: ParsedProductAiFilter): Boolean {
        return parsed.exactPrice != null &&
            parsed.minPrice == null &&
            parsed.maxPrice == null &&
            parsed.warnings.any { it.contains("price", ignoreCase = true) }
    }

    private fun ParsedProductAiFilter.isBroadSearch(): Boolean {
        val tokens = searchText.normalizedForClarification()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        return searchText.isBlank() ||
            tokens.size <= BROAD_SEARCH_TOKEN_LIMIT ||
            tokens.none { token -> token.any(Char::isDigit) || token.contains("-") || token.length >= SPECIFIC_TOKEN_LENGTH }
    }

    private fun buildCategoryQuestion(parsed: ParsedProductAiFilter): ProductAiClarificationQuestion? {
        val normalized = parsed.originalQuery.normalizedForClarification()
        val options = lookupContext.dynamicCategories
            .sortedWith(
                compareByDescending<ProductAiCategoryContext> { categoryRelevanceScore(it, normalized) }
                    .thenBy { it.name }
            )
            .map { category ->
                ProductAiClarificationOption(
                    id = category.name,
                    label = category.name
                )
            }
        if (options.isEmpty()) return null
        return ProductAiClarificationQuestion(
            id = QUESTION_CATEGORY,
            type = ProductAiClarificationType.Category,
            eyebrow = "CATEGORY",
            title = "Which category best fits this product?",
            subtitle = "This helps us ask the right spec questions.",
            searchHint = "Search category",
            options = options
        )
    }

    private fun buildCategorySpecQuestion(
        categoryName: String,
        parsed: ParsedProductAiFilter,
        answers: Map<String, ProductAiClarificationAnswer>
    ): ProductAiClarificationQuestion? {
        val category = lookupContext.dynamicCategories.firstOrNull {
            it.name.equals(categoryName, ignoreCase = true)
        } ?: return null
        val normalized = parsed.originalQuery.normalizedForClarification()
        val selectedByCategoryQuestion = answers.containsKey(QUESTION_CATEGORY)
        val filter = category.filters
            .filter { it.options.isNotEmpty() }
            .filter { parsed.dynamicCategorySpecs.selectedOptions[it.selectionKey].isNullOrEmpty() }
            .mapIndexed { index, filter -> IndexedSpecFilter(index, filter, specRelevanceScore(filter, normalized)) }
            .sortedWith(
                compareByDescending<IndexedSpecFilter> { it.score }
                    .thenBy { it.index }
            )
            .firstOrNull()
            ?.filter
            ?: return null
        val score = specRelevanceScore(filter, normalized)
        if (score <= 0 && selectedByCategoryQuestion) return null

        val options = filter.options
            .filter { it.name.isNotBlank() }
            .sortedWith(
                compareByDescending<ProductAiFilterOption> { optionRelevanceScore(it.name, normalized) }
                    .thenBy { it.name }
            )
            .map { option ->
                ProductAiClarificationOption(
                    id = option.name,
                    label = option.name
                )
            }
        if (options.isEmpty()) return null
        return ProductAiClarificationQuestion(
            id = specQuestionId(category.name, filter.selectionKey),
            type = ProductAiClarificationType.CategorySpec,
            eyebrow = category.name.uppercase(Locale.US),
            title = "Any ${filter.displayLabel} preference?",
            subtitle = if (filter.allowsMultipleSelections) {
                "Select all that matter for better matches."
            } else {
                "Select one if it matters for this search."
            },
            searchHint = "Search ${filter.displayLabel}",
            options = options,
            allowsMultiple = filter.allowsMultipleSelections,
            categoryName = category.name,
            specKey = filter.selectionKey,
            specLabel = filter.displayLabel
        )
    }

    private fun buildManufacturerQuestion(parsed: ParsedProductAiFilter): ProductAiClarificationQuestion? {
        val options = lookupContext.optionsForCommonKey(COMMON_MANUFACTURER_KEY)
            .ifEmpty { lookupContext.manufacturers }
            .prioritizedFor(parsed)
        if (options.isEmpty()) return null
        return ProductAiClarificationQuestion(
            id = QUESTION_MANUFACTURER,
            type = ProductAiClarificationType.Manufacturer,
            eyebrow = "MANUFACTURER",
            title = "Do you want a specific manufacturer?",
            subtitle = "Useful when brand matters; skip for all brands.",
            searchHint = "Search manufacturers",
            options = options
        )
    }

    private fun buildVendorQuestion(parsed: ParsedProductAiFilter): ProductAiClarificationQuestion? {
        val options = lookupContext.optionsForCommonKey(COMMON_VENDOR_KEY)
            .ifEmpty { lookupContext.vendors }
            .prioritizedFor(parsed)
        if (options.isEmpty()) return null
        return ProductAiClarificationQuestion(
            id = QUESTION_VENDOR,
            type = ProductAiClarificationType.Vendor,
            eyebrow = "VENDOR",
            title = "Which vendor should supply the product?",
            subtitle = "Helps narrow source-specific results.",
            searchHint = "Search vendors",
            options = options
        )
    }

    private fun buildPriceIntentQuestion(parsed: ParsedProductAiFilter): ProductAiClarificationQuestion? {
        val price = parsed.exactPrice ?: return null
        val label = price.formatPlain()
        return ProductAiClarificationQuestion(
            id = QUESTION_PRICE_INTENT,
            type = ProductAiClarificationType.PriceIntent,
            eyebrow = "PRICE",
            title = "How should we use price $label?",
            subtitle = "Clarifies min, max, or exact price.",
            options = listOf(
                ProductAiClarificationOption(PRICE_INTENT_MAX, "Maximum"),
                ProductAiClarificationOption(PRICE_INTENT_MIN, "Minimum"),
                ProductAiClarificationOption(PRICE_INTENT_EXACT, "Exact")
            )
        )
    }

    private fun selectedCategoryName(
        parsed: ParsedProductAiFilter,
        answers: Map<String, ProductAiClarificationAnswer>
    ): String? {
        return answers[QUESTION_CATEGORY]
            ?.selectedOptionIds
            ?.firstOrNull()
            ?.let(::categoryNameForOptionId)
            ?: parsed.dynamicCategorySpecs.categoryName
    }

    private fun categoryNameForOptionId(optionId: String): String {
        return lookupContext.dynamicCategories
            .firstOrNull { it.name.equals(optionId, ignoreCase = true) }
            ?.name
            ?: optionId
    }

    private fun List<ProductAiFilterOption>.prioritizedFor(
        parsed: ParsedProductAiFilter
    ): List<ProductAiClarificationOption> {
        val normalized = parsed.originalQuery.normalizedForClarification()
        return distinctBy { it.name.lowercase(Locale.US) }
            .sortedWith(
                compareByDescending<ProductAiFilterOption> { optionRelevanceScore(it.name, normalized) }
                    .thenBy { it.name }
            )
            .map { option ->
                ProductAiClarificationOption(
                    id = option.id,
                    label = option.name
                )
            }
    }

    private fun categoryRelevanceScore(category: ProductAiCategoryContext, query: String): Int {
        val categoryScore = optionRelevanceScore(category.name, query)
        val specScore = category.filters.maxOfOrNull { specRelevanceScore(it, query) } ?: 0
        return categoryScore + specScore
    }

    private fun specRelevanceScore(filter: ProductAiContextFilter, query: String): Int {
        val labelScore = optionRelevanceScore(filter.displayLabel, query)
        val optionScore = filter.options.maxOfOrNull { option -> optionRelevanceScore(option.name, query) } ?: 0
        return labelScore + optionScore
    }

    private val ProductAiContextFilter.displayLabel: String
        get() = label.ifBlank { key }

    private fun optionRelevanceScore(value: String, query: String): Int {
        val normalized = value.normalizedForClarification()
        if (normalized.isBlank() || query.isBlank()) return 0
        if (query.containsWordOrPhrase(normalized)) return 100
        val tokens = normalized
            .split(Regex("\\s+"))
            .filter { it.length >= MIN_RELEVANT_TOKEN_LENGTH }
        return when {
            tokens.isNotEmpty() && tokens.all { query.containsWordOrPhrase(it) } -> 80
            tokens.any { query.containsWordOrPhrase(it) } -> 35
            else -> 0
        }
    }

    private fun specQuestionId(categoryName: String, specKey: String): String {
        return "$CATEGORY_SPEC_QUESTION_PREFIX:${categoryName.lowercase(Locale.US)}:${specKey.lowercase(Locale.US)}"
    }

    private fun String.normalizedForClarification(): String {
        return lowercase(Locale.US)
            .replace("&", " and ")
            .replace(Regex("[_/]+"), " ")
            .replace(Regex("[^a-z0-9.$+\\-\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.containsWordOrPhrase(value: String): Boolean {
        val cleanValue = value.trim()
        if (cleanValue.isBlank()) return false
        return Regex("(^|\\s)${Regex.escape(cleanValue)}(\\s|$)").containsMatchIn(this)
    }

    private fun Double.formatPlain(): String {
        return if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
    }

    private suspend fun ensureLookupsLoaded(): List<String> {
        if (lookupContext.hasJsonFilters) {
            return emptyList()
        }

        val commonFilters = staticCategoryFilterRepository.getCommonFilters()
        val categories = staticCategoryFilterRepository.getCategories()
        lookupContext = ProductAiSearchContext(
            commonFilters = commonFilters.toAiContextFilters(),
            dynamicCategories = categories.toAiCategoryContexts(),
            manufacturers = commonFilters.optionsForKey(COMMON_MANUFACTURER_KEY),
            vendors = commonFilters.optionsForKey(COMMON_VENDOR_KEY),
            categories = emptyList()
        )
        return emptyList()
    }

    private fun ParsedProductAiFilter.withRemovedFilter(
        type: ProductAiFilterType,
        id: String?
    ): ParsedProductAiFilter {
        return when (type) {
            ProductAiFilterType.SearchText -> copy(searchText = "")
            ProductAiFilterType.Manufacturer -> copy(manufacturers = manufacturers.filterNot { it.id == id })
            ProductAiFilterType.Vendor -> copy(vendors = vendors.filterNot { it.id == id })
            ProductAiFilterType.Category -> copy(
                categories = categories.filterNot { it.id == id },
                dynamicCategorySpecs = ProductAiCategorySpecSuggestion()
            )
            ProductAiFilterType.MinPrice -> copy(minPrice = null, exactPrice = null)
            ProductAiFilterType.MaxPrice -> copy(maxPrice = null, exactPrice = null)
            ProductAiFilterType.Availability -> copy(availability = null)
            ProductAiFilterType.Status -> copy(status = null)
        }
    }

    private fun LinkedHashMap<String, String>.toAiFilterOptions(): List<ProductAiFilterOption> {
        return map { (id, name) -> ProductAiFilterOption(id = id, name = name) }
    }

    private fun List<DynamicSpecFilter>.toAiContextFilters(): List<ProductAiContextFilter> {
        return map { filter ->
            ProductAiContextFilter(
                key = filter.key,
                label = filter.label,
                type = filter.type,
                options = filter.options.mapNotNull { option ->
                    option.name.trim().takeIf { it.isNotEmpty() }?.let { name ->
                        ProductAiFilterOption(id = name, name = name)
                    }
                }
            )
        }
    }

    private fun List<DynamicCategoryFilter>.toAiCategoryContexts(): List<ProductAiCategoryContext> {
        return mapNotNull { category ->
            category.name.trim().takeIf { it.isNotEmpty() }?.let { name ->
                ProductAiCategoryContext(
                    name = name,
                    filters = category.filters.toAiContextFilters()
                )
            }
        }
    }

    private fun List<DynamicSpecFilter>.optionsForKey(key: String): List<ProductAiFilterOption> {
        return firstOrNull { filter -> filter.selectionKey.equals(key, ignoreCase = true) }
            ?.options
            .orEmpty()
            .mapNotNull { option ->
                option.name.trim().takeIf { it.isNotEmpty() }?.let { name ->
                    ProductAiFilterOption(id = name, name = name)
                }
            }
            .distinctBy { it.name.lowercase(Locale.US) }
    }

    private companion object {
        const val COMMON_MANUFACTURER_KEY = "ManufacturerName"
        const val COMMON_VENDOR_KEY = "vendor"
        const val QUESTION_CATEGORY = "category"
        const val QUESTION_MANUFACTURER = "manufacturer"
        const val QUESTION_VENDOR = "vendor"
        const val QUESTION_PRICE_INTENT = "price_intent"
        const val CATEGORY_SPEC_QUESTION_PREFIX = "category_spec"
        const val PRICE_INTENT_MIN = "min"
        const val PRICE_INTENT_MAX = "max"
        const val PRICE_INTENT_EXACT = "exact"
        const val MAX_CLARIFICATION_QUESTIONS = 4
        const val HIGH_CONFIDENCE_SCORE = 75
        const val REFINED_CONFIDENCE_SCORE = 82
        const val REFINEMENT_LOADING_DELAY_MS = 350L
        const val NO_REFINEMENT_QUESTIONS_MESSAGE = "No extra refine questions are available for this search."
        const val BROAD_SEARCH_TOKEN_LIMIT = 4
        const val SPECIFIC_TOKEN_LENGTH = 7
        const val MIN_RELEVANT_TOKEN_LENGTH = 3
        val VENDOR_CUES = listOf("vendor", "supplier", "seller", "sourced", "from")
    }
}

sealed class AskAiSearchUiState {
    data class Input(
        val query: String = "",
        val errorMessage: String? = null
    ) : AskAiSearchUiState()

    data class Loading(
        val mode: AskAiLoadingMode = AskAiLoadingMode.Extracting,
        val review: Review? = null
    ) : AskAiSearchUiState()

    data class Review(
        val parsed: ParsedProductAiFilter,
        val isEditing: Boolean = false,
        val hasUserEdited: Boolean = false,
        val warningsOverride: List<String> = emptyList()
    ) : AskAiSearchUiState()

    data class Clarifying(
        val parsed: ParsedProductAiFilter,
        val questions: List<ProductAiClarificationQuestion>,
        val currentIndex: Int = 0,
        val answers: Map<String, ProductAiClarificationAnswer> = emptyMap(),
        val optionQuery: String = ""
    ) : AskAiSearchUiState() {
        val currentQuestion: ProductAiClarificationQuestion?
            get() = questions.getOrNull(currentIndex)

        val currentAnswer: ProductAiClarificationAnswer
            get() = currentQuestion?.let { answers[it.id] } ?: ProductAiClarificationAnswer()

        val isLastQuestion: Boolean
            get() = currentIndex >= questions.lastIndex

        val hasSelectedAnswers: Boolean
            get() = answers.values.any { it.selectedOptionIds.isNotEmpty() }

        val progressPercent: Int
            get() = if (questions.isEmpty()) {
                0
            } else {
                (((currentIndex + 1).toFloat() / questions.size) * 100).toInt().coerceIn(0, 100)
            }

        fun visibleOptions(): List<ProductAiClarificationOption> {
            val question = currentQuestion ?: return emptyList()
            val cleanQuery = optionQuery.trim()
            val source = if (cleanQuery.isBlank()) {
                question.options
            } else {
                question.options.filter { option ->
                    option.label.contains(cleanQuery, ignoreCase = true) ||
                        option.subtitle.orEmpty().contains(cleanQuery, ignoreCase = true)
                }
            }
            val limit = if (cleanQuery.isBlank()) {
                CLARIFICATION_INITIAL_OPTION_LIMIT
            } else {
                CLARIFICATION_SEARCH_OPTION_LIMIT
            }
            return source.take(limit)
        }

        fun selectedOptionsForCurrent(): List<ProductAiClarificationOption> {
            val question = currentQuestion ?: return emptyList()
            val selectedIds = currentAnswer.selectedOptionIds
            return selectedIds.mapNotNull { id -> question.options.firstOrNull { it.id == id } }
        }
    }
}

data class ProductAiClarificationQuestion(
    val id: String,
    val type: ProductAiClarificationType,
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val searchHint: String? = null,
    val options: List<ProductAiClarificationOption>,
    val allowsMultiple: Boolean = false,
    val categoryName: String? = null,
    val specKey: String? = null,
    val specLabel: String? = null
)

data class ProductAiClarificationOption(
    val id: String,
    val label: String,
    val subtitle: String? = null
)

data class ProductAiClarificationAnswer(
    val selectedOptionIds: List<String> = emptyList()
)

enum class ProductAiClarificationType {
    Category,
    CategorySpec,
    Manufacturer,
    Vendor,
    PriceIntent
}

enum class AskAiLoadingMode {
    Extracting,
    Refining
}

private data class PriceSelection(
    val minPrice: Double?,
    val maxPrice: Double?,
    val exactPrice: Double?
)

private data class IndexedSpecFilter(
    val index: Int,
    val filter: ProductAiContextFilter,
    val score: Int
)

private const val CLARIFICATION_INITIAL_OPTION_LIMIT = 12
private const val CLARIFICATION_SEARCH_OPTION_LIMIT = 30
