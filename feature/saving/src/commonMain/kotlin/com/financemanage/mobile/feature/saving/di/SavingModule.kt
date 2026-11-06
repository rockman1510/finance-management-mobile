package com.financemanage.mobile.feature.saving.di

import com.financemanage.mobile.feature.saving.data.SavingRepositoryImpl
import com.financemanage.mobile.feature.saving.domain.repository.SavingRepository
import com.financemanage.mobile.feature.saving.domain.usecase.CreateGoalUseCase
import com.financemanage.mobile.feature.saving.domain.usecase.DeleteGoalUseCase
import com.financemanage.mobile.feature.saving.domain.usecase.GetGoalUseCase
import com.financemanage.mobile.feature.saving.domain.usecase.MoveGoalMoneyUseCase
import com.financemanage.mobile.feature.saving.domain.usecase.ObserveGoalsUseCase
import com.financemanage.mobile.feature.saving.domain.usecase.RefreshGoalsUseCase
import com.financemanage.mobile.feature.saving.domain.usecase.UpdateAutoContributionUseCase
import com.financemanage.mobile.feature.saving.domain.usecase.UpdateGoalUseCase
import com.financemanage.mobile.feature.saving.domain.usecase.UpdateRoundUpUseCase
import com.financemanage.mobile.feature.saving.presentation.GoalDetailViewModel
import com.financemanage.mobile.feature.saving.presentation.GoalFormViewModel
import com.financemanage.mobile.feature.saving.presentation.GoalListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val savingModule =
    module {
        // Data
        single<SavingRepository> { SavingRepositoryImpl(get(), get(), get()) }
        // Domain
        factoryOf(::ObserveGoalsUseCase)
        factoryOf(::RefreshGoalsUseCase)
        factoryOf(::GetGoalUseCase)
        factoryOf(::CreateGoalUseCase)
        factoryOf(::UpdateGoalUseCase)
        factoryOf(::DeleteGoalUseCase)
        factoryOf(::MoveGoalMoneyUseCase)
        factoryOf(::UpdateAutoContributionUseCase)
        factoryOf(::UpdateRoundUpUseCase)
        // Presentation
        viewModelOf(::GoalListViewModel)
        viewModel { params ->
            GoalDetailViewModel(params.get<String>(0), get(), get(), get(), get(), get(), get(), get(), get())
        }
        viewModel { params -> GoalFormViewModel(params.values.getOrNull(0) as String?, get(), get(), get(), get()) }
    }
