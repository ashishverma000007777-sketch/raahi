package `in`.raahi.app.ui.screens.aimechanic

import `in`.raahi.app.network.AiMessageDto
import `in`.raahi.app.network.toNetworkError
import `in`.raahi.app.network.toUserFriendlyMessage
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

class AiMechanicStateTest {

    @Test
    fun testInitialUiState() {
        val state = AiChatUiState()
        assertTrue(state.loading)
        assertNull(state.sessionId)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.sending)
        assertNull(state.error)
        assertNull(state.lastFailedMessage)
        assertFalse(state.inChatMode)
    }

    @Test
    fun testInChatModeTransitions() {
        var state = AiChatUiState()
        assertFalse(state.inChatMode)

        // Enter chat mode on send
        state = state.copy(inChatMode = true, sending = true)
        assertTrue(state.inChatMode)
        assertTrue(state.sending)

        // On failure, inChatMode remains true to keep diagnostic results context
        state = state.copy(sending = false, error = "Connection timed out", lastFailedMessage = "Check engine light")
        assertTrue("inChatMode must be preserved on failure", state.inChatMode)
        assertFalse("sending must be reset on failure", state.sending)
        assertEquals("Check engine light", state.lastFailedMessage)

        // Reset to describe clears inChatMode
        state = state.copy(inChatMode = false)
        assertFalse(state.inChatMode)
    }

    @Test
    fun testFailedMessagePreservation() {
        val originalQuestion = "Engine making rattling noise at 40 km/h"
        val pendingId = "pending-12345"
        val optimisticMessage = AiMessageDto(
            id = pendingId,
            role = "USER",
            content = originalQuestion,
            createdAt = "",
            unavailable = false
        )

        // Simulating sending state
        var state = AiChatUiState(
            loading = false,
            sessionId = "session-1",
            messages = listOf(optimisticMessage),
            sending = true
        )

        // Simulating failure (timeout or network error)
        val error = SocketTimeoutException("timeout")
        val friendlyMessage = error.toUserFriendlyMessage("AI Mechanic is temporarily busy.")

        state = state.copy(
            sending = false,
            error = friendlyMessage,
            lastFailedMessage = originalQuestion,
            messages = state.messages.filterNot { it.id == pendingId }
        )

        assertFalse("Sending must be reset on failure", state.sending)
        assertEquals("Connection timed out. Please check your connection and try again.", state.error)
        assertEquals("User's question must be preserved for retry", originalQuestion, state.lastFailedMessage)
        assertTrue("Optimistic uncommitted message must be removed from history", state.messages.isEmpty())
    }

    @Test
    fun testTimeoutExceptionTranslatesToActionableMessage() {
        val timeoutEx = SocketTimeoutException("Read timed out")
        val netError = timeoutEx.toNetworkError()
        assertTrue(netError is `in`.raahi.app.network.NetworkError.Timeout)
        assertEquals("Connection timed out. Please check your connection and try again.", netError.userMessage)
    }
}
