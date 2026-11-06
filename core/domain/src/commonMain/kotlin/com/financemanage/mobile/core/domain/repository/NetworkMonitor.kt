package com.financemanage.mobile.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Emits whether the device currently has a network connection (offline banner, write queue). */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
