package com.financemanage.mobile.core.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.domain.model.UpdateProfileRequest
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.domain.Profile
import kotlinx.coroutines.flow.Flow

/** The signed-in user's profile (`/me`), cached. */
interface ProfileRepository {
    val profile: Flow<Cached<Profile>?>

    suspend fun current(): Profile?

    suspend fun refresh(): AppResult<Profile>

    suspend fun update(request: UpdateProfileRequest): AppResult<Profile>
}
