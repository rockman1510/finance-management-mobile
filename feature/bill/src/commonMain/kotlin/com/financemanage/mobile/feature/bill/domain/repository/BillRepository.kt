package com.financemanage.mobile.feature.bill.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.feature.bill.domain.model.Bill
import com.financemanage.mobile.feature.bill.domain.model.CreateBillRequest
import com.financemanage.mobile.feature.bill.domain.model.PayBillRequest
import com.financemanage.mobile.feature.bill.domain.model.Payment
import com.financemanage.mobile.feature.bill.domain.model.UpdateBillRequest
import kotlinx.coroutines.flow.Flow

/** Bills and their payments (API §10). */
interface BillRepository {
    val bills: Flow<Cached<List<Bill>>?>

    suspend fun refresh(): AppResult<List<Bill>>

    suspend fun upcoming(days: Int): AppResult<List<Bill>>

    suspend fun get(id: String): AppResult<Bill>

    suspend fun create(request: CreateBillRequest): AppResult<Bill>

    suspend fun update(
        id: String,
        request: UpdateBillRequest,
    ): AppResult<Bill>

    suspend fun delete(id: String): AppResult<Unit>

    /**
     * Pays the period currently due (202). The expense is recorded asynchronously: the payment is polled until it
     * leaves PENDING (API §2.1).
     */
    suspend fun pay(
        id: String,
        request: PayBillRequest,
    ): AppResult<PollResult<Payment>>

    suspend fun skip(id: String): AppResult<Unit>
}
