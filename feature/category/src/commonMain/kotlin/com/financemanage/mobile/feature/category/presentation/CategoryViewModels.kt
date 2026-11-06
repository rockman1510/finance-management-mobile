package com.financemanage.mobile.feature.category.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.usecase.CreateCategoryUseCase
import com.financemanage.mobile.core.domain.usecase.DeleteCategoryUseCase
import com.financemanage.mobile.core.domain.usecase.GetCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.RefreshCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.UpdateCategoryUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.ui.state.Resource
import com.financemanage.mobile.core.ui.state.ResourceHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryListUiState(
    val categories: Resource<List<Category>> = Resource(loading = true),
    val type: CategoryType = CategoryType.EXPENSE,
    val actionError: AppError? = null,
)

class CategoryListViewModel(
    observeCategories: ObserveCategoriesUseCase,
    refreshCategories: RefreshCategoriesUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, observeCategories()) { refreshCategories() }
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
        viewModelScope.launch { actionError.value = (deleteCategory(id) as? AppResult.Failure)?.error }
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
    getCategories: GetCategoriesUseCase,
    private val createCategory: CreateCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(
            CategoryFormUiState(isEdit = categoryId != null, type = initialType, parentId = initialParentId),
        )
    val state: StateFlow<CategoryFormUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val all = getCategories()
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
                    createCategory(form.name.trim(), form.type, form.parentId, form.icon)
                } else {
                    updateCategory(categoryId, form.name.trim(), form.icon)
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
