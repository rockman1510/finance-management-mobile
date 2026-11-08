package com.financemanage.mobile.core.domain.model

/** Status of a write waiting to reach the server (F21). */
enum class OutboxStatus { WAITING, FAILED }

/** A write created offline (transaction, transfer) that is waiting to be sent, or failed (F21). */
data class PendingWrite(
    val id: String,
    val kind: String,
    val summary: String,
    val status: OutboxStatus,
    val errorCode: String?,
    val createdAtEpochMillis: Long,
) {
    val failed: Boolean get() = status == OutboxStatus.FAILED
}
