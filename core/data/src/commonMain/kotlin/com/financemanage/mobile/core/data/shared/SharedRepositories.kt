package com.financemanage.mobile.core.data.shared

import com.financemanage.mobile.core.data.cache.Cached
import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.model.domain.ExchangeRate
import com.financemanage.mobile.core.model.domain.Profile
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.map
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer

internal inline fun <reified T> io.ktor.client.request.HttpRequestBuilder.json(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

/** The signed-in user's profile (`/me`), cached. */
class ProfileRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
) {
    val profile: Flow<Cached<Profile>?> =
        cache.observe(KEY, ProfileDto.serializer()).map { it?.let { c -> Cached(c.value.toDomain(), c.updatedAt) } }

    suspend fun current(): Profile? = cache.get(KEY, ProfileDto.serializer())?.value?.toDomain()

    suspend fun refresh(): AppResult<Profile> =
        apiCall { client.get(api("me")).body<ProfileDto>() }
            .onSuccess { cache.put(KEY, ProfileDto.serializer(), it) }
            .map { it.toDomain() }

    suspend fun update(request: UpdateProfileRequest): AppResult<Profile> =
        apiCall { client.patch(api("me")) { json(request) }.body<ProfileDto>() }
            .onSuccess { cache.put(KEY, ProfileDto.serializer(), it) }
            .map { it.toDomain() }

    private companion object {
        const val KEY = "me"
    }
}

/** Wallets (API §5.1). The full list (archived included) is cached; screens filter it. */
class WalletRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
) {
    private val listSerializer = ListSerializer(WalletDto.serializer())

    val wallets: Flow<Cached<List<Wallet>>?> =
        cache.observe(KEY, listSerializer).map { cached ->
            cached?.let { Cached(it.value.map(WalletDto::toDomain), it.updatedAt) }
        }

    suspend fun refresh(): AppResult<List<Wallet>> =
        apiCall { client.get(api("wallets")) { parameter("includeArchived", true) }.body<List<WalletDto>>() }
            .onSuccess { cache.put(KEY, listSerializer, it) }
            .map { list -> list.map(WalletDto::toDomain) }

    suspend fun get(id: String): AppResult<Wallet> =
        apiCall { client.get(api("wallets/$id")).body<WalletDto>() }.map(WalletDto::toDomain)

    suspend fun create(request: CreateWalletRequest): AppResult<Wallet> =
        apiCall { client.post(api("wallets")) { json(request) }.body<WalletDto>() }
            .onSuccess { refresh() }
            .map(WalletDto::toDomain)

    suspend fun update(
        id: String,
        request: UpdateWalletRequest,
    ): AppResult<Wallet> =
        apiCall { client.patch(api("wallets/$id")) { json(request) }.body<WalletDto>() }
            .onSuccess { refresh() }
            .map(WalletDto::toDomain)

    suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("wallets/$id"))
            Unit
        }.onSuccess { refresh() }

    suspend fun adjustBalance(
        id: String,
        request: AdjustBalanceRequest,
    ): AppResult<Unit> =
        apiCall {
            client.post(api("wallets/$id/adjust-balance")) { json(request) }
            Unit
        }.onSuccess { refresh() }

    suspend fun total(currency: CurrencyCode): AppResult<WalletTotal> =
        apiCall {
            client.get(api("wallets/total")) { parameter("currency", currency.value) }.body<WalletTotalDto>()
        }.map { dto -> WalletTotal(money(dto.total, currencyOf(dto.currency ?: currency.value)), dto.missingRates) }

    private companion object {
        const val KEY = "wallets"
    }
}

/** Categories (API §5.2), cached. Parents come before their children. */
class CategoryRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
) {
    private val listSerializer = ListSerializer(CategoryDto.serializer())

    val categories: Flow<Cached<List<Category>>?> =
        cache.observe(KEY, listSerializer).map { cached ->
            cached?.let { Cached(it.value.map(CategoryDto::toDomain), it.updatedAt) }
        }

    suspend fun refresh(): AppResult<List<Category>> =
        apiCall { client.get(api("categories")).body<List<CategoryDto>>() }
            .onSuccess { cache.put(KEY, listSerializer, it) }
            .map { list -> list.map(CategoryDto::toDomain) }

    suspend fun create(
        name: String,
        type: CategoryType,
        parentId: String?,
        icon: String?,
    ): AppResult<Category> =
        apiCall {
            client
                .post(
                    api("categories"),
                ) { json(CreateCategoryRequest(name, type, parentId, icon)) }
                .body<CategoryDto>()
        }.onSuccess { refresh() }.map(CategoryDto::toDomain)

    suspend fun update(
        id: String,
        name: String?,
        icon: String?,
    ): AppResult<Category> =
        apiCall { client.patch(api("categories/$id")) { json(UpdateCategoryRequest(name, icon)) }.body<CategoryDto>() }
            .onSuccess { refresh() }
            .map(CategoryDto::toDomain)

    suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("categories/$id"))
            Unit
        }.onSuccess { refresh() }

    private companion object {
        const val KEY = "categories"
    }
}

/** Exchange rates and conversion (API §6). */
class ExchangeRateRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
) {
    private val listSerializer = ListSerializer(ExchangeRateDto.serializer())

    val rates: Flow<Cached<List<ExchangeRate>>?> =
        cache.observe(KEY, listSerializer).map { cached ->
            cached?.let { Cached(it.value.map(ExchangeRateDto::toDomain), it.updatedAt) }
        }

    suspend fun refresh(): AppResult<List<ExchangeRate>> =
        apiCall { client.get(api("exchange-rates")).body<List<ExchangeRateDto>>() }
            .onSuccess { cache.put(KEY, listSerializer, it) }
            .map { list -> list.map(ExchangeRateDto::toDomain) }

    suspend fun save(
        base: CurrencyCode,
        quote: CurrencyCode,
        rate: BigDecimal,
        effectiveDate: LocalDate?,
    ): AppResult<Unit> =
        apiCall {
            client.post(api("exchange-rates")) {
                json(SaveExchangeRateRequest(base.value, quote.value, rate, effectiveDate))
            }
            Unit
        }.onSuccess { refresh() }

    suspend fun delete(id: String): AppResult<Unit> =
        apiCall {
            client.delete(api("exchange-rates/$id"))
            Unit
        }.onSuccess { refresh() }

    suspend fun convert(
        amount: BigDecimal,
        from: CurrencyCode,
        to: CurrencyCode,
        date: LocalDate?,
    ): AppResult<ConversionDto> =
        apiCall {
            client
                .get(api("exchange-rates/convert")) {
                    parameter("amount", amount.toStringExpanded())
                    parameter("from", from.value)
                    parameter("to", to.value)
                    date?.let { parameter("date", it.toString()) }
                }.body<ConversionDto>()
        }

    private companion object {
        const val KEY = "exchange-rates"
    }
}
