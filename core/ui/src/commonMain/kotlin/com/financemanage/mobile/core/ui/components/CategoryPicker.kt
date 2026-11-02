package com.financemanage.mobile.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.category_recent
import com.financemanage.mobile.core.ui.resources.field_search
import com.financemanage.mobile.core.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Read-only field showing the chosen category; tapping it opens [CategoryPickerSheet]. */
@Composable
fun CategoryField(
    label: String,
    selected: Category?,
    all: List<Category>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.let { categoryLabel(it, all) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = selected?.let { { CategoryIcon(it.code, it.icon, size = 28.dp) } },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

/**
 * Shared category picker (F06): icon grid, "Recent" first, search. Shows only categories of [type]; children are
 * listed right after their parent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<Category>,
    type: CategoryType,
    recentIds: List<String>,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val ofType = categories.filter { it.type == type }
    val ordered =
        ofType.filter { it.parentId == null }.flatMap { parent ->
            listOf(parent) +
                ofType.filter { it.parentId == parent.id }
        } +
            ofType.filter { child -> child.parentId != null && ofType.none { it.id == child.parentId } }
    val filtered =
        if (query.isBlank()) {
            ordered
        } else {
            ordered.filter {
                it.name.contains(
                    query.trim(),
                    ignoreCase = true,
                )
            }
        }
    val recent = if (query.isBlank()) recentIds.mapNotNull { id -> ofType.firstOrNull { it.id == id } } else emptyList()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = Spacing.md)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(Res.string.field_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (recent.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(stringResource(Res.string.category_recent), style = MaterialTheme.typography.labelLarge)
                    }
                    items(recent, key = { "recent-" + it.id }) { CategoryCell(it) { onSelect(it) } }
                    item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.padding(Spacing.xs)) }
                }
                items(filtered, key = { it.id }) { CategoryCell(it) { onSelect(it) } }
            }
        }
    }
}

@Composable
private fun CategoryCell(
    category: Category,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        CategoryIcon(category.code, category.icon, size = if (category.parentId == null) 44.dp else 36.dp)
        Text(
            category.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
