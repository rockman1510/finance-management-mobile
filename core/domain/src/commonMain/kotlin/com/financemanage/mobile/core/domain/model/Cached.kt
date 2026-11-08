package com.financemanage.mobile.core.domain.model

import kotlinx.datetime.Instant

/** A cached value and when it was fetched ("Updated at HH:mm"). */
data class Cached<out T>(
    val value: T,
    val updatedAt: Instant,
)
