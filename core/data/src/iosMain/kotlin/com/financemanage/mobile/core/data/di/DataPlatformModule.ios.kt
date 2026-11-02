package com.financemanage.mobile.core.data.di

import androidx.room.Room
import com.financemanage.mobile.core.data.connectivity.IosNetworkMonitor
import com.financemanage.mobile.core.data.connectivity.NetworkMonitor
import com.financemanage.mobile.core.data.database.DATABASE_FILE_NAME
import com.financemanage.mobile.core.data.database.FinanceDatabase
import com.financemanage.mobile.core.data.database.buildFinanceDatabase
import com.financemanage.mobile.core.data.settings.DATASTORE_FILE_NAME
import com.financemanage.mobile.core.data.settings.createPreferencesDataStore
import com.liftric.kvault.KVault
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val dataPlatformModule: Module =
    module {
        single { KVault(serviceName = "com.financemanage.mobile", accessGroup = null) }
        single<NetworkMonitor> { IosNetworkMonitor() }
        single { createPreferencesDataStore { "${documentDirectory()}/$DATASTORE_FILE_NAME" } }
        single {
            Room
                .databaseBuilder<FinanceDatabase>(name = "${documentDirectory()}/$DATABASE_FILE_NAME")
                .buildFinanceDatabase()
        }
    }

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(url?.path) { "Documents directory not found" }
}
