package `in`.raahi.app.data

import `in`.raahi.app.network.ApiEnvelope
import `in`.raahi.app.network.parseErrorMessage
import `in`.raahi.app.network.toNetworkError
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ApiCallErrorTest {

    @Test
    fun testParseApiEnvelopeError() {
        val json = """{"success":false,"data":null,"error":{"code":"EMAIL_NOT_VERIFIED","message":"Email must be verified before submitting helper application"}}"""
        val message = parseErrorMessage(json)
        assertEquals("Email must be verified before submitting helper application", message)

        val responseBody = json.toResponseBody("application/json".toMediaType())
        val response = Response.error<Any>(400, responseBody)
        val httpEx = HttpException(response)

        val netError = httpEx.toNetworkError(message ?: "Request failed (${httpEx.code()})")
        assertEquals("Email must be verified before submitting helper application", netError.userMessage)
    }

    @Test
    fun testParseSpringDefaultError() {
        val json = """{"timestamp":"2026-10-13T20:14:59.000+00:00","status":400,"error":"Bad Request","message":"Required request parameter 'email' is not present","path":"/api/v1/helper/apply"}"""
        val message = parseErrorMessage(json)
        assertEquals("Required request parameter 'email' is not present", message)

        val responseBody = json.toResponseBody("application/json".toMediaType())
        val response = Response.error<Any>(400, responseBody)
        val httpEx = HttpException(response)

        val netError = httpEx.toNetworkError(message ?: "Request failed (${httpEx.code()})")
        assertEquals("Required request parameter 'email' is not present", netError.userMessage)
    }

    @Test
    fun testParseValidationError() {
        val json = """{"success":false,"data":null,"error":{"code":"INVALID_VEHICLE_REG","message":"Enter a valid vehicle registration number"}}"""
        val message = parseErrorMessage(json)
        assertEquals("Enter a valid vehicle registration number", message)

        val responseBody = json.toResponseBody("application/json".toMediaType())
        val response = Response.error<Any>(400, responseBody)
        val httpEx = HttpException(response)

        val netError = httpEx.toNetworkError(message ?: "Request failed (${httpEx.code()})")
        assertEquals("Enter a valid vehicle registration number", netError.userMessage)
    }

    @Test
    fun testParseEndpointNotFound404() {
        val json = """{"success":false,"data":null,"error":{"code":"NOT_FOUND","message":"Endpoint not found: api/v1/intercity/trips/agent"}}"""
        val message = parseErrorMessage(json)
        assertEquals("Endpoint not found: api/v1/intercity/trips/agent", message)

        val responseBody = json.toResponseBody("application/json".toMediaType())
        val response = Response.error<Any>(404, responseBody)
        val httpEx = HttpException(response)

        val netError = httpEx.toNetworkError(message ?: "Request failed (${httpEx.code()})")
        assertTrue(netError.userMessage.contains("Deployment pending"))
    }

    @Test
    fun testParseEntityNotFound404() {
        val json = """{"success":false,"data":null,"error":{"code":"NOT_FOUND","message":"Intercity trip not found"}}"""
        val message = parseErrorMessage(json)
        assertEquals("Intercity trip not found", message)

        val responseBody = json.toResponseBody("application/json".toMediaType())
        val response = Response.error<Any>(404, responseBody)
        val httpEx = HttpException(response)

        val netError = httpEx.toNetworkError(message ?: "Request failed (${httpEx.code()})")
        assertEquals("Intercity trip not found", netError.userMessage)
    }
}
