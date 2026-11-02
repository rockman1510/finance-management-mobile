package com.financemanage.mobile.core.network.auth

/** Secure persistence of [AuthTokens]. Implemented in `core:data` (Keystore on Android, Keychain on iOS). */
interface TokenStore {
    suspend fun read(): AuthTokens?

    suspend fun save(tokens: AuthTokens)

    suspend fun clear()
}
