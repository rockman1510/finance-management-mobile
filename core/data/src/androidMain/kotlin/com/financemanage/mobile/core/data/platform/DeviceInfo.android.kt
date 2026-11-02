package com.financemanage.mobile.core.data.platform

import android.os.Build

actual fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

actual fun platformName(): String = "android"
