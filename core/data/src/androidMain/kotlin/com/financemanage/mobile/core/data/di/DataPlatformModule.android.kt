package com.financemanage.mobile.core.data.di

import androidx.room.Room
import com.financemanage.mobile.core.data.connectivity.AndroidNetworkMonitor
import com.financemanage.mobile.core.data.database.DATABASE_FILE_NAME
import com.financemanage.mobile.core.data.database.FinanceDatabase
import com.financemanage.mobile.core.data.database.buildFinanceDatabase
import com.financemanage.mobile.core.data.settings.DATASTORE_FILE_NAME
import com.financemanage.mobile.core.data.settings.createPreferencesDataStore
import com.financemanage.mobile.core.domain.repository.NetworkMonitor
import com.liftric.kvault.KVault
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val dataPlatformModule: Module =
    module {
        single { KVault(androidContext(), "finance_manage_secure") }
        single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
        single {
            createPreferencesDataStore { androidContext().filesDir.resolve(DATASTORE_FILE_NAME).absolutePath }
        }
        single {
            val context = androidContext()
            Room
                .databaseBuilder<FinanceDatabase>(
                    context = context,
                    name = context.getDatabasePath(DATABASE_FILE_NAME).absolutePath,
                ).buildFinanceDatabase()
        }
    }
