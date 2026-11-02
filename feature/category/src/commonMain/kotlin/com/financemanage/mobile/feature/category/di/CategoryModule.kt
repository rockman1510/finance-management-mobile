package com.financemanage.mobile.feature.category.di

import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.feature.category.presentation.CategoryFormViewModel
import com.financemanage.mobile.feature.category.presentation.CategoryListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val categoryModule =
    module {
        viewModel { CategoryListViewModel(get()) }
        viewModel { params ->
            CategoryFormViewModel(
                categoryId = params.values.getOrNull(0) as String?,
                initialType =
                    CategoryType.entries.firstOrNull { it.name == params.values.getOrNull(1) } ?: CategoryType.EXPENSE,
                initialParentId = params.values.getOrNull(2) as String?,
                repository = get(),
            )
        }
    }
