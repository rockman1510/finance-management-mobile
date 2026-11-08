package com.financemanage.mobile.core.domain.model

/**
 * Where a notification points to (API §14): `referenceType` (`BILL`, `BUDGET`, `SAVING_GOAL`, `DEPOSIT`, `LOAN`) and
 * `referenceId`. Both are null for `DAILY_REMINDER` (→ Quick add).
 */
data class NotificationLink(
    val referenceType: String?,
    val referenceId: String?,
) {
    companion object {
        private const val SCHEME = "financemanage://"

        /** Parses `financemanage://<referenceType>/<id>` (also `financemanage://quick-add`). */
        fun parse(url: String): NotificationLink? {
            if (!url.startsWith(SCHEME, ignoreCase = true)) return null
            val parts =
                url
                    .substring(SCHEME.length)
                    .trim('/')
                    .split('/')
                    .filter { it.isNotBlank() }
            val type = parts.getOrNull(0)?.uppercase()?.replace('-', '_')
            return when {
                type == null || type == "QUICK_ADD" -> NotificationLink(null, null)
                else -> NotificationLink(type, parts.getOrNull(1))
            }
        }
    }
}
