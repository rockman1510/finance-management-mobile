package com.financemanage.mobile.core.model.time

import kotlinx.datetime.Clock
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** The backend's default time zone (API §2). */
const val DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh"

/** Resolves an IANA zone id, falling back to Vietnam time when it is unknown. */
fun timeZoneOrDefault(zoneId: String?): TimeZone =
    try {
        TimeZone.of(zoneId ?: DEFAULT_TIME_ZONE)
    } catch (e: IllegalTimeZoneException) {
        TimeZone.of(DEFAULT_TIME_ZONE)
    }

/** "Today" in the user's profile time zone. */
fun today(
    zoneId: String?,
    clock: Clock = Clock.System,
): LocalDate = clock.todayIn(timeZoneOrDefault(zoneId))

/** `dd/MM/yyyy`, e.g. `01/11/2026`. */
fun LocalDate.toDisplayString(): String =
    "${dayOfMonth.toString().padStart(2, '0')}/${monthNumber.toString().padStart(2, '0')}/$year"
