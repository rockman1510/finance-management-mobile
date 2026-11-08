package com.financemanage.mobile.feature.home.data

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.feature.home.domain.model.HomeBill
import com.financemanage.mobile.feature.home.domain.model.HomeBudget
import com.financemanage.mobile.feature.home.domain.model.HomeGoal
import com.financemanage.mobile.feature.home.domain.model.HomePayment
import com.financemanage.mobile.feature.home.domain.model.HomeSummary
import com.financemanage.mobile.feature.home.domain.model.HomeTransaction
import com.financemanage.mobile.feature.home.domain.repository.HomeRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
private data class HomeTransactionPage(
    val items: List<HomeTransaction> = emptyList(),
)

@Serializable
private data class UnreadCount(
    val count: Long = 0,
)

class HomeRepositoryImpl(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val poller: PendingPoller,
) : HomeRepository {
    private val budgetList = ListSerializer(HomeBudget.serializer())
    private val billList = ListSerializer(HomeBill.serializer())
    private val goalList = ListSerializer(HomeGoal.serializer())
    private val transactionList = ListSerializer(HomeTransaction.serializer())

    override val summary: Flow<Cached<HomeSummary>?> = cache.observe(KEY_SUMMARY, HomeSummary.serializer())
    override val budgets: Flow<Cached<List<HomeBudget>>?> = cache.observe(KEY_BUDGETS, budgetList)
    override val bills: Flow<Cached<List<HomeBill>>?> = cache.observe(KEY_BILLS, billList)
    override val goals: Flow<Cached<List<HomeGoal>>?> = cache.observe(KEY_GOALS, goalList)
    override val transactions: Flow<Cached<List<HomeTransaction>>?> = cache.observe(KEY_TRANSACTIONS, transactionList)

    override suspend fun refreshSummary(currency: String): AppResult<HomeSummary> =
        fetch(KEY_SUMMARY, HomeSummary.serializer()) {
            client.get(api("reports/summary")) { parameter("currency", currency) }.body()
        }

    override suspend fun refreshBudgets(): AppResult<List<HomeBudget>> =
        fetch(KEY_BUDGETS, budgetList) {
            client.get(api("budgets")).body()
        }

    override suspend fun refreshBills(): AppResult<List<HomeBill>> =
        fetch(KEY_BILLS, billList) { client.get(api("bills/upcoming")) { parameter("days", UPCOMING_DAYS) }.body() }

    override suspend fun refreshGoals(): AppResult<List<HomeGoal>> =
        fetch(KEY_GOALS, goalList) {
            client.get(api("saving-goals")).body()
        }

    override suspend fun refreshTransactions(): AppResult<List<HomeTransaction>> =
        fetch(KEY_TRANSACTIONS, transactionList) {
            client
                .get(api("transactions")) {
                    parameter("page", 0)
                    parameter("size", LATEST_COUNT)
                }.body<HomeTransactionPage>()
                .items
        }

    override suspend fun unreadCount(): AppResult<Long> =
        when (val result = apiCall { client.get(api("notifications/unread-count")).body<UnreadCount>() }) {
            is AppResult.Success -> AppResult.Success(result.value.count)
            is AppResult.Failure -> result
        }

    /** Quick Pay (F12): pays the bill's current period with its defaults and polls the payment (API §2.1). */
    override suspend fun payBill(id: String): AppResult<PollResult<HomePayment>> {
        val accepted =
            apiCall {
                client
                    .post(api("bills/$id/pay")) {
                        contentType(ContentType.Application.Json)
                        setBody(EmptyBody())
                    }.body<HomePayment>()
            }
        if (accepted is AppResult.Failure) return accepted
        val paymentId = (accepted as AppResult.Success).value.id
        val result =
            poller.poll(
                fetch = {
                    when (val bill = apiCall { client.get(api("bills/$id")).body<HomeBill>() }) {
                        is AppResult.Failure -> bill
                        is AppResult.Success ->
                            bill.value.payments
                                .firstOrNull { it.id == paymentId }
                                ?.let { AppResult.Success(it) }
                                ?: AppResult.Failure(AppError.Unexpected())
                    }
                },
                isPending = { it.status.isPending },
            )
        refreshBills()
        return AppResult.Success(result)
    }

    private suspend fun <T> fetch(
        key: String,
        serializer: KSerializer<T>,
        call: suspend () -> T,
    ): AppResult<T> = apiCall { call() }.onSuccess { cache.put(key, serializer, it) }

    @Serializable
    private class EmptyBody

    private companion object {
        const val UPCOMING_DAYS = 7
        const val LATEST_COUNT = 5
        const val KEY_SUMMARY = "home:summary"
        const val KEY_BUDGETS = "home:budgets"
        const val KEY_BILLS = "home:bills"
        const val KEY_GOALS = "home:goals"
        const val KEY_TRANSACTIONS = "home:transactions"
    }
}
