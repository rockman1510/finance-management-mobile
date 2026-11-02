package com.financemanage.mobile.core.data.platform

import platform.UIKit.UIDevice

actual fun deviceName(): String = UIDevice.currentDevice.name

actual fun platformName(): String = "ios"
