package com.financemanage.mobile.feature.home.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.feature.home.domain.model.HomeBill
import com.financemanage.mobile.feature.home.domain.model.HomeBudget
import com.financemanage.mobile.feature.home.domain.model.HomeGoal
import com.financemanage.mobile.feature.home.domain.model.HomePayment
import com.financemanage.mobile.feature.home.domain.model.HomeSummary
import com.financemanage.mobile.feature.home.domain.model.HomeTransaction
import kotlinx.coroutines.flow.Flow

/** Read-only views of other modules' data shown on Home (F12); features never depend on each other. */
interface HomeRepository {
    val summary: Flow<Cached<HomeSummary>?>

    val budgets: Flow<Cached<List<HomeBudget>>?>

    val bills: Flow<Cached<List<HomeBill>>?>

    val goals: Flow<Cached<List<HomeGoal>>?>

    val transactions: Flow<Cached<List<HomeTransaction>>?>

    suspend fun refreshSummary(currency: String): AppResult<HomeSummary>

    suspend fun refreshBudgets(): AppResult<List<HomeBudget>>

    suspend fun refreshBills(): AppResult<List<HomeBill>>

    suspend fun refreshGoals(): AppResult<List<HomeGoal>>

    suspend fun refreshTransactions(): AppResult<List<HomeTransaction>>

    suspend fun unreadCount(): AppResult<Long>

    /** Quick Pay (F12): pays the bill's current period with its defaults and polls the payment (API §2.1). */
    suspend fun payBill(id: String): AppResult<PollResult<HomePayment>>
}
