package com.axelliant.hris.features.inventory.products.ai

import java.util.Locale
import javax.inject.Inject

// TODO: RuleBasedProductAiSearchEngine can later be replaced or assisted by TensorFlow/LiteRT/custom NLP model.
class RuleBasedProductAiSearchEngine @Inject constructor() : ProductAiSearchEngine {

    override fun parse(query: String, context: ProductAiSearchContext): ParsedProductAiFilter {
        val normalizedQuery = query.normalizedForSearch()
        if (normalizedQuery.isBlank()) {
            return ParsedProductAiFilter(
                originalQuery = query,
                confidenceScore = 0,
                warnings = listOf("Please enter or speak a search request.")
            )
        }

        val warnings = mutableListOf<String>()
        val reasons = mutableListOf<String>()
        val statusResult = extractStatus(normalizedQuery, reasons)
        val availabilityResult = extractAvailability(normalizedQuery, reasons)
        var priceResult = extractPrice(normalizedQuery, warnings, reasons)
        val lookupResult = extractLookupEntities(normalizedQuery, context)
        val selectedFilters = selectLookupFilters(lookupResult, warnings, reasons)
        var intent = detectQueryIntent(
            text = normalizedQuery,
            selectedFilters = selectedFilters,
            statusResult = statusResult,
            priceResult = priceResult,
            availabilityResult = availabilityResult
        )

        if (intent == ProductAiQueryIntent.FILTER_ONLY && !priceResult.hasPrice) {
            standaloneFilterPrice(normalizedQuery, selectedFilters, statusResult, availabilityResult)?.let { standalone ->
                priceResult = standalone
                reasons.add("Detected exact price ${standalone.minPrice?.formatPlain()}.")
            }
        }

        val categorySpecs = extractDynamicCategorySpecs(normalizedQuery, context, warnings, reasons)
        val searchText = buildSearchText(
            text = normalizedQuery,
            intent = intent,
            selectedFilters = selectedFilters,
            priceResult = priceResult,
            availabilityResult = availabilityResult,
            statusResult = statusResult
        )
        val nonsense = (searchText.isBlank() &&
            !selectedFilters.hasAnyFilter &&
            !priceResult.hasPrice &&
            availabilityResult.availability == null &&
            statusResult.status == null &&
            !categorySpecs.hasCategory) ||
            isNonsenseSearch(searchText)

        if (searchText.isNotBlank()) {
            reasons.add("Detected searchable keywords: \"$searchText\".")
        }
        if (nonsense) {
            intent = ProductAiQueryIntent.UNCLEAR
            warnings.add("I could not understand this request. Try a product name, brand, part number, or category.")
        }

        val confidence = calculateConfidence(
            intent = intent,
            searchText = searchText,
            selectedFilters = selectedFilters,
            priceResult = priceResult,
            availabilityResult = availabilityResult,
            statusResult = statusResult,
            categorySpecs = categorySpecs,
            warnings = warnings,
            nonsense = nonsense
        )

        return ParsedProductAiFilter(
            originalQuery = query,
            queryIntent = intent,
            searchText = searchText.takeUnless { nonsense && confidence < LOW_CONFIDENCE_THRESHOLD }.orEmpty(),
            manufacturers = selectedFilters.manufacturers.map { it.option },
            vendors = selectedFilters.vendors.map { it.option },
            categories = emptyList(),
            minPrice = priceResult.minPrice,
            maxPrice = priceResult.maxPrice,
            exactPrice = priceResult.exactPrice,
            dynamicCategorySpecs = categorySpecs,
            availability = availabilityResult.availability,
            status = statusResult.status,
            confidenceScore = confidence,
            reasons = reasons.distinct(),
            warnings = warnings.distinct()
        )
    }

    private fun extractStatus(text: String, reasons: MutableList<String>): StatusResult {
        INACTIVE_STATUS_PATTERNS.firstOrNull { it.containsMatchIn(text) }?.let { pattern ->
            val phrase = pattern.find(text)?.value.orEmpty()
            reasons.add("Detected status: Inactive.")
            return StatusResult(ProductAiStatus.Inactive, phrase)
        }
        ACTIVE_STATUS_PATTERNS.firstOrNull { it.containsMatchIn(text) }?.let { pattern ->
            val phrase = pattern.find(text)?.value.orEmpty()
            reasons.add("Detected status: Active.")
            return StatusResult(ProductAiStatus.Active, phrase)
        }
        return StatusResult()
    }

