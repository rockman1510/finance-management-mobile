package com.financemanage.mobile.feature.bill.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.feature.bill.domain.model.Bill
import com.financemanage.mobile.feature.bill.domain.model.CreateBillRequest
import com.financemanage.mobile.feature.bill.domain.model.PayBillRequest
import com.financemanage.mobile.feature.bill.domain.model.Payment
import com.financemanage.mobile.feature.bill.domain.model.UpdateBillRequest
import com.financemanage.mobile.feature.bill.domain.repository.BillRepository
import kotlinx.coroutines.flow.Flow

/** Observes the cached bills. */
class ObserveBillsUseCase(
    private val repository: BillRepository,
) {
    operator fun invoke(): Flow<Cached<List<Bill>>?> = repository.bills
}

class RefreshBillsUseCase(
    private val repository: BillRepository,
) {
    suspend operator fun invoke(): AppResult<List<Bill>> = repository.refresh()
}

class GetBillUseCase(
    private val repository: BillRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Bill> = repository.get(id)
}

class CreateBillUseCase(
    private val repository: BillRepository,
) {
    suspend operator fun invoke(request: CreateBillRequest): AppResult<Bill> = repository.create(request)
}

class UpdateBillUseCase(
    private val repository: BillRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: UpdateBillRequest,
    ): AppResult<Bill> = repository.update(id, request)
}

class DeleteBillUseCase(
    private val repository: BillRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}

/** Pays the period currently due; the expense is recorded asynchronously and polled (API §2.1). */
class PayBillUseCase(
    private val repository: BillRepository,
) {
    suspend operator fun invoke(
        id: String,
        request: PayBillRequest,
    ): AppResult<PollResult<Payment>> = repository.pay(id, request)
}

/** Skips the period currently due. */
class SkipBillUseCase(
    private val repository: BillRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.skip(id)
}
