package com.financemanage.mobile.feature.transaction

import com.financemanage.mobile.core.data.outbox.OutboxEntity
import com.financemanage.mobile.core.data.outbox.OutboxStatus
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.core.network.IDEMPOTENCY_KEY_HEADER
import com.financemanage.mobile.core.testing.awaitCondition
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.keepCollecting
import com.financemanage.mobile.core.testing.respondJson
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.feature.transaction.data.OUTBOX_TRANSACTION
import com.financemanage.mobile.feature.transaction.domain.TransactionFilter
import com.financemanage.mobile.feature.transaction.presentation.OutboxViewModel
import com.financemanage.mobile.feature.transaction.presentation.RecurringEditViewModel
import com.financemanage.mobile.feature.transaction.presentation.RecurringListViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransactionDetailViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransactionListViewModel
import com.financemanage.mobile.feature.transaction.presentation.TransferViewModel
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val RECURRING =
    """{"id":"r1","walletId":"$CASH","categoryId":"food","type":"EXPENSE","amount":30000,"currency":"VND",
    "frequency":"MONTHLY","interval":1,"startDate":"2026-11-01","nextOccurrence":"2026-12-01","active":true}"""

class TransactionViewModelsTest {
    @Test
    fun listPagesFiltersAndShowsTotals() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.on(HttpMethod.Get, "transactions") { request ->
                val pageNo = request.url.parameters["page"]!!.toInt()
                respondJson(
                    """{"items":[${transactionJson(id = "t$pageNo")}],"page":$pageNo,"size":30,"totalPages":2}""",
                )
            }
            val viewModel = TransactionListViewModel(TransactionFilter(), env.repository, env.wallets, env.categories)
            keepCollecting(viewModel.state)

            viewModel.refresh()
            val first = viewModel.state.awaitValue { !it.content.loading && it.content.items.isNotEmpty() }
            assertTrue(first.content.hasMore)
            assertEquals(
                "-45000",
                first.content.totals
                    .single()
                    .net.amount
                    .toStringExpanded(),
            )
            viewModel.state.awaitValue { it.wallets.size == 3 && it.categories.size == 2 }

            viewModel.loadMore()
            val second = viewModel.state.awaitValue { it.content.items.size == 2 }
            assertEquals(listOf("t0", "t1"), second.content.items.map { it.id })
            assertFalse(second.content.hasMore)

