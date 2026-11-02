package com.financemanage.mobile.core.data.platform

/** Human-readable device model, sent as `deviceName` so users recognise their sessions (API §4.1). */
expect fun deviceName(): String

/** `android` or `ios` (push device registration, API §14). */
expect fun platformName(): String
