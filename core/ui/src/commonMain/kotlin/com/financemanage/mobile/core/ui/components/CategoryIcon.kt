package com.financemanage.mobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.financemanage.mobile.core.model.domain.WalletType

/** Icon + colour for a category. Default categories are keyed by `code`; custom ones by the picked icon name. */
data class CategoryVisual(
    val icon: ImageVector,
    val color: Color,
)

/** Icon names the user can pick for custom categories (stored in `icon`). */
val CategoryIconChoices: Map<String, ImageVector> =
    linkedMapOf(
        "food" to Icons.Outlined.Restaurant,
        "coffee" to Icons.Outlined.LocalCafe,
        "bills" to Icons.AutoMirrored.Outlined.ReceiptLong,
        "electricity" to Icons.Outlined.Bolt,
        "water" to Icons.Outlined.WaterDrop,
        "internet" to Icons.Outlined.Wifi,
        "phone" to Icons.Outlined.Phone,
        "home" to Icons.Outlined.Home,
        "transport" to Icons.Outlined.DirectionsCar,
        "fuel" to Icons.Outlined.LocalGasStation,
        "parking" to Icons.Outlined.LocalParking,
        "taxi" to Icons.Outlined.LocalTaxi,
        "shopping" to Icons.Outlined.ShoppingBag,
        "clothes" to Icons.Outlined.Checkroom,
        "household" to Icons.Outlined.Weekend,
        "entertainment" to Icons.Outlined.Movie,
        "health" to Icons.Outlined.LocalHospital,
        "education" to Icons.Outlined.School,
        "family" to Icons.Outlined.FamilyRestroom,
        "friends" to Icons.Outlined.People,
        "gift" to Icons.Outlined.CardGiftcard,
        "donation" to Icons.Outlined.VolunteerActivism,
        "insurance" to Icons.Outlined.Security,
        "fees" to Icons.Outlined.CreditCard,
        "salary" to Icons.Outlined.Payments,
        "bonus" to Icons.Outlined.EmojiEvents,
        "interest" to Icons.AutoMirrored.Outlined.TrendingUp,
        "selling" to Icons.Outlined.Sell,
        "savings" to Icons.Outlined.Savings,
        "other" to Icons.Outlined.Category,
    )

private val CodeVisuals: Map<String, CategoryVisual> =
    mapOf(
        "FOOD" to CategoryVisual(Icons.Outlined.Restaurant, Color(0xFFEF6C00)),
        "FOOD_RESTAURANT" to CategoryVisual(Icons.Outlined.Restaurant, Color(0xFFEF6C00)),
        "FOOD_COFFEE" to CategoryVisual(Icons.Outlined.LocalCafe, Color(0xFF8D6E63)),
        "BILLS" to CategoryVisual(Icons.AutoMirrored.Outlined.ReceiptLong, Color(0xFF5C6BC0)),
        "BILLS_ELECTRICITY" to CategoryVisual(Icons.Outlined.Bolt, Color(0xFFF9A825)),
        "BILLS_WATER" to CategoryVisual(Icons.Outlined.WaterDrop, Color(0xFF039BE5)),
        "BILLS_INTERNET" to CategoryVisual(Icons.Outlined.Wifi, Color(0xFF3949AB)),
        "BILLS_PHONE" to CategoryVisual(Icons.Outlined.Phone, Color(0xFF00897B)),
        "BILLS_RENT" to CategoryVisual(Icons.Outlined.Home, Color(0xFF6D4C41)),
        "TRANSPORT" to CategoryVisual(Icons.Outlined.DirectionsCar, Color(0xFF546E7A)),
        "TRANSPORT_FUEL" to CategoryVisual(Icons.Outlined.LocalGasStation, Color(0xFF546E7A)),
        "TRANSPORT_PARKING" to CategoryVisual(Icons.Outlined.LocalParking, Color(0xFF546E7A)),
        "TRANSPORT_TAXI" to CategoryVisual(Icons.Outlined.LocalTaxi, Color(0xFFFFB300)),
        "SHOPPING" to CategoryVisual(Icons.Outlined.ShoppingBag, Color(0xFFD81B60)),
        "SHOPPING_CLOTHES" to CategoryVisual(Icons.Outlined.Checkroom, Color(0xFFD81B60)),
        "SHOPPING_HOUSEHOLD" to CategoryVisual(Icons.Outlined.Weekend, Color(0xFFAD1457)),
        "ENTERTAINMENT" to CategoryVisual(Icons.Outlined.Movie, Color(0xFF8E24AA)),
        "HEALTH" to CategoryVisual(Icons.Outlined.LocalHospital, Color(0xFFE53935)),
        "EDUCATION" to CategoryVisual(Icons.Outlined.School, Color(0xFF1E88E5)),
        "FAMILY" to CategoryVisual(Icons.Outlined.FamilyRestroom, Color(0xFF43A047)),
        "FRIENDS" to CategoryVisual(Icons.Outlined.People, Color(0xFF00ACC1)),
        "GIFTS_DONATIONS" to CategoryVisual(Icons.Outlined.VolunteerActivism, Color(0xFFEC407A)),
        "INSURANCE" to CategoryVisual(Icons.Outlined.Security, Color(0xFF5E35B1)),
        "FEES" to CategoryVisual(Icons.Outlined.CreditCard, Color(0xFF757575)),
        "OTHER_EXPENSE" to CategoryVisual(Icons.Outlined.Category, Color(0xFF757575)),
        "SALARY" to CategoryVisual(Icons.Outlined.Payments, Color(0xFF2E7D32)),
        "BONUS" to CategoryVisual(Icons.Outlined.EmojiEvents, Color(0xFF2E7D32)),
        "INTEREST" to CategoryVisual(Icons.AutoMirrored.Outlined.TrendingUp, Color(0xFF00897B)),
        "SELLING" to CategoryVisual(Icons.Outlined.Sell, Color(0xFF558B2F)),
        "GIFTS_RECEIVED" to CategoryVisual(Icons.Outlined.CardGiftcard, Color(0xFFEC407A)),
        "OTHER_INCOME" to CategoryVisual(Icons.Outlined.Category, Color(0xFF2E7D32)),
    )

private val DefaultVisual = CategoryVisual(Icons.Outlined.Category, Color(0xFF757575))

fun categoryVisual(
    code: String?,
    icon: String?,
): CategoryVisual =
    code?.let { CodeVisuals[it] }
        ?: icon?.let { name -> CategoryIconChoices[name]?.let { CategoryVisual(it, DefaultVisual.color) } }
        ?: DefaultVisual

/** Round coloured badge with the category's icon. */
@Composable
fun CategoryIcon(
    code: String?,
    icon: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val visual = categoryVisual(code, icon)
    Box(
        modifier = modifier.size(size).background(visual.color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(visual.icon, contentDescription = null, tint = visual.color, modifier = Modifier.size(size * 0.55f))
    }
}

fun walletTypeIcon(type: WalletType): ImageVector =
    when (type) {
        WalletType.CASH -> Icons.Outlined.Wallet
        WalletType.BANK_ACCOUNT -> Icons.Outlined.AccountBalance
        WalletType.E_WALLET -> Icons.Outlined.AccountBalanceWallet
        WalletType.CREDIT_CARD -> Icons.Outlined.CreditCard
        WalletType.OTHER, WalletType.UNKNOWN -> Icons.Outlined.Savings
    }
