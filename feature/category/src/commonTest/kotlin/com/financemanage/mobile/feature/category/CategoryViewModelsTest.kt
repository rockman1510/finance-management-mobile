package com.financemanage.mobile.feature.category

import com.financemanage.mobile.core.data.shared.CategoryRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.keepCollecting
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.feature.category.presentation.CategoryFormViewModel
import com.financemanage.mobile.feature.category.presentation.CategoryListViewModel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val CATEGORIES =
    """[{"id":"food","name":"Ăn uống","type":"EXPENSE","parentId":null,"icon":"restaurant","code":"FOOD"},
    {"id":"cafe","name":"Cà phê","type":"EXPENSE","parentId":"food","icon":null},
    {"id":"salary","name":"Lương","type":"INCOME","parentId":null,"icon":"payments"}]"""

class CategoryViewModelsTest {
    private val backend = MockBackend().apply { get("categories", CATEGORIES) }
    private val repository = CategoryRepository(backend.client, testJsonCache())

    @Test
    fun listLoadsAndReportsADeleteRefusal() =
        runViewModelTest {
            backend.fail(HttpMethod.Delete, "categories/food", HttpStatusCode.Conflict, "CATEGORY_IN_USE")
            val viewModel = CategoryListViewModel(repository)
            keepCollecting(viewModel.state)

            viewModel.refresh()
            val loaded = viewModel.state.awaitValue { it.categories.data != null && !it.categories.loading }
            assertEquals(3, loaded.categories.data?.size)
            assertEquals(CategoryType.EXPENSE, loaded.type)

            viewModel.selectType(CategoryType.INCOME)
            viewModel.state.awaitValue { it.type == CategoryType.INCOME }

            viewModel.delete("food")
            val failed = viewModel.state.awaitValue { it.actionError != null }
            assertEquals("CATEGORY_IN_USE", (failed.actionError as AppError.Api).code)
        }

    @Test
    fun newCategoryOffersTopLevelParentsAndRequiresAName() =
        runViewModelTest {
            backend.post("categories", """{"id":"tea","name":"Trà","type":"EXPENSE","parentId":"food"}""")
            val viewModel = CategoryFormViewModel(null, CategoryType.EXPENSE, "food", repository)
            val loaded = viewModel.state.awaitValue { it.parents.isNotEmpty() }
            assertEquals(listOf("food", "salary"), loaded.parents.map { it.id })

            viewModel.save()
            assertTrue(viewModel.state.value.nameMissing)

            viewModel.onName(" Trà ")
            viewModel.save()
            viewModel.state.awaitValue { it.saved }
            val request = backend.single(HttpMethod.Post, "categories")
            assertEquals("Trà", request.bodyField("name"))
            assertEquals("EXPENSE", request.bodyField("type"))
            assertEquals("food", request.bodyField("parentId"))
        }

    @Test
    fun changingTheTypeClearsTheParent() =
        runViewModelTest {
            val viewModel = CategoryFormViewModel(null, CategoryType.EXPENSE, "food", repository)
            viewModel.onType(CategoryType.INCOME)
            assertNull(viewModel.state.value.parentId)
        }

    @Test
    fun editPrefillsAndOnlySendsNameAndIcon() =
        runViewModelTest {
            backend.patch("categories/cafe", """{"id":"cafe","name":"Cafe","type":"EXPENSE","parentId":"food"}""")
            val viewModel = CategoryFormViewModel("cafe", CategoryType.EXPENSE, null, repository)
            val loaded = viewModel.state.awaitValue { it.name.isNotEmpty() }
            assertEquals("Cà phê", loaded.name)
            assertEquals("food", loaded.parentId)
            assertTrue(loaded.parents.none { it.id == "cafe" })

            viewModel.onName("Cafe")
            viewModel.onIcon("local_cafe")
            viewModel.save()
            viewModel.state.awaitValue { it.saved }
            val request = backend.single(HttpMethod.Patch, "categories/cafe")
            assertEquals("Cafe", request.bodyField("name"))
            assertEquals("local_cafe", request.bodyField("icon"))
            assertNull(request.bodyField("parentId"))
        }
}
