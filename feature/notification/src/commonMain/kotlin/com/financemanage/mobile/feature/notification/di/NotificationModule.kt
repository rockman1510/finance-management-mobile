package com.financemanage.mobile.feature.notification.di

import com.financemanage.mobile.feature.notification.data.NotificationRepositoryImpl
import com.financemanage.mobile.feature.notification.domain.repository.NotificationRepository
import com.financemanage.mobile.feature.notification.domain.usecase.GetNotificationPreferencesUseCase
import com.financemanage.mobile.feature.notification.domain.usecase.MarkAllNotificationsReadUseCase
import com.financemanage.mobile.feature.notification.domain.usecase.MarkNotificationReadUseCase
import com.financemanage.mobile.feature.notification.domain.usecase.ObserveNotificationsUseCase
import com.financemanage.mobile.feature.notification.domain.usecase.RefreshNotificationsUseCase
import com.financemanage.mobile.feature.notification.domain.usecase.SaveNotificationPreferencesUseCase
import com.financemanage.mobile.feature.notification.presentation.NotificationSettingsViewModel
import com.financemanage.mobile.feature.notification.presentation.NotificationsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val notificationModule =
    module {
        // Data
        single<NotificationRepository> { NotificationRepositoryImpl(get(), get()) }
        // Domain
        factoryOf(::ObserveNotificationsUseCase)
        factoryOf(::RefreshNotificationsUseCase)
        factoryOf(::MarkNotificationReadUseCase)
        factoryOf(::MarkAllNotificationsReadUseCase)
        factoryOf(::GetNotificationPreferencesUseCase)
        factoryOf(::SaveNotificationPreferencesUseCase)
        // Presentation
        viewModelOf(::NotificationsViewModel)
        viewModelOf(::NotificationSettingsViewModel)
    }
