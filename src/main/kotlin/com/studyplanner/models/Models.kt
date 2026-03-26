package com.studyplanner.models

import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    val id: String,
    val name: String,
    val difficulty: Int, // 1-5
    val hoursPerWeek: Double,
    val examDate: String? = null,
    val currentScore: Int = 50 // 0-100
)

@Serializable
data class StudentProfile(
    val id: String,
    val name: String,
    val subjects: List<Subject>,
    val studyHoursPerDay: Double = 6.0,
    val preferredStudyTime: String = "morning", // morning, afternoon, evening, night
    val learningStyle: String = "visual", // visual, auditory, reading, kinesthetic
    val examGoal: String = "distinction" // pass, merit, distinction
)

@Serializable
data class StudySession(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val sessionType: String, // study, revision, practice, break
    val topics: List<String> = emptyList(),
    val completed: Boolean = false,
    val notes: String = ""
)

@Serializable
data class WeeklyPlan(
    val weekStartDate: String,
    val weekEndDate: String,
    val sessions: List<StudySession>,
    val totalStudyHours: Double,
    val focusSubjects: List<String>,
    val weekGoal: String
)

@Serializable
data class SubjectInsight(
    val subjectId: String,
    val subjectName: String,
    val weaknessScore: Int, // 0-100, higher = more attention needed
    val recommendedHoursPerDay: Double,
    val keyTopicsToRevise: List<String>,
    val suggestedTechnique: String,
    val priority: String // high, medium, low
)

@Serializable
data class AIStudyPlan(
    val studentId: String,
    val generatedAt: String,
    val weeklyPlan: WeeklyPlan,
    val subjectInsights: List<SubjectInsight>,
    val dailyTips: List<String>,
    val motivationalMessage: String,
    val overallStrategy: String
)

@Serializable
data class PlanRequest(
    val profile: StudentProfile,
    val openaiApiKey: String = ""
)

@Serializable
data class SessionUpdateRequest(
    val sessionId: String,
    val completed: Boolean,
    val notes: String = ""
)

@Serializable
data class ProgressEntry(
    val date: String,
    val subjectId: String,
    val subjectName: String,
    val minutesStudied: Int,
    val topicsCovered: List<String>,
    val selfRating: Int // 1-5
)

@Serializable
data class ProgressReport(
    val studentId: String,
    val totalHoursThisWeek: Double,
    val completionRate: Double,
    val strongSubjects: List<String>,
    val weakSubjects: List<String>,
    val streak: Int,
    val entries: List<ProgressEntry>
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String = ""
)
