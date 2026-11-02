package com.financemanage.mobile.core.data.security

import com.financemanage.mobile.core.network.auth.AuthTokens
import com.financemanage.mobile.core.network.auth.TokenStore
import com.liftric.kvault.KVault
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Stores the token pair in the Android Keystore (EncryptedSharedPreferences) / iOS Keychain through KVault.
 */
class SecureTokenStore(
    private val vault: KVault,
    private val json: Json,
) : TokenStore {
    private val mutex = Mutex()

    override suspend fun read(): AuthTokens? =
        mutex.withLock {
            val raw = vault.string(forKey = KEY) ?: return@withLock null
            try {
                json.decodeFromString(AuthTokens.serializer(), raw)
            } catch (e: SerializationException) {
                vault.deleteObject(forKey = KEY)
                null
            }
        }

    override suspend fun save(tokens: AuthTokens) {
        mutex.withLock { vault.set(key = KEY, stringValue = json.encodeToString(AuthTokens.serializer(), tokens)) }
    }

    override suspend fun clear() {
        mutex.withLock { vault.deleteObject(forKey = KEY) }
    }

    private companion object {
        const val KEY = "auth_tokens"
    }
}
