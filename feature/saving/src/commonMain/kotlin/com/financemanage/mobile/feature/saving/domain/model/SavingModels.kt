package com.financemanage.mobile.feature.saving.domain.model

import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class MovementDto(
    val id: String,
    val type: String = "DEPOSIT",
    val source: String = "MANUAL",
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val walletId: String? = null,
    val date: LocalDate? = null,
    val status: AsyncStatus = AsyncStatus.UNKNOWN,
    val transactionId: String? = null,
    val failureReason: String? = null,
)

@Serializable
data class AutoContributionDto(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val walletId: String? = null,
    val frequency: Frequency = Frequency.MONTHLY,
    val nextDate: LocalDate? = null,
)

@Serializable
data class RoundUpDto(
    val walletId: String? = null,
    @Serializable(with = MoneySerializer::class) val unit: BigDecimal? = null,
)

@Serializable
data class GoalDto(
    val id: String,
    val name: String = "",
    @Serializable(with = MoneySerializer::class) val target: BigDecimal? = null,
    val currency: String? = null,
    val deadline: LocalDate? = null,
    val icon: String? = null,
    @Serializable(with = MoneySerializer::class) val saved: BigDecimal? = null,
    val percent: Int = 0,
    @Serializable(with = MoneySerializer::class) val remaining: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val monthlySuggestion: BigDecimal? = null,
    val daysLeft: Long? = null,
    val reached: Boolean = false,
    val autoContribution: AutoContributionDto? = null,
    val roundUp: RoundUpDto? = null,
    val movements: List<MovementDto> = emptyList(),
) {
    val currencyCode: CurrencyCode get() = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

    fun money(value: BigDecimal?): Money = Money(value ?: BigDecimal.ZERO, currencyCode)
}

@Serializable
data class CreateGoalRequest(
    val name: String,
    @Serializable(with = MoneySerializer::class) val target: BigDecimal,
    val currency: String? = null,
    val deadline: LocalDate? = null,
    val icon: String? = null,
)

@Serializable
data class UpdateGoalRequest(
    val name: String? = null,
    @Serializable(with = MoneySerializer::class) val target: BigDecimal? = null,
    val deadline: LocalDate? = null,
    val clearDeadline: Boolean? = null,
    val icon: String? = null,
)

@Serializable
data class MoveRequest(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val walletId: String,
    val date: LocalDate? = null,
)

@Serializable
data class AutoContributionRequest(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val walletId: String,
    val frequency: Frequency,
    val startDate: LocalDate? = null,
)

@Serializable
data class RoundUpRequest(
    val walletId: String,
    @Serializable(with = MoneySerializer::class) val unit: BigDecimal,
)
