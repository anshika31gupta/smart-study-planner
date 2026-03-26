package com.studyplanner.services

import com.studyplanner.models.*
import java.util.concurrent.ConcurrentHashMap

object DataStore {
    val profiles = ConcurrentHashMap<String, StudentProfile>()
    val plans = ConcurrentHashMap<String, AIStudyPlan>()
    val sessions = ConcurrentHashMap<String, MutableList<StudySession>>()

    fun updateSession(studentId: String, sessionId: String, completed: Boolean, notes: String): Boolean {
        val studentSessions = sessions[studentId] ?: return false
        val idx = studentSessions.indexOfFirst { it.id == sessionId }
        if (idx == -1) return false
        studentSessions[idx] = studentSessions[idx].copy(completed = completed, notes = notes)
        return true
    }

    fun getAllSessions(studentId: String): List<StudySession> {
        return sessions[studentId] ?: emptyList()
    }

    // Pre-populate demo data
    fun seedDemoData() {
        val demoProfile = StudentProfile(
            id = "demo-student",
            name = "Arjun Kumar",
            subjects = listOf(
                Subject("sub1", "Mathematics", difficulty = 4, hoursPerWeek = 8.0, currentScore = 62, examDate = null),
                Subject("sub2", "Physics", difficulty = 5, hoursPerWeek = 6.0, currentScore = 45, examDate = null),
                Subject("sub3", "Chemistry", difficulty = 3, hoursPerWeek = 5.0, currentScore = 70, examDate = null),
                Subject("sub4", "English", difficulty = 2, hoursPerWeek = 3.0, currentScore = 85, examDate = null),
                Subject("sub5", "Computer Science", difficulty = 3, hoursPerWeek = 4.0, currentScore = 78, examDate = null)
            ),
            studyHoursPerDay = 6.0,
            preferredStudyTime = "morning",
            learningStyle = "visual",
            examGoal = "distinction"
        )
        profiles["demo-student"] = demoProfile
    }
}
