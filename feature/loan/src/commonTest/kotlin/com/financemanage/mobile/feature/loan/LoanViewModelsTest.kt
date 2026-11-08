package com.financemanage.mobile.feature.loan

import com.financemanage.mobile.core.data.shared.ProfileRepositoryImpl
import com.financemanage.mobile.core.domain.usecase.GetDefaultCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.network.IDEMPOTENCY_KEY_HEADER
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.feature.loan.data.LoanRepositoryImpl
import com.financemanage.mobile.feature.loan.domain.usecase.ChangeLoanRateUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.CreateLoanUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.DeleteLoanUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.GetLoanUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.GetLoansUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.PrepayLoanUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.PreviewLoanScheduleUseCase
import com.financemanage.mobile.feature.loan.domain.usecase.RepayLoanUseCase
import com.financemanage.mobile.feature.loan.presentation.LoanAction
import com.financemanage.mobile.feature.loan.presentation.LoanDetailViewModel
import com.financemanage.mobile.feature.loan.presentation.LoanFormViewModel
import com.financemanage.mobile.feature.loan.presentation.LoanListViewModel
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SUMMARY =
    """{"id":"l1","counterpartyName":"VPBank","direction":"BORROW","type":"BANK","currency":"VND","termMonths":12,
    "startDate":"2026-11-01","repaymentMethod":"ANNUITY","status":"ACTIVE",
    "nextDueDate":"2026-12-01","overdueInstallments":1}"""

private const val INSTALLMENT =
    """{"id":"i1","periodNo":1,"dueDate":"2026-12-01","principalDue":3960265,"interestDue":120000,"totalDue":4080265,
    "principalPaid":0,"interestPaid":0,"remaining":4080265,"status":"PENDING"}"""

private const val DETAIL = """{"summary":$SUMMARY,"installments":[$INSTALLMENT],"repayments":[]}"""

class LoanViewModelsTest {
    private val backend = MockBackend()
    private val repository = LoanRepositoryImpl(backend.client)
    private val profiles = ProfileRepositoryImpl(backend.client, testJsonCache())

    private fun formViewModel() =
        LoanFormViewModel(
            PreviewLoanScheduleUseCase(repository),
            CreateLoanUseCase(repository),
            GetDefaultCurrencyUseCase(profiles),
            GetTodayUseCase(profiles),
        )

    private fun detailViewModel(id: String) =
        LoanDetailViewModel(
            id,
            GetLoanUseCase(repository),
            RepayLoanUseCase(repository),
            PrepayLoanUseCase(repository),
            ChangeLoanRateUseCase(repository),
            DeleteLoanUseCase(repository),
            GetTodayUseCase(profiles),
        )

    @Test
    fun listLoads() =
        runViewModelTest {
            backend.get("loans", "[$SUMMARY]")
            val viewModel = LoanListViewModel(GetLoansUseCase(repository))
            viewModel.refresh()
            assertEquals(
                1,
                viewModel.state
                    .awaitValue { it.data != null }
                    .data!!
                    .single()
                    .overdueInstallments,
            )
        }

    @Test
    fun formPreviewsTheScheduleAndCreates() =
        runViewModelTest {
            backend.post("loans/preview-schedule", """{"installments":[$INSTALLMENT],"currency":"VND"}""")
            backend.post("loans", DETAIL, HttpStatusCode.Created)
            val viewModel = formViewModel()
            viewModel.state.awaitValue { it.startDate != null }

            viewModel.preview()
            assertTrue(viewModel.state.value.invalid)

            viewModel.update {
                it.copy(
                    counterparty = " VPBank ",
                    principal = "48.000.000",
                    rate = "12",
                    termMonths = "12",
                )
            }
            viewModel.preview()
            assertEquals(
                1,
                viewModel.state
                    .awaitValue { it.preview != null }
                    .preview!!
                    .installments.size,
            )

            viewModel.update { it.copy(feeRate = "1,5") }
            assertNull(viewModel.state.value.preview, "a change invalidates the preview")
            viewModel.save()
            assertEquals("l1", viewModel.state.awaitValue { it.createdId != null }.createdId)
            val request = backend.single(HttpMethod.Post, "loans")
            assertEquals("VPBank", request.bodyField("counterpartyName"))
            assertEquals("48000000", request.bodyField("principal"))
            assertEquals("12", request.bodyField("annualRate"))
            assertEquals("1.5", request.bodyField("earlyRepaymentFeeRate"))
            assertEquals("ANNUITY", request.bodyField("repaymentMethod"))
        }

