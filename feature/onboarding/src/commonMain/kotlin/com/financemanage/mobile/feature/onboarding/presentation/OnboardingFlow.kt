package com.financemanage.mobile.feature.onboarding.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.CommonCurrencies
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.ui.components.AmountTextField
import com.financemanage.mobile.core.ui.components.CreatableWalletTypes
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.LoadingButton
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.currencyLabel
import com.financemanage.mobile.core.ui.components.walletTypeLabel
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_next
import com.financemanage.mobile.core.ui.resources.action_skip
import com.financemanage.mobile.core.ui.resources.field_invalid_amount
import com.financemanage.mobile.core.ui.resources.field_name
import com.financemanage.mobile.core.ui.resources.field_search
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.onboarding.resources.Res
import com.financemanage.mobile.feature.onboarding.resources.budget_amount
import com.financemanage.mobile.feature.onboarding.resources.budget_default_name
import com.financemanage.mobile.feature.onboarding.resources.budget_message
import com.financemanage.mobile.feature.onboarding.resources.budget_title
import com.financemanage.mobile.feature.onboarding.resources.checking
import com.financemanage.mobile.feature.onboarding.resources.currency_message
import com.financemanage.mobile.feature.onboarding.resources.currency_title
import com.financemanage.mobile.feature.onboarding.resources.current_balance
import com.financemanage.mobile.feature.onboarding.resources.security_message
import com.financemanage.mobile.feature.onboarding.resources.security_title
import com.financemanage.mobile.feature.onboarding.resources.step_of
import com.financemanage.mobile.feature.onboarding.resources.turn_on_now
import com.financemanage.mobile.feature.onboarding.resources.wallet_default_name
import com.financemanage.mobile.feature.onboarding.resources.wallet_message
import com.financemanage.mobile.feature.onboarding.resources.wallet_title
import com.financemanage.mobile.feature.onboarding.resources.wallet_type
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

/**
 * First-run onboarding (F03).
 *
 * @param twoFactorSetup the "enable 2FA" flow from the settings feature, provided by the app; it calls its
 *   `onFinished` argument when the user is done or cancels.
 * @param onFinished onboarding is complete (or not needed): show the main app.
 */
@Composable
fun OnboardingFlow(
    twoFactorSetup: @Composable (onFinished: () -> Unit) -> Unit,
    onFinished: () -> Unit,
) {
    val walletName = stringResource(Res.string.wallet_default_name)
    val budgetName = stringResource(Res.string.budget_default_name)
    val viewModel: OnboardingViewModel = koinViewModel { parametersOf(walletName, budgetName) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showTwoFactor by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.step) { if (state.step == OnboardingStep.DONE) onFinished() }

    if (showTwoFactor) {
        twoFactorSetup { viewModel.finish() }
        return
    }

    when (state.step) {
        OnboardingStep.CHECKING, OnboardingStep.DONE ->
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(stringResource(Res.string.checking))
            }
        else ->
            StepLayout(state.stepNumber) {
                when (state.step) {
                    OnboardingStep.CURRENCY -> CurrencyStep(state, viewModel)
                    OnboardingStep.WALLET -> WalletStep(state, viewModel)
                    OnboardingStep.BUDGET -> BudgetStep(state, viewModel)
                    OnboardingStep.SECURITY ->
                        SecurityStep(
                            onTurnOn = { showTwoFactor = true },
                            onSkip = viewModel::finish,
                        )
                    else -> Unit
                }
            }
    }
}

@Composable
private fun StepLayout(
    step: Int,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            stringResource(Res.string.step_of, step, OnboardingUiState.TOTAL_STEPS),
            style = MaterialTheme.typography.labelLarge,
        )
        LinearProgressIndicator(
            progress = { step.toFloat() / OnboardingUiState.TOTAL_STEPS },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun Header(
    title: String,
    message: String,
) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorLine(state: OnboardingUiState) {
    state.error?.let { Text(it.userMessage(), color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun CurrencyStep(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Header(stringResource(Res.string.currency_title), stringResource(Res.string.currency_message))
        FormTextField(
            value = state.currencyQuery,
            onValueChange = viewModel::onCurrencyQuery,
            label = stringResource(CoreRes.string.field_search),
        )
        val query = state.currencyQuery.trim().uppercase()
        val typed = CurrencyCode.parse(query)
        val options =
            (CommonCurrencies + listOfNotNull(typed)).distinct().filter { query.isEmpty() || it.value.contains(query) }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
            items(options, key = { it.value }) { currency ->
                ListItem(
                    headlineContent = { Text(currencyLabel(currency)) },
                    trailingContent = {
                        if (currency == state.currency) Icon(Icons.Outlined.Check, contentDescription = null)
                    },
                    modifier = Modifier.clickable { viewModel.onCurrencySelected(currency) },
                )
            }
        }
        ErrorLine(state)
        LoadingButton(
            stringResource(CoreRes.string.action_next),
            onClick = viewModel::confirmCurrency,
            loading = state.loading,
        )
    }
}

@Composable
private fun WalletStep(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Header(stringResource(Res.string.wallet_title), stringResource(Res.string.wallet_message))
        FormTextField(
            value = state.walletName,
            onValueChange = viewModel::onWalletName,
            label = stringResource(CoreRes.string.field_name),
            maxLength = 60,
        )
        OptionPicker(
            label = stringResource(Res.string.wallet_type),
            options = CreatableWalletTypes,
            selected = state.walletType,
            optionLabel = { walletTypeLabel(it) },
            onSelect = viewModel::onWalletType,
        )
        AmountTextField(
            value = state.walletBalance,
            onValueChange = viewModel::onWalletBalance,
            currency = state.currency,
            label = stringResource(Res.string.current_balance),
            allowNegative = true,
            error = if (state.amountInvalid) stringResource(CoreRes.string.field_invalid_amount) else null,
        )
        ErrorLine(state)
        LoadingButton(
            stringResource(CoreRes.string.action_next),
            onClick = viewModel::confirmWallet,
            loading = state.loading,
        )
    }
}

@Composable
private fun BudgetStep(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Header(stringResource(Res.string.budget_title), stringResource(Res.string.budget_message))
        AmountTextField(
            value = state.budgetAmount,
            onValueChange = viewModel::onBudgetAmount,
            currency = state.currency,
            label = stringResource(Res.string.budget_amount),
            error = if (state.amountInvalid) stringResource(CoreRes.string.field_invalid_amount) else null,
        )
        ErrorLine(state)
        LoadingButton(
            stringResource(CoreRes.string.action_next),
            onClick = viewModel::confirmBudget,
            loading = state.loading,
        )
        OutlinedButton(onClick = viewModel::skipBudget, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(CoreRes.string.action_skip))
        }
    }
}

@Composable
private fun SecurityStep(
    onTurnOn: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Header(stringResource(Res.string.security_title), stringResource(Res.string.security_message))
        // Both choices are equally prominent; skipping is never warned about nor asked again (F03).
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(CoreRes.string.action_skip)) }
            OutlinedButton(
                onClick = onTurnOn,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(Res.string.turn_on_now)) }
        }
    }
}
