package com.financemanage.mobile.feature.settings.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorSetup(
    val secret: String = "",
    val otpauthUri: String = "",
)

@Serializable
data class DeviceSession(
    val id: String,
    val deviceName: String? = null,
    val lastActiveAt: Instant? = null,
    val expiresAt: Instant? = null,
    val current: Boolean = false,
)
