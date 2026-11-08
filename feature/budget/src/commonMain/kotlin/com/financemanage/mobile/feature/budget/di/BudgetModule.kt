package com.financemanage.mobile.feature.budget.di

import com.financemanage.mobile.feature.budget.data.BudgetRepositoryImpl
import com.financemanage.mobile.feature.budget.domain.repository.BudgetRepository
import com.financemanage.mobile.feature.budget.domain.usecase.CreateBudgetUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.DeleteBudgetUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.GetBudgetTransactionsUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.GetBudgetUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.ObserveBudgetsUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.RefreshBudgetsUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.UpdateBudgetUseCase
import com.financemanage.mobile.feature.budget.presentation.BudgetDetailViewModel
import com.financemanage.mobile.feature.budget.presentation.BudgetFormViewModel
import com.financemanage.mobile.feature.budget.presentation.BudgetListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val budgetModule =
    module {
        // Data
        single<BudgetRepository> { BudgetRepositoryImpl(get(), get()) }
        // Domain
        factoryOf(::ObserveBudgetsUseCase)
        factoryOf(::RefreshBudgetsUseCase)
        factoryOf(::GetBudgetUseCase)
        factoryOf(::CreateBudgetUseCase)
        factoryOf(::UpdateBudgetUseCase)
        factoryOf(::DeleteBudgetUseCase)
        factoryOf(::GetBudgetTransactionsUseCase)
        // Presentation
        viewModelOf(::BudgetListViewModel)
        viewModel { params -> BudgetDetailViewModel(params.get<String>(0), get(), get(), get(), get()) }
        viewModel { params ->
            BudgetFormViewModel(
                params.values.getOrNull(0) as String?,
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
    }
