package com.financemanage.mobile.feature.investment.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.feature.investment.domain.model.AssetDto
import com.financemanage.mobile.feature.investment.domain.model.AssetRequest
import com.financemanage.mobile.feature.investment.domain.model.PortfolioDto
import com.financemanage.mobile.feature.investment.domain.model.TradeDto
import com.financemanage.mobile.feature.investment.domain.model.TradeKind
import com.financemanage.mobile.feature.investment.domain.repository.InvestmentRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Observes the cached investment assets. */
class ObserveAssetsUseCase(
    private val repository: InvestmentRepository,
) {
    operator fun invoke(): Flow<Cached<List<AssetDto>>?> = repository.assets
}

class RefreshAssetsUseCase(
    private val repository: InvestmentRepository,
) {
    suspend operator fun invoke(): AppResult<List<AssetDto>> = repository.refresh()
}

/** Portfolio value, gains and allocation converted to [currency]. */
class GetPortfolioUseCase(
    private val repository: InvestmentRepository,
) {
    suspend operator fun invoke(currency: CurrencyCode): AppResult<PortfolioDto> = repository.portfolio(currency)
}

class GetAssetUseCase(
    private val repository: InvestmentRepository,
) {
    suspend operator fun invoke(id: String): AppResult<AssetDto> = repository.get(id)
}

class CreateAssetUseCase(
    private val repository: InvestmentRepository,
) {
    suspend operator fun invoke(request: AssetRequest): AppResult<AssetDto> = repository.create(request)
}

class UpdateAssetUseCase(
    private val repository: InvestmentRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: AssetRequest,
    ): AppResult<AssetDto> = repository.update(id, request)
}

class DeleteAssetUseCase(
    private val repository: InvestmentRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}

/** Records the current market price of an asset. */
class UpdateAssetPriceUseCase(
    private val repository: InvestmentRepository,
) {
    suspend operator fun invoke(
        id: String,
        price: BigDecimal,
        date: LocalDate?,
    ): AppResult<AssetDto> = repository.updatePrice(id, price, date)
}

/** Buy / sell / income; with a wallet the cash movement is recorded asynchronously and polled (API §2.1). */
class RecordTradeUseCase(
    private val repository: InvestmentRepository,
) {
    suspend operator fun invoke(
        id: String,
        kind: TradeKind,
        body: Any,
    ): AppResult<PollResult<TradeDto>> = repository.trade(id, kind, body)
}
