package com.financemanage.mobile.core.data.database

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Upsert
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

/**
 * Local cache – the single source of truth the UI observes (offline-first). Every feature adds its entities
 * and DAOs here; bump [version] when the schema changes (it is a cache: destructive migration is acceptable).
 */
@Database(
    entities = [SyncMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(FinanceDatabaseConstructor::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun syncMetadataDao(): SyncMetadataDao
}

// Room generates the `actual` implementations for every target.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object FinanceDatabaseConstructor : RoomDatabaseConstructor<FinanceDatabase> {
    override fun initialize(): FinanceDatabase
}

internal const val DATABASE_FILE_NAME = "finance_manage.db"

internal fun RoomDatabase.Builder<FinanceDatabase>.buildFinanceDatabase(): FinanceDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

/** When a cached collection was last refreshed from the backend ("Updated at HH:mm"). */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val resource: String,
    val updatedAtEpochMillis: Long,
)

@Dao
interface SyncMetadataDao {
    @Upsert
    suspend fun upsert(entity: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE resource = :resource")
    fun observe(resource: String): Flow<SyncMetadataEntity?>

    @Query("DELETE FROM sync_metadata")
    suspend fun clear()
}
