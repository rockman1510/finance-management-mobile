package com.financemanage.mobile.feature.saving.di

import com.financemanage.mobile.feature.saving.data.SavingRepository
import com.financemanage.mobile.feature.saving.presentation.GoalDetailViewModel
import com.financemanage.mobile.feature.saving.presentation.GoalFormViewModel
import com.financemanage.mobile.feature.saving.presentation.GoalListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val savingModule =
    module {
        single { SavingRepository(get(), get(), get()) }
        viewModel { GoalListViewModel(get()) }
        viewModel { params -> GoalDetailViewModel(params.get<String>(0), get(), get(), get()) }
        viewModel { params -> GoalFormViewModel(params.values.getOrNull(0) as String?, get(), get()) }
    }
