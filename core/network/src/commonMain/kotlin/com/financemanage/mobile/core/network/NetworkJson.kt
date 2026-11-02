package com.financemanage.mobile.core.network

import kotlinx.serialization.json.Json

/**
 * JSON settings shared by the whole app (API §2, §15): the backend may add fields or enum values at any time;
 * `null` fields are not sent, so a `PATCH` leaves them unchanged.
 */
val NetworkJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = false
    }
