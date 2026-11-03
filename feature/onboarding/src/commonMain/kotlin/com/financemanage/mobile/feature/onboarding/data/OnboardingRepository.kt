package com.financemanage.mobile.feature.onboarding.data

import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.data.shared.CreateWalletRequest
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.UpdateProfileRequest
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.WalletType
import com.financemanage.mobile.core.model.map
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

@Serializable
private data class CreateMonthlyBudgetRequest(
    val name: String,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val currency: String,
    // No default values: NetworkJson does not encode defaults, and `period` is required (API §7).
    val period: String,
    val repeat: Boolean,
)

/** Everything onboarding needs; it only uses shared repositories plus one budget call. */
class OnboardingRepository(
    private val client: HttpClient,
    private val settings: AppSettingsRepository,
    private val profiles: ProfileRepository,
    private val wallets: WalletRepository,
) {
    suspend fun isCompleted(): Boolean = settings.preferences.first().onboardingCompleted

    /** An existing user signing in on a new device already has wallets: skip onboarding. */
    suspend fun hasWallets(): AppResult<Boolean> = wallets.refresh().map { it.isNotEmpty() }

    suspend fun defaultCurrency(): CurrencyCode = profiles.current()?.defaultCurrency ?: CurrencyCode.DEFAULT

    suspend fun setDefaultCurrency(currency: CurrencyCode): AppResult<Unit> =
        profiles.update(UpdateProfileRequest(defaultCurrency = currency.value)).map { }

    suspend fun createWallet(
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

    suspend fun createMonthlyBudget(
        name: String,
        amount: Money,
    ): AppResult<Unit> =
        apiCall {
            client.post(api("budgets")) {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateMonthlyBudgetRequest(
                        name = name,
                        amount = amount.rounded().amount,
                        currency = amount.currency.value,
                        period = "MONTHLY",
                        repeat = true,
                    ),
                )
            }
            Unit
        }

    suspend fun markCompleted() = settings.setOnboardingCompleted(true)
}
