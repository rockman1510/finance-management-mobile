package com.financemanage.mobile.feature.investment

import com.financemanage.mobile.core.data.shared.CategoryRepositoryImpl
import com.financemanage.mobile.core.data.shared.ProfileRepositoryImpl
import com.financemanage.mobile.core.data.shared.WalletRepositoryImpl
import com.financemanage.mobile.core.domain.usecase.GetCachedCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.GetCachedWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.GetDefaultCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.bodyJson
import com.financemanage.mobile.core.testing.keepCollecting
import com.financemanage.mobile.core.testing.respondJson
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.feature.investment.data.InvestmentRepositoryImpl
import com.financemanage.mobile.feature.investment.domain.model.TradeKind
import com.financemanage.mobile.feature.investment.domain.usecase.CreateAssetUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.DeleteAssetUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.GetAssetUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.GetPortfolioUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.ObserveAssetsUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.RecordTradeUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.RefreshAssetsUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.UpdateAssetPriceUseCase
import com.financemanage.mobile.feature.investment.domain.usecase.UpdateAssetUseCase
import com.financemanage.mobile.feature.investment.presentation.AssetDetailViewModel
import com.financemanage.mobile.feature.investment.presentation.AssetFormViewModel
import com.financemanage.mobile.feature.investment.presentation.PortfolioViewModel
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun assetJson(trade: String = "") =
    """{"id":"a1","name":"FPT","symbol":"FPT","type":"STOCK","currency":"VND","quantity":100,"averageCost":120000,
    "costBasis":12000000,"lastPrice":130000,"priced":true,"marketValue":13000000,"unrealized":1000000,
    "trades":[$trade]}"""

private fun trade(cashStatus: String) =
    """{"id":"tr1","type":"BUY","quantity":100,"price":120000,"walletId":"w1","cashStatus":"$cashStatus"}"""

class InvestmentViewModelsTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val repository = InvestmentRepositoryImpl(backend.client, cache, PendingPoller())
    private val profiles = ProfileRepositoryImpl(backend.client, cache)
    private val wallets = WalletRepositoryImpl(backend.client, cache)
    private val categories = CategoryRepositoryImpl(backend.client, cache)

    private fun detail() =
        AssetDetailViewModel(
            "a1",
            GetAssetUseCase(repository),
            UpdateAssetPriceUseCase(repository),
            RecordTradeUseCase(repository),
            DeleteAssetUseCase(repository),
            GetCachedWalletsUseCase(wallets),
            GetCachedCategoriesUseCase(categories),
            GetTodayUseCase(profiles),
        )

    private fun formViewModel(id: String?) =
        AssetFormViewModel(
            id,
            GetAssetUseCase(repository),
            CreateAssetUseCase(repository),
            UpdateAssetUseCase(repository),
            GetDefaultCurrencyUseCase(profiles),
        )

    @Test
    fun portfolioLoadsInTheChosenCurrency() =
        runViewModelTest {
            backend.get("investments", "[${assetJson()}]")
            backend.get("investments/portfolio", """{"currency":"VND","marketValue":13000000}""")
            val viewModel =
                PortfolioViewModel(
                    ObserveAssetsUseCase(repository),
                    RefreshAssetsUseCase(repository),
                    GetPortfolioUseCase(repository),
                    GetDefaultCurrencyUseCase(profiles),
                )
            keepCollecting(viewModel.state)
            viewModel.refresh()
            val state = viewModel.state.awaitValue { it.assets.data != null && it.portfolio != null }
            assertEquals(
                "FPT",
                state.assets.data!!
                    .single()
                    .symbol,
            )

            viewModel.setCurrency(CurrencyCode.USD)
            backend.await(HttpMethod.Get, "investments/portfolio", count = 2)
            assertEquals(
                "USD",
                backend
                    .requests(HttpMethod.Get, "investments/portfolio")
                    .last()
                    .url.parameters["currency"],
            )
            viewModel.state.awaitValue { it.currency == CurrencyCode.USD }
        }

    @Test
    fun buyingWithAWalletPollsTheCash() =
        runViewModelTest {
            backend.post("investments/a1/buys", trade("PENDING"), HttpStatusCode.Created)
            backend.get("investments/a1", assetJson(trade("RECORDED")))
            backend.get("investments", "[]")
            val viewModel = detail()
            viewModel.trade(
                TradeKind.BUY,
                BigDecimal.parseString("100"),
                BigDecimal.parseString("120000"),
                null,
                "w1",
                null,
                null,
            )
            val done = viewModel.state.awaitValue { !it.busy && it.asset != null }
            assertEquals(PendingAction.Recorded, done.pending)
            val request = backend.single(HttpMethod.Post, "investments/a1/buys")
            assertEquals("100", request.bodyField("quantity"))
            assertEquals("120000", request.bodyField("price"))
            assertEquals("w1", request.bodyField("walletId"))
        }

    @Test
    fun aTradeWithoutAWalletNeedsNoPolling() =
        runViewModelTest {
            backend.post("investments/a1/sells", trade("NONE"), HttpStatusCode.Created)
            backend.get("investments/a1", assetJson())
            backend.get("investments", "[]")
            val viewModel = detail()
            viewModel.trade(
                TradeKind.SELL,
                BigDecimal.parseString("10"),
                BigDecimal.parseString("130000"),
                BigDecimal.parseString("1500"),
                null,
                null,
                null,
            )
            val done = viewModel.state.awaitValue { !it.busy && it.asset != null }
            assertNull(done.pending)
            assertEquals(1, backend.requests(HttpMethod.Get, "investments/a1").size, "only the refresh")
            assertEquals("1500", backend.single(HttpMethod.Post, "investments/a1/sells").bodyField("fee"))
        }

    @Test
    fun dividendsSendAnIncomeAndARefusedCashIsFailed() =
        runViewModelTest {
            backend.post("investments/a1/incomes", trade("PENDING").replace("BUY", "DIVIDEND"), HttpStatusCode.Created)
            backend.get("investments/a1", assetJson(trade("FAILED")))
            backend.get("investments", "[]")
            val viewModel = detail()
            viewModel.trade(
                TradeKind.INCOME,
                BigDecimal.parseString("500000"),
                null,
                null,
                "w1",
                "div",
                LocalDate(2026, 11, 2),
            )
            assertEquals(PendingAction.Failed(null), viewModel.state.awaitValue { !it.busy }.pending)
            val body = backend.single(HttpMethod.Post, "investments/a1/incomes").bodyJson()
            assertEquals("500000", body["amount"].toString())
            assertEquals("\"div\"", body["categoryId"].toString())
            assertFalse("quantity" in body)
        }

    @Test
    fun sellingMoreThanHeldIsRefused() =
        runViewModelTest {
            backend.fail(
                HttpMethod.Post,
                "investments/a1/sells",
                HttpStatusCode.UnprocessableEntity,
                "INSUFFICIENT_QUANTITY",
            )
            val viewModel = detail()
            viewModel.trade(TradeKind.SELL, BigDecimal.parseString("1000"), BigDecimal.ONE, null, "w1", null, null)
            val failed = viewModel.state.awaitValue { it.error != null }
            assertEquals("INSUFFICIENT_QUANTITY", (failed.error as AppError.Api).code)
            assertNull(failed.pending)
        }

    @Test
    fun priceUpdate() =
        runViewModelTest {
            backend.on(HttpMethod.Put, "investments/a1/price") { respondJson(assetJson()) }
            val viewModel = detail()
            viewModel.updatePrice(BigDecimal.parseString("130000"), LocalDate(2026, 11, 3))
            viewModel.state.awaitValue { it.asset != null && !it.busy }
            assertEquals("130000", backend.single(HttpMethod.Put, "investments/a1/price").bodyField("price"))
        }

    @Test
    fun assetFormCreatesAndEditDoesNotChangeTypeOrCurrency() =
        runViewModelTest {
            backend.post("investments", assetJson(), HttpStatusCode.Created)
            backend.get("investments", "[]")
            val create = formViewModel(null)
            create.save()
            assertTrue(create.state.value.nameMissing)
            create.update { it.copy(name = "FPT", symbol = "fpt", type = "STOCK") }
            create.save()
            create.state.awaitValue { it.saved }
            assertEquals("STOCK", backend.single(HttpMethod.Post, "investments").bodyField("type"))

            backend.get("investments/a1", assetJson())
            backend.patch("investments/a1", assetJson())
            val edit = formViewModel("a1")
            edit.state.awaitValue { it.name == "FPT" }
            edit.update { it.copy(note = "Dài hạn") }
            edit.save()
            edit.state.awaitValue { it.saved }
            val patch = backend.single(HttpMethod.Patch, "investments/a1").bodyJson()
            assertFalse("type" in patch)
            assertFalse("currency" in patch)
            assertEquals("\"Dài hạn\"", patch["note"].toString())
        }
}
