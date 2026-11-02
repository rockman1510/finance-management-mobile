package com.financemanage.mobile.transactions

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.tab_transactions
import com.financemanage.mobile.navigation.ComingSoon
import org.jetbrains.compose.resources.stringResource

/** Transactions tab – implemented by the transaction feature (F07). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen() {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.tab_transactions)) },
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { padding ->
        ComingSoon(Modifier.padding(padding))
    }
}
