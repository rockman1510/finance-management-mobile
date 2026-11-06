package com.financemanage.mobile.feature.saving.data

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.feature.saving.domain.model.AutoContributionRequest
import com.financemanage.mobile.feature.saving.domain.model.CreateGoalRequest
import com.financemanage.mobile.feature.saving.domain.model.GoalDto
import com.financemanage.mobile.feature.saving.domain.model.MoveRequest
import com.financemanage.mobile.feature.saving.domain.model.MovementDto
import com.financemanage.mobile.feature.saving.domain.model.RoundUpRequest
import com.financemanage.mobile.feature.saving.domain.model.UpdateGoalRequest
import com.financemanage.mobile.feature.saving.domain.repository.SavingRepository
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
import kotlinx.serialization.builtins.ListSerializer

class SavingRepositoryImpl(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val poller: PendingPoller,
) : SavingRepository {
    private val listSerializer = ListSerializer(GoalDto.serializer())

    override val goals: Flow<Cached<List<GoalDto>>?> = cache.observe(KEY, listSerializer)

    override suspend fun refresh(): AppResult<List<GoalDto>> =
        apiCall {
            client
                .get(
                    api("saving-goals"),
                ).body<List<GoalDto>>()
        }.onSuccess { cache.put(KEY, listSerializer, it) }

    override suspend fun get(id: String): AppResult<GoalDto> =
        apiCall {
            client.get(api("saving-goals/$id")).body<GoalDto>()
        }

    override suspend fun create(request: CreateGoalRequest): AppResult<GoalDto> =
        apiCall { client.post(api("saving-goals")) { json(request) }.body<GoalDto>() }.onSuccess { refresh() }

    override suspend fun update(
        id: String,
        request: UpdateGoalRequest,
    ): AppResult<GoalDto> =
        apiCall {
            client.patch(api("saving-goals/$id")) { json(request) }.body<GoalDto>()
        }.onSuccess { refresh() }

    override suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("saving-goals/$id"))
            Unit
        }.onSuccess { refresh() }

    /** Deposit (true) or withdrawal (false): 202, then the movement is polled until it leaves PENDING. */
    override suspend fun move(
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

    override suspend fun setAutoContribution(
        id: String,
        request: AutoContributionRequest,
    ): AppResult<GoalDto> =
        apiCall {
            client.put(api("saving-goals/$id/auto-contribution")) { json(request) }.body<GoalDto>()
        }

    override suspend fun stopAutoContribution(id: String): AppResult<GoalDto> =
        apiCall { client.delete(api("saving-goals/$id/auto-contribution")).body<GoalDto>() }

    override suspend fun setRoundUp(
        id: String,
        request: RoundUpRequest,
    ): AppResult<GoalDto> = apiCall { client.put(api("saving-goals/$id/round-up")) { json(request) }.body<GoalDto>() }

    override suspend fun stopRoundUp(id: String): AppResult<GoalDto> =
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
