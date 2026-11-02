package com.financemanage.mobile.feature.budget.di

import com.financemanage.mobile.feature.budget.data.BudgetRepository
import com.financemanage.mobile.feature.budget.presentation.BudgetDetailViewModel
import com.financemanage.mobile.feature.budget.presentation.BudgetFormViewModel
import com.financemanage.mobile.feature.budget.presentation.BudgetListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val budgetModule =
    module {
        single { BudgetRepository(get(), get()) }
        viewModel { BudgetListViewModel(get()) }
        viewModel { params -> BudgetDetailViewModel(params.get<String>(0), get(), get()) }
        viewModel { params -> BudgetFormViewModel(params.values.getOrNull(0) as String?, get(), get(), get(), get()) }
    }
