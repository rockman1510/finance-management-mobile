package com.financemanage.mobile.feature.transaction

import com.financemanage.mobile.core.data.outbox.OutboxStatus
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.network.IDEMPOTENCY_KEY_HEADER
import com.financemanage.mobile.core.testing.awaitCondition
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.ui.components.KeypadKey
import com.financemanage.mobile.feature.transaction.platform.PickedFile
import com.financemanage.mobile.feature.transaction.presentation.QuickAddEvent
import com.financemanage.mobile.feature.transaction.presentation.QuickAddProblem
import com.financemanage.mobile.feature.transaction.presentation.QuickAddViewModel
import com.financemanage.mobile.feature.transaction.presentation.RepeatSettings
import com.financemanage.mobile.feature.transaction.presentation.toKeypadRaw
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickAddViewModelTest {
    private fun TestScope.newViewModel(
        env: TransactionTestEnv,
        kind: String = "EXPENSE",
        id: String? = null,
    ) = QuickAddViewModel(kind, id, env.repository, env.wallets, env.categories, env.settings, env.profiles)

    private fun QuickAddViewModel.type(digits: String) =
        digits.forEach { c -> onKey(if (c == '.') KeypadKey.Decimal else KeypadKey.Digit(c.digitToInt())) }

    @Test
    fun keypadRawDropsTrailingZeros() {
        assertEquals("12.5", "12.50".toKeypadRaw())
        assertEquals("12", "12.00".toKeypadRaw())
        assertEquals("45000", "45000".toKeypadRaw())
    }

    @Test
    fun loadsActiveWalletsAndRemembersTheLastOne() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.settings.setLastWallet(USD_CARD)
            val viewModel = newViewModel(env)
            val loaded = viewModel.state.awaitValue { it.wallets.isNotEmpty() }
            assertEquals(listOf(CASH, USD_CARD), loaded.wallets.map { it.id })
            assertEquals(USD_CARD, loaded.walletId)
            assertEquals(2, loaded.amount.maxFractionDigits)
            assertEquals(loaded.today, loaded.date)
        }

    @Test
    fun validatesAndSavesWithAnIdempotencyKey() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.post("transactions", transactionJson(), HttpStatusCode.Created)
            val viewModel = newViewModel(env)
            viewModel.state.awaitValue { it.wallets.isNotEmpty() }

            viewModel.save()
            assertEquals(setOf(QuickAddProblem.AMOUNT, QuickAddProblem.CATEGORY), viewModel.state.value.problems)

            viewModel.type("45000")
            viewModel.onCategory(
                viewModel.state.value.categories
                    .first { it.id == "food" },
            )
            viewModel.onNote("  Phở ")
            viewModel.setYesterday()
            viewModel.save()
            assertEquals(QuickAddEvent.Done, viewModel.events.first())

            val request = env.backend.single(HttpMethod.Post, "transactions")
            assertEquals(CASH, request.bodyField("walletId"))
            assertEquals("food", request.bodyField("categoryId"))
            assertEquals("45000", request.bodyField("amount"))
            assertEquals("Phở", request.bodyField("note"))
            assertNull(request.bodyField("excludeFromReport"))
            assertEquals(
                viewModel.state.value.today!!
                    .let { kotlinx.datetime.LocalDate.fromEpochDays(it.toEpochDays() - 1) }
                    .toString(),
                request.bodyField("occurredOn"),
            )
            assertTrue(request.headers[IDEMPOTENCY_KEY_HEADER]!!.isNotBlank())
            assertEquals(
                listOf("food"),
                env.settings.preferences
                    .first()
                    .recentCategoryIds,
            )
            assertEquals(
                CASH,
                env.settings.preferences
                    .first()
                    .lastWalletId,
            )
            // Balances and the cached first page are refreshed after the write.
            env.backend.await(HttpMethod.Get, "transactions")
        }

    @Test
    fun aRetryAfterAServerErrorReusesTheKeyButAnEditStartsANewOne() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.fail(HttpMethod.Post, "transactions", HttpStatusCode.InternalServerError, "INTERNAL_ERROR")
            val viewModel = newViewModel(env)
            viewModel.state.awaitValue { it.wallets.isNotEmpty() }
            viewModel.type("1000")
            viewModel.onCategory(
                viewModel.state.value.categories
                    .first(),
            )

            viewModel.save()
            viewModel.state.awaitValue { it.error != null }
            viewModel.save()
            env.backend.await(HttpMethod.Post, "transactions", count = 2)
            viewModel.state.awaitValue { !it.saving }
            viewModel.type("0")
            viewModel.save()
            env.backend.await(HttpMethod.Post, "transactions", count = 3)

            val keys = env.backend.requests(HttpMethod.Post, "transactions").map { it.headers[IDEMPOTENCY_KEY_HEADER] }
            assertEquals(keys[0], keys[1])
            assertNotEquals(keys[1], keys[2])
            viewModel.state.awaitValue { !it.saving }
        }

    @Test
    fun offlineSaveIsQueuedAndSentLaterWithTheSameKey() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.offline(HttpMethod.Post, "transactions")
            val viewModel = newViewModel(env)
            viewModel.state.awaitValue { it.wallets.isNotEmpty() }
            viewModel.type("45000")
            viewModel.onCategory(
                viewModel.state.value.categories
                    .first(),
            )
            viewModel.addFile(PickedFile(byteArrayOf(1), "a.jpg", "image/jpeg"))

            viewModel.save()
            assertEquals(QuickAddEvent.Queued(droppedFiles = true), viewModel.events.first())
            val queued =
                env.outboxDao.rows.value
                    .single()
            assertEquals(OutboxStatus.WAITING.name, queued.status)
            assertTrue(queued.summary.startsWith("Ăn uống · 45.000"))
            val firstKey =
                env.backend
                    .requests(HttpMethod.Post, "transactions")
                    .first()
                    .headers[IDEMPOTENCY_KEY_HEADER]
            assertEquals(firstKey, queued.idempotencyKey)

            env.backend.post("transactions", transactionJson(), HttpStatusCode.Created)
            env.network.online.value = true
            env.outbox.start()
            awaitCondition("outbox sent") {
                env.outboxDao.rows.value
                    .isEmpty()
            }
            val attempts = env.backend.requests(HttpMethod.Post, "transactions")
            assertTrue(attempts.size >= 2)
            assertTrue(attempts.all { it.headers[IDEMPOTENCY_KEY_HEADER] == firstKey })
            assertEquals("45000", attempts.last().bodyField("amount"))
        }

    @Test
    fun businessErrorIsShownAndNothingIsQueued() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.fail(HttpMethod.Post, "transactions", HttpStatusCode.UnprocessableEntity, "WALLET_ARCHIVED")
            val viewModel = newViewModel(env)
            viewModel.state.awaitValue { it.wallets.isNotEmpty() }
            viewModel.type("1")
            viewModel.onCategory(
                viewModel.state.value.categories
                    .first(),
            )
            viewModel.save()
            val failed = viewModel.state.awaitValue { it.error != null }
            assertEquals("WALLET_ARCHIVED", (failed.error as AppError.Api).code)
            assertTrue(
                env.outboxDao.rows.value
                    .isEmpty(),
            )
        }

    @Test
    fun savedTransactionUploadsReceiptsAndAddAnotherClearsTheForm() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.post("transactions", transactionJson(), HttpStatusCode.Created)
            env.backend.post(
                "transactions/t1/attachments",
                """{"id":"a1","fileName":"a.jpg"}""",
                HttpStatusCode.Created,
            )
            val viewModel = newViewModel(env)
            viewModel.state.awaitValue { it.wallets.isNotEmpty() }
            viewModel.onAddAnother(true)
            viewModel.type("45000")
            viewModel.onCategory(
                viewModel.state.value.categories
                    .first(),
            )
            viewModel.onNote("Phở")
            viewModel.addFile(PickedFile(byteArrayOf(1, 2), "a.jpg", "image/jpeg"))
            viewModel.addFile(PickedFile(byteArrayOf(3), "b.pdf", "application/pdf"))

            viewModel.save()
            assertEquals(QuickAddEvent.SavedAndCleared, viewModel.events.first())
            assertEquals(2, env.backend.requests(HttpMethod.Post, "transactions/t1/attachments").size)
            with(viewModel.state.value) {
                assertTrue(amount.isEmpty)
                assertEquals("", note)
                assertTrue(files.isEmpty())
                assertEquals("food", categoryId)
                assertEquals(listOf("food"), recentCategoryIds)
            }
        }

    @Test
    fun repeatCreatesARecurringTransactionInstead() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.post("recurring-transactions", """{"id":"r1","frequency":"WEEKLY"}""", HttpStatusCode.Created)
            env.backend.get("recurring-transactions", "[]")
            val viewModel = newViewModel(env, kind = "INCOME")
            viewModel.state.awaitValue { it.wallets.isNotEmpty() }
            viewModel.type("1000")
            viewModel.onCategory(
                viewModel.state.value.categories
                    .first { it.id == "salary" },
            )
            viewModel.onRepeat(RepeatSettings(enabled = true, frequency = Frequency.WEEKLY, interval = 2))
            viewModel.save()
            assertEquals(QuickAddEvent.Done, viewModel.events.first())

            val request = env.backend.single(HttpMethod.Post, "recurring-transactions")
            assertEquals("WEEKLY", request.bodyField("frequency"))
            assertEquals("2", request.bodyField("interval"))
            assertEquals("salary", request.bodyField("categoryId"))
            assertTrue(env.backend.requests(HttpMethod.Post, "transactions").isEmpty())
        }

    @Test
    fun switchingTypeDropsACategoryOfTheOtherType() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            val viewModel = newViewModel(env)
            viewModel.state.awaitValue { it.categories.isNotEmpty() }
            viewModel.onCategory(
                viewModel.state.value.categories
                    .first { it.id == "food" },
            )
            viewModel.setIncome(true)
            assertNull(viewModel.state.value.categoryId)
        }

    @Test
    fun editLoadsTheTransactionAndPatchesIt() =
        runViewModelTest {
            val env = TransactionTestEnv(backgroundScope)
            env.backend.get(
                "transactions/t9",
                transactionJson(id = "t9", wallet = USD_CARD, currency = "USD", amount = "12.50"),
            )
            env.backend.patch("transactions/t9", transactionJson(id = "t9"))
            val viewModel = newViewModel(env, id = "t9")
            val loaded = viewModel.state.awaitValue { it.walletId == USD_CARD && !it.amount.isEmpty }
            assertEquals("12.5", loaded.amount.raw)
            assertEquals("Phở", loaded.note)

            viewModel.onKey(KeypadKey.Backspace)
            viewModel.save()
            assertEquals(QuickAddEvent.Done, viewModel.events.first())
            assertEquals("12", env.backend.single(HttpMethod.Patch, "transactions/t9").bodyField("amount"))
        }
}
