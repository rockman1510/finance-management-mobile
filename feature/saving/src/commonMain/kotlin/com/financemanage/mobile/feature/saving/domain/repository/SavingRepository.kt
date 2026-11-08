package com.financemanage.mobile.feature.saving.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.feature.saving.domain.model.AutoContributionRequest
import com.financemanage.mobile.feature.saving.domain.model.CreateGoalRequest
import com.financemanage.mobile.feature.saving.domain.model.GoalDto
import com.financemanage.mobile.feature.saving.domain.model.MoveRequest
import com.financemanage.mobile.feature.saving.domain.model.MovementDto
import com.financemanage.mobile.feature.saving.domain.model.RoundUpRequest
import com.financemanage.mobile.feature.saving.domain.model.UpdateGoalRequest
import kotlinx.coroutines.flow.Flow

/** Saving goals (API §11). */
interface SavingRepository {
    val goals: Flow<Cached<List<GoalDto>>?>

    suspend fun refresh(): AppResult<List<GoalDto>>

    suspend fun get(id: String): AppResult<GoalDto>

    suspend fun create(request: CreateGoalRequest): AppResult<GoalDto>

    suspend fun update(
        id: String,
        request: UpdateGoalRequest,
    ): AppResult<GoalDto>

    suspend fun delete(id: String): AppResult<Unit>

    /** Deposit (true) or withdrawal (false): 202, then the movement is polled until it leaves PENDING. */
    suspend fun move(
        id: String,
        deposit: Boolean,
        request: MoveRequest,
    ): AppResult<PollResult<MovementDto>>

    suspend fun setAutoContribution(
        id: String,
        request: AutoContributionRequest,
    ): AppResult<GoalDto>

    suspend fun stopAutoContribution(id: String): AppResult<GoalDto>

    suspend fun setRoundUp(
        id: String,
        request: RoundUpRequest,
    ): AppResult<GoalDto>

    suspend fun stopRoundUp(id: String): AppResult<GoalDto>
}
