package com.financemanage.mobile.core.data

import com.financemanage.mobile.core.data.outbox.OutboxEntity
import com.financemanage.mobile.core.data.session.SessionManager
import com.financemanage.mobile.core.data.session.SessionState
import com.financemanage.mobile.core.data.session.SignOutHook
import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.network.auth.AuthTokens
import com.financemanage.mobile.core.network.auth.SessionEvent
import com.financemanage.mobile.core.network.auth.SessionEvents
import com.financemanage.mobile.core.testing.FakeOutboxDao
import com.financemanage.mobile.core.testing.FakeTokenStore
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import com.financemanage.mobile.core.testing.apiPath
import com.financemanage.mobile.core.testing.bodyText
import com.financemanage.mobile.core.testing.mockHttpClient
import com.financemanage.mobile.core.testing.respondJson
import com.financemanage.mobile.core.testing.testJsonCache
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {
    private val tokens = AuthTokens("access", "refresh-1")

    @Test
    fun startsSignedInWhenTokensExist() =
        runTest(UnconfinedTestDispatcher()) {
            val manager = manager(FakeTokenStore(tokens), events = SessionEvents())
            assertEquals(SessionState.SIGNED_IN, manager.state.first { it != SessionState.UNKNOWN })
        }

    @Test
    fun signOutRunsHooksRevokesTheRefreshTokenAndWipesLocalData() =
        runTest(UnconfinedTestDispatcher()) {
            val tokenStore = FakeTokenStore(tokens)
            val requests = mutableListOf<HttpRequestData>()
            val cache = testJsonCache()
            val outbox = FakeOutboxDao()
            var hookCalled = false
            cache.put("wallets", String.serializer(), "[]")
            outbox.upsert(OutboxEntity("1", "transaction", "{}", "key"))
            val manager =
                SessionManager(
                    tokenStore = tokenStore,
                    httpClient =
                        mockHttpClient(tokenStore) { request ->
                            requests += request
                            respondJson("", HttpStatusCode.NoContent)
                        },
                    cache = cache,
                    outboxDao = outbox,
                    settings = AppSettingsRepository(InMemoryPreferencesDataStore()),
                    hooks = { listOf(SignOutHook { hookCalled = true }, SignOutHook { error("ignored") }) },
                    sessionEvents = SessionEvents(),
                    scope = backgroundScope,
                )

            manager.signOut()

            assertTrue(hookCalled)
            val logout = requests.single()
            assertEquals("auth/logout", logout.apiPath)
            assertTrue(logout.bodyText().contains("refresh-1"))
            assertNull(tokenStore.tokens.value)
            assertNull(cache.get("wallets", String.serializer()))
            assertTrue(outbox.rows.value.isEmpty())
            assertEquals(SessionState.SIGNED_OUT, manager.state.value)
        }

    @Test
    fun aRejectedRefreshSignsOut() =
        runTest(UnconfinedTestDispatcher()) {
            val events = SessionEvents()
            val tokenStore = FakeTokenStore(tokens)
            val manager = manager(tokenStore, events)
            assertEquals(SessionState.SIGNED_IN, manager.state.value)

            events.emit(SessionEvent.SignedOut)

            assertEquals(SessionState.SIGNED_OUT, manager.state.value)
            assertNull(tokenStore.tokens.value)
        }

    @Test
    fun signInStoresTokens() =
        runTest(UnconfinedTestDispatcher()) {
            val tokenStore = FakeTokenStore()
            val manager = manager(tokenStore, SessionEvents())
            assertEquals(SessionState.SIGNED_OUT, manager.state.value)

            manager.signIn(tokens)

            assertEquals(tokens, tokenStore.tokens.value)
            assertEquals(SessionState.SIGNED_IN, manager.state.value)
        }

    private fun kotlinx.coroutines.test.TestScope.manager(
        tokenStore: FakeTokenStore,
        events: SessionEvents,
    ) = SessionManager(
        tokenStore = tokenStore,
        httpClient = mockHttpClient(tokenStore) { respondJson("", HttpStatusCode.NoContent) },
        cache = testJsonCache(),
        outboxDao = FakeOutboxDao(),
        settings = AppSettingsRepository(InMemoryPreferencesDataStore()),
        hooks = { emptyList() },
        sessionEvents = events,
        scope = backgroundScope,
    )
}
