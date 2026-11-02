@file:Suppress("MaxLineLength")

package com.financemanage.mobile.core.testing

/** Payloads captured from the real backend (docs/MOBILE_API_GUIDE.en.md). */
object SampleJson {
    const val TOKEN_PAIR =
        """{"accessToken":"eyJhbGciOiJIUzI1NiJ9.test","refreshToken":"4XBcRuJiTY30RjWSKpZBRqlFck33YbYRBtCIe-0KBF4","expiresInSeconds":900}"""

    const val LOGIN_TWO_FACTOR_REQUIRED =
        """{"accessToken":null,"refreshToken":null,"expiresInSeconds":null,"twoFactorRequired":true,"challengeToken":"challenge-1","challengeExpiresInSeconds":300}"""

    const val ME =
        """{"id":"b068e29e-15ad-441a-bd70-a64e74d262a0","email":"doc9074@example.com","fullName":"Nguyễn Văn A",
        "emailVerified":false,"hasPassword":true,"googleLinked":false,"twoFactorEnabled":false,
        "defaultCurrency":"VND","locale":"vi","timeZone":"Asia/Ho_Chi_Minh","monthStartDay":1,
        "createdAt":"2026-11-01T10:15:07.400614Z"}"""

    const val WALLET =
        """{"id":"1e1a4d7a-b041-43d7-bea8-542d2b4eaeee","name":"Tiền mặt","type":"CASH","currency":"VND","initialBalance":5000000,
        "balance":5000000,"excludedFromTotal":false,"archived":false,"icon":null,"creditLimit":null,"availableCredit":null,
        "statementDay":null,"paymentDueDay":null}"""

    const val VALIDATION_ERROR =
        """{"code":"VALIDATION_FAILED","message":"Request validation failed",
        "fieldErrors":{"name":"must not be blank","type":"must not be null"},"timestamp":"2026-11-01T10:15:07.798400582Z"}"""
}
