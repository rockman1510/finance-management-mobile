package com.financemanage.mobile.core.model

import com.financemanage.mobile.core.model.time.timeZoneOrDefault
import com.financemanage.mobile.core.model.time.toDisplayString
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DatesTest {
    @Test
    fun displaysDayMonthYear() {
        assertEquals("01/11/2026", LocalDate(2026, 11, 1).toDisplayString())
    }

    @Test
    fun unknownTimeZoneFallsBackToVietnam() {
        assertEquals("Asia/Ho_Chi_Minh", timeZoneOrDefault("Mars/Olympus").id)
        assertEquals("Asia/Ho_Chi_Minh", timeZoneOrDefault(null).id)
    }
}
