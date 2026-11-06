package com.financemanage.mobile.feature.budget.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.feature.budget.domain.model.Budget
import com.financemanage.mobile.feature.budget.domain.model.BudgetTransactionDto
import com.financemanage.mobile.feature.budget.domain.model.CreateBudgetRequest
import com.financemanage.mobile.feature.budget.domain.model.UpdateBudgetRequest
import com.financemanage.mobile.feature.budget.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Observes the cached budgets. */
class ObserveBudgetsUseCase(
    private val repository: BudgetRepository,
) {
    operator fun invoke(): Flow<Cached<List<Budget>>?> = repository.budgets
}

class RefreshBudgetsUseCase(
    private val repository: BudgetRepository,
) {
    suspend operator fun invoke(): AppResult<List<Budget>> = repository.refresh()
}

class GetBudgetUseCase(
    private val repository: BudgetRepository,
) {
    suspend operator fun invoke(
        id: String,
        date: LocalDate?,
    ): AppResult<Budget> = repository.get(id, date)
}

class CreateBudgetUseCase(
    private val repository: BudgetRepository,
) {
    suspend operator fun invoke(request: CreateBudgetRequest): AppResult<Budget> = repository.create(request)
}

class UpdateBudgetUseCase(
    private val repository: BudgetRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: UpdateBudgetRequest,
    ): AppResult<Budget> = repository.update(id, request)
}

class DeleteBudgetUseCase(
    private val repository: BudgetRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}

class GetBudgetTransactionsUseCase(
    private val repository: BudgetRepository,
) {
    suspend operator fun invoke(budget: Budget): AppResult<List<BudgetTransactionDto>> = repository.transactions(budget)
}
