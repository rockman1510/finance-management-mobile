package com.financemanage.mobile.core.domain.usecase

import com.financemanage.mobile.core.domain.model.PendingWrite
import com.financemanage.mobile.core.domain.repository.OutboxRepository
import kotlinx.coroutines.flow.Flow

/** Writes created offline that have not reached the server yet (F21). */
class ObservePendingWritesUseCase(
    private val repository: OutboxRepository,
) {
    operator fun invoke(): Flow<List<PendingWrite>> = repository.pending
}

class RetryPendingWriteUseCase(
    private val repository: OutboxRepository,
) {
    suspend operator fun invoke(write: PendingWrite) = repository.retry(write.id)
}

class DiscardPendingWriteUseCase(
    private val repository: OutboxRepository,
) {
    suspend operator fun invoke(write: PendingWrite) = repository.discard(write.id)
}
