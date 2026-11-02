package com.financemanage.mobile.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.home_greeting
import com.financemanage.mobile.core.ui.resources.home_notifications
import com.financemanage.mobile.core.ui.resources.home_settings
import com.financemanage.mobile.navigation.ComingSoon
import org.jetbrains.compose.resources.stringResource

/** Home (F12): greeting, notification bell, avatar → Settings. Blocks come with their features. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.home_greeting)) },
                windowInsets = WindowInsets(0.dp),
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = stringResource(Res.string.home_notifications),
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Outlined.AccountCircle,
                            contentDescription = stringResource(Res.string.home_settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        ComingSoon(Modifier.padding(padding))
    }
}
