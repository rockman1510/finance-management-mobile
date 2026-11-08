package com.financemanage.mobile.core.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.domain.model.UpdateProfileRequest
import com.financemanage.mobile.core.domain.repository.ProfileRepository
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Profile
import com.financemanage.mobile.core.model.time.today
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Observes the cached profile (`/me`). */
class ObserveProfileUseCase(
    private val repository: ProfileRepository,
) {
    operator fun invoke(): Flow<Cached<Profile>?> = repository.profile
}

/** The cached profile, if any (no network). */
class GetCachedProfileUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): Profile? = repository.current()
}

/** Reloads the profile from the server. */
class RefreshProfileUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): AppResult<Profile> = repository.refresh()
}

/** The cached profile, or the server's when nothing is cached yet. */
class GetProfileUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): Profile? =
        repository.current() ?: (repository.refresh() as? AppResult.Success)?.value
}

class UpdateProfileUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(request: UpdateProfileRequest): AppResult<Profile> = repository.update(request)
}

/** "Today" in the user's time zone (`profile.timeZone`), the device's when the profile is not cached. */
class GetTodayUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): LocalDate = today(repository.current()?.timeZone)
}

/** The user's default currency (profile), [CurrencyCode.DEFAULT] when the profile is not cached. */
class GetDefaultCurrencyUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): CurrencyCode = repository.current()?.defaultCurrency ?: CurrencyCode.DEFAULT
}
