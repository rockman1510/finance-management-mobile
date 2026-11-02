package com.financemanage.mobile.feature.category.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financemanage.mobile.core.navigation.CategoriesRoute
import com.financemanage.mobile.core.navigation.CategoryEditRoute

/** Category destinations (F06). */
fun NavGraphBuilder.categoryGraph(navController: NavController) {
    composable<CategoriesRoute> {
        CategoryListScreen(
            onAdd = {
                type,
                parentId,
                ->
                navController.navigate(CategoryEditRoute(type = type.name, parentId = parentId))
            },
            onEdit = { navController.navigate(CategoryEditRoute(id = it.id, type = it.type.name)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<CategoryEditRoute> { entry ->
        val route = entry.toRoute<CategoryEditRoute>()
        CategoryFormScreen(
            categoryId = route.id,
            type = route.type,
            parentId = route.parentId,
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
}
