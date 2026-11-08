package com.financemanage.mobile.feature.currency

import com.financemanage.mobile.core.data.shared.ExchangeRateRepositoryImpl
import com.financemanage.mobile.core.data.shared.ProfileRepositoryImpl
import com.financemanage.mobile.core.domain.usecase.ConvertCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.DeleteExchangeRateUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveExchangeRatesUseCase
import com.financemanage.mobile.core.domain.usecase.RefreshExchangeRatesUseCase
import com.financemanage.mobile.core.domain.usecase.SaveExchangeRateUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.keepCollecting
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.feature.currency.presentation.AddRateViewModel
import com.financemanage.mobile.feature.currency.presentation.ConverterViewModel
import com.financemanage.mobile.feature.currency.presentation.RateProblem
import com.financemanage.mobile.feature.currency.presentation.RatesViewModel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val RATES =
    """[{"id":"r1","base":"USD","quote":"VND","rate":25400,"effectiveDate":"2026-11-01","source":"MANUAL","global":false},
    {"id":"r2","base":"EUR","quote":"VND","rate":27150.5,"effectiveDate":"2026-10-30",
    "source":"SYSTEM","global":true}]"""

class CurrencyViewModelsTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val repository = ExchangeRateRepositoryImpl(backend.client, cache)
    private val profiles = ProfileRepositoryImpl(backend.client, cache)

    @Test
    fun ratesLoadAndDeleteRefreshesTheList() =
        runViewModelTest {
            backend.get("exchange-rates", RATES)
            backend.delete("exchange-rates/r1")
            val viewModel =
                RatesViewModel(
                    ObserveExchangeRatesUseCase(repository),
                    RefreshExchangeRatesUseCase(repository),
                    DeleteExchangeRateUseCase(repository),
                )
            keepCollecting(viewModel.state)

            viewModel.refresh()
            val loaded = viewModel.state.awaitValue { it.rates.data?.size == 2 }
            assertTrue(
                loaded.rates.data!!
                    .last()
                    .global,
            )

            viewModel.delete(loaded.rates.data!!.first())
            backend.await(HttpMethod.Get, "exchange-rates", count = 2)
            assertEquals(1, backend.requests(HttpMethod.Delete, "exchange-rates/r1").size)
        }

    @Test
    fun addRateIsPrefilledAndValidated() =
        runViewModelTest {
            backend.post("exchange-rates", "{}", HttpStatusCode.Created)
            backend.get("exchange-rates", RATES)
            val viewModel =
                AddRateViewModel("eur", "VND", SaveExchangeRateUseCase(repository), GetTodayUseCase(profiles))
            val loaded = viewModel.state.awaitValue { it.date != null }
            assertEquals(CurrencyCode("EUR"), loaded.base)

            viewModel.onQuote(CurrencyCode("EUR"))
            viewModel.onRate("27150")
            viewModel.save()
            assertEquals(RateProblem.SAME_CURRENCY, viewModel.state.value.problem)

            viewModel.onQuote(CurrencyCode.VND)
            viewModel.onRate("-1")
            viewModel.save()
            assertEquals(RateProblem.INVALID_RATE, viewModel.state.value.problem)

            viewModel.onRate("27150,5")
            viewModel.save()
            viewModel.state.awaitValue { it.done }
            val request = backend.single(HttpMethod.Post, "exchange-rates")
            assertEquals("EUR", request.bodyField("base"))
            assertEquals("VND", request.bodyField("quote"))
            assertEquals("27150.5", request.bodyField("rate"))
            assertEquals(loaded.date.toString(), request.bodyField("effectiveDate"))
        }

    @Test
    fun converterShowsTheResultOrAMissingRate() =
        runViewModelTest {
            backend.get(
                "exchange-rates/convert",
                """{"amount":10,"from":"USD","to":"VND","rate":25400,"converted":254000,"date":"2026-11-01"}""",
            )
            val viewModel = ConverterViewModel(ConvertCurrencyUseCase(repository))
            viewModel.convert()
            assertTrue(backend.requests.isEmpty())

            viewModel.onAmount("10")
            viewModel.convert()
            val converted = viewModel.state.awaitValue { it.result != null }
            assertEquals("254000", converted.result?.converted?.toStringExpanded())
            val query = backend.single(HttpMethod.Get, "exchange-rates/convert").url.parameters
            assertEquals("10", query["amount"])
            assertEquals("USD", query["from"])
            assertEquals("VND", query["to"])

            backend.fail(
                HttpMethod.Get,
                "exchange-rates/convert",
                HttpStatusCode.UnprocessableEntity,
                "EXCHANGE_RATE_MISSING",
            )
            viewModel.onTo(CurrencyCode("JPY"))
            viewModel.convert()
            val failed = viewModel.state.awaitValue { it.error != null }
            assertEquals("EXCHANGE_RATE_MISSING", (failed.error as AppError.Api).code)
        }
}
