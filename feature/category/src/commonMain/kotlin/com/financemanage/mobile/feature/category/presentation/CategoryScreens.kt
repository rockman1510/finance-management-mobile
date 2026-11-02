package com.financemanage.mobile.feature.category.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.ui.components.CategoryIcon
import com.financemanage.mobile.core.ui.components.CategoryIconChoices
import com.financemanage.mobile.core.ui.components.ConfirmDialog
import com.financemanage.mobile.core.ui.components.FinanceScaffold
import com.financemanage.mobile.core.ui.components.FormColumn
import com.financemanage.mobile.core.ui.components.FormTextField
import com.financemanage.mobile.core.ui.components.LoadingButton
import com.financemanage.mobile.core.ui.components.OptionPicker
import com.financemanage.mobile.core.ui.components.RefreshOnResume
import com.financemanage.mobile.core.ui.components.ResourceContent
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.action_save
import com.financemanage.mobile.core.ui.resources.delete_confirm_message
import com.financemanage.mobile.core.ui.resources.field_name
import com.financemanage.mobile.core.ui.resources.field_required
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.feature.category.resources.Res
import com.financemanage.mobile.feature.category.resources.add_category
import com.financemanage.mobile.feature.category.resources.add_subcategory
import com.financemanage.mobile.feature.category.resources.categories_title
import com.financemanage.mobile.feature.category.resources.category_type
import com.financemanage.mobile.feature.category.resources.delete_category
import com.financemanage.mobile.feature.category.resources.edit_category
import com.financemanage.mobile.feature.category.resources.error_has_children
import com.financemanage.mobile.feature.category.resources.error_in_use
import com.financemanage.mobile.feature.category.resources.error_too_deep
import com.financemanage.mobile.feature.category.resources.error_type_mismatch
import com.financemanage.mobile.feature.category.resources.expense
import com.financemanage.mobile.feature.category.resources.icon
import com.financemanage.mobile.feature.category.resources.income
import com.financemanage.mobile.feature.category.resources.no_parent
import com.financemanage.mobile.feature.category.resources.parent_category
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.financemanage.mobile.core.ui.resources.Res as CoreRes

private val CategoryErrors: Map<String, StringResource> =
    mapOf(
        "CATEGORY_IN_USE" to Res.string.error_in_use,
        "CATEGORY_HAS_CHILDREN" to Res.string.error_has_children,
        "CATEGORY_TOO_DEEP" to Res.string.error_too_deep,
        "CATEGORY_TYPE_MISMATCH" to Res.string.error_type_mismatch,
    )

@Composable
private fun ErrorLine(error: AppError?) {
    error?.let {
        Text(
            it.userMessage(CategoryErrors),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(Spacing.md),
        )
    }
}

@Composable
fun CategoryListScreen(
    onAdd: (type: CategoryType, parentId: String?) -> Unit,
    onEdit: (Category) -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Category?>(null) }
    RefreshOnResume(viewModel::refresh)
    FinanceScaffold(
        title = stringResource(Res.string.categories_title),
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { onAdd(state.type, null) }) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(Res.string.add_category))
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val tabs = listOf(CategoryType.EXPENSE to Res.string.expense, CategoryType.INCOME to Res.string.income)
            PrimaryTabRow(selectedTabIndex = tabs.indexOfFirst { it.first == state.type }.coerceAtLeast(0)) {
                tabs.forEach { (type, label) ->
                    Tab(
                        selected = state.type == type,
                        onClick = { viewModel.selectType(type) },
                        text = { Text(stringResource(label)) },
                    )
                }
            }
            ErrorLine(state.actionError)
            ResourceContent(
                resource = state.categories,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) { all ->
                val ofType = all.filter { it.type == state.type }
                val parents = ofType.filter { it.parentId == null }
                LazyColumn {
                    parents.forEach { parent ->
                        item(key = parent.id) {
                            CategoryRow(parent, indent = false, onClick = { onEdit(parent) }, onDelete = {
                                toDelete =
                                    parent
                            })
                        }
                        items(ofType.filter { it.parentId == parent.id }, key = { it.id }) { child ->
                            CategoryRow(
                                child,
                                indent = true,
                                onClick = { onEdit(child) },
                                onDelete = { toDelete = child },
                            )
                        }
                        item(key = "add-${parent.id}") {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        stringResource(Res.string.add_subcategory),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                modifier =
                                    Modifier
                                        .padding(
                                            start = Spacing.xl,
                                        ).clickable { onAdd(state.type, parent.id) },
                            )
                        }
                    }
                }
            }
        }
    }
    toDelete?.let { category ->
        ConfirmDialog(
            title = stringResource(Res.string.delete_category),
            message = "${category.name}\n${stringResource(CoreRes.string.delete_confirm_message)}",
            onConfirm = {
                toDelete = null
                viewModel.delete(category.id)
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    indent: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(category.name) },
        leadingContent = { CategoryIcon(category.code, category.icon, size = if (indent) 32.dp else 40.dp) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(Res.string.delete_category))
            }
        },
        modifier = Modifier.padding(start = if (indent) Spacing.xl else 0.dp).clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryFormScreen(
    categoryId: String?,
    type: String,
    parentId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryFormViewModel =
        koinViewModel(key = "category-$categoryId-$type-$parentId") { parametersOf(categoryId, type, parentId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    FinanceScaffold(
        title = stringResource(if (state.isEdit) Res.string.edit_category else Res.string.add_category),
        onBack = onBack,
    ) { padding ->
        FormColumn(padding) {
            FormTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = stringResource(CoreRes.string.field_name),
                maxLength = 60,
                error = if (state.nameMissing) stringResource(CoreRes.string.field_required) else null,
            )
            OptionPicker(
                label = stringResource(Res.string.category_type),
                options = listOf(CategoryType.EXPENSE, CategoryType.INCOME),
                selected = state.type,
                optionLabel = {
                    stringResource(
                        if (it ==
                            CategoryType.INCOME
                        ) {
                            Res.string.income
                        } else {
                            Res.string.expense
                        },
                    )
                },
                onSelect = viewModel::onType,
                enabled = !state.isEdit,
            )
            val parentOptions: List<Category?> = listOf(null) + state.parents.filter { it.type == state.type }
            OptionPicker(
                label = stringResource(Res.string.parent_category),
                options = parentOptions,
                selected = parentOptions.firstOrNull { it?.id == state.parentId },
                optionLabel = { it?.name ?: stringResource(Res.string.no_parent) },
                onSelect = { viewModel.onParent(it?.id) },
                enabled = !state.isEdit,
            )
            Text(stringResource(Res.string.icon), style = MaterialTheme.typography.labelLarge)
            FlowRow {
                CategoryIconChoices.keys.forEach { name ->
                    val selected = state.icon == name
                    CategoryIcon(
                        code = null,
                        icon = name,
                        modifier =
                            Modifier
                                .padding(Spacing.xs)
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ).clickable { viewModel.onIcon(name) }
                                .size(44.dp),
                    )
                }
            }
            state.error?.let { Text(it.userMessage(CategoryErrors), color = MaterialTheme.colorScheme.error) }
            LoadingButton(stringResource(CoreRes.string.action_save), onClick = viewModel::save, loading = state.saving)
        }
    }
}
