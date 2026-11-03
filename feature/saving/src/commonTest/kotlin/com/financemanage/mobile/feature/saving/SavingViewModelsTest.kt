package com.financemanage.mobile.feature.saving

import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.respondJson
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.feature.saving.data.SavingRepository
import com.financemanage.mobile.feature.saving.presentation.GoalDetailViewModel
import com.financemanage.mobile.feature.saving.presentation.GoalFormViewModel
import com.financemanage.mobile.feature.saving.presentation.GoalListViewModel
import com.financemanage.mobile.feature.saving.presentation.Milestone
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun goalJson(
    saved: Int = 4000000,
    percent: Int = 40,
    movement: String? = null,
    deadline: String? = "2027-04-30",
) = """{"id":"g1","name":"Du lịch","target":10000000,"currency":"VND","deadline":${deadline?.let {
    "\"$it\""
} ?: "null"},
    "saved":$saved,"percent":$percent,"remaining":${10000000 - saved},"monthlySuggestion":1200000,"reached":${percent >= 100},
    "movements":[${movement ?: ""}]}"""

private fun movement(status: String) =
    """{"id":"m1","type":"DEPOSIT","amount":2000000,"walletId":"w1","date":"2026-11-01","status":"$status"}"""

class SavingViewModelsTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val repository = SavingRepository(backend.client, cache, PendingPoller())
    private val wallets = WalletRepository(backend.client, cache)
    private val profiles = ProfileRepository(backend.client, cache)

    @Test
    fun listIsCached() =
        runViewModelTest {
            backend.get("saving-goals", "[${goalJson()}]")
            val viewModel = GoalListViewModel(repository)
            viewModel.refresh()
            val goal =
                viewModel.state
                    .awaitValue { it.data != null }
                    .data!!
                    .single()
            assertEquals("6000000", goal.money(goal.remaining).amount.toStringExpanded())
        }

    @Test
    fun depositPollsTheMovementAndCelebratesHalfWay() =
        runViewModelTest {
            backend.get("saving-goals", "[]")
            backend.post("saving-goals/g1/deposits", movement("PENDING"), HttpStatusCode.Accepted)
            var calls = 0
            backend.on(HttpMethod.Get, "saving-goals/g1") {
                calls++
                respondJson(if (calls == 1) goalJson() else goalJson(6000000, 60, movement("RECORDED")))
            }
            val viewModel = GoalDetailViewModel("g1", repository, wallets, profiles)
            viewModel.refresh()
            viewModel.state.awaitValue { it.goal != null }

            viewModel.move(deposit = true, amount = BigDecimal.parseString("2000000"), walletId = "w1", date = null)
            val done = viewModel.state.awaitValue { it.milestone != null }
            assertEquals(PendingAction.Recorded, done.pending)
            assertEquals(Milestone.HALF, done.milestone)
            assertEquals(60, done.goal?.percent)
            val request = backend.single(HttpMethod.Post, "saving-goals/g1/deposits")
            assertEquals("2000000", request.bodyField("amount"))
            assertEquals("w1", request.bodyField("walletId"))

            viewModel.dismissMilestone()
            assertNull(viewModel.state.value.milestone)
        }

    @Test
    fun withdrawingTooMuchIsRefused() =
        runViewModelTest {
            backend.get("saving-goals/g1", goalJson())
            backend.fail(
                HttpMethod.Post,
                "saving-goals/g1/withdrawals",
                HttpStatusCode.UnprocessableEntity,
                "INSUFFICIENT_SAVINGS",
            )
            val viewModel = GoalDetailViewModel("g1", repository, wallets, profiles)
            viewModel.refresh()
            viewModel.state.awaitValue { it.goal != null }
            viewModel.move(deposit = false, amount = BigDecimal.parseString("9000000"), walletId = "w1", date = null)
            val failed = viewModel.state.awaitValue { it.error != null }
            assertEquals("INSUFFICIENT_SAVINGS", (failed.error as AppError.Api).code)
            assertNull(failed.pending)
        }

    @Test
    fun autoContributionAndRoundUp() =
        runViewModelTest {
            backend.get("saving-goals", "[]")
            backend.on(HttpMethod.Put, "saving-goals/g1/auto-contribution") { respondJson(goalJson()) }
            backend.on(HttpMethod.Delete, "saving-goals/g1/auto-contribution") { respondJson(goalJson()) }
            backend.on(HttpMethod.Put, "saving-goals/g1/round-up") { respondJson(goalJson()) }
            val viewModel = GoalDetailViewModel("g1", repository, wallets, profiles)

            viewModel.setAutoContribution(
                BigDecimal.parseString("500000"),
                "w1",
                Frequency.WEEKLY,
                LocalDate(2026, 11, 3),
            )
            val auto = backend.await(HttpMethod.Put, "saving-goals/g1/auto-contribution")
            assertEquals("WEEKLY", auto.bodyField("frequency"))
            assertEquals("2026-11-03", auto.bodyField("startDate"))
            viewModel.state.awaitValue { it.goal != null && !it.busy }

            viewModel.stopAutoContribution()
            backend.await(HttpMethod.Delete, "saving-goals/g1/auto-contribution")
            viewModel.state.awaitValue { !it.busy }

            viewModel.setRoundUp("w1", BigDecimal.parseString("10000"))
            assertEquals("10000", backend.await(HttpMethod.Put, "saving-goals/g1/round-up").bodyField("unit"))
            backend.await(HttpMethod.Get, "saving-goals", count = 3)
        }

    @Test
    fun formCreatesAndClearsADeadlineOnEdit() =
        runViewModelTest {
            backend.post("saving-goals", goalJson(), HttpStatusCode.Created)
            backend.get("saving-goals", "[${goalJson()}]")
            val create = GoalFormViewModel(null, repository, profiles)
            create.save()
            assertTrue(create.state.value.nameMissing)
            assertTrue(create.state.value.invalidTarget)
            create.onName("Du lịch")
            create.onTarget("10.000.000")
            create.onDeadline(LocalDate(2027, 4, 30))
            create.save()
            create.state.awaitValue { it.saved }
            val created = backend.single(HttpMethod.Post, "saving-goals")
            assertEquals("10000000", created.bodyField("target"))
            assertEquals("2027-04-30", created.bodyField("deadline"))

            backend.get("saving-goals/g1", goalJson())
            backend.patch("saving-goals/g1", goalJson(deadline = null))
            val edit = GoalFormViewModel("g1", repository, profiles)
            edit.state.awaitValue { it.hadDeadline }
            edit.onDeadline(null)
            edit.save()
            edit.state.awaitValue { it.saved }
            val patch = backend.single(HttpMethod.Patch, "saving-goals/g1")
            assertEquals("true", patch.bodyField("clearDeadline"))
            assertNull(patch.bodyField("deadline"))
        }
}
