package com.financemanage.mobile.core.data.di

import com.financemanage.mobile.core.domain.usecase.AdjustWalletBalanceUseCase
import com.financemanage.mobile.core.domain.usecase.ClearLocalSessionUseCase
import com.financemanage.mobile.core.domain.usecase.ConsumeOpenedNotificationUseCase
import com.financemanage.mobile.core.domain.usecase.ConvertCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.CreateCategoryUseCase
import com.financemanage.mobile.core.domain.usecase.CreateWalletUseCase
import com.financemanage.mobile.core.domain.usecase.DeleteCategoryUseCase
import com.financemanage.mobile.core.domain.usecase.DeleteExchangeRateUseCase
import com.financemanage.mobile.core.domain.usecase.DeleteWalletUseCase
import com.financemanage.mobile.core.domain.usecase.DiscardPendingWriteUseCase
import com.financemanage.mobile.core.domain.usecase.GetCachedCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.GetCachedProfileUseCase
import com.financemanage.mobile.core.domain.usecase.GetCachedWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.GetCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.GetDefaultCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.GetPreferencesUseCase
import com.financemanage.mobile.core.domain.usecase.GetProfileUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.domain.usecase.GetWalletTotalUseCase
import com.financemanage.mobile.core.domain.usecase.GetWalletUseCase
import com.financemanage.mobile.core.domain.usecase.GetWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.HideTwoFactorCardUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveConnectivityUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveExchangeRatesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveNotificationPermissionRequestsUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveOpenedNotificationUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePendingWritesUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePreferencesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveProfileUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePushReceivedUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveSessionStateUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.RefreshCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.RefreshExchangeRatesUseCase
import com.financemanage.mobile.core.domain.usecase.RefreshProfileUseCase
import com.financemanage.mobile.core.domain.usecase.RefreshWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.RememberFormChoicesUseCase
import com.financemanage.mobile.core.domain.usecase.RequestNotificationPermissionUseCase
import com.financemanage.mobile.core.domain.usecase.RetryPendingWriteUseCase
import com.financemanage.mobile.core.domain.usecase.SaveExchangeRateUseCase
import com.financemanage.mobile.core.domain.usecase.SetAppLockUseCase
import com.financemanage.mobile.core.domain.usecase.SetHideBalancesUseCase
import com.financemanage.mobile.core.domain.usecase.SetOnboardingCompletedUseCase
import com.financemanage.mobile.core.domain.usecase.SetThemeModeUseCase
import com.financemanage.mobile.core.domain.usecase.ShouldAskNotificationPermissionUseCase
import com.financemanage.mobile.core.domain.usecase.SignOutUseCase
import com.financemanage.mobile.core.domain.usecase.ToggleHideBalancesUseCase
import com.financemanage.mobile.core.domain.usecase.UpdateCategoryUseCase
import com.financemanage.mobile.core.domain.usecase.UpdateProfileUseCase
import com.financemanage.mobile.core.domain.usecase.UpdateWalletUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** The shared use cases of `core:domain` (stateless: a new instance per injection). */
val domainModule: Module =
    module {
        // Profile
        factoryOf(::ObserveProfileUseCase)
        factoryOf(::GetCachedProfileUseCase)
        factoryOf(::GetProfileUseCase)
        factoryOf(::GetDefaultCurrencyUseCase)
        factoryOf(::GetTodayUseCase)
        factoryOf(::RefreshProfileUseCase)
        factoryOf(::UpdateProfileUseCase)
        // Wallets
        factoryOf(::ObserveWalletsUseCase)
        factoryOf(::GetCachedWalletsUseCase)
        factoryOf(::GetWalletsUseCase)
        factoryOf(::RefreshWalletsUseCase)
        factoryOf(::GetWalletUseCase)
        factoryOf(::CreateWalletUseCase)
        factoryOf(::UpdateWalletUseCase)
        factoryOf(::DeleteWalletUseCase)
        factoryOf(::AdjustWalletBalanceUseCase)
        factoryOf(::GetWalletTotalUseCase)
        // Categories
        factoryOf(::ObserveCategoriesUseCase)
        factoryOf(::GetCachedCategoriesUseCase)
        factoryOf(::GetCategoriesUseCase)
        factoryOf(::RefreshCategoriesUseCase)
        factoryOf(::CreateCategoryUseCase)
        factoryOf(::UpdateCategoryUseCase)
        factoryOf(::DeleteCategoryUseCase)
        // Exchange rates
        factoryOf(::ObserveExchangeRatesUseCase)
        factoryOf(::RefreshExchangeRatesUseCase)
        factoryOf(::SaveExchangeRateUseCase)
        factoryOf(::DeleteExchangeRateUseCase)
        factoryOf(::ConvertCurrencyUseCase)
        // Settings
        factoryOf(::ObservePreferencesUseCase)
        factoryOf(::GetPreferencesUseCase)
        factoryOf(::SetThemeModeUseCase)
        factoryOf(::SetHideBalancesUseCase)
        factoryOf(::ToggleHideBalancesUseCase)
        factoryOf(::SetOnboardingCompletedUseCase)
        factoryOf(::HideTwoFactorCardUseCase)
        factoryOf(::SetAppLockUseCase)
        factoryOf(::RememberFormChoicesUseCase)
        // Session & connectivity
        factoryOf(::ObserveSessionStateUseCase)
        factoryOf(::SignOutUseCase)
        factoryOf(::ClearLocalSessionUseCase)
        factoryOf(::ObserveConnectivityUseCase)
        // Outbox (F21)
        factoryOf(::ObservePendingWritesUseCase)
        factoryOf(::RetryPendingWriteUseCase)
        factoryOf(::DiscardPendingWriteUseCase)
        // Push (F20)
        factoryOf(::ObservePushReceivedUseCase)
        factoryOf(::RequestNotificationPermissionUseCase)
        factoryOf(::ObserveOpenedNotificationUseCase)
        factoryOf(::ConsumeOpenedNotificationUseCase)
        factoryOf(::ObserveNotificationPermissionRequestsUseCase)
        factoryOf(::ShouldAskNotificationPermissionUseCase)
    }
