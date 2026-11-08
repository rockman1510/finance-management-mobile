package com.financemanage.mobile.core.domain.model

/** The backend the app talks to. [editable] is true only in debug builds. */
data class ApiServer(
    val baseUrl: String,
    val defaultBaseUrl: String,
    val editable: Boolean,
) {
    val isDefault: Boolean get() = baseUrl == defaultBaseUrl
}