    @Test
    fun interestFreeLoansSendNoRate() =
        runViewModelTest {
            backend.post("loans", DETAIL, HttpStatusCode.Created)
            val viewModel = formViewModel()
            viewModel.state.awaitValue { it.startDate != null }
            viewModel.update {
                it.copy(
                    counterparty = "Anh Ba",
                    direction = "LEND",
                    principal = "5.000.000",
                    rate = "10",
                    method = "INTEREST_FREE",
                )
            }
            viewModel.save()
            viewModel.state.awaitValue { it.createdId != null }
            val request = backend.single(HttpMethod.Post, "loans")
            assertNull(request.bodyField("annualRate"))
            assertEquals("LEND", request.bodyField("direction"))
        }

    @Test
    fun repaymentReusesItsKeyOnRetryAndANewActionGetsANewKey() =
        runViewModelTest {
            backend.get("loans/l1", DETAIL)
            backend.fail(HttpMethod.Post, "loans/l1/repayments", HttpStatusCode.InternalServerError, "INTERNAL_ERROR")
            val viewModel = detailViewModel("l1")
            viewModel.refresh()
            viewModel.state.awaitValue { it.loan != null }
            val amount = BigDecimal.parseString("4080265")

            viewModel.newAction()
            viewModel.perform(LoanAction.REPAY, amount, null)
            viewModel.state.awaitValue { it.error != null && !it.busy }
            backend.post("loans/l1/repayments", """{"id":"r1","type":"REGULAR"}""", HttpStatusCode.Created)
            viewModel.perform(LoanAction.REPAY, amount, null)
            backend.await(HttpMethod.Post, "loans/l1/repayments", count = 2)
            viewModel.state.awaitValue { it.error == null && !it.busy }

            backend.post("loans/l1/prepayments", """{"id":"r2","type":"PREPAYMENT"}""", HttpStatusCode.Created)
            viewModel.newAction()
            viewModel.perform(LoanAction.PREPAY, BigDecimal.parseString("10000000"), LocalDate(2026, 11, 15))
            val prepay = backend.await(HttpMethod.Post, "loans/l1/prepayments")
            viewModel.state.awaitValue { !it.busy }

            val keys =
                backend
                    .requests(
                        HttpMethod.Post,
                        "loans/l1/repayments",
                    ).map { it.headers[IDEMPOTENCY_KEY_HEADER] }
            assertEquals(keys[0], keys[1])
            assertNotEquals(keys[1], prepay.headers[IDEMPOTENCY_KEY_HEADER])
            assertEquals("10000000", prepay.bodyField("principal"))
            assertEquals("2026-11-15", prepay.bodyField("paidOn"))
        }

    @Test
    fun rateChangeNeedsADate() =
        runViewModelTest {
            backend.post("loans/l1/rate-changes", DETAIL)
            backend.get("loans/l1", DETAIL)
            val viewModel = detailViewModel("l1")
            viewModel.perform(LoanAction.RATE, BigDecimal.parseString("9.5"), null)
            assertFalse(viewModel.state.value.busy)
            assertTrue(backend.requests.isEmpty())

            viewModel.perform(LoanAction.RATE, BigDecimal.parseString("9.5"), LocalDate(2027, 1, 1))
            val request = backend.await(HttpMethod.Post, "loans/l1/rate-changes")
            assertEquals("9.5", request.bodyField("annualRate"))
            assertEquals("2027-01-01", request.bodyField("effectiveFrom"))
            viewModel.state.awaitValue { !it.busy && it.loan != null }
        }
}
