package com.axelliant.hris.core.di

import com.axelliant.hris.features.inventory.products.ai.ProductAiSearchEngine
import com.axelliant.hris.features.inventory.products.ai.RuleBasedProductAiSearchEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductAiModule {
    @Binds
    @Singleton
    abstract fun bindProductAiSearchEngine(
        implementation: RuleBasedProductAiSearchEngine
    ): ProductAiSearchEngine
}
