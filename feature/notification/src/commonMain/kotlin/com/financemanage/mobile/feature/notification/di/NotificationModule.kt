package com.financemanage.mobile.feature.notification.di

import com.financemanage.mobile.feature.notification.data.NotificationRepository
import com.financemanage.mobile.feature.notification.presentation.NotificationSettingsViewModel
import com.financemanage.mobile.feature.notification.presentation.NotificationsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val notificationModule =
    module {
        single { NotificationRepository(get(), get()) }
        viewModel { NotificationsViewModel(get(), get()) }
        viewModel { NotificationSettingsViewModel(get()) }
    }
