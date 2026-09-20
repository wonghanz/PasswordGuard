package dev.capriguard.passwordguard.audit

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.capriguard.passwordguard.core.AiClient
import dev.capriguard.passwordguard.core.AiConfig
import dev.capriguard.passwordguard.core.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the thing under examination for as long as the screen is in the
 * foreground, and for no longer.
 *
 * [wipe] is called when the activity stops. The value is never written to
 * DataStore, to a file, to a log, or to the network — including by the optional
 * coaching pass, which is built from [aiBrief] and so carries structure only.
 */
class PasswordGuardViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)
    private val ai = AiClient()

    var config by mutableStateOf(
        AiConfig(SettingsRepository.DEFAULT_BASE_URL, "", SettingsRepository.DEFAULT_MODEL, false),
    )
        private set

    init {
        viewModelScope.launch { settings.config.collect { config = it } }
    }

    fun setBaseUrl(v: String) { viewModelScope.launch { settings.setBaseUrl(v) } }
    fun setApiKey(v: String) { viewModelScope.launch { settings.setApiKey(v) } }
    fun setModel(v: String) { viewModelScope.launch { settings.setModel(v) } }
    fun setCoachOn(v: Boolean) { viewModelScope.launch { settings.setVisionOn(v) } }

    var mode by mutableStateOf(Mode.Passphrase)
        private set
    var input by mutableStateOf("")
        private set
    var contextRaw by mutableStateOf("")
        private set
    var revealed by mutableStateOf(false)
        private set
    var audit by mutableStateOf<Audit?>(null)
        private set
    var narrative by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var busyRemote by mutableStateOf(false)
        private set

    private val contextWords: List<String>
        get() = contextRaw.split(',', '\n', ';').map { it.trim() }.filter { it.length >= 2 }

    private fun recompute() {
        error = null
        narrative = null
        if (input.isEmpty()) { audit = null; return }
        audit = when (mode) {
            Mode.Passphrase -> Passphrase.analyze(input, contextWords)
            Mode.Email -> EmailShape.analyze(input, contextWords)
        }
    }

    fun pickMode(m: Mode) {
        if (m == mode) return
        mode = m
        recompute()
    }

    fun typeInput(v: String) {
        input = v
        recompute()
    }

    fun setContextWords(v: String) {
        contextRaw = v
        recompute()
    }

    fun toggleReveal() { revealed = !revealed }

    fun clearInput() {
        input = ""
        audit = null
        narrative = null
        error = null
        revealed = false
    }

    /** Called when the activity stops: nothing typed survives a trip to the background. */
    fun wipe() {
        input = ""
        contextRaw = ""
        audit = null
        narrative = null
        revealed = false
    }

    /**
     * Optional pass through the user's own endpoint. The body is [aiBrief], which
     * contains counts, finding keys and deltas — never the input and never a
     * fragment of it.
     */
    fun askCoach() {
        val a = audit ?: return
        viewModelScope.launch {
            busyRemote = true
            error = null
            runCatching {
                val cfg = settings.current()
                if (!cfg.usable) throw IllegalArgumentException("Add a base URL, model and key in Settings first.")
                withContext(Dispatchers.IO) {
                    ai.complete(
                        endpoint = cfg.endpoint,
                        apiKey = cfg.apiKey,
                        model = cfg.model,
                        prompt = buildPrompt(a),
                        jpegBase64 = null,
                    )
                }
            }.onSuccess { narrative = it.trim() }
                .onFailure { error = it.message ?: "The request failed." }
            busyRemote = false
        }
    }

    private fun buildPrompt(a: Audit): String = buildString {
        appendLine("You are the optional plain-language coach inside PasswordGuard, an open-source Android app")
        appendLine("that judges passwords and email addresses entirely on the device.")
        appendLine("The user's secret is NOT included below and you must not ask for it or guess at it.")
        appendLine("What follows is the structural reading this app computed locally.")
        appendLine()
        appendLine(aiBrief(a))
        appendLine()
        appendLine("Task:")
        if (a.mode == Mode.Passphrase) {
            appendLine("- In three or four plain sentences, explain which one of those findings is doing the most damage.")
            appendLine("- Suggest a way to rebuild this as something memorable and longer, without proposing any specific secret.")
        } else {
            appendLine("- In three or four plain sentences, explain what this address reveals and which behaviour of the provider is most likely to surprise the user.")
            appendLine("- Suggest a structural change to the address, not a specific address.")
        }
        appendLine("Rules:")
        appendLine("- No percentage, no time-to-crack figure, no score. Bands only, as given above.")
        appendLine("- Do not invent findings that are not in the list.")
        appendLine("- If the reading is inconclusive, say so and stop.")
        appendLine("- Format: 3 to 5 short bullet lines, no headings.")
    }
}
