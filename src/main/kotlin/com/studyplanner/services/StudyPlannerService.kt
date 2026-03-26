package com.studyplanner.services

import com.studyplanner.models.*
import kotlinx.serialization.json.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class StudyPlannerService {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    // ─── Main plan generation ───────────────────────────────────────────────

    suspend fun generateStudyPlan(profile: StudentProfile, openaiApiKey: String): AIStudyPlan {
        val insights = analyzeSubjects(profile)
        val weeklyPlan = buildWeeklySchedule(profile, insights)
        val tips = generateDailyTips(profile, insights)
        val motivation = generateMotivationalMessage(profile)
        val strategy = buildOverallStrategy(profile, insights)

        // If API key provided, enhance with OpenAI
        val enhancedTips = if (openaiApiKey.isNotBlank()) {
            try { enhanceWithOpenAI(profile, insights, openaiApiKey) } catch (e: Exception) { tips }
        } else tips

        return AIStudyPlan(
            studentId = profile.id,
            generatedAt = LocalDate.now().toString(),
            weeklyPlan = weeklyPlan,
            subjectInsights = insights,
            dailyTips = enhancedTips,
            motivationalMessage = motivation,
            overallStrategy = strategy
        )
    }

    // ─── Subject Analysis (ML-like scoring) ─────────────────────────────────

    fun analyzeSubjects(profile: StudentProfile): List<SubjectInsight> {
        return profile.subjects.map { subject ->
            val weaknessScore = calculateWeaknessScore(subject, profile)
            val recommendedHours = calculateRecommendedHours(subject, weaknessScore, profile)
            val topics = suggestTopics(subject)
            val technique = suggestTechnique(subject, profile)
            val priority = when {
                weaknessScore >= 70 -> "high"
                weaknessScore >= 40 -> "medium"
                else -> "low"
            }

            SubjectInsight(
                subjectId = subject.id,
                subjectName = subject.name,
                weaknessScore = weaknessScore,
                recommendedHoursPerDay = recommendedHours,
                keyTopicsToRevise = topics,
                suggestedTechnique = technique,
                priority = priority
            )
        }.sortedByDescending { it.weaknessScore }
    }

    private fun calculateWeaknessScore(subject: Subject, profile: StudentProfile): Int {
        var score = 0

        // Low current score → high weakness
        score += (100 - subject.currentScore) / 2

        // High difficulty → more attention
        score += subject.difficulty * 8

        // Less study hours allocated → weakness
        val avgHours = profile.subjects.map { it.hoursPerWeek }.average()
        if (subject.hoursPerWeek < avgHours) score += 15

        // Exam soon → priority boost
        subject.examDate?.let {
            val examDate = LocalDate.parse(it)
            val daysLeft = LocalDate.now().until(examDate).days
            when {
                daysLeft <= 7  -> score += 25
                daysLeft <= 14 -> score += 15
                daysLeft <= 30 -> score += 8
            }
        }

        return score.coerceIn(0, 100)
    }

    private fun calculateRecommendedHours(
        subject: Subject,
        weaknessScore: Int,
        profile: StudentProfile
    ): Double {
        val baseHours = profile.studyHoursPerDay / profile.subjects.size
        val multiplier = when {
            weaknessScore >= 70 -> 1.8
            weaknessScore >= 50 -> 1.4
            weaknessScore >= 30 -> 1.0
            else -> 0.7
        }
        return (baseHours * multiplier).coerceIn(0.5, 4.0)
    }

    private fun suggestTopics(subject: Subject): List<String> {
        // In production: fetch from curriculum DB or AI
        return when {
            subject.name.contains("Math", true) -> listOf(
                "Calculus fundamentals", "Integration techniques",
                "Differential equations", "Linear algebra", "Statistics"
            )
            subject.name.contains("Physics", true) -> listOf(
                "Mechanics", "Thermodynamics", "Electromagnetism",
                "Optics", "Modern physics"
            )
            subject.name.contains("Chemistry", true) -> listOf(
                "Organic reactions", "Equilibrium", "Electrochemistry",
                "Chemical bonding", "Periodic trends"
            )
            subject.name.contains("Biology", true) -> listOf(
                "Cell biology", "Genetics", "Evolution",
                "Human physiology", "Ecology"
            )
            subject.name.contains("English", true) -> listOf(
                "Grammar rules", "Essay writing", "Comprehension",
                "Vocabulary", "Literature analysis"
            )
            subject.name.contains("History", true) -> listOf(
                "Key events timeline", "Causes & effects",
                "Important figures", "Document analysis", "Essay structure"
            )
            else -> listOf(
                "Core concepts review", "Practice problems",
                "Past papers", "Formula sheet", "Key definitions"
            )
        }.take(if (subject.currentScore < 50) 5 else 3)
    }

    private fun suggestTechnique(subject: Subject, profile: StudentProfile): String {
        return when (profile.learningStyle) {
            "visual" -> when {
                subject.difficulty >= 4 -> "Mind maps + Color-coded diagrams + Flowcharts"
                else -> "Visual summaries + Infographics + YouTube explanations"
            }
            "auditory" -> "Record yourself explaining concepts + Podcast-style revision + Study groups"
            "reading" -> "Cornell notes + Textbook annotation + Summary writing"
            "kinesthetic" -> "Practice problems first + Lab work + Teach-back method"
            else -> "Pomodoro technique (25 min study + 5 min break) + Active recall"
        }
    }

    // ─── Weekly Schedule Builder ─────────────────────────────────────────────

    private fun buildWeeklySchedule(profile: StudentProfile, insights: List<SubjectInsight>): WeeklyPlan {
        val today = LocalDate.now()
        val weekStart = today.toString()
        val weekEnd = today.plusDays(6).toString()
        val sessions = mutableListOf<StudySession>()

        val startHour = when (profile.preferredStudyTime) {
            "morning" -> 7
            "afternoon" -> 12
            "evening" -> 16
            "night" -> 20
            else -> 8
        }

        val insightMap = insights.associateBy { it.subjectId }

        for (dayOffset in 0..6) {
            val date = today.plusDays(dayOffset.toLong()).toString()
            val isSunday = today.plusDays(dayOffset.toLong()).dayOfWeek.value == 7

            var currentHour = startHour
            var currentMin = 0

            // Sunday = lighter schedule
            val dailyHours = if (isSunday) profile.studyHoursPerDay * 0.5 else profile.studyHoursPerDay
            var remainingMinutes = (dailyHours * 60).toInt()
            var sessionIndex = 0

            // Sort subjects: high priority first, rotate daily
            val orderedSubjects = insights
                .sortedByDescending { it.weaknessScore }
                .let { list ->
                    val rotated = list.toMutableList()
                    repeat(dayOffset % list.size) {
                        rotated.add(rotated.removeAt(0))
                    }
                    rotated
                }

            while (remainingMinutes > 30 && sessionIndex < orderedSubjects.size * 2) {
                val insight = orderedSubjects[sessionIndex % orderedSubjects.size]
                val subject = profile.subjects.find { it.id == insight.subjectId } ?: continue

                // Session duration based on priority
                val sessionMins = when (insight.priority) {
                    "high"   -> if (isSunday) 45 else 60
                    "medium" -> if (isSunday) 30 else 45
                    else     -> if (isSunday) 20 else 30
                }.coerceAtMost(remainingMinutes)

                if (sessionMins < 20) break

                val startTime = String.format("%02d:%02d", currentHour, currentMin)
                val endMinutes = currentHour * 60 + currentMin + sessionMins
                val endTime = String.format("%02d:%02d", endMinutes / 60, endMinutes % 60)

                val sessionType = when {
                    isSunday -> "revision"
                    dayOffset % 3 == 0 -> "practice"
                    insight.weaknessScore > 60 -> "study"
                    else -> if (sessionIndex % 2 == 0) "study" else "revision"
                }

                sessions.add(
                    StudySession(
                        id = UUID.randomUUID().toString(),
                        subjectId = subject.id,
                        subjectName = subject.name,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        durationMinutes = sessionMins,
                        sessionType = sessionType,
                        topics = insightMap[subject.id]?.keyTopicsToRevise?.take(2) ?: emptyList()
                    )
                )

                // Add break after each session
                val breakMins = if (sessionMins >= 45) 15 else 10
                currentHour = (endMinutes + breakMins) / 60
                currentMin = (endMinutes + breakMins) % 60
                remainingMinutes -= (sessionMins + breakMins)
                sessionIndex++

                if (orderedSubjects.size <= 1 || sessionIndex >= orderedSubjects.size) break
            }
        }

        val focusSubjects = insights
            .filter { it.priority == "high" }
            .map { it.subjectName }
            .take(3)

        val totalHours = sessions.sumOf { it.durationMinutes } / 60.0

        return WeeklyPlan(
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            sessions = sessions,
            totalStudyHours = totalHours,
            focusSubjects = focusSubjects,
            weekGoal = buildWeekGoal(profile, insights)
        )
    }

    private fun buildWeekGoal(profile: StudentProfile, insights: List<SubjectInsight>): String {
        val highPriority = insights.filter { it.priority == "high" }.map { it.subjectName }
        return when {
            highPriority.isEmpty() -> "Maintain consistency and complete all revision sessions"
            highPriority.size == 1 -> "Focus on strengthening ${highPriority[0]} while revising other subjects"
            else -> "Priority focus: ${highPriority.take(2).joinToString(" & ")} — dedicate extra time daily"
        }
    }

    // ─── Tips & Motivation ───────────────────────────────────────────────────

    private fun generateDailyTips(profile: StudentProfile, insights: List<SubjectInsight>): List<String> {
        val tips = mutableListOf<String>()

        // Learning style tip
        tips.add(when (profile.learningStyle) {
            "visual" -> "🎨 Use color-coding in your notes — different colors for different concepts help visual learners retain 65% more"
            "auditory" -> "🎧 Record your summaries and listen while commuting — your brain processes audio 4x faster when relaxed"
            "reading" -> "📖 Use the Cornell Method: divide your page into notes, cues, and summary sections for maximum retention"
            "kinesthetic" -> "✋ Don't just read — solve problems first, then check theory. Struggle → Learn → Master"
            else -> "⏱️ Use Pomodoro: 25 min deep focus + 5 min break. After 4 rounds, take a 30 min break"
        })

        // Weak subject tip
        insights.firstOrNull { it.priority == "high" }?.let {
            tips.add("⚠️ ${it.subjectName} needs your attention! Start each study session with it when your mind is freshest")
        }

        // Study time tip
        tips.add(when (profile.preferredStudyTime) {
            "morning" -> "🌅 Morning studying is your superpower — cortisol peaks help with focus and memory encoding"
            "night" -> "🌙 Night studying + sleep = powerful combo. Your brain consolidates memory during REM sleep"
            "afternoon" -> "☀️ Beat the afternoon slump: have a 10-min walk before your session to boost alertness by 20%"
            else -> "📅 Consistency beats intensity — studying 2hrs daily beats 14hrs on Sunday"
        })

        // Goal-based tip
        tips.add(when (profile.examGoal) {
            "distinction" -> "🏆 Distinction strategy: master the 'why' not just the 'what'. Examiners reward conceptual depth"
            "merit" -> "🎯 Merit strategy: focus on understanding patterns in past papers — 70% of questions repeat"
            else -> "✅ Pass strategy: identify the minimum viable topics and nail them completely before expanding"
        })

        // Spaced repetition
        tips.add("🔄 Spaced repetition: Review new material after 1 day, 3 days, 7 days, 21 days for long-term retention")
        tips.add("💧 Drink water! Even 2% dehydration reduces cognitive performance. Keep a bottle on your desk")
        tips.add("📵 Phone in another room (not face-down) — just seeing your phone reduces working memory by 10%")

        return tips
    }

    private fun generateMotivationalMessage(profile: StudentProfile): String {
        val messages = listOf(
            "Hey ${profile.name}! Remember — every expert was once a beginner. Today's struggle is tomorrow's strength. Let's go! 💪",
            "${profile.name}, you chose to plan — that already puts you ahead of 80% of students. Now execute! 🚀",
            "Progress > Perfection, ${profile.name}. A 1% improvement every day = 37x better in a year. Start today! 📈",
            "The difference between who you are and who you want to be is what you do right now, ${profile.name}. Let's make it count! ⚡",
            "${profile.name}, your future self is watching what you do today. Make them proud! 🌟"
        )
        return messages.random()
    }

    private fun buildOverallStrategy(profile: StudentProfile, insights: List<SubjectInsight>): String {
        val highPriority = insights.filter { it.priority == "high" }
        val examGoalText = when (profile.examGoal) {
            "distinction" -> "achieve distinction"
            "merit" -> "secure merit"
            else -> "clear your exams"
        }

        return buildString {
            append("Your personalized strategy to $examGoalText: ")
            if (highPriority.isNotEmpty()) {
                append("Dedicate 60% of study time to ${highPriority.map { it.subjectName }.joinToString(", ")} ")
                append("(your weakest areas). ")
            }
            append("Study during ${profile.preferredStudyTime} hours when your focus peaks. ")
            append("Use ${profile.learningStyle}-based techniques for all subjects. ")
            append("Review this plan weekly and track your progress daily.")
        }
    }

    // ─── OpenAI Enhancement ─────────────────────────────────────────────────

    private suspend fun enhanceWithOpenAI(
        profile: StudentProfile,
        insights: List<SubjectInsight>,
        apiKey: String
    ): List<String> {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json() }
        }

        val prompt = """
            Student: ${profile.name}, studying ${profile.subjects.map { it.name }.joinToString(", ")}
            Weak subjects: ${insights.filter { it.priority == "high" }.map { it.subjectName }.joinToString(", ")}
            Learning style: ${profile.learningStyle}, Goal: ${profile.examGoal}
            
            Generate 5 highly specific, actionable study tips for this student. Be encouraging and concrete.
            Return as a JSON array of strings only.
        """.trimIndent()

        return try {
            val response = client.post("https://api.openai.com/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("model", JsonPrimitive("gpt-3.5-turbo"))
                    put("max_tokens", JsonPrimitive(500))
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(prompt))
                        }
                    }
                }.toString())
            }
            val body = response.bodyAsText()
            val jsonResponse = Json.parseToJsonElement(body).jsonObject
            val content = jsonResponse["choices"]?.jsonArray?.get(0)
                ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: ""
            Json.decodeFromString<List<String>>(content.trim())
        } catch (e: Exception) {
            generateDailyTips(profile, insights)
        } finally {
            client.close()
        }
    }

    // ─── Progress Tracking ───────────────────────────────────────────────────

    fun generateProgressReport(
        studentId: String,
        sessions: List<StudySession>,
        allSubjects: List<Subject>
    ): ProgressReport {
        val completed = sessions.filter { it.completed }
        val completionRate = if (sessions.isEmpty()) 0.0 else completed.size.toDouble() / sessions.size * 100

        val hoursPerSubject = completed.groupBy { it.subjectId }
            .mapValues { (_, s) -> s.sumOf { it.durationMinutes } / 60.0 }

        val avgHours = hoursPerSubject.values.average().takeIf { !it.isNaN() } ?: 0.0

        val strong = hoursPerSubject.filter { it.value >= avgHours }.keys
            .mapNotNull { id -> allSubjects.find { it.id == id }?.name }

        val weak = allSubjects.map { it.name } - strong.toSet()

        return ProgressReport(
            studentId = studentId,
            totalHoursThisWeek = completed.sumOf { it.durationMinutes } / 60.0,
            completionRate = completionRate,
            strongSubjects = strong,
            weakSubjects = weak.take(3),
            streak = calculateStreak(completed),
            entries = completed.map { session ->
                ProgressEntry(
                    date = session.date,
                    subjectId = session.subjectId,
                    subjectName = session.subjectName,
                    minutesStudied = session.durationMinutes,
                    topicsCovered = session.topics,
                    selfRating = 3
                )
            }
        )
    }

    private fun calculateStreak(sessions: List<StudySession>): Int {
        val dates = sessions.map { it.date }.toSortedSet().toList().reversed()
        var streak = 0
        var expectedDate = LocalDate.now()
        for (dateStr in dates) {
            val date = LocalDate.parse(dateStr)
            if (date == expectedDate || date == expectedDate.minusDays(1)) {
                streak++
                expectedDate = date.minusDays(1)
            } else break
        }
        return streak
    }
}