    private fun extractAvailability(text: String, reasons: MutableList<String>): AvailabilityResult {
        OUT_OF_STOCK_PATTERNS.firstOrNull { it.containsMatchIn(text) }?.let { pattern ->
            val phrase = pattern.find(text)?.value.orEmpty()
            reasons.add("Detected availability: Out of Stock.")
            return AvailabilityResult(ProductAiAvailability.OutOfStock, phrase)
        }
        IN_STOCK_PATTERNS.firstOrNull { it.containsMatchIn(text) }?.let { pattern ->
            val phrase = pattern.find(text)?.value.orEmpty()
            reasons.add("Detected availability: In Stock.")
            return AvailabilityResult(ProductAiAvailability.InStock, phrase)
        }
        return AvailabilityResult()
    }

    private fun extractPrice(
        text: String,
        warnings: MutableList<String>,
        reasons: MutableList<String>
    ): PriceParseResult {
        rangePrice(text)?.let { range ->
            reasons.add("Detected price range ${range.minPrice?.formatPlain()} to ${range.maxPrice?.formatPlain()}.")
            return range
        }

        val maxPriceMatch = MAX_PRICE_PHRASES.firstNotNullOfOrNull { phrase -> phrasePriceMatch(text, phrase) }
        val minPriceMatch = MIN_PRICE_PHRASES.firstNotNullOfOrNull { phrase -> phrasePriceMatch(text, phrase) }
        val exactPriceMatch = EXACT_PRICE_PHRASES.firstNotNullOfOrNull { phrase -> phrasePriceMatch(text, phrase) }
        val ambiguousPriceMatch = AMBIGUOUS_PRICE_PHRASES.firstNotNullOfOrNull { phrase -> phrasePriceMatch(text, phrase) }
        val bareDollarMatch = Regex("\\$\\s*([0-9][0-9,]*(?:\\.\\d+)?)")
            .find(text)
            ?.let { PricePhraseMatch(it.groupValues.getOrNull(1)?.toPrice(), it.value) }
        val trailingDollarMatch = Regex("\\b([0-9][0-9,]*(?:\\.\\d+)?)\\s+dollars\\b")
            .find(text)
            ?.let { PricePhraseMatch(it.groupValues.getOrNull(1)?.toPrice(), it.value) }

        val minPrice = minPriceMatch?.price
        val maxPrice = maxPriceMatch?.price
        val exactPrice = exactPriceMatch?.price
        val ambiguousPrice = ambiguousPriceMatch?.price ?: bareDollarMatch?.price ?: trailingDollarMatch?.price
        val ambiguousPhrase = ambiguousPriceMatch?.phrase ?: bareDollarMatch?.phrase ?: trailingDollarMatch?.phrase

        if (minPrice != null) reasons.add("Detected minimum price ${minPrice.formatPlain()}.")
        if (maxPrice != null) reasons.add("Detected maximum price ${maxPrice.formatPlain()}.")
        if (exactPrice != null) reasons.add("Detected exact price ${exactPrice.formatPlain()}.")
        if (ambiguousPrice != null && minPrice == null && maxPrice == null && exactPrice == null) {
            warnings.add("A price was mentioned but the condition may need confirmation.")
            reasons.add("Detected price ${ambiguousPrice.formatPlain()}.")
        }
        if (minPrice != null && maxPrice != null && maxPrice < minPrice) {
            warnings.add("Price range looks reversed, so please confirm before applying.")
        }

        return PriceParseResult(
            minPrice = minPrice,
            maxPrice = maxPrice,
            exactPrice = exactPrice ?: ambiguousPrice,
            ambiguous = ambiguousPrice != null && minPrice == null && maxPrice == null && exactPrice == null,
            removablePhrases = listOfNotNull(
                minPriceMatch?.phrase,
                maxPriceMatch?.phrase,
                exactPriceMatch?.phrase,
                ambiguousPhrase
            )
        )
    }

