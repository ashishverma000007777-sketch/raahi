package `in`.raahi.app.ui.screens.aimechanic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.AiMechanicRepository
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.VehicleRepository
import `in`.raahi.app.network.AiMessageDto
import `in`.raahi.app.network.VehicleDto
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
    val activeVehicle: VehicleDto? = null,
    val lastFailedMessage: String? = null,
    val inChatMode: Boolean = false,
)

@HiltViewModel
class AiMechanicViewModel @Inject constructor(
    private val repository: AiMechanicRepository,
    private val authRepository: AuthRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    init { start() }

    fun start() {
        _state.update { it.copy(loading = it.sessionId == null && it.messages.isEmpty(), error = null) }
        viewModelScope.launch {
            // Load vehicle context if available
            runCatching {
                val vehicles = vehicleRepository.allVehicles()
                val activeId = vehicleRepository.getActiveVehicleId()
                vehicles.firstOrNull { it.id == activeId } ?: vehicles.firstOrNull() ?: vehicleRepository.myVehicle()
            }.onSuccess { vehicle ->
                _state.update { it.copy(activeVehicle = vehicle) }
            }

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
                _state.update {
                    it.copy(
                        loading = false,
                        sessionId = sessionId,
                        messages = history,
                        error = null,
                        inChatMode = history.isNotEmpty()
                    )
                }
            }.onFailure { e ->
                val friendly = e.toUserFriendlyMessage("AI Mechanic is currently offline. Responses will resume when connected.")
                _state.update { it.copy(loading = false, error = friendly) }
            }
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _state.value.sending || !authRepository.hasAuthToken()) return
        val currentSessionId = _state.value.sessionId

        val currentMessages = _state.value.messages
        val historyMessages = currentMessages.filterNot { it.id.startsWith("pending-") }

        // Contextual vehicle augmentation for Gemini completion if vehicle is registered and starting conversation
        val vehicle = _state.value.activeVehicle
        val vehicleContext = if (vehicle != null && historyMessages.isEmpty()) {
            val variantInfo = if (!vehicle.variant.isNullOrBlank()) " ${vehicle.variant}" else ""
            "\n[Context: Vehicle is ${vehicle.brand} ${vehicle.model}$variantInfo, Fuel: ${vehicle.fuelType}, Reg: ${vehicle.registrationNumber}]"
        } else ""

        val payload = trimmed + vehicleContext

        val optimisticUserMessage = AiMessageDto(
            id = "pending-${System.currentTimeMillis()}", role = "USER", content = trimmed,
            createdAt = "", unavailable = false,
        )

        val updatedMessages = if (currentMessages.none { it.id.startsWith("pending-") && it.content == trimmed }) {
            currentMessages + optimisticUserMessage
        } else {
            currentMessages
        }

        _state.update {
            it.copy(
                messages = updatedMessages,
                sending = true,
                error = null,
                lastFailedMessage = null,
                inChatMode = true
            )
        }

        if (currentSessionId == null) {
            viewModelScope.launch {
                runCatching { repository.createSession() }
                    .onSuccess { session ->
                        _state.update { it.copy(sessionId = session.id) }
                        sendMessageInternal(session.id, payload, trimmed, optimisticUserMessage.id)
                    }
                    .onFailure { e ->
                        val friendly = e.toUserFriendlyMessage("AI Mechanic is currently offline. Check your internet connection.")
                        _state.update {
                            it.copy(
                                sending = false,
                                error = friendly,
                                lastFailedMessage = trimmed,
                                messages = it.messages.filterNot { m -> m.id == optimisticUserMessage.id }
                            )
                        }
                    }
            }
            return
        }

        sendMessageInternal(currentSessionId, payload, trimmed, optimisticUserMessage.id)
    }

    private fun sendMessageInternal(sessionId: String, payload: String, userText: String, optimisticId: String) {
        viewModelScope.launch {
            runCatching { repository.sendMessage(sessionId, payload) }
                .onSuccess { reply ->
                    if (reply.unavailable) {
                        _state.update {
                            it.copy(
                                sending = false,
                                messages = it.messages + reply,
                                error = reply.content,
                                lastFailedMessage = userText
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                sending = false,
                                messages = it.messages + reply,
                                error = null,
                                lastFailedMessage = null
                            )
                        }
                    }
                }
                .onFailure { e ->
                    val friendly = e.toUserFriendlyMessage("Message could not be sent. Check your internet connection.")
                    _state.update {
                        it.copy(
                            sending = false,
                            error = friendly,
                            lastFailedMessage = userText,
                            messages = it.messages.filterNot { m -> m.id == optimisticId }
                        )
                    }
                }
        }
    }

    fun retry() {
        val failed = _state.value.lastFailedMessage
        if (!failed.isNullOrBlank() && !_state.value.sending) {
            send(failed)
        }
    }

    fun clearFailedMessage() {
        _state.update { it.copy(lastFailedMessage = null) }
    }

    fun resetToDescribe() {
        _state.update { it.copy(inChatMode = false) }
    }
}
