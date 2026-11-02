package com.financemanage.mobile

import androidx.compose.ui.window.ComposeUIViewController
import com.financemanage.mobile.di.initKoin
import platform.UIKit.UIViewController

private var koinStarted = false

/** Entry point called from Swift (`MainViewControllerKt.MainViewController()`). */
@Suppress("FunctionName", "ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        initKoin(platformApiConfig())
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}
