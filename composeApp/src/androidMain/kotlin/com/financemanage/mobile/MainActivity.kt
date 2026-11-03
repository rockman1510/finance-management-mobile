package com.financemanage.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.financemanage.mobile.push.Push

/**
 * The single Activity of the app; everything else is Compose navigation.
 * A [FragmentActivity] because the biometric prompt (app lock) needs one.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Tapped push / financemanage:// link that started the app.
        if (savedInstanceState == null) Push.handleIntent(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Push.handleIntent(intent)
    }
}
