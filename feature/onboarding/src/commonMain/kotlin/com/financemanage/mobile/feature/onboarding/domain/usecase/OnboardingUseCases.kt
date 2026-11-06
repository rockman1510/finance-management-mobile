package com.financemanage.mobile.feature.onboarding.domain.usecase

import com.financemanage.mobile.core.domain.model.CreateWalletRequest
import com.financemanage.mobile.core.domain.model.UpdateProfileRequest
import com.financemanage.mobile.core.domain.repository.AppSettingsRepository
import com.financemanage.mobile.core.domain.repository.ProfileRepository
import com.financemanage.mobile.core.domain.repository.WalletRepository
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.WalletType
import com.financemanage.mobile.core.model.map
import com.financemanage.mobile.feature.onboarding.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.first

/**
 * Onboarding is skipped when it was completed on this device, or when the user already has wallets (an existing
 * user signing in on a new device).
 */
class ShouldSkipOnboardingUseCase(
    private val settings: AppSettingsRepository,
    private val wallets: WalletRepository,
) {
    suspend operator fun invoke(): Boolean =
        settings.preferences.first().onboardingCompleted ||
            (wallets.refresh().map { it.isNotEmpty() } as? AppResult.Success)?.value == true
}

/** Saves the default currency on the profile. */
class SetDefaultCurrencyUseCase(
    private val profiles: ProfileRepository,
) {
    suspend operator fun invoke(currency: CurrencyCode): AppResult<Unit> =
        profiles.update(UpdateProfileRequest(defaultCurrency = currency.value)).map { }
}

/** Creates the first wallet with its opening balance (sent with the currency's decimals). */
class CreateFirstWalletUseCase(
    private val wallets: WalletRepository,
) {
    suspend operator fun invoke(
        name: String,
        type: WalletType,
        balance: Money,
    ): AppResult<Unit> =
        wallets
            .create(
                CreateWalletRequest(
                    name = name,
                    type = type,
                    currency = balance.currency.value,
                    initialBalance = balance.rounded().amount,
                ),
            ).map { }
}

/** Creates the optional first monthly budget. */
class CreateMonthlyBudgetUseCase(
    private val repository: OnboardingRepository,
) {
    suspend operator fun invoke(
        name: String,
        amount: Money,
    ): AppResult<Unit> = repository.createMonthlyBudget(name, amount)
}
