package `in`.raahi.app.ui.screens.aimechanic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.AiMechanicRepository
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.network.AiMessageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import `in`.raahi.app.network.toUserFriendlyMessage

data class AiChatUiState(
    val loading: Boolean = true,
    val sessionId: String? = null,
    val messages: List<AiMessageDto> = emptyList(),
    val sending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AiMechanicViewModel @Inject constructor(
    private val repository: AiMechanicRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    init { start() }

    fun start() {
        _state.update { it.copy(loading = it.sessionId == null && it.messages.isEmpty(), error = null) }
        viewModelScope.launch {
            if (!authRepository.hasAuthToken()) {
                _state.update {
                    it.copy(
                        loading = false,
                        sessionId = null,
                        messages = emptyList(),
                        error = null,
                    )
                }
                return@launch
            }

            runCatching {
                // Reuse the most recent session if one exists, otherwise start a fresh one —
                // matches a normal "continue where you left off" chat experience.
                val existing = repository.mySessions().firstOrNull()
                val session = existing ?: repository.createSession()
                val history = if (existing != null) repository.messages(session.id) else emptyList()
                session.id to history
            }.onSuccess { (sessionId, history) ->
                _state.update { it.copy(loading = false, sessionId = sessionId, messages = history, error = null) }
            }.onFailure { e ->
                val friendly = e.toUserFriendlyMessage("AI Mechanic is currently offline. Responses will resume when connected.")
                _state.update { it.copy(loading = false, error = friendly) }
            }
        }
    }

    fun send(text: String) {
        if (text.isBlank() || !authRepository.hasAuthToken()) return
        val currentSessionId = _state.value.sessionId

        val optimisticUserMessage = AiMessageDto(
            id = "pending-${System.currentTimeMillis()}", role = "USER", content = text,
            createdAt = "", unavailable = false,
        )
        _state.update { it.copy(messages = it.messages + optimisticUserMessage, sending = true, error = null) }

        if (currentSessionId == null) {
            viewModelScope.launch {
                runCatching { repository.createSession() }
                    .onSuccess { session ->
                        _state.update { it.copy(sessionId = session.id) }
                        sendMessageInternal(session.id, text)
                    }
                    .onFailure { e ->
                        val friendly = e.toUserFriendlyMessage("AI Mechanic is currently offline. Check your internet connection.")
                        _state.update { it.copy(sending = false, error = friendly) }
                    }
            }
            return
        }

        sendMessageInternal(currentSessionId, text)
    }

    private fun sendMessageInternal(sessionId: String, text: String) {
        viewModelScope.launch {
            runCatching { repository.sendMessage(sessionId, text) }
                .onSuccess { reply -> _state.update { it.copy(sending = false, messages = it.messages + reply, error = null) } }
                .onFailure { e ->
                    val friendly = e.toUserFriendlyMessage("Message could not be sent. Check your internet connection.")
                    _state.update { it.copy(sending = false, error = friendly) }
                }
        }
    }
}
