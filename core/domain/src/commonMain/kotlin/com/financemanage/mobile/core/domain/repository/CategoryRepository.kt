package com.financemanage.mobile.core.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import kotlinx.coroutines.flow.Flow

/** Categories (API §5.2), cached. Parents come before their children. */
interface CategoryRepository {
    val categories: Flow<Cached<List<Category>>?>

    suspend fun refresh(): AppResult<List<Category>>

    suspend fun create(
        name: String,
        type: CategoryType,
        parentId: String?,
        icon: String?,
    ): AppResult<Category>

    suspend fun update(
        id: String,
        name: String?,
        icon: String?,
    ): AppResult<Category>

    suspend fun delete(id: String): AppResult<Unit>
}