    private fun rangePrice(text: String): PriceParseResult? {
        val patterns = listOf(
            Regex("\\bbetween\\s+\\$?([0-9][0-9,]*(?:\\.\\d+)?)\\s+(?:and|to)\\s+\\$?([0-9][0-9,]*(?:\\.\\d+)?)"),
            Regex("\\bfrom\\s+\\$?([0-9][0-9,]*(?:\\.\\d+)?)\\s+to\\s+\\$?([0-9][0-9,]*(?:\\.\\d+)?)"),
            Regex("\\b\\$?([0-9][0-9,]*(?:\\.\\d+)?)\\s+to\\s+\\$?([0-9][0-9,]*(?:\\.\\d+)?)\\b")
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.let { match ->
                val first = match.groupValues[1].toPrice()
                val second = match.groupValues[2].toPrice()
                if (first == null || second == null) {
                    null
                } else {
                    PriceParseResult(
                        minPrice = minOf(first, second),
                        maxPrice = maxOf(first, second),
                        removablePhrases = listOf(match.value)
                    )
                }
            }
        }
    }

    private fun phrasePriceMatch(text: String, phrase: String): PricePhraseMatch? {
        val escapedPhrase = Regex.escape(phrase)
        return Regex("\\b$escapedPhrase\\b\\s*(?:of\\s*)?\\$?\\s*([0-9][0-9,]*(?:\\.\\d+)?)")
            .find(text)
            ?.let { PricePhraseMatch(it.groupValues.getOrNull(1)?.toPrice(), it.value) }
    }

    private fun extractLookupEntities(text: String, context: ProductAiSearchContext): LookupExtractionResult {
        val manufacturers = context.optionsForCommonKey(COMMON_MANUFACTURER_KEY).ifEmpty { context.manufacturers }
        val vendors = context.optionsForCommonKey(COMMON_VENDOR_KEY).ifEmpty { context.vendors }
        // TODO: Add AttributeTags rule extraction after product search UX/backend expectations are confirmed.
        return LookupExtractionResult(
            vendors = matchLookupOptions(text, vendors, EntityType.Vendor),
            manufacturers = matchLookupOptions(text, manufacturers, EntityType.Manufacturer)
        )
    }

    private fun matchLookupOptions(
        text: String,
        options: List<ProductAiFilterOption>,
        type: EntityType
    ): List<EntityMatch> {
        return options.mapNotNull { option ->
            val match = bestNameMatch(text, option.name) ?: return@mapNotNull null
            val explicitPhrases = match.aliases
                .flatMap { alias -> explicitPhrasesFor(type, alias.value) }
                .sortedByDescending { it.length }
            val explicitPhrase = explicitPhrases.firstOrNull { text.containsWordOrPhrase(it) }
            if (type == EntityType.Vendor && explicitPhrase == null) return@mapNotNull null

            EntityMatch(
                option = option,
                type = type,
                score = if (explicitPhrase != null) EXACT_MATCH_SCORE else match.score,
                explicit = explicitPhrase != null,
                matchedValue = match.value,
                removablePhrases = listOfNotNull(explicitPhrase)
            )
        }
            .sortedWith(compareByDescending<EntityMatch> { it.score }.thenBy { it.option.name.length })
            .distinctBy { it.option.id }
            .take(MAX_MATCHES_PER_TYPE)
    }

    private fun explicitPhrasesFor(type: EntityType, value: String): List<String> {
        return when (type) {
            EntityType.Vendor -> listOf(
                "from $value",
                "from vendor $value",
                "vendor $value",
                "vendor is $value",
                "by vendor $value",
                "$value vendor",
                "supplier $value",
                "seller $value",
                "of $value",
                "with $value"
            )

            EntityType.Manufacturer -> listOf(
                "manufacturer $value",
                "manufacturer is $value",
                "by manufacturer $value",
                "from manufacturer $value",
                "$value manufacturer",
                "mfg $value",
                "made by $value",
                "brand $value"
            )
        }
    }

