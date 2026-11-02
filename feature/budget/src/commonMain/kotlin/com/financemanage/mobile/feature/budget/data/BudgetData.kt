package com.financemanage.mobile.feature.budget.data

import com.financemanage.mobile.core.data.cache.Cached
import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.core.model.map
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.model.serialization.FallbackEnumSerializer
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
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

@Serializable(with = BudgetPeriod.Serializer::class)
enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM,
    UNKNOWN,
    ;

    internal object Serializer : FallbackEnumSerializer<BudgetPeriod>("BudgetPeriod", BudgetPeriod.entries, UNKNOWN)
}

@Serializable(with = BudgetState.Serializer::class)
enum class BudgetState {
    NOT_STARTED,
    ON_TRACK,
    WARNING,
    EXCEEDED,
    ENDED,
    UNKNOWN,
    ;

    internal object Serializer : FallbackEnumSerializer<BudgetState>("BudgetState", BudgetState.entries, UNKNOWN)
}

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

@Serializable
data class CreateBudgetRequest(
    val name: String,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val currency: String? = null,
    val categoryIds: List<String> = emptyList(),
    val walletId: String? = null,
    val period: BudgetPeriod,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val repeat: Boolean? = null,
)

@Serializable
data class UpdateBudgetRequest(
    val name: String? = null,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val categoryIds: List<String>? = null,
    val walletId: String? = null,
    val allWallets: Boolean? = null,
    val repeat: Boolean? = null,
)

/** Minimal transaction row for the budget's daily chart and list. */
@Serializable
data class BudgetTransactionDto(
    val id: String,
    val type: TransactionType = TransactionType.UNKNOWN,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    val categoryId: String? = null,
    val occurredOn: LocalDate? = null,
    val note: String? = null,
)

@Serializable
private data class BudgetTransactionPage(
    val items: List<BudgetTransactionDto> = emptyList(),
)

data class BudgetStatus(
    val periodStart: LocalDate?,
    val periodEnd: LocalDate?,
    val spent: Money,
    val remaining: Money,
    val percent: Int,
    val dailySuggestion: Money?,
    val daysLeft: Long,
    val state: BudgetState,
    val unconvertedCurrencies: List<String>,
)

data class Budget(
    val id: String,
    val name: String,
    val amount: Money,
    val categoryIds: List<String>,
    val walletId: String?,
    val period: BudgetPeriod,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val repeat: Boolean,
    val current: BudgetStatus?,
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

class BudgetRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
) {
    private val listSerializer = ListSerializer(BudgetDto.serializer())

    /** Budgets with the status of the current period, cached (Home and Plan tab). */
    val budgets: Flow<Cached<List<Budget>>?> =
        cache.observe(KEY, listSerializer).map { cached ->
            cached?.let { Cached(it.value.map(BudgetDto::toDomain), it.updatedAt) }
        }

    suspend fun refresh(): AppResult<List<Budget>> =
        apiCall { client.get(api("budgets")).body<List<BudgetDto>>() }
            .onSuccess { cache.put(KEY, listSerializer, it) }
            .map { list -> list.map(BudgetDto::toDomain) }

    /** A budget with the status of the period containing [date] (previous/next period navigation). */
    suspend fun get(
        id: String,
        date: LocalDate?,
    ): AppResult<Budget> =
        apiCall {
            client.get(api("budgets/$id")) { date?.let { parameter("date", it.toString()) } }.body<BudgetDto>()
        }.map(BudgetDto::toDomain)

    suspend fun create(request: CreateBudgetRequest): AppResult<Budget> =
        apiCall {
            client.post(api("budgets")) { json(request) }.body<BudgetDto>()
        }.onSuccess { refresh() }.map(BudgetDto::toDomain)

    suspend fun update(
        id: String,
        request: UpdateBudgetRequest,
    ): AppResult<Budget> =
        apiCall {
            client.patch(api("budgets/$id")) { json(request) }.body<BudgetDto>()
        }.onSuccess { refresh() }.map(BudgetDto::toDomain)

    suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("budgets/$id"))
            Unit
        }.onSuccess { refresh() }

    /** Expenses counted by the budget in its period (categories + wallet + dates). */
    suspend fun transactions(budget: Budget): AppResult<List<BudgetTransactionDto>> {
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

    private fun io.ktor.client.request.HttpRequestBuilder.json(body: Any) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val KEY = "budgets"
        const val MAX_PAGE = 100
    }
}
