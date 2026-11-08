package com.financemanage.mobile.core.domain.usecase

import com.financemanage.mobile.core.domain.model.SessionState
import com.financemanage.mobile.core.domain.repository.NetworkMonitor
import com.financemanage.mobile.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class ObserveSessionStateUseCase(
    private val repository: SessionRepository,
) {
    operator fun invoke(): Flow<SessionState> = repository.state
}

/** Signs out: revokes the session on the server (best effort) and wipes local data. */
class SignOutUseCase(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke() = repository.signOut()
}

class ObserveConnectivityUseCase(
    private val networkMonitor: NetworkMonitor,
) {
    operator fun invoke(): Flow<Boolean> = networkMonitor.isOnline
}