    private fun detectQueryIntent(
        text: String,
        selectedFilters: LookupFilterSelection,
        statusResult: StatusResult,
        priceResult: PriceParseResult,
        availabilityResult: AvailabilityResult
    ): ProductAiQueryIntent {
        val hasFilterOnlyCue = FILTER_ONLY_CUES.any { text.containsWordOrPhrase(it) }
        val hasHardFilter = selectedFilters.hasAnyFilter ||
            statusResult.status != null ||
            priceResult.hasPrice ||
            availabilityResult.availability != null
        val onlyImplicitEntityFilters = selectedFilters.hasAnyFilter &&
            selectedFilters.allMatches.all { !it.explicit } &&
            statusResult.status == null &&
            !priceResult.hasPrice &&
            availabilityResult.availability == null
        val productCandidate = buildSearchText(
            text = text,
            intent = ProductAiQueryIntent.PRODUCT_SEARCH,
            selectedFilters = selectedFilters,
            priceResult = priceResult,
            availabilityResult = availabilityResult,
            statusResult = statusResult
        )
        val filterOnlyRemainder = buildFilterOnlyRemainder(
            text = text,
            selectedFilters = selectedFilters,
            priceResult = priceResult,
            availabilityResult = availabilityResult,
            statusResult = statusResult
        )

        return when {
            hasFilterOnlyCue && hasHardFilter && filterOnlyRemainder.isBlank() -> ProductAiQueryIntent.FILTER_ONLY
            hasFilterOnlyCue && hasHardFilter && filterOnlyRemainder.hasSingleStandaloneNumber() -> {
                ProductAiQueryIntent.FILTER_ONLY
            }
            onlyImplicitEntityFilters && productCandidate.hasModelLikeSignal() -> ProductAiQueryIntent.PRODUCT_SEARCH
            hasHardFilter && productCandidate.isBlank() -> ProductAiQueryIntent.FILTER_ONLY
            hasHardFilter && productCandidate.isNotBlank() -> ProductAiQueryIntent.MIXED_SEARCH
            productCandidate.isNotBlank() -> ProductAiQueryIntent.PRODUCT_SEARCH
            else -> ProductAiQueryIntent.UNCLEAR
        }
    }

    private fun selectLookupFilters(
        lookupResult: LookupExtractionResult,
        warnings: MutableList<String>,
        reasons: MutableList<String>
    ): LookupFilterSelection {
        val selected = lookupResult.allEntityMatches.preferExplicitMatchesForAmbiguousNames()
        val ambiguousNames = selected
            .groupBy { it.option.name.lowercase(Locale.US) }
            .filterValues { matches -> matches.map { it.type }.distinct().size > 1 }
        ambiguousNames.forEach { (name, matches) ->
            if (matches.none { it.explicit }) {
                warnings.add("${name.toDisplayText()} matched multiple filter types. Please confirm.")
            }
        }

        selected.forEach { match ->
            reasons.add("Matched ${match.option.name} as ${match.type.displayName}.")
        }

        return LookupFilterSelection(
            vendors = selected.filter { it.type == EntityType.Vendor }.distinctBy { it.option.id },
            manufacturers = selected.filter { it.type == EntityType.Manufacturer }.distinctBy { it.option.id }
        )
    }

    private fun standaloneFilterPrice(
        text: String,
        selectedFilters: LookupFilterSelection,
        statusResult: StatusResult,
        availabilityResult: AvailabilityResult
    ): PriceParseResult? {
        val cleaned = buildFilterOnlyRemainder(
            text = text,
            selectedFilters = selectedFilters,
            priceResult = PriceParseResult(),
            availabilityResult = availabilityResult,
            statusResult = statusResult
        )
        val numericTokens = cleaned
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { token -> token.matches(Regex("\\$?[0-9][0-9,]*(?:\\.\\d+)?")) }
            .mapNotNull { it.removePrefix("$").toPrice() }
        val price = numericTokens.singleOrNull() ?: return null
        return PriceParseResult(
            minPrice = price,
            maxPrice = price,
            removablePhrases = listOf(price.formatPlain())
        )
    }

    private fun buildFilterOnlyRemainder(
        text: String,
        selectedFilters: LookupFilterSelection,
        priceResult: PriceParseResult,
        availabilityResult: AvailabilityResult,
        statusResult: StatusResult
    ): String {
        var cleaned = " $text "
        selectedFilters.allMatches
            .flatMap { match -> match.removablePhrases + match.matchedValue }
            .sortedByDescending { it.length }
            .forEach { cleaned = cleaned.replaceWholePhrase(it, " ") }
        priceResult.removablePhrases
            .sortedByDescending { it.length }
            .forEach { cleaned = cleaned.replaceWholePhrase(it, " ") }
        availabilityResult.removablePhrase.takeIf { it.isNotBlank() }?.let {
            cleaned = cleaned.replaceWholePhrase(it, " ")
        }
        statusResult.removablePhrase.takeIf { it.isNotBlank() }?.let {
            cleaned = cleaned.replaceWholePhrase(it, " ")
        }
        return cleaned.withoutCommandNoise()
    }

