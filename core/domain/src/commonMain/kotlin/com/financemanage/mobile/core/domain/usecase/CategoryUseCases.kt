package com.financemanage.mobile.core.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.domain.repository.CategoryRepository
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ObserveCategoriesUseCase(
    private val repository: CategoryRepository,
) {
    operator fun invoke(): Flow<Cached<List<Category>>?> = repository.categories
}

/** The cached categories right now (empty when nothing is cached). */
class GetCachedCategoriesUseCase(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(): List<Category> =
        repository.categories
            .first()
            ?.value
            .orEmpty()
}

class RefreshCategoriesUseCase(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(): AppResult<List<Category>> = repository.refresh()
}

class CreateCategoryUseCase(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(
        name: String,
        type: CategoryType,
        parentId: String?,
        icon: String?,
    ): AppResult<Category> = repository.create(name, type, parentId, icon)
}

class UpdateCategoryUseCase(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(
        id: String,
        name: String?,
        icon: String?,
    ): AppResult<Category> = repository.update(id, name, icon)
}

class DeleteCategoryUseCase(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}

/** The cached categories, or the server's when nothing is cached yet. */
class GetCategoriesUseCase(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(): List<Category> =
        repository.categories.first()?.value ?: (repository.refresh() as? AppResult.Success)?.value.orEmpty()
}
