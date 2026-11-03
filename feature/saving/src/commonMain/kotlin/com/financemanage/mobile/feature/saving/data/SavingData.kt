package com.financemanage.mobile.feature.saving.data

import com.financemanage.mobile.core.data.cache.Cached
import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.core.network.pending.PollResult
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class MovementDto(
    val id: String,
    val type: String = "DEPOSIT",
    val source: String = "MANUAL",
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val walletId: String? = null,
    val date: LocalDate? = null,
    val status: AsyncStatus = AsyncStatus.UNKNOWN,
    val transactionId: String? = null,
    val failureReason: String? = null,
)

@Serializable
data class AutoContributionDto(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val walletId: String? = null,
    val frequency: Frequency = Frequency.MONTHLY,
    val nextDate: LocalDate? = null,
)

@Serializable
data class RoundUpDto(
    val walletId: String? = null,
    @Serializable(with = MoneySerializer::class) val unit: BigDecimal? = null,
)

@Serializable
data class GoalDto(
    val id: String,
    val name: String = "",
    @Serializable(with = MoneySerializer::class) val target: BigDecimal? = null,
    val currency: String? = null,
    val deadline: LocalDate? = null,
    val icon: String? = null,
    @Serializable(with = MoneySerializer::class) val saved: BigDecimal? = null,
    val percent: Int = 0,
    @Serializable(with = MoneySerializer::class) val remaining: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val monthlySuggestion: BigDecimal? = null,
    val daysLeft: Long? = null,
    val reached: Boolean = false,
    val autoContribution: AutoContributionDto? = null,
    val roundUp: RoundUpDto? = null,
    val movements: List<MovementDto> = emptyList(),
) {
    val currencyCode: CurrencyCode get() = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

    fun money(value: BigDecimal?): Money = Money(value ?: BigDecimal.ZERO, currencyCode)
}

@Serializable
data class CreateGoalRequest(
    val name: String,
    @Serializable(with = MoneySerializer::class) val target: BigDecimal,
    val currency: String? = null,
    val deadline: LocalDate? = null,
    val icon: String? = null,
)

@Serializable
data class UpdateGoalRequest(
    val name: String? = null,
    @Serializable(with = MoneySerializer::class) val target: BigDecimal? = null,
    val deadline: LocalDate? = null,
    val clearDeadline: Boolean? = null,
    val icon: String? = null,
)

@Serializable
data class MoveRequest(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val walletId: String,
    val date: LocalDate? = null,
)

@Serializable
data class AutoContributionRequest(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val walletId: String,
    val frequency: Frequency,
    val startDate: LocalDate? = null,
)

@Serializable
data class RoundUpRequest(
    val walletId: String,
    @Serializable(with = MoneySerializer::class) val unit: BigDecimal,
)

class SavingRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val poller: PendingPoller,
) {
    private val listSerializer = ListSerializer(GoalDto.serializer())

    val goals: Flow<Cached<List<GoalDto>>?> = cache.observe(KEY, listSerializer)

    suspend fun refresh(): AppResult<List<GoalDto>> =
        apiCall {
            client
                .get(
                    api("saving-goals"),
                ).body<List<GoalDto>>()
        }.onSuccess { cache.put(KEY, listSerializer, it) }

    suspend fun get(id: String): AppResult<GoalDto> = apiCall { client.get(api("saving-goals/$id")).body<GoalDto>() }

    suspend fun create(request: CreateGoalRequest): AppResult<GoalDto> =
        apiCall { client.post(api("saving-goals")) { json(request) }.body<GoalDto>() }.onSuccess { refresh() }

    suspend fun update(
        id: String,
        request: UpdateGoalRequest,
    ): AppResult<GoalDto> =
        apiCall {
            client.patch(api("saving-goals/$id")) { json(request) }.body<GoalDto>()
        }.onSuccess { refresh() }

    suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("saving-goals/$id"))
            Unit
        }.onSuccess { refresh() }

    /** Deposit (true) or withdrawal (false): 202, then the movement is polled until it leaves PENDING. */
    suspend fun move(
        id: String,
        deposit: Boolean,
        request: MoveRequest,
    ): AppResult<PollResult<MovementDto>> {
        val path = if (deposit) "saving-goals/$id/deposits" else "saving-goals/$id/withdrawals"
        val accepted = apiCall { client.post(api(path)) { json(request) }.body<MovementDto>() }
        if (accepted is AppResult.Failure) return accepted
        val movementId = (accepted as AppResult.Success).value.id
        val result =
            poller.poll(
                fetch = {
                    when (val goal = get(id)) {
                        is AppResult.Failure -> goal
                        is AppResult.Success ->
                            goal.value.movements
                                .firstOrNull { it.id == movementId }
                                ?.let { AppResult.Success(it) }
                                ?: AppResult.Failure(AppError.Unexpected())
                    }
                },
                isPending = { it.status.isPending },
            )
        refresh()
        return AppResult.Success(result)
    }

    suspend fun setAutoContribution(
        id: String,
        request: AutoContributionRequest,
    ): AppResult<GoalDto> =
        apiCall {
            client.put(api("saving-goals/$id/auto-contribution")) { json(request) }.body<GoalDto>()
        }

    suspend fun stopAutoContribution(id: String): AppResult<GoalDto> =
        apiCall { client.delete(api("saving-goals/$id/auto-contribution")).body<GoalDto>() }

    suspend fun setRoundUp(
        id: String,
        request: RoundUpRequest,
    ): AppResult<GoalDto> = apiCall { client.put(api("saving-goals/$id/round-up")) { json(request) }.body<GoalDto>() }

    suspend fun stopRoundUp(id: String): AppResult<GoalDto> =
        apiCall {
            client.delete(api("saving-goals/$id/round-up")).body<GoalDto>()
        }

    private inline fun <reified T> HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val KEY = "saving-goals"
    }
}
