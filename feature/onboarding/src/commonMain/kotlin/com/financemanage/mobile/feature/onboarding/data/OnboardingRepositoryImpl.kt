package com.financemanage.mobile.feature.onboarding.data

import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.feature.onboarding.domain.repository.OnboardingRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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

class OnboardingRepositoryImpl(
    private val client: HttpClient,
) : OnboardingRepository {
    override suspend fun createMonthlyBudget(
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
}
