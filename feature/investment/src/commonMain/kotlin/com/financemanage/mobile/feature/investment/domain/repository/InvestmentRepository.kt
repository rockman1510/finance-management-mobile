package com.financemanage.mobile.feature.investment.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.feature.investment.domain.model.AssetDto
import com.financemanage.mobile.feature.investment.domain.model.AssetRequest
import com.financemanage.mobile.feature.investment.domain.model.PortfolioDto
import com.financemanage.mobile.feature.investment.domain.model.TradeDto
import com.financemanage.mobile.feature.investment.domain.model.TradeKind
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Investment assets and trades (API §15). */
interface InvestmentRepository {
    val assets: Flow<Cached<List<AssetDto>>?>

    suspend fun refresh(): AppResult<List<AssetDto>>

    suspend fun portfolio(currency: CurrencyCode): AppResult<PortfolioDto>

    suspend fun get(id: String): AppResult<AssetDto>

    suspend fun create(request: AssetRequest): AppResult<AssetDto>

    suspend fun update(
        id: String,
        request: AssetRequest,
    ): AppResult<AssetDto>

    suspend fun delete(id: String): AppResult<Unit>

    suspend fun updatePrice(
        id: String,
        price: BigDecimal,
        date: LocalDate?,
    ): AppResult<AssetDto>

    /** Buy / sell / dividend (201). With a wallet the cash is recorded asynchronously and polled (API §2.1). */
    suspend fun trade(
        id: String,
        kind: TradeKind,
        body: Any,
    ): AppResult<PollResult<TradeDto>>
}