    private fun buildSearchText(
        text: String,
        intent: ProductAiQueryIntent,
        selectedFilters: LookupFilterSelection,
        priceResult: PriceParseResult,
        availabilityResult: AvailabilityResult,
        statusResult: StatusResult
    ): String {
        if (intent == ProductAiQueryIntent.FILTER_ONLY) return ""

        var cleaned = " $text "
        selectedFilters.allMatches
            .flatMap { it.removablePhrases }
            .sortedByDescending { it.length }
            .forEach { cleaned = cleaned.replaceWholePhrase(it, " ") }
        priceResult.removablePhrases
            .sortedByDescending { it.length }
            .forEach { cleaned = cleaned.replaceWholePhrase(it, " ") }
        availabilityResult.removablePhrase.takeIf { it.isNotBlank() }?.let {
            cleaned = cleaned.replaceWholePhrase(it, " ")
        }
        statusResult.removablePhrase.takeIf { it.isNotBlank() }?.let {
            cleaned = cleaned.replaceWholePhrase(it, " ")
        }
        return cleaned.withoutCommandNoise()
    }

    private fun extractDynamicCategorySpecs(
        text: String,
        context: ProductAiSearchContext,
        warnings: MutableList<String>,
        reasons: MutableList<String>
    ): ProductAiCategorySpecSuggestion {
        val category = context.dynamicCategories.firstOrNull { category ->
            bestNameMatch(text, category.name, allowShortAliases = false) != null
        } ?: run {
            warnForAmbiguousCategoryOption(text, context, warnings)
            return ProductAiCategorySpecSuggestion()
        }

        val selectedOptions = linkedMapOf<String, List<String>>()
        category.filters.forEach { filter ->
            val matches = filter.options
                .mapNotNull { option ->
                    bestNameMatch(text, option.name, allowShortAliases = false)?.let { option }
                }
                .distinctBy { it.name.lowercase(Locale.US) }
                .let { matches -> if (filter.allowsMultipleSelections) matches else matches.take(1) }
            if (matches.isNotEmpty()) {
                selectedOptions[filter.selectionKey] = matches.map { it.name }
            }
        }

        reasons.add("Detected category suggestion: ${category.name}.")
        selectedOptions.forEach { (key, values) ->
            reasons.add("Detected ${key.toDisplayText()} specs: ${values.joinToString()}.")
        }
        return ProductAiCategorySpecSuggestion(
            categoryName = category.name,
            selectedOptions = selectedOptions
        )
    }

    private fun warnForAmbiguousCategoryOption(
        text: String,
        context: ProductAiSearchContext,
        warnings: MutableList<String>
    ) {
        val matchedOptions = context.dynamicCategories.flatMap { category ->
            category.filters.flatMap { filter ->
                filter.options.mapNotNull { option ->
                    bestNameMatch(text, option.name, allowShortAliases = false)?.let {
                        CategoryOptionMatch(categoryName = category.name, optionName = option.name)
                    }
                }
            }
        }
        val ambiguous = matchedOptions
            .groupBy { it.optionName.lowercase(Locale.US) }
            .values
            .firstOrNull { matches ->
                matches.map { it.categoryName }.distinct().size > 1 &&
                    matches.first().optionName.isSpecificSpecOption()
            }
            ?: return
        warnings.add(
            "${ambiguous.first().optionName} matches specs in multiple categories. Choose a category to apply specs."
        )
    }

    private fun calculateConfidence(
        intent: ProductAiQueryIntent,
        searchText: String,
        selectedFilters: LookupFilterSelection,
        priceResult: PriceParseResult,
        availabilityResult: AvailabilityResult,
        statusResult: StatusResult,
        categorySpecs: ProductAiCategorySpecSuggestion,
        warnings: List<String>,
        nonsense: Boolean
    ): Int {
        if (nonsense) return 12

        val searchScore = if (intent == ProductAiQueryIntent.FILTER_ONLY) 0 else searchQualityScore(searchText)
        var filterScore = when (intent) {
            ProductAiQueryIntent.FILTER_ONLY -> 35
            ProductAiQueryIntent.MIXED_SEARCH -> 25
            ProductAiQueryIntent.PRODUCT_SEARCH -> 0
            ProductAiQueryIntent.UNCLEAR -> -10
        }
        if (selectedFilters.hasAnyFilter) {
            filterScore += 24 + (selectedFilters.allMatches.size * 10).coerceAtMost(26)
        }
        if (priceResult.hasPrice) filterScore += if (priceResult.ambiguous) 14 else 24
        if (availabilityResult.availability != null) filterScore += 14
        if (statusResult.status != null) filterScore += 20
        if (categorySpecs.hasCategory) filterScore += 6
        if (categorySpecs.hasSelectedOptions) filterScore += 6
        if (intent == ProductAiQueryIntent.FILTER_ONLY && filterScore >= 55) filterScore += 12

        return (maxOf(searchScore, filterScore) - warnings.size * 7).coerceIn(0, 100)
    }

