package com.financemanage.mobile.core.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.CoroutineContext

/**
 * Runs a ViewModel test: `Dispatchers.Main` (hence `viewModelScope`) is an [UnconfinedTestDispatcher] sharing the
 * test's scheduler, and is reset afterwards.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runViewModelTest(block: suspend TestScope.() -> Unit) {
    val dispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(dispatcher)
    TestEngineDispatcher.target = dispatcher
    try {
        runTest(dispatcher) { block() }
    } finally {
        TrackedViewModels.clear()
        TestEngineDispatcher.target = null
        Dispatchers.resetMain()
    }
}

/**
 * Clears this ViewModel (cancelling its `viewModelScope`) when the [runViewModelTest] ends. Needed for ViewModels
 * that collect app-wide flows such as `PushBridge`, which would otherwise keep reacting during later tests.
 */
fun <T : ViewModel> T.tracked(): T = also { TrackedViewModels.add(it) }

private object TrackedViewModels {
    private val live = mutableListOf<ViewModel>()

    fun add(viewModel: ViewModel) {
        live += viewModel
    }

    fun clear() {
        val store = ViewModelStore()
        live.forEachIndexed { index, viewModel -> store.put("vm$index", viewModel) }
        store.clear()
        live.clear()
    }
}

/**
 * Dispatcher of the [MockBackend] engine: the running ViewModel test's dispatcher (so mocked HTTP calls run on the
 * test scheduler, never on another thread while `Dispatchers.Main` is reset), [Dispatchers.Default] otherwise.
 */
internal object TestEngineDispatcher : CoroutineDispatcher() {
    var target: CoroutineDispatcher? = null

    private val current: CoroutineDispatcher get() = target ?: Dispatchers.Default

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = current.isDispatchNeeded(context)

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) = current.dispatch(context, block)
}

/** Keeps a `WhileSubscribed` flow active for the rest of the test. */
fun TestScope.keepCollecting(flow: Flow<*>) {
    backgroundScope.launch { flow.collect {} }
}

/** Waits until the state matches [predicate] and returns it. */
suspend fun <T> StateFlow<T>.awaitValue(
    message: String = "state",
    predicate: (T) -> Boolean,
): T {
    awaitCondition(message) { predicate(value) }
    return value
}
