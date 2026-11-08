package com.financemanage.mobile.core.domain.repository

import com.financemanage.mobile.core.domain.model.PendingWrite
import kotlinx.coroutines.flow.Flow

/** Writes created offline, waiting to reach the server (F21). */
interface OutboxRepository {
    val pending: Flow<List<PendingWrite>>

    /** Puts a FAILED write back in the queue after the user fixed the cause. */
    suspend fun retry(id: String)

    suspend fun discard(id: String)
}
