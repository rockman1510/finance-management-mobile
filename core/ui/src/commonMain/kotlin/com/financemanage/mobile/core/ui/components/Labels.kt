package com.financemanage.mobile.core.ui.components

import androidx.compose.runtime.Composable
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.domain.WalletType
import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.wallet_type_BANK_ACCOUNT
import com.financemanage.mobile.core.ui.resources.wallet_type_CASH
import com.financemanage.mobile.core.ui.resources.wallet_type_CREDIT_CARD
import com.financemanage.mobile.core.ui.resources.wallet_type_E_WALLET
import com.financemanage.mobile.core.ui.resources.wallet_type_OTHER
import org.jetbrains.compose.resources.stringResource

@Composable
fun walletTypeLabel(type: WalletType): String =
    stringResource(
        when (type) {
            WalletType.CASH -> Res.string.wallet_type_CASH
            WalletType.BANK_ACCOUNT -> Res.string.wallet_type_BANK_ACCOUNT
            WalletType.E_WALLET -> Res.string.wallet_type_E_WALLET
            WalletType.CREDIT_CARD -> Res.string.wallet_type_CREDIT_CARD
            WalletType.OTHER, WalletType.UNKNOWN -> Res.string.wallet_type_OTHER
        },
    )

/** Wallet types a user can create. */
val CreatableWalletTypes: List<WalletType> = WalletType.entries.filter { it != WalletType.UNKNOWN }

fun currencyLabel(currency: CurrencyCode): String {
    val symbol = MoneyFormatter.symbol(currency)
    return if (symbol == currency.value) currency.value else "${currency.value} ($symbol)"
}

/** Wallet picker label: "Tiền mặt · VND". */
fun walletLabel(wallet: Wallet): String = "${wallet.name} · ${wallet.currency.value}"

/** Category label with its parent: "Hoá đơn › Điện". */
fun categoryLabel(
    category: Category,
    all: List<Category>,
): String {
    val parent = category.parentId?.let { id -> all.firstOrNull { it.id == id } }
    return if (parent != null) "${parent.name} › ${category.name}" else category.name
}
