package com.financemanage.mobile.feature.budget.data

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.core.model.map
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.feature.budget.domain.model.Budget
import com.financemanage.mobile.feature.budget.domain.model.BudgetPeriod
import com.financemanage.mobile.feature.budget.domain.model.BudgetState
import com.financemanage.mobile.feature.budget.domain.model.BudgetStatus
import com.financemanage.mobile.feature.budget.domain.model.BudgetTransactionDto
import com.financemanage.mobile.feature.budget.domain.model.CreateBudgetRequest
import com.financemanage.mobile.feature.budget.domain.model.UpdateBudgetRequest
import com.financemanage.mobile.feature.budget.domain.repository.BudgetRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class BudgetStatusDto(
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val spent: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val remaining: BigDecimal? = null,
    val percent: Int = 0,
    @Serializable(with = MoneySerializer::class) val dailySuggestion: BigDecimal? = null,
    val daysLeft: Long = 0,
    val state: BudgetState = BudgetState.UNKNOWN,
    val unconvertedCurrencies: List<String> = emptyList(),
)

@Serializable
data class BudgetDto(
    val id: String,
    val name: String = "",
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    val categoryIds: List<String> = emptyList(),
    val walletId: String? = null,
    val period: BudgetPeriod = BudgetPeriod.UNKNOWN,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val repeat: Boolean = false,
    val current: BudgetStatusDto? = null,
)

fun BudgetDto.toDomain(): Budget {
    val code = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

    fun m(value: BigDecimal?) = Money(value ?: BigDecimal.ZERO, code)
    return Budget(
        id = id,
        name = name,
        amount = m(amount),
        categoryIds = categoryIds,
        walletId = walletId,
        period = period,
        startDate = startDate,
        endDate = endDate,
        repeat = repeat,
        current =
            current?.let {
                BudgetStatus(
                    it.periodStart,
                    it.periodEnd,
                    m(it.spent),
                    m(it.remaining),
                    it.percent,
                    it.dailySuggestion?.let(::m),
                    it.daysLeft,
                    it.state,
                    it.unconvertedCurrencies,
                )
            },
    )
}

class BudgetRepositoryImpl(
    private val client: HttpClient,
    private val cache: JsonCache,
) : BudgetRepository {
    private val listSerializer = ListSerializer(BudgetDto.serializer())

    /** Budgets with the status of the current period, cached (Home and Plan tab). */
    override val budgets: Flow<Cached<List<Budget>>?> =
        cache.observe(KEY, listSerializer).map { cached ->
            cached?.let { Cached(it.value.map(BudgetDto::toDomain), it.updatedAt) }
        }

    override suspend fun refresh(): AppResult<List<Budget>> =
        apiCall { client.get(api("budgets")).body<List<BudgetDto>>() }
            .onSuccess { cache.put(KEY, listSerializer, it) }
            .map { list -> list.map(BudgetDto::toDomain) }

    /** A budget with the status of the period containing [date] (previous/next period navigation). */
    override suspend fun get(
        id: String,
        date: LocalDate?,
    ): AppResult<Budget> =
        apiCall {
            client.get(api("budgets/$id")) { date?.let { parameter("date", it.toString()) } }.body<BudgetDto>()
        }.map(BudgetDto::toDomain)

    override suspend fun create(request: CreateBudgetRequest): AppResult<Budget> =
        apiCall {
            client.post(api("budgets")) { json(request) }.body<BudgetDto>()
        }.onSuccess { refresh() }.map(BudgetDto::toDomain)

    override suspend fun update(
        id: String,
        request: UpdateBudgetRequest,
    ): AppResult<Budget> =
        apiCall {
            client.patch(api("budgets/$id")) { json(request) }.body<BudgetDto>()
        }.onSuccess { refresh() }.map(BudgetDto::toDomain)

    override suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("budgets/$id"))
            Unit
        }.onSuccess { refresh() }

    /** Expenses counted by the budget in its period (categories + wallet + dates). */
    override suspend fun transactions(budget: Budget): AppResult<List<BudgetTransactionDto>> {
        val status = budget.current ?: return AppResult.Success(emptyList())
        return apiCall {
            client
                .get(api("transactions")) {
                    parameter("type", TransactionType.EXPENSE.name)
                    budget.categoryIds.forEach { parameter("categoryId", it) }
                    budget.walletId?.let { parameter("walletId", it) }
                    status.periodStart?.let { parameter("from", it.toString()) }
                    status.periodEnd?.let { parameter("to", it.toString()) }
                    parameter("size", MAX_PAGE)
                }.body<BudgetTransactionPage>()
        }.map { it.items }
    }

    private inline fun <reified T> io.ktor.client.request.HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val KEY = "budgets"
        const val MAX_PAGE = 100
    }
}

@Serializable
private data class BudgetTransactionPage(
    val items: List<BudgetTransactionDto> = emptyList(),
)
