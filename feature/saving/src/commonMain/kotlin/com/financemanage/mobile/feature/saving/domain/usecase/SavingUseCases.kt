package com.financemanage.mobile.feature.saving.domain.usecase

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
import com.financemanage.mobile.feature.saving.domain.repository.SavingRepository
import kotlinx.coroutines.flow.Flow

/** Observes the cached saving goals. */
class ObserveGoalsUseCase(
    private val repository: SavingRepository,
) {
    operator fun invoke(): Flow<Cached<List<GoalDto>>?> = repository.goals
}

class RefreshGoalsUseCase(
    private val repository: SavingRepository,
) {
    suspend operator fun invoke(): AppResult<List<GoalDto>> = repository.refresh()
}

class GetGoalUseCase(
    private val repository: SavingRepository,
) {
    suspend operator fun invoke(id: String): AppResult<GoalDto> = repository.get(id)
}

class CreateGoalUseCase(
    private val repository: SavingRepository,
) {
    suspend operator fun invoke(request: CreateGoalRequest): AppResult<GoalDto> = repository.create(request)
}

class UpdateGoalUseCase(
    private val repository: SavingRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: UpdateGoalRequest,
    ): AppResult<GoalDto> = repository.update(id, request)
}

class DeleteGoalUseCase(
    private val repository: SavingRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}

/** Deposits into or withdraws from a goal; the movement is recorded asynchronously (API §2.1). */
class MoveGoalMoneyUseCase(
    private val repository: SavingRepository,
) {
    suspend operator fun invoke(
        id: String,
        deposit: Boolean,
        request: MoveRequest,
    ): AppResult<PollResult<MovementDto>> = repository.move(id, deposit, request)
}

/** Sets up (or, with a null [request], stops) the automatic contribution of a goal. */
class UpdateAutoContributionUseCase(
    private val repository: SavingRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: AutoContributionRequest?,
    ): AppResult<GoalDto> =
        if (request == null) repository.stopAutoContribution(id) else repository.setAutoContribution(id, request)
}

/** Sets up (or, with a null [request], stops) the round-up of a goal. */
class UpdateRoundUpUseCase(
    private val repository: SavingRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: RoundUpRequest?,
    ): AppResult<GoalDto> = if (request == null) repository.stopRoundUp(id) else repository.setRoundUp(id, request)
}
