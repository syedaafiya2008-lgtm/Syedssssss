package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeneratedIdea(
    val startupName: String,
    val tagline: String,
    val brandingNames: List<String>,
    val domains: List<String>,
    val pitch: String,
    val problem: String,
    val targetCustomers: String,
    val revenueModel: String,
    val difficulty: String,
    val estTimeToFirstCustomer: String,
    val whyPotential: String,
    val mvpDescription: String,
    val mvpCoreFeatures: List<String>,
    val mvp24h: String,
    val mvp7d: String,
    val mvp30d: String,
    val validationPlaces: List<String>,
    val validationQuestions: List<String>,
    val firstAction: String,
    val angleDifference: String,
    val angleAiImprovement: String,
    val angleTeenagerStart: String,
    val anglePayingCustomerStrat: String,
    val angleLaunchRoadmap30d: String,
    val angleRisks: String,
    val scoreProfit: Int,
    val scoreEase: Int,
    val scoreCompetition: Int,
    val scoreAi: Int,
    val scoreScalability: Int,
    val scoreOverall: Float
)

@JsonClass(generateAdapter = true)
data class GeneratedIdeaList(
    val ideas: List<GeneratedIdea>
)

@Entity(tableName = "ideas")
data class StartupIdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val querySkills: String,
    val queryInterests: String,
    val queryBudget: String,
    val queryTime: String,
    val querySoloOrTeam: String,
    
    // Core Details
    val startupName: String,
    val tagline: String,
    val brandingNamesJoined: String, // newline-joined list
    val domainsJoined: String, // newline-joined list
    val pitch: String,
    val problem: String,
    val targetCustomers: String,
    val revenueModel: String,
    val difficulty: String,
    val estTimeToFirstCustomer: String,
    val whyPotential: String,
    
    // MVP
    val mvpDescription: String,
    val mvpCoreFeaturesJoined: String, // newline-joined list
    val mvp24h: String,
    val mvp7d: String,
    val mvp30d: String,
    
    // Validation
    val validationPlacesJoined: String, // newline-joined list
    val validationQuestionsJoined: String, // newline-joined list
    val firstAction: String,
    
    // Million Dollar Angle
    val angleDifference: String,
    val angleAiImprovement: String,
    val angleTeenagerStart: String,
    val anglePayingCustomerStrat: String,
    val angleLaunchRoadmap30d: String,
    val angleRisks: String,
    
    // Scores
    val scoreProfit: Int,
    val scoreEase: Int,
    val scoreCompetition: Int,
    val scoreAi: Int,
    val scoreScalability: Int,
    val scoreOverall: Float,
    
    val isFavorite: Boolean = false
) {
    fun toGeneratedIdea(): GeneratedIdea {
        return GeneratedIdea(
            startupName = startupName,
            tagline = tagline,
            brandingNames = brandingNamesJoined.split("\n").filter { it.isNotBlank() },
            domains = domainsJoined.split("\n").filter { it.isNotBlank() },
            pitch = pitch,
            problem = problem,
            targetCustomers = targetCustomers,
            revenueModel = revenueModel,
            difficulty = difficulty,
            estTimeToFirstCustomer = estTimeToFirstCustomer,
            whyPotential = whyPotential,
            mvpDescription = mvpDescription,
            mvpCoreFeatures = mvpCoreFeaturesJoined.split("\n").filter { it.isNotBlank() },
            mvp24h = mvp24h,
            mvp7d = mvp7d,
            mvp30d = mvp30d,
            validationPlaces = validationPlacesJoined.split("\n").filter { it.isNotBlank() },
            validationQuestions = validationQuestionsJoined.split("\n").filter { it.isNotBlank() },
            firstAction = firstAction,
            angleDifference = angleDifference,
            angleAiImprovement = angleAiImprovement,
            angleTeenagerStart = angleTeenagerStart,
            anglePayingCustomerStrat = anglePayingCustomerStrat,
            angleLaunchRoadmap30d = angleLaunchRoadmap30d,
            angleRisks = angleRisks,
            scoreProfit = scoreProfit,
            scoreEase = scoreEase,
            scoreCompetition = scoreCompetition,
            scoreAi = scoreAi,
            scoreScalability = scoreScalability,
            scoreOverall = scoreOverall
        )
    }

    companion object {
        fun fromGeneratedIdea(
            idea: GeneratedIdea,
            sessionId: String,
            querySkills: String,
            queryInterests: String,
            queryBudget: String,
            queryTime: String,
            querySoloOrTeam: String
        ): StartupIdeaEntity {
            return StartupIdeaEntity(
                sessionId = sessionId,
                querySkills = querySkills,
                queryInterests = queryInterests,
                queryBudget = queryBudget,
                queryTime = queryTime,
                querySoloOrTeam = querySoloOrTeam,
                startupName = idea.startupName,
                tagline = idea.tagline,
                brandingNamesJoined = idea.brandingNames.joinToString("\n"),
                domainsJoined = idea.domains.joinToString("\n"),
                pitch = idea.pitch,
                problem = idea.problem,
                targetCustomers = idea.targetCustomers,
                revenueModel = idea.revenueModel,
                difficulty = idea.difficulty,
                estTimeToFirstCustomer = idea.estTimeToFirstCustomer,
                whyPotential = idea.whyPotential,
                mvpDescription = idea.mvpDescription,
                mvpCoreFeaturesJoined = idea.mvpCoreFeatures.joinToString("\n"),
                mvp24h = idea.mvp24h,
                mvp7d = idea.mvp7d,
                mvp30d = idea.mvp30d,
                validationPlacesJoined = idea.validationPlaces.joinToString("\n"),
                validationQuestionsJoined = idea.validationQuestions.joinToString("\n"),
                firstAction = idea.firstAction,
                angleDifference = idea.angleDifference,
                angleAiImprovement = idea.angleAiImprovement,
                angleTeenagerStart = idea.angleTeenagerStart,
                anglePayingCustomerStrat = idea.anglePayingCustomerStrat,
                angleLaunchRoadmap30d = idea.angleLaunchRoadmap30d,
                angleRisks = idea.angleRisks,
                scoreProfit = idea.scoreProfit,
                scoreEase = idea.scoreEase,
                scoreCompetition = idea.scoreCompetition,
                scoreAi = idea.scoreAi,
                scoreScalability = idea.scoreScalability,
                scoreOverall = idea.scoreOverall
            )
        }
    }
}
