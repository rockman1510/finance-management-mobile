package com.financemanage.mobile.feature.budget.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.feature.budget.domain.model.Budget
import com.financemanage.mobile.feature.budget.domain.model.BudgetTransactionDto
import com.financemanage.mobile.feature.budget.domain.model.CreateBudgetRequest
import com.financemanage.mobile.feature.budget.domain.model.UpdateBudgetRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Budgets and their consumption (API §9). */
interface BudgetRepository {
    /** Budgets with the status of the current period, cached (Home and Plan tab). */
    val budgets: Flow<Cached<List<Budget>>?>

    suspend fun refresh(): AppResult<List<Budget>>

    /** A budget with the status of the period containing [date] (previous/next period navigation). */
    suspend fun get(
        id: String,
        date: LocalDate?,
    ): AppResult<Budget>

    suspend fun create(request: CreateBudgetRequest): AppResult<Budget>

    suspend fun update(
        id: String,
        request: UpdateBudgetRequest,
    ): AppResult<Budget>

    suspend fun delete(id: String): AppResult<Unit>

    /** Expenses counted by the budget in its period (categories + wallet + dates). */
    suspend fun transactions(budget: Budget): AppResult<List<BudgetTransactionDto>>
}
