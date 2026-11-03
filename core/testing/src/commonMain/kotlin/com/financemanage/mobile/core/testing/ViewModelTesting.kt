package com.financemanage.mobile.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Runs a ViewModel test: `Dispatchers.Main` (hence `viewModelScope`) is an [UnconfinedTestDispatcher] sharing the
 * test's scheduler, and is reset afterwards.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runViewModelTest(block: suspend TestScope.() -> Unit) {
    val dispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(dispatcher)
    try {
        runTest(dispatcher) { block() }
    } finally {
        Dispatchers.resetMain()
    }
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
