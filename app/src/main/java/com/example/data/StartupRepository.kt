package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.network.Content
import com.example.network.GeminiRequest
import com.example.network.GenerationConfig
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class StartupRepository(private val dao: StartupIdeaDao) {

    val allIdeas: Flow<List<StartupIdeaEntity>> = dao.getAllIdeasFlow()
    val favorites: Flow<List<StartupIdeaEntity>> = dao.getFavoritesFlow()

    fun getIdeasBySession(sessionId: String): Flow<List<StartupIdeaEntity>> {
        return dao.getIdeasBySessionFlow(sessionId)
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun setFavoriteByName(startupName: String, isFavorite: Boolean) {
        dao.updateFavoriteByName(startupName, isFavorite)
    }

    suspend fun deleteSession(sessionId: String) {
        dao.deleteSession(sessionId)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    /**
     * Contacts Gemini API to generate 5 startup ideas based on user inputs.
     * Parses the JSON output and stores them in Room under a new Session ID.
     * Returns the generated session ID upon success.
     */
    suspend fun generateAndSaveIdeas(
        skills: String,
        interests: String,
        budget: String,
        timeAvailable: String,
        soloOrTeam: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key is missing! Please configure GEMINI_API_KEY in the AI Studio Secrets panel.")
        }

        val prompt = """
Generate 5 unique, creative, and highly realistic startup ideas tailored precisely for an entrepreneur with:
- Skills: $skills
- Interests: $interests
- Budget available: $budget
- Time available per week: $timeAvailable
- Team composition: $soloOrTeam

For each idea, you MUST generate and fill in all of the following fields in the requested JSON structure:

1. Basic Details:
   - startupName: A catchy, modern name. Avoid boring/cliché names.
   - tagline: A brief, inspiring, highly professional tagline.
   - brandingNames: List of exactly 3 alternative brand name suggestions.
   - domains: List of exactly 3 domain name ideas, including catchy extensions and realistic availability tags (e.g. 'domainname.com (Available)', 'techname.io (Premium)', 'getname.co (Available)').
   - pitch: A striking one-line elevator pitch.
   - problem: A clear description of the specific pain point being solved.
   - targetCustomers: Who exactly are the very first niche of customers?
   - revenueModel: How it makes money (SaaS recurring subscription, commission marketplace, pay-per-use, freemium, etc.).
   - difficulty: Choose exactly "Easy", "Medium", or "Hard".
   - estTimeToFirstCustomer: Estimated physical time to secure the first paying client (e.g., "7 Days", "14 Days", "30 Days").
   - whyPotential: Explains why this specific idea has unfair potential in the current market and how it utilizes the founder's specific inputs.

2. MVP Planner:
   - mvpDescription: A detailed definition of the Minimum Viable Product.
   - mvpCoreFeatures: List of 3 to 5 core features the MVP must have to provide value.
   - mvp24h: What can be built and launched in exactly 24 hours (e.g., splash landing page, manual list).
   - mvp7d: What can be built and validated in 7 days (e.g., automation forms, cold emails).
   - mvp30d: What can be built and launched in 30 days (e.g., basic product core, payments integration).

3. Validation Section:
   - validationPlaces: List of exactly 5 specific places (physical or online forums, subreddits, Facebook groups, locations) to observe and find potential customers.
   - validationQuestions: List of exactly 5 high-signal questions to ask prospects to validate real demand before writing a line of code.
   - firstAction: The exact first high-impact, easy action to take today to get started (e.g. 'Post a question on /r/smallbusiness on Reddit').

4. Million Dollar Angle:
   - angleDifference: What makes this idea fundamentally different from existing competitors (the custom moat).
   - angleAiImprovement: How AI can be integrated to uniquely improve, automate, or scale this concept.
   - angleTeenagerStart: How a motivated teenager can launch this from their bedroom with zero budget and high hustle.
   - anglePayingCustomerStrat: First paying customer acquisition strategy (how to get user #1).
   - angleLaunchRoadmap30d: A concise, chronological 30-day roadmap for launching.
   - angleRisks: Core potential risks, blind spots, or challenges and brief mitigating strategies.

5. Scoring (Give scores out of 10):
   - scoreProfit: Profit potential (1 to 10)
   - scoreEase: Ease of building/launching (1 to 10)
   - scoreCompetition: Saturated-level inverse, i.e. 10 is low competition (blue-ocean), 1 is extremely saturated.
   - scoreAi: AI leverage/potential integration benefit (1 to 10)
   - scoreScalability: Long-term scalability potential (1 to 10)
   - scoreOverall: Display score (float) calculated based on average of the above parameters.

You MUST respond strictly with a valid JSON object matching the following structure:
{
  "ideas": [
    {
      "startupName": "...",
      "tagline": "...",
      "brandingNames": ["Alternative 1", "Alternative 2", "Alternative 3"],
      "domains": ["brandcom.com (Available)", "brandio.io (Available)", "getbrand.co (Available)"],
      "pitch": "...",
      "problem": "...",
      "targetCustomers": "...",
      "revenueModel": "...",
      "difficulty": "Easy",
      "estTimeToFirstCustomer": "14 Days",
      "whyPotential": "...",
      "mvpDescription": "...",
      "mvpCoreFeatures": ["Feature 1", "Feature 2", "Feature 3"],
      "mvp24h": "...",
      "mvp7d": "...",
      "mvp30d": "...",
      "validationPlaces": ["Place 1", "Place 2", "Place 3", "Place 4", "Place 5"],
      "validationQuestions": ["Question 1", "Question 2", "Question 3", "Question 4", "Question 5"],
      "firstAction": "...",
      "angleDifference": "...",
      "angleAiImprovement": "...",
      "angleTeenagerStart": "...",
      "anglePayingCustomerStrat": "...",
      "angleLaunchRoadmap30d": "...",
      "angleRisks": "...",
      "scoreProfit": 8,
      "scoreEase": 9,
      "scoreCompetition": 7,
      "scoreAi": 8,
      "scoreScalability": 9,
      "scoreOverall": 8.2
    }
  ]
}

Only return raw JSON context. No trailing text. No comments in JSON. Valid format ONLY.
        """.trimIndent()

        val systemInstruction = "You are a world-class startup incubation coach, tech lead, and micro-SaaS specialist. Your output is strictly compliant with the JSON structure requested."

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.8f),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        Log.d("StartupRepository", "Sending request to Gemini API...")
        val response = RetrofitClient.service.generateContent(apiKey, request)
        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Gemini returned empty response.")

        Log.d("StartupRepository", "Received raw response: $rawText")
        val cleanJson = sanitizeJson(rawText)

        val adapter = RetrofitClient.moshiInstance.adapter(GeneratedIdeaList::class.java)
        val generatedIdeaList = try {
            adapter.fromJson(cleanJson) ?: throw IllegalStateException("Failed to parse JSON result.")
        } catch (e: Exception) {
            Log.e("StartupRepository", "Moshi parsing failed. Clean JSON: $cleanJson", e)
            throw IllegalStateException("Failed to parse generated idea schemas. Error: ${e.localizedMessage}")
        }

        val sessionId = UUID.randomUUID().toString()
        val entities = generatedIdeaList.ideas.map { idea ->
            StartupIdeaEntity.fromGeneratedIdea(
                idea = idea,
                sessionId = sessionId,
                querySkills = skills,
                queryInterests = interests,
                queryBudget = budget,
                queryTime = timeAvailable,
                querySoloOrTeam = soloOrTeam
            )
        }

        if (entities.isNotEmpty()) {
            dao.insertIdeas(entities)
            Log.d("StartupRepository", "Successfully saved ${entities.size} ideas to Room.")
        }

        sessionId
    }

    private fun sanitizeJson(rawJson: String): String {
        var clean = rawJson.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}
