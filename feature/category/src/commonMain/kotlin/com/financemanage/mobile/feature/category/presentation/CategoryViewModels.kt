package com.financemanage.mobile.feature.category.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.cache.Resource
import com.financemanage.mobile.core.data.cache.ResourceHolder
import com.financemanage.mobile.core.data.shared.CategoryRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryListUiState(
    val categories: Resource<List<Category>> = Resource(loading = true),
    val type: CategoryType = CategoryType.EXPENSE,
    val actionError: AppError? = null,
)

class CategoryListViewModel(
    private val repository: CategoryRepository,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, repository.categories) { repository.refresh() }
    private val type = MutableStateFlow(CategoryType.EXPENSE)
    private val actionError = MutableStateFlow<AppError?>(null)

    val state: StateFlow<CategoryListUiState> =
        combine(holder.state, type, actionError) { categories, selectedType, error ->
            CategoryListUiState(categories, selectedType, error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), CategoryListUiState())

    fun refresh() = holder.refresh()

    fun selectType(value: CategoryType) {
        type.value = value
    }

    fun delete(id: String) {
        viewModelScope.launch { actionError.value = (repository.delete(id) as? AppResult.Failure)?.error }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}

data class CategoryFormUiState(
    val isEdit: Boolean = false,
    val name: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val parentId: String? = null,
    val icon: String? = null,
    val parents: List<Category> = emptyList(),
    val nameMissing: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
)

class CategoryFormViewModel(
    private val categoryId: String?,
    initialType: CategoryType,
    initialParentId: String?,
    private val repository: CategoryRepository,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(
            CategoryFormUiState(isEdit = categoryId != null, type = initialType, parentId = initialParentId),
        )
    val state: StateFlow<CategoryFormUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val all =
                repository.categories.first()?.value ?: (repository.refresh() as? AppResult.Success)?.value.orEmpty()
            val existing = all.firstOrNull { it.id == categoryId }
            mutableState.update {
                it.copy(
                    parents = all.filter { c -> c.parentId == null && c.id != categoryId },
                    name = existing?.name ?: it.name,
                    type = existing?.type ?: it.type,
                    parentId = existing?.parentId ?: it.parentId,
                    icon = existing?.icon ?: it.icon,
                )
            }
        }
    }

    fun onName(value: String) = mutableState.update { it.copy(name = value, nameMissing = false, error = null) }

    fun onType(value: CategoryType) = mutableState.update { it.copy(type = value, parentId = null) }

    fun onParent(value: String?) = mutableState.update { it.copy(parentId = value) }

    fun onIcon(value: String) = mutableState.update { it.copy(icon = value) }

    fun save() {
        val form = mutableState.value
        if (form.name.isBlank()) {
            mutableState.update { it.copy(nameMissing = true) }
            return
        }
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result =
                if (categoryId == null) {
                    repository.create(form.name.trim(), form.type, form.parentId, form.icon)
                } else {
                    repository.update(categoryId, form.name.trim(), form.icon)
                }
            mutableState.update {
                it.copy(
                    saving = false,
                    saved = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }
}
