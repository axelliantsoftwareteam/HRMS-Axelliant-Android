package com.axelliant.hris.features.inventory.products.ai

interface ProductAiSearchEngine {
    /**
     * RuleBasedProductAiSearchEngine can later be replaced by LiteRtProductAiSearchEngine
     * for custom on-device NLP model inference without changing the Ask AI UI flow.
     */
    fun parse(query: String, context: ProductAiSearchContext): ParsedProductAiFilter
}
