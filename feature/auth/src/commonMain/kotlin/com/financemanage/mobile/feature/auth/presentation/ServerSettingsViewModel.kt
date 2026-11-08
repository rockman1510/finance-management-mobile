package com.financemanage.mobile.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.model.ApiServer
import com.financemanage.mobile.core.domain.usecase.ObserveApiServerUseCase
import com.financemanage.mobile.core.domain.usecase.ResetApiBaseUrlUseCase
import com.financemanage.mobile.core.domain.usecase.SetApiBaseUrlUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Debug-only backend address editor, opened by a long press on the Welcome screen. */
data class ServerSettingsUiState(
    /** `null` until loaded; [ApiServer.editable] is false in release builds (nothing is shown). */
    val server: ApiServer? = null,
    val editing: Boolean = false,
    val input: String = "",
    val invalid: Boolean = false,
) {
    val available: Boolean get() = server?.editable == true
}

class ServerSettingsViewModel(
    observeApiServer: ObserveApiServerUseCase,
    private val setApiBaseUrl: SetApiBaseUrlUseCase,
    private val resetApiBaseUrl: ResetApiBaseUrlUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ServerSettingsUiState())
    val state: StateFlow<ServerSettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            observeApiServer().collect { server ->
                mutableState.update { it.copy(server = server) }
            }
        }
    }

    fun open() {
        val server = mutableState.value.server ?: return
        if (!server.editable) return
        mutableState.update { it.copy(editing = true, input = server.baseUrl, invalid = false) }
    }

    fun onInput(value: String) = mutableState.update { it.copy(input = value, invalid = false) }

    fun save() {
        val input = mutableState.value.input
        viewModelScope.launch {
            val saved = setApiBaseUrl(input)
            mutableState.update { if (saved) it.closed() else it.copy(invalid = true) }
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            resetApiBaseUrl()
            mutableState.update { it.closed() }
        }
    }

    fun dismiss() = mutableState.update { it.closed() }

    private fun ServerSettingsUiState.closed() = copy(editing = false, input = "", invalid = false)
}
