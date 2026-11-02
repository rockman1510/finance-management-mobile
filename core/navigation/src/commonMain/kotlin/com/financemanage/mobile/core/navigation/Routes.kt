package com.financemanage.mobile.core.navigation

import kotlinx.serialization.Serializable

// Route contracts shared by all features. A feature registers the destinations it owns and may navigate to any
// route here without depending on the feature that owns it. Arguments are primitives/strings only.

// --- Bottom bar tabs
@Serializable data object HomeRoute

@Serializable data class TransactionsRoute(
    val walletId: String? = null,
    val categoryId: String? = null,
)

@Serializable data class PlanRoute(
    val tab: Int = 0,
)

@Serializable data object AssetsRoute

// --- Settings (F04, F11, F20)
@Serializable data object SettingsRoute

@Serializable data object ProfileEditRoute

@Serializable data object EmailVerificationRoute

@Serializable data object PasswordRoute

@Serializable data object TwoFactorRoute

@Serializable data object SessionsRoute

@Serializable data object DeleteAccountRoute

@Serializable data object AppLockRoute

@Serializable data object NotificationSettingsRoute

@Serializable data object ExchangeRatesRoute

@Serializable data object CurrencyConverterRoute

@Serializable data object OutboxRoute

// --- Wallets (F05)
@Serializable data object WalletsRoute

@Serializable data class WalletDetailRoute(
    val id: String,
)

@Serializable data class WalletEditRoute(
    val id: String? = null,
)

@Serializable data class AdjustBalanceRoute(
    val id: String,
)

// --- Categories (F06)
@Serializable data object CategoriesRoute

@Serializable data class CategoryEditRoute(
    val id: String? = null,
    val type: String = "EXPENSE",
    val parentId: String? = null,
)

// --- Transactions (F07–F10)
@Serializable data class QuickAddRoute(
    val kind: String = "EXPENSE",
    val transactionId: String? = null,
)

@Serializable data class TransactionDetailRoute(
    val id: String,
)

@Serializable data object TransferRoute

@Serializable data object RecurringListRoute

@Serializable data class RecurringEditRoute(
    val id: String,
)

// --- Plan (F13–F15)
@Serializable data class BudgetDetailRoute(
    val id: String,
)

@Serializable data class BudgetEditRoute(
    val id: String? = null,
)

@Serializable data class BillDetailRoute(
    val id: String,
)

@Serializable data class BillEditRoute(
    val id: String? = null,
)

@Serializable data class GoalDetailRoute(
    val id: String,
)

@Serializable data class GoalEditRoute(
    val id: String? = null,
)

// --- Assets (F16–F19)
@Serializable data object DepositsRoute

@Serializable data class DepositDetailRoute(
    val id: String,
)

@Serializable data object DepositCreateRoute

@Serializable data object LoansRoute

@Serializable data class LoanDetailRoute(
    val id: String,
)

@Serializable data object LoanCreateRoute

@Serializable data object InvestmentsRoute

@Serializable data class InvestmentDetailRoute(
    val id: String,
)

@Serializable data class InvestmentEditRoute(
    val id: String? = null,
)

@Serializable data object ReportsRoute

@Serializable data object NetWorthRoute

// --- Notifications (F20)
@Serializable data object NotificationsRoute

/** Kinds of [QuickAddRoute]. */
object QuickAddKind {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}
