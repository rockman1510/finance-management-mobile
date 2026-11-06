package com.financemanage.mobile.core.domain.usecase

import com.financemanage.mobile.core.domain.model.AdjustBalanceRequest
import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.domain.model.CreateWalletRequest
import com.financemanage.mobile.core.domain.model.UpdateWalletRequest
import com.financemanage.mobile.core.domain.model.WalletTotal
import com.financemanage.mobile.core.domain.repository.WalletRepository
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Wallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Observes every cached wallet (archived included). */
class ObserveWalletsUseCase(
    private val repository: WalletRepository,
) {
    operator fun invoke(): Flow<Cached<List<Wallet>>?> = repository.wallets
}

/** The cached wallets right now (empty when nothing is cached). */
class GetCachedWalletsUseCase(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(): List<Wallet> =
        repository.wallets
            .first()
            ?.value
            .orEmpty()
}

class RefreshWalletsUseCase(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(): AppResult<List<Wallet>> = repository.refresh()
}

class GetWalletUseCase(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Wallet> = repository.get(id)
}

class CreateWalletUseCase(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(request: CreateWalletRequest): AppResult<Wallet> = repository.create(request)
}

class UpdateWalletUseCase(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: UpdateWalletRequest,
    ): AppResult<Wallet> = repository.update(id, request)
}

class DeleteWalletUseCase(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}

class AdjustWalletBalanceUseCase(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: AdjustBalanceRequest,
    ): AppResult<Unit> = repository.adjustBalance(id, request)
}

/** Total of the wallets converted to [currency] (API §5.1). */
class GetWalletTotalUseCase(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(currency: CurrencyCode): AppResult<WalletTotal> = repository.total(currency)
}

/** The cached wallets, or the server's when nothing is cached yet. */
class GetWalletsUseCase(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(): List<Wallet> =
        repository.wallets.first()?.value ?: (repository.refresh() as? AppResult.Success)?.value.orEmpty()
}
