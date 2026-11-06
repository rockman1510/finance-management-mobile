package com.financemanage.mobile.core.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.domain.model.ConversionDto
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.ExchangeRate
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Exchange rates and conversion (API §6). */
interface ExchangeRateRepository {
    val rates: Flow<Cached<List<ExchangeRate>>?>

    suspend fun refresh(): AppResult<List<ExchangeRate>>

    suspend fun save(
        base: CurrencyCode,
        quote: CurrencyCode,
        rate: BigDecimal,
        effectiveDate: LocalDate?,
    ): AppResult<Unit>

    suspend fun delete(id: String): AppResult<Unit>

    suspend fun convert(
        amount: BigDecimal,
        from: CurrencyCode,
        to: CurrencyCode,
        date: LocalDate?,
    ): AppResult<ConversionDto>
}
