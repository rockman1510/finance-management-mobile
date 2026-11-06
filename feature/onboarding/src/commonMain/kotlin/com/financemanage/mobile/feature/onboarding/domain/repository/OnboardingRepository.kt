package com.financemanage.mobile.feature.onboarding.domain.repository

import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.Money

/** The one call onboarding owns; everything else goes through the shared repositories. */
interface OnboardingRepository {
    /** Creates a repeating monthly budget (API §7). */
    suspend fun createMonthlyBudget(
        name: String,
        amount: Money,
    ): AppResult<Unit>
}