            viewModel.setFilter(
                TransactionFilter(
                    walletId = CASH,
                    categoryIds = listOf("food"),
                    types = listOf(TransactionType.EXPENSE),
                    from = LocalDate(2026, 11, 1),
                    query = " phở ",
                ),
            )
            viewModel.state.awaitValue { it.filter.walletId == CASH && !it.content.loading }
            val query =
                env.backend
                    .requests(HttpMethod.Get, "transactions")
                    .last()
                    .url.parameters
            assertEquals(CASH, query["walletId"])
            assertEquals("food", query["categoryId"])
            assertEquals("EXPENSE", query["type"])
            assertEquals("2026-11-01", query["from"])
            assertEquals("phở", query["q"])
            assertEquals("0", query["page"])
        }

    @Test
    fun offlineListFallsBackToTheCachedFirstPage() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.get("transactions", page(transactionJson()))
            env.repository.search(TransactionFilter(), 0, 30)
            env.backend.offline(HttpMethod.Get, "transactions")
            val viewModel = TransactionListViewModel(TransactionFilter(), env.repository, env.wallets, env.categories)
            keepCollecting(viewModel.state)

            viewModel.refresh()
            val state = viewModel.state.awaitValue { it.content.error != null }
            assertTrue(state.content.fromCache)
            assertEquals(listOf("t1"), state.content.items.map { it.id })
            assertTrue(state.content.error is AppError.Network)
        }

    @Test
    fun listDeleteRemovesTheRowAndShowsPendingWrites() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.get("transactions", page(transactionJson()))
            env.backend.delete("transactions/t1")
            env.outboxDao.upsert(
                OutboxEntity("o1", OUTBOX_TRANSACTION, "{}", "k", OutboxStatus.FAILED.name, "WALLET_ARCHIVED"),
            )
            val viewModel = TransactionListViewModel(TransactionFilter(), env.repository, env.wallets, env.categories)
            keepCollecting(viewModel.state)
            viewModel.refresh()
            val loaded = viewModel.state.awaitValue { it.content.items.isNotEmpty() && it.pending.isNotEmpty() }

            viewModel.delete(loaded.content.items.single())
            viewModel.state.awaitValue { it.content.items.isEmpty() }

            viewModel.discardPending(loaded.pending.single())
            viewModel.state.awaitValue { it.pending.isEmpty() }
        }

    @Test
    fun detailLoadsReceiptsAndDeletesATransferAsAWhole() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.get(
                "transactions/t1",
                transactionJson(
                    type = "TRANSFER_OUT",
                    category = null,
                    extra =
                        ""","transferId":"tr1","attachments":[{"id":"a1","fileName":"r.jpg","contentType":"image/jpeg"},
                        {"id":"a2","fileName":"r.pdf","contentType":"application/pdf"}]""",
                ),
            )
            env.backend.on(HttpMethod.Get, "attachments/a1") {
                respond(byteArrayOf(9, 9), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "image/jpeg"))
            }
            env.backend.delete("transfers/tr1")
            val viewModel = TransactionDetailViewModel("t1", env.repository, env.wallets, env.categories)

            viewModel.refresh()
            val loaded = viewModel.state.awaitValue { it.images.isNotEmpty() }
            assertFalse(loaded.transaction!!.editable)
            assertEquals(listOf("a1"), loaded.images.keys.toList())
            assertTrue(env.backend.requests(HttpMethod.Get, "attachments/a2").isEmpty())

            viewModel.delete()
            viewModel.state.awaitValue { it.deleted }
            assertTrue(env.backend.requests(HttpMethod.Delete, "transactions/t1").isEmpty())
        }

    @Test
    fun transferChecksWalletsEstimatesTheReceivedAmountAndSends() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.get(
                "exchange-rates/convert",
                """{"amount":10,"from":"USD","to":"VND","rate":25400,"converted":254000}""",
            )
            env.backend.post("transfers", """{"transferId":"tr1"}""", HttpStatusCode.Created)
            val viewModel = TransferViewModel(env.repository, env.wallets, env.rates, env.profiles)
            val loaded = viewModel.state.awaitValue { it.wallets.isNotEmpty() }
            assertEquals(CASH, loaded.fromId)
            assertEquals(USD_CARD, loaded.toId)

            viewModel.onTo(loaded.wallets.first { it.id == CASH })
            viewModel.onAmount("10")
            viewModel.save()
            assertTrue(viewModel.state.value.sameWallet)

            viewModel.onFrom(loaded.wallets.first { it.id == USD_CARD })
            viewModel.onAmount("10")
            val estimated = viewModel.state.awaitValue { it.received.isNotEmpty() }
            assertTrue(estimated.crossCurrency)
            assertEquals("254000", estimated.received)

            viewModel.onReceived("250.000")
            viewModel.onNote(" ")
            viewModel.save()
            val done = viewModel.state.awaitValue { it.done }
            assertFalse(done.queued)
            val request = env.backend.single(HttpMethod.Post, "transfers")
            assertEquals(USD_CARD, request.bodyField("fromWalletId"))
            assertEquals(CASH, request.bodyField("toWalletId"))
            assertEquals("10", request.bodyField("amount"))
            assertEquals("250000", request.bodyField("receivedAmount"))
            assertNull(request.bodyField("note"))
            assertTrue(request.headers[IDEMPOTENCY_KEY_HEADER]!!.isNotBlank())
        }

    @Test
    fun offlineTransferIsQueued() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.offline(HttpMethod.Post, "transfers")
            val viewModel = TransferViewModel(env.repository, env.wallets, env.rates, env.profiles)
            val loaded = viewModel.state.awaitValue { it.wallets.isNotEmpty() }
            viewModel.onTo(loaded.wallets.first { it.id == USD_CARD })
            viewModel.onAmount("abc")
            viewModel.save()
            assertTrue(viewModel.state.value.invalidAmount)

            viewModel.onAmount("100.000")
            viewModel.save()
            assertTrue(viewModel.state.awaitValue { it.done }.queued)
            assertEquals(
                "transfer",
                env.outboxDao.rows.value
                    .single()
                    .kind,
            )
            awaitCondition { env.backend.requests(HttpMethod.Post, "transfers").size >= 2 }
        }

    @Test
    fun recurringListTogglesAndEditValidates() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.get("recurring-transactions", "[$RECURRING]")
            env.backend.get("recurring-transactions/r1", RECURRING)
            env.backend.patch("recurring-transactions/r1", RECURRING)
            env.backend.delete("recurring-transactions/r1")
            val list = RecurringListViewModel(env.repository, env.wallets, env.categories)
            keepCollecting(list.state)
            list.refresh()
            val item =
                list.state
                    .awaitValue { it.items.data?.isNotEmpty() == true }
                    .items.data!!
                    .single()
            list.setActive(item, false)
            assertEquals("false", env.backend.await(HttpMethod.Patch, "recurring-transactions/r1").bodyField("active"))

            val edit = RecurringEditViewModel("r1", env.repository, env.wallets, env.categories)
            assertEquals("30000", edit.state.awaitValue { it.recurring != null }.amount)
            edit.onAmount("0")
            edit.save()
            assertTrue(edit.state.value.invalidAmount)
            edit.onAmount("35.000")
            edit.save()
            edit.state.awaitValue { it.done }
            assertEquals(
                "35000",
                env.backend
                    .requests(HttpMethod.Patch, "recurring-transactions/r1")
                    .last()
                    .bodyField("amount"),
            )
        }

    @Test
    fun outboxScreenRetriesAFailedWrite() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.post("transactions", transactionJson(), HttpStatusCode.Created)
            val payload = """{"walletId":"$CASH","categoryId":"food","amount":45000}"""
            env.outboxDao.upsert(OutboxEntity("o1", OUTBOX_TRANSACTION, payload, "key-1", OutboxStatus.FAILED.name))
            val viewModel = OutboxViewModel(env.repository)
            keepCollecting(viewModel.pending)
            val entry = viewModel.pending.awaitValue { it.isNotEmpty() }.single()

            viewModel.retry(entry)
            viewModel.pending.awaitValue { it.isEmpty() }
            assertEquals("key-1", env.backend.single(HttpMethod.Post, "transactions").headers[IDEMPOTENCY_KEY_HEADER])
        }
}
