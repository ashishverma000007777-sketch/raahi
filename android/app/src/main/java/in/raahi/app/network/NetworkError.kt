package `in`.raahi.app.network

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import retrofit2.HttpException

/**
 * Centralized API & Network Error Representation for Raahi.
 *
 * Categorizes all low-level network failures (DNS, timeout, no internet, server unreachable)
 * and HTTP error responses into user-friendly domain errors.
 *
 * Raw network exceptions (like "Unable to resolve host api.raahi.in") MUST NEVER be exposed
 * directly to the UI.
 */
sealed class NetworkError(
    val userMessage: String,
    val isOfflineOrUnreachable: Boolean,
    cause: Throwable? = null,
) : Exception(userMessage, cause) {

    class DnsFailure(
        msg: String = "Unable to reach Raahi servers. You appear to be offline.",
        cause: Throwable? = null,
    ) : NetworkError(msg, isOfflineOrUnreachable = true, cause)

    class Offline(
        msg: String = "No internet connection. Please check your network.",
        cause: Throwable? = null,
    ) : NetworkError(msg, isOfflineOrUnreachable = true, cause)

    class Timeout(
        msg: String = "Connection timed out. Please check your internet and try again.",
        cause: Throwable? = null,
    ) : NetworkError(msg, isOfflineOrUnreachable = false, cause)

    class ServerUnavailable(
        val statusCode: Int? = null,
        msg: String = "Server is temporarily unavailable. Please try again later.",
        cause: Throwable? = null,
    ) : NetworkError(msg, isOfflineOrUnreachable = (statusCode == null), cause)

    class ClientError(
        val statusCode: Int,
        msg: String,
        cause: Throwable? = null,
    ) : NetworkError(msg, isOfflineOrUnreachable = false, cause)

    class Generic(
        msg: String = "Something went wrong. Please try again.",
        cause: Throwable? = null,
    ) : NetworkError(msg, isOfflineOrUnreachable = false, cause)
}

/**
 * Translates any Throwable into a clean, human-readable user message.
 * Strips technical stack traces, class names, DNS error strings, and Retrofit/OkHttp specifics.
 */
fun Throwable.toUserFriendlyMessage(defaultMessage: String = "Service temporarily unavailable. Please try again."): String {
    return this.toNetworkError(defaultMessage).userMessage
}

/**
 * Inspects a Throwable and its cause chain to categorize into a [NetworkError].
 */
fun Throwable.toNetworkError(defaultMessage: String = "Service temporarily unavailable. Please try again."): NetworkError {
    if (this is NetworkError) return this

    var current: Throwable? = this
    while (current != null) {
        when (current) {
            is UnknownHostException -> {
                return NetworkError.DnsFailure(
                    "Unable to reach Raahi servers. Please check your internet connection.",
                    this
                )
            }
            is SocketTimeoutException -> {
                return NetworkError.Timeout(
                    "Connection timed out. Please check your connection and try again.",
                    this
                )
            }
            is ConnectException, is PortUnreachableException, is NoRouteToHostException -> {
                return NetworkError.ServerUnavailable(
                    msg = "Could not connect to Raahi servers. Please check your connection.",
                    cause = this
                )
            }
            is SSLException -> {
                return NetworkError.ServerUnavailable(
                    msg = "Secure connection to server failed. Please check your network.",
                    cause = this
                )
            }
            is SocketException -> {
                return NetworkError.Offline(
                    "Network connection was interrupted. Please check your internet.",
                    this
                )
            }
            is IOException -> {
                val msg = current.message.orEmpty()
                if (msg.contains("Unable to resolve host", ignoreCase = true) ||
                    msg.contains("No address associated", ignoreCase = true)
                ) {
                    return NetworkError.DnsFailure(
                        "Internet connection check karo aur dobara try karo.",
                        this
                    )
                }
                if (msg.contains("timeout", ignoreCase = true)) {
                    return NetworkError.Timeout(
                        "Connection timed out. Internet connection check karo aur dobara try karo.",
                        this
                    )
                }
                if (msg.contains("failed to connect", ignoreCase = true) ||
                    msg.contains("connection refused", ignoreCase = true)
                ) {
                    return NetworkError.ServerUnavailable(
                        msg = "Internet connection check karo aur dobara try karo.",
                        cause = this
                    )
                }
            }
            is HttpException -> {
                val httpEx = current
                val code = httpEx.code()
                val bodyMessage = runCatching {
                    val raw = httpEx.response()?.errorBody()?.string()
                    parseErrorMessage(raw)
                }.getOrNull()

                val resolvedMsg = bodyMessage
                    ?: defaultMessage.takeIf { it.isNotBlank() && !it.startsWith("Request failed (") && !it.startsWith("Service temporarily unavailable") }

                return when (code) {
                    in 500..599 -> {
                        val finalMsg = when {
                            defaultMessage.isNotBlank() && !defaultMessage.startsWith("Request failed (") && !defaultMessage.startsWith("Service temporarily unavailable") ->
                                defaultMessage
                            bodyMessage != null && !bodyMessage.contains("DATABASE_ERROR", ignoreCase = true) && !bodyMessage.contains("Database service error", ignoreCase = true) ->
                                bodyMessage
                            else ->
                                "Service temporarily unavailable. Please try again."
                        }
                        NetworkError.ServerUnavailable(
                            statusCode = code,
                            msg = finalMsg,
                            cause = this
                        )
                    }
                    401 -> NetworkError.ClientError(code, "Session expire ho gaya. Please login again.", this)
                    403 -> NetworkError.ClientError(code, resolvedMsg ?: "Access not permitted (403).", this)
                    404 -> {
                        val finalMsg = if (resolvedMsg != null && resolvedMsg.contains("Endpoint not found", ignoreCase = true)) {
                            "Service endpoint is not available on the server (Deployment pending). Please verify backend deployment."
                        } else {
                            resolvedMsg ?: "Requested item was not found (404)."
                        }
                        NetworkError.ClientError(code, finalMsg, this)
                    }
                    429 -> NetworkError.ClientError(code, resolvedMsg ?: "Too many requests (429). Please wait a moment and try again.", this)
                    else -> NetworkError.ClientError(code, resolvedMsg ?: "Request could not be completed ($code).", this)
                }
            }
        }
        current = current.cause
    }

    // Fallback string matching on top-level message
    val rawMsg = this.message.orEmpty()
    if (rawMsg.contains("Unable to resolve host", ignoreCase = true) ||
        rawMsg.contains("No address associated", ignoreCase = true)
    ) {
        return NetworkError.DnsFailure(
            "Internet connection check karo aur dobara try karo.",
            this
        )
    }
    if (rawMsg.contains("Failed to connect to", ignoreCase = true) ||
        rawMsg.contains("Connection refused", ignoreCase = true)
    ) {
        return NetworkError.ServerUnavailable(
            msg = "Internet connection check karo aur dobara try karo.",
            cause = this
        )
    }
    if (rawMsg.contains("timeout", ignoreCase = true)) {
        return NetworkError.Timeout("Connection timed out. Please try again.", this)
    }

    val isTechnical = rawMsg.contains("Exception") ||
            rawMsg.contains("retrofit", ignoreCase = true) ||
            rawMsg.contains("okhttp", ignoreCase = true) ||
            rawMsg.contains("java.", ignoreCase = true) ||
            rawMsg.isBlank()

    return NetworkError.Generic(
        if (isTechnical) defaultMessage else rawMsg,
        this
    )
}
