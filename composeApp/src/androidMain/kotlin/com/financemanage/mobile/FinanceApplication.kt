package com.financemanage.mobile

import android.app.Application
import com.financemanage.mobile.di.initKoin
import org.koin.android.ext.koin.androidContext

class FinanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(platformApiConfig()) {
            androidContext(this@FinanceApplication)
        }
    }
}
