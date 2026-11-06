package com.financemanage.mobile.feature.home.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.domain.model.WalletTotal
import com.financemanage.mobile.core.domain.repository.ProfileRepository
import com.financemanage.mobile.core.domain.repository.WalletRepository
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.core.model.getOrNull
import com.financemanage.mobile.feature.home.domain.model.HomeBill
import com.financemanage.mobile.feature.home.domain.model.HomeBudget
import com.financemanage.mobile.feature.home.domain.model.HomeGoal
import com.financemanage.mobile.feature.home.domain.model.HomePayment
import com.financemanage.mobile.feature.home.domain.model.HomeSummary
import com.financemanage.mobile.feature.home.domain.model.HomeTransaction
import com.financemanage.mobile.feature.home.domain.repository.HomeRepository
import kotlinx.coroutines.flow.Flow

/** The default currency; on a first launch the profile is not cached yet, so it is fetched first. */
class GetHomeCurrencyUseCase(
    private val profiles: ProfileRepository,
) {
    suspend operator fun invoke(): CurrencyCode =
        (profiles.current() ?: profiles.refresh().getOrNull())?.defaultCurrency ?: CurrencyCode.DEFAULT
}

class ObserveHomeSummaryUseCase(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<Cached<HomeSummary>?> = repository.summary
}

/** Reloads the month summary in the user's default currency. */
class RefreshHomeSummaryUseCase(
    private val repository: HomeRepository,
    private val getHomeCurrency: GetHomeCurrencyUseCase,
) {
    suspend operator fun invoke(): AppResult<HomeSummary> = repository.refreshSummary(getHomeCurrency().value)
}

class ObserveHomeBudgetsUseCase(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<Cached<List<HomeBudget>>?> = repository.budgets
}

class RefreshHomeBudgetsUseCase(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke(): AppResult<List<HomeBudget>> = repository.refreshBudgets()
}

class ObserveHomeBillsUseCase(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<Cached<List<HomeBill>>?> = repository.bills
}

class RefreshHomeBillsUseCase(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke(): AppResult<List<HomeBill>> = repository.refreshBills()
}

class ObserveHomeGoalsUseCase(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<Cached<List<HomeGoal>>?> = repository.goals
}

class RefreshHomeGoalsUseCase(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke(): AppResult<List<HomeGoal>> = repository.refreshGoals()
}

class ObserveHomeTransactionsUseCase(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<Cached<List<HomeTransaction>>?> = repository.transactions
}

class RefreshHomeTransactionsUseCase(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke(): AppResult<List<HomeTransaction>> = repository.refreshTransactions()
}

/** Number of unread notifications (bell badge). */
class GetUnreadNotificationCountUseCase(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke(): AppResult<Long> = repository.unreadCount()
}

/** Quick Pay of a bill from Home; the expense is recorded asynchronously and polled (API §2.1). */
class PayHomeBillUseCase(
    private val repository: HomeRepository,
    private val wallets: WalletRepository,
) {
    suspend operator fun invoke(billId: String): AppResult<PollResult<HomePayment>> =
        repository.payBill(billId).also { wallets.refresh() }
}

/** What the header of Home shows besides the blocks: the wallets' total and the unread badge. */
data class HomeOverview(
    val total: WalletTotal?,
    val unread: Long?,
)

/** Reloads the profile and the wallets, then the total in the default currency and the unread badge. */
class RefreshHomeOverviewUseCase(
    private val profiles: ProfileRepository,
    private val wallets: WalletRepository,
    private val repository: HomeRepository,
    private val getHomeCurrency: GetHomeCurrencyUseCase,
) {
    suspend operator fun invoke(): HomeOverview {
        profiles.refresh()
        wallets.refresh()
        val total = wallets.total(getHomeCurrency()).getOrNull()
        val unread = repository.unreadCount().getOrNull()
        return HomeOverview(total, unread)
    }
}

/** The use cases behind Home's independent blocks, injected together. */
data class HomeUseCases(
    val observeSummary: ObserveHomeSummaryUseCase,
    val refreshSummary: RefreshHomeSummaryUseCase,
    val observeBudgets: ObserveHomeBudgetsUseCase,
    val refreshBudgets: RefreshHomeBudgetsUseCase,
    val observeBills: ObserveHomeBillsUseCase,
    val refreshBills: RefreshHomeBillsUseCase,
    val observeGoals: ObserveHomeGoalsUseCase,
    val refreshGoals: RefreshHomeGoalsUseCase,
    val observeTransactions: ObserveHomeTransactionsUseCase,
    val refreshTransactions: RefreshHomeTransactionsUseCase,
    val getUnreadCount: GetUnreadNotificationCountUseCase,
    val payBill: PayHomeBillUseCase,
    val refreshOverview: RefreshHomeOverviewUseCase,
)
