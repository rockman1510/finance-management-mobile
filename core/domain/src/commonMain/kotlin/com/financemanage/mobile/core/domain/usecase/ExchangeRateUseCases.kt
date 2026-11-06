package com.financemanage.mobile.core.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.domain.model.ConversionDto
import com.financemanage.mobile.core.domain.repository.ExchangeRateRepository
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.ExchangeRate
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class ObserveExchangeRatesUseCase(
    private val repository: ExchangeRateRepository,
) {
    operator fun invoke(): Flow<Cached<List<ExchangeRate>>?> = repository.rates
}

class RefreshExchangeRatesUseCase(
    private val repository: ExchangeRateRepository,
) {
    suspend operator fun invoke(): AppResult<List<ExchangeRate>> = repository.refresh()
}

class SaveExchangeRateUseCase(
    private val repository: ExchangeRateRepository,
) {
    suspend operator fun invoke(
        base: CurrencyCode,
        quote: CurrencyCode,
        rate: BigDecimal,
        effectiveDate: LocalDate?,
    ): AppResult<Unit> = repository.save(base, quote, rate, effectiveDate)
}

class DeleteExchangeRateUseCase(
    private val repository: ExchangeRateRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}

/** Converts [amount] from one currency to another at the rate of [date] (today when null). */
class ConvertCurrencyUseCase(
    private val repository: ExchangeRateRepository,
) {
    suspend operator fun invoke(
        amount: BigDecimal,
        from: CurrencyCode,
        to: CurrencyCode,
        date: LocalDate? = null,
    ): AppResult<ConversionDto> = repository.convert(amount, from, to, date)
}
