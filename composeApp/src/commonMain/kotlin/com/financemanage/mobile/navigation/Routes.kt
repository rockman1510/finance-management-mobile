package com.financemanage.mobile.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object TransactionsRoute

@Serializable
data object PlanRoute

@Serializable
data object AssetsRoute

/** Quick add opened from the ➕ sheet. Replaced by the transaction feature (F07/F08). */
@Serializable
data class QuickAddRoute(
    val kind: QuickAddKind,
)

@Serializable
enum class QuickAddKind { EXPENSE, INCOME, TRANSFER }
