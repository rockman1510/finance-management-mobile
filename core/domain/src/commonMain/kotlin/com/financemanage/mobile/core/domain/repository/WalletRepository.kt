package com.financemanage.mobile.core.domain.repository

import com.financemanage.mobile.core.domain.model.AdjustBalanceRequest
import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.domain.model.CreateWalletRequest
import com.financemanage.mobile.core.domain.model.UpdateWalletRequest
import com.financemanage.mobile.core.domain.model.WalletTotal
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Wallet
import kotlinx.coroutines.flow.Flow

/** Wallets (API §5.1). The full list (archived included) is cached; screens filter it. */
interface WalletRepository {
    val wallets: Flow<Cached<List<Wallet>>?>

    suspend fun refresh(): AppResult<List<Wallet>>

    suspend fun get(id: String): AppResult<Wallet>

    suspend fun create(request: CreateWalletRequest): AppResult<Wallet>

    suspend fun update(
        id: String,
        request: UpdateWalletRequest,
    ): AppResult<Wallet>

    suspend fun delete(id: String): AppResult<Unit>

    suspend fun adjustBalance(
        id: String,
        request: AdjustBalanceRequest,
    ): AppResult<Unit>

    suspend fun total(currency: CurrencyCode): AppResult<WalletTotal>
}