    private fun searchQualityScore(searchText: String): Int {
        if (searchText.isBlank()) return 0
        if (isNonsenseSearch(searchText)) return 12

        val tokens = searchText.split(Regex("\\s+")).filter { it.isNotBlank() }
        var score = 35 + (tokens.size * 8).coerceAtMost(32)
        if (tokens.any { it.isTechnicalToken() }) score += 24
        if (tokens.any { it.any(Char::isDigit) }) score += 12
        if (tokens.any { it.length >= LONG_TOKEN_LENGTH }) score += 8
        if (tokens.size == 1 && !tokens.first().isTechnicalToken() && !tokens.first().any(Char::isDigit)) score -= 12
        if (tokens.all { it in LOW_SIGNAL_WORDS }) score -= 42
        return score.coerceIn(0, 100)
    }

    private fun isNonsenseSearch(searchText: String): Boolean {
        val tokens = searchText.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        if (tokens.all { it in LOW_SIGNAL_WORDS }) return true
        if (tokens.size == 1) {
            val token = tokens.first()
            val hasVowels = token.count { it in "aeiou" }
            val hasDigits = token.any(Char::isDigit)
            val letterCount = token.count(Char::isLetter)
            return token.length >= RANDOM_TOKEN_MIN_LENGTH &&
                letterCount >= 12 &&
                (hasVowels < 2 || hasDigits || token.uniqueCharacterRatio() < RANDOM_UNIQUE_RATIO)
        }
        return false
    }

    private fun bestNameMatch(
        text: String,
        name: String,
        allowShortAliases: Boolean = true
    ): NameMatch? {
        val aliases = aliasesFor(name, allowShortAliases)
        val best = aliases
            .sortedWith(compareByDescending<OptionAlias> { it.score }.thenByDescending { it.value.length })
            .firstOrNull { alias -> text.containsWordOrPhrase(alias.value) }
            ?: return null
        return NameMatch(value = best.value, score = best.score, aliases = aliases)
    }

    private fun aliasesFor(value: String, allowShortAliases: Boolean): List<OptionAlias> {
        val normalized = value.normalizedForSearch()
        val separatorNormalized = normalized.replace(Regex("[_\\-/]+"), " ").replace(Regex("\\s+"), " ").trim()
        val tokens = separatorNormalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        val compact = tokens.joinToString("")
        val hyphenated = tokens.joinToString("-")
        val acronym = tokens
            .filterNot { it in CORPORATE_SUFFIX_TOKENS }
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
        val firstTokenAlias = tokens.firstOrNull()?.takeIf {
            allowShortAliases &&
                tokens.size > 1 &&
                it !in PRODUCT_CLASS_WORDS &&
                tokens.drop(1).all { token -> token in SHORT_NAME_SUFFIX_TOKENS }
        }

        return buildList {
            add(OptionAlias(normalized, EXACT_MATCH_SCORE))
            if (separatorNormalized != normalized) add(OptionAlias(separatorNormalized, EXACT_MATCH_SCORE))
            if (hyphenated != normalized && hyphenated != separatorNormalized) {
                add(OptionAlias(hyphenated, COMPACT_MATCH_SCORE))
            }
            if (compact != normalized) add(OptionAlias(compact, COMPACT_MATCH_SCORE))
            if (allowShortAliases && acronym.length > 1) add(OptionAlias(acronym, ACRONYM_MATCH_SCORE))
            if (!firstTokenAlias.isNullOrBlank()) add(OptionAlias(firstTokenAlias, FIRST_TOKEN_MATCH_SCORE))
        }
            .distinctBy { it.value }
            .filter { it.value.isNotBlank() }
    }

