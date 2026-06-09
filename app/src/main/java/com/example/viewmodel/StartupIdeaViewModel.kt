package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.StartupIdeaEntity
import com.example.data.StartupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistorySession(
    val sessionId: String,
    val timestamp: Long,
    val skills: String,
    val interests: String,
    val budget: String,
    val time: String,
    val soloOrTeam: String,
    val ideasCount: Int,
    val ideas: List<StartupIdeaEntity>
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    AI
}

class StartupIdeaViewModel(
    application: Application,
    private val repository: StartupRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

    val isOnboarded = MutableStateFlow(prefs.getBoolean("is_onboarded", false))
    val userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userDob = MutableStateFlow(prefs.getString("user_dob", "") ?: "")

    // Chat with Volt State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    init {
        initChatIfNeeded()
    }

    fun initChatIfNeeded() {
        if (_chatMessages.value.isEmpty()) {
            val name = userName.value.trim().ifBlank { "Founder" }
            val initialMessage = ChatMessage(
                sender = MessageSender.AI,
                text = "Hey $name! I'm **Volt**, your AI startup wingman & tech-lead co-founder. ⚡\n\nWhether you need an MVP design, growth strategies, or want to critique a wild micro-SaaS concept, I'm ready. What are we incubating today?"
            )
            _chatMessages.value = listOf(initialMessage)
        }
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        // 1. Add user message
        val userMsg = ChatMessage(sender = MessageSender.USER, text = trimmed)
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(userMsg)
        _chatMessages.value = currentList

        // Calculate user message count to check for first-time interaction
        val userMessageCount = currentList.count { it.sender == MessageSender.USER }
        val isFirstInteraction = userMessageCount <= 1

        // 2. Launch coroutine to get AI response
        viewModelScope.launch {
            _isChatLoading.value = true
            try {
                // Build simple dialogue flow from recent history
                val contextBuilder = StringBuilder()
                contextBuilder.append("You are Volt, an approachable, friendly, and ultra-smart AI co-founder & startup coach. ⚡\n")
                contextBuilder.append("Address the user as ${userName.value.ifBlank { "Founder" }}.\n\n")

                contextBuilder.append("CRITICAL SPEED & STYLE RULES:\n")
                contextBuilder.append("- Respond in less than 5-8 sentences. Keep answers on-point, extremely direct, and concise (delivered in under 10 seconds).\n")
                contextBuilder.append("- Be highly adaptive to the user's mood: if they are curious, match their excitement; if they just want to chit-chat, respond warmly and play along.\n")
                contextBuilder.append("- Utilize clean, bold formatting (**bold tags**) for emphasis. Avoid long walls of text.\n\n")

                if (isFirstInteraction) {
                    contextBuilder.append("FIRST INTERACTION CONTEXT CHECK:\n")
                    contextBuilder.append("- If the user asks about their personal interests, skills, or who they are, you MUST start formatted like: 'I don't know what to say yet since this is our very first interaction!' or 'I don't know you yet! Since this is our first chat, tell me a bit about...' and warmly invite them to share their passions.\n\n")
                } else {
                    contextBuilder.append("DEVELOPING RELATIONSHIP CONTEXT:\n")
                    contextBuilder.append("- Reference and recall earlier details (topics, interests, questions, or ideas they brought up) to maintain continuity and provide hyper-personalized feedback.\n\n")
                }

                contextBuilder.append("Conversation History:\n")
                // Get last 12 messages for quick context window
                val conversationHistory = currentList.takeLast(12)
                for (msg in conversationHistory) {
                    val prefix = if (msg.sender == MessageSender.USER) "Founder" else "Volt"
                    contextBuilder.append("$prefix: ${msg.text}\n")
                }
                contextBuilder.append("Volt: ")

                val prompt = contextBuilder.toString()
                
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("Gemini API key is not configured. Please enter your API key in the AI Studio Secrets panel.")
                }

                val request = com.example.network.GeminiRequest(
                    contents = listOf(com.example.network.Content(parts = listOf(com.example.network.Part(text = prompt)))),
                    generationConfig = com.example.network.GenerationConfig(responseMimeType = "text/plain", temperature = 0.8f),
                    systemInstruction = com.example.network.Content(parts = listOf(com.example.network.Part(text = "You are Volt, a friendly, ultra-fast tech startup co-founder companion who can chit-chat, remembers past details, and gives clever, compact feedback.")))
                )

                val response = com.example.network.RetrofitClient.service.generateContent(apiKey, request)
                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Volt did not receive any content from the system. Try again!"
                
                val updatedWithAi = _chatMessages.value.toMutableList()
                updatedWithAi.add(ChatMessage(sender = MessageSender.AI, text = aiText))
                _chatMessages.value = updatedWithAi
            } catch (e: Exception) {
                val updatedWithError = _chatMessages.value.toMutableList()
                updatedWithError.add(ChatMessage(sender = MessageSender.AI, text = "⚠️ Connection error: ${e.localizedMessage ?: "Please verify your network/API key config."}"))
                _chatMessages.value = updatedWithError
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
        initChatIfNeeded()
    }

    // Controls whether we are currently showing the greeting splash/gate screen vs the main ideation interface
    private val _showGreetingGate = MutableStateFlow(prefs.getBoolean("is_onboarded", false))
    val showGreetingGate: StateFlow<Boolean> = _showGreetingGate.asStateFlow()

    fun completeOnboarding(name: String, email: String, dob: String) {
        prefs.edit()
            .putBoolean("is_onboarded", true)
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("user_dob", dob)
            .apply()

        userName.value = name
        userEmail.value = email
        userDob.value = dob
        isOnboarded.value = true
        _showGreetingGate.value = true // Show greeting right after onboarding
    }

    fun dismissGreetingGate() {
        _showGreetingGate.value = false
    }

    fun logoutOrResetProfile() {
        prefs.edit().clear().apply()
        userName.value = ""
        userEmail.value = ""
        userDob.value = ""
        isOnboarded.value = false
        _showGreetingGate.value = false
    }

    // Bottom Navigation tab: 0 = Generate, 1 = History, 2 = Favorites
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Form inputs state
    val skills = MutableStateFlow("")
    val interests = MutableStateFlow("")
    val budget = MutableStateFlow("₹5,000–₹50,000")
    val timeAvailable = MutableStateFlow("15 hours/week")
    val soloOrTeam = MutableStateFlow("Solo Founder")

    // UI Loading / Error / Display states
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    // Screen selection details
    private val _selectedIdea = MutableStateFlow<StartupIdeaEntity?>(null)
    val selectedIdea: StateFlow<StartupIdeaEntity?> = _selectedIdea.asStateFlow()

    // Reactive Favorites List
    val favorites: StateFlow<List<StartupIdeaEntity>> = repository.favorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive History batches grouped in-memory
    val historySessions: StateFlow<List<HistorySession>> = repository.allIdeas
        .map { ideas ->
            ideas.groupBy { it.sessionId }
                .map { (sessionId, sessionIdeas) ->
                    val first = sessionIdeas.first()
                    HistorySession(
                        sessionId = sessionId,
                        timestamp = first.timestamp,
                        skills = first.querySkills,
                        interests = first.queryInterests,
                        budget = first.queryBudget,
                        time = first.queryTime,
                        soloOrTeam = first.querySoloOrTeam,
                        ideasCount = sessionIdeas.size,
                        ideas = sessionIdeas
                    )
                }
                .sortedByDescending { it.timestamp }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dynamic stream of ideas corresponding to the currently active generation session
    val activeSessionIdeas: StateFlow<List<StartupIdeaEntity>> = combine(
        repository.allIdeas,
        _activeSessionId
    ) { ideas, sessionId ->
        if (sessionId == null) emptyList()
        else ideas.filter { it.sessionId == sessionId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun changeTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        // Reset specific view detail elements when switching tabs
        if (tabIndex != 0) {
            _selectedIdea.value = null
        }
    }

    fun selectHistorySession(sessionId: String) {
        _activeSessionId.value = sessionId
        _selectedTab.value = 0 // Go back to generation page to display ideas
        _selectedIdea.value = null // Close detail card
    }

    fun selectIdeaDetail(idea: StartupIdeaEntity?) {
        _selectedIdea.value = idea
    }

    fun clearError() {
        _generationError.value = null
    }

    fun toggleFavorite(idea: StartupIdeaEntity) {
        viewModelScope.launch {
            val newStatus = !idea.isFavorite
            repository.setFavorite(idea.id, newStatus)
            
            // Sync active details representation if open
            if (_selectedIdea.value?.id == idea.id) {
                _selectedIdea.value = _selectedIdea.value?.copy(isFavorite = newStatus)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _activeSessionId.value = null
            _selectedIdea.value = null
        }
    }

    fun generateIdeas() {
        if (skills.value.isBlank() || interests.value.isBlank()) {
            _generationError.value = "Please complete skills and interests fields to begin ideating!"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            _activeSessionId.value = null
            _selectedIdea.value = null
            try {
                val newSessionId = repository.generateAndSaveIdeas(
                    skills = skills.value.trim(),
                    interests = interests.value.trim(),
                    budget = budget.value,
                    timeAvailable = timeAvailable.value,
                    soloOrTeam = soloOrTeam.value
                )
                _activeSessionId.value = newSessionId
            } catch (e: Exception) {
                _generationError.value = e.localizedMessage ?: "Connection error. Please verify network or key configuration."
            } finally {
                _isGenerating.value = false
            }
        }
    }
}

class StartupIdeaViewModelFactory(
    private val application: Application,
    private val repository: StartupRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartupIdeaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StartupIdeaViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
