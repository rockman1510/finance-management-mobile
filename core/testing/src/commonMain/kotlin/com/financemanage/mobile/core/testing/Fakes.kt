package com.financemanage.mobile.core.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.financemanage.mobile.core.domain.repository.NetworkMonitor
import com.financemanage.mobile.core.network.auth.AuthTokens
import com.financemanage.mobile.core.network.auth.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeTokenStore(
    initial: AuthTokens? = null,
) : TokenStore {
    val tokens = MutableStateFlow(initial)

    override suspend fun read(): AuthTokens? = tokens.value

    override suspend fun save(tokens: AuthTokens) {
        this.tokens.value = tokens
    }

    override suspend fun clear() {
        tokens.value = null
    }
}

class FakeNetworkMonitor(
    online: Boolean = true,
) : NetworkMonitor {
    val online = MutableStateFlow(online)
    override val isOnline: StateFlow<Boolean> = this.online
}

/** In-memory `DataStore<Preferences>` for repository / ViewModel tests. */
class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: StateFlow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