    private fun String.normalizedForSearch(): String {
        return lowercase(Locale.US)
            .replace("&", " and ")
            .replace(Regex("\\bin\\s*-?\\s*active\\b"), "in active")
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

    private fun String.replaceWholePhrase(phrase: String, replacement: String): String {
        val cleanPhrase = phrase.trim()
        if (cleanPhrase.isBlank()) return this
        return Regex("(^|\\s)${Regex.escape(cleanPhrase)}(\\s|$)")
            .replace(this) { matchResult ->
                "${matchResult.groupValues[1]}$replacement${matchResult.groupValues[2]}"
            }
    }

    private fun String.withoutCommandNoise(): String {
        var cleaned = this
        COMMAND_PHRASES
            .sortedByDescending { it.length }
            .forEach { cleaned = cleaned.replaceWholePhrase(it, " ") }
        return cleaned
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filterUsefulSearchTokens()
            .joinToString(" ")
            .trim()
    }

    private fun String.isUsefulSearchToken(): Boolean {
        if (isBlank()) return false
        if (this in STOP_WORDS) return false
        if (length >= MIN_SEARCH_TOKEN_LENGTH) return true
        return isShortTechnicalToken()
    }

    private fun List<String>.filterUsefulSearchTokens(): List<String> {
        return filterIndexed { index, token ->
            token.isUsefulSearchToken() ||
                (token.length == 1 && token.all(Char::isDigit) && getOrNull(index + 1) in TECHNICAL_UNIT_TOKENS)
        }
    }

    private fun String.isShortTechnicalToken(): Boolean {
        return this in SHORT_TECHNICAL_TOKENS || matches(Regex("[a-z][0-9]"))
    }

    private fun String.isTechnicalToken(): Boolean {
        return any(Char::isDigit) ||
            contains("-") ||
            contains("+") ||
            contains(".") ||
            matches(Regex("[a-z]+-[a-z0-9+.-]+")) ||
            matches(Regex("[a-z]*[0-9]+[a-z0-9]*")) ||
            matches(Regex("[0-9]+(?:gb|tb|mb|mo|mhz|ghz|g|k)"))
    }

    private fun String.hasSingleStandaloneNumber(): Boolean {
        val numericTokens = split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.matches(Regex("\\$?[0-9][0-9,]*(?:\\.\\d+)?")) }
        val nonNumericTokens = split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it !in STOP_WORDS && !it.matches(Regex("\\$?[0-9][0-9,]*(?:\\.\\d+)?")) }
        return numericTokens.size == 1 && nonNumericTokens.isEmpty()
    }

    private fun String.hasModelLikeSignal(): Boolean {
        return split(Regex("\\s+")).any { token ->
            token.any(Char::isDigit) || token.contains("-") || token.length >= LONG_TOKEN_LENGTH
        }
    }

    private fun String.toPrice(): Double? = replace(",", "").toDoubleOrNull()

    private fun Double.formatPlain(): String {
        return if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
    }

    private fun String.uniqueCharacterRatio(): Double {
        return toSet().size.toDouble() / length
    }

    private fun String.toDisplayText(): String {
        return split(" ").joinToString(" ") { token ->
            token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
    }

    private fun String.isSpecificSpecOption(): Boolean {
        val normalized = normalizedForSearch()
        return normalized.any(Char::isDigit) || normalized.split(Regex("\\s+")).size > 1
    }

    private fun List<EntityMatch>.preferExplicitMatchesForAmbiguousNames(): List<EntityMatch> {
        return groupBy { it.option.name.lowercase(Locale.US) }
            .flatMap { (_, matches) ->
                matches.takeIf { group -> group.none { it.explicit } } ?: matches.filter { it.explicit }
            }
    }

    private data class StatusResult(
        val status: ProductAiStatus? = null,
        val removablePhrase: String = ""
    )

    private data class AvailabilityResult(
        val availability: ProductAiAvailability? = null,
        val removablePhrase: String = ""
    )

    private data class PricePhraseMatch(
        val price: Double?,
        val phrase: String
    )

    private data class PriceParseResult(
        val minPrice: Double? = null,
        val maxPrice: Double? = null,
        val exactPrice: Double? = null,
        val ambiguous: Boolean = false,
        val removablePhrases: List<String> = emptyList()
    ) {
        val hasPrice: Boolean
            get() = minPrice != null || maxPrice != null || exactPrice != null
    }

    private data class EntityMatch(
        val option: ProductAiFilterOption,
        val type: EntityType,
        val score: Double,
        val explicit: Boolean,
        val matchedValue: String,
        val removablePhrases: List<String>
    )

    private data class LookupExtractionResult(
        val vendors: List<EntityMatch>,
        val manufacturers: List<EntityMatch>
    ) {
        val allEntityMatches: List<EntityMatch>
            get() = vendors + manufacturers
    }

    private data class LookupFilterSelection(
        val vendors: List<EntityMatch>,
        val manufacturers: List<EntityMatch>
    ) {
        val allMatches: List<EntityMatch>
            get() = vendors + manufacturers

        val hasAnyFilter: Boolean
            get() = allMatches.isNotEmpty()
    }

    private data class NameMatch(
        val value: String,
        val score: Double,
        val aliases: List<OptionAlias>
    )

    private data class OptionAlias(
        val value: String,
        val score: Double
    )

    private data class CategoryOptionMatch(
        val categoryName: String,
        val optionName: String
    )

    private enum class EntityType(val displayName: String) {
        Vendor("vendor"),
        Manufacturer("manufacturer")
    }

    private companion object {
        const val COMMON_MANUFACTURER_KEY = "ManufacturerName"
        const val COMMON_VENDOR_KEY = "vendor"
        const val EXACT_MATCH_SCORE = 1.0
        const val COMPACT_MATCH_SCORE = 0.98
        const val ACRONYM_MATCH_SCORE = 0.93
        const val FIRST_TOKEN_MATCH_SCORE = 0.9
        const val MAX_MATCHES_PER_TYPE = 4
        const val MIN_SEARCH_TOKEN_LENGTH = 2
        const val LONG_TOKEN_LENGTH = 8
        const val LOW_CONFIDENCE_THRESHOLD = 25
        const val RANDOM_TOKEN_MIN_LENGTH = 18
        const val RANDOM_UNIQUE_RATIO = 0.45

        val FILTER_ONLY_CUES = setOf("all", "products", "product", "items", "item", "list")
        val COMMAND_PHRASES = setOf("give me", "looking for", "look for")
        val STOP_WORDS = setOf(
            "find",
            "search",
            "show",
            "me",
            "get",
            "give",
            "looking",
            "look",
            "need",
            "want",
            "please",
            "products",
            "product",
            "items",
            "item",
            "all",
            "the",
            "do",
            "we",
            "have",
            "available",
            "list",
            "for",
            "from",
            "with",
            "by",
            "of",
            "and"
        )
        val LOW_SIGNAL_WORDS = setOf(
            "good",
            "cheap",
            "things",
            "thing",
            "something",
            "anything",
            "random",
            "garbage",
            "text"
        )
        val SHORT_TECHNICAL_TOKENS = setOf("c")
        val TECHNICAL_UNIT_TOKENS = setOf("gb", "tb", "mb", "mo", "mhz", "ghz", "g", "k")
        val CORPORATE_SUFFIX_TOKENS = setOf(
            "inc",
            "llc",
            "ltd",
            "corp",
            "corporation",
            "co",
            "company"
        )
        val SHORT_NAME_SUFFIX_TOKENS = CORPORATE_SUFFIX_TOKENS + setOf(
            "system",
            "systems",
            "technology",
            "technologies",
            "network",
            "networks",
            "software",
            "solutions"
        )
        val PRODUCT_CLASS_WORDS = setOf(
            "adapter",
            "cable",
            "desktop",
            "firewall",
            "headset",
            "laptop",
            "monitor",
            "printer",
            "router",
            "scanner",
            "server",
            "storage",
            "switch",
            "tablet",
            "webcam"
        )
        val MAX_PRICE_PHRASES = listOf("under", "below", "less than", "max", "maximum", "up to")
        val MIN_PRICE_PHRASES = listOf("over", "above", "greater than", "more than", "min", "minimum", "starting at")
        val EXACT_PRICE_PHRASES = listOf("exactly", "at price", "priced at", "at")
        val AMBIGUOUS_PRICE_PHRASES = listOf("price", "with price", "price of", "around", "near", "budget")
        val INACTIVE_STATUS_PATTERNS = listOf(
            Regex("\\binactive\\b"),
            Regex("\\bin\\s+active\\b"),
            Regex("\\bin-active\\b"),
            Regex("\\bnot\\s+active\\b"),
            Regex("\\bnon\\s+active\\b"),
            Regex("\\bdisabled\\b")
        )
        val ACTIVE_STATUS_PATTERNS = listOf(
            Regex("\\bactive\\b"),
            Regex("\\benabled\\b")
        )
        val OUT_OF_STOCK_PATTERNS = listOf(
            Regex("\\bout\\s+of\\s+stock\\b"),
            Regex("\\bnot\\s+available\\b"),
            Regex("\\bunavailable\\b")
        )
        val IN_STOCK_PATTERNS = listOf(
            Regex("\\bin\\s+stock\\b"),
            Regex("\\bavailable\\s+stock\\b")
        )
    }
}
