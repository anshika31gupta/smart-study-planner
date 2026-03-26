package com.studyplanner.routes

import com.studyplanner.models.*
import com.studyplanner.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.apiRoutes() {
    val plannerService = StudyPlannerService()

    route("/api") {

        // ── Health check ──────────────────────────────────────────────────
        get("/health") {
            call.respond(mapOf("status" to "ok", "version" to "1.0.0"))
        }

        // ── Profile endpoints ─────────────────────────────────────────────
        route("/profile") {
            post {
                val profile = call.receive<StudentProfile>()
                val savedProfile = profile.copy(id = if (profile.id.isBlank()) UUID.randomUUID().toString() else profile.id)
                DataStore.profiles[savedProfile.id] = savedProfile
                call.respond(HttpStatusCode.Created, ApiResponse(true, savedProfile, "Profile saved"))
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, ApiResponse<StudentProfile>(false, message = "Missing ID")
                )
                val profile = DataStore.profiles[id]
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<StudentProfile>(false, message = "Profile not found"))
                call.respond(ApiResponse(true, profile))
            }

            get("/demo") {
                val profile = DataStore.profiles["demo-student"]
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<StudentProfile>(false, message = "Demo not found"))
                call.respond(ApiResponse(true, profile))
            }
        }

        // ── Plan generation ───────────────────────────────────────────────
        route("/plan") {
            post("/generate") {
                val request = call.receive<PlanRequest>()
                val profile = request.profile.copy(
                    id = if (request.profile.id.isBlank()) UUID.randomUUID().toString() else request.profile.id
                )
                DataStore.profiles[profile.id] = profile

                val plan = plannerService.generateStudyPlan(profile, request.openaiApiKey)
                DataStore.plans[profile.id] = plan
                DataStore.sessions[profile.id] = plan.weeklyPlan.sessions.toMutableList()

                call.respond(HttpStatusCode.Created, ApiResponse(true, plan, "Plan generated successfully!"))
            }

            get("/{studentId}") {
                val studentId = call.parameters["studentId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, ApiResponse<AIStudyPlan>(false, message = "Missing student ID")
                )
                val plan = DataStore.plans[studentId]
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<AIStudyPlan>(false, message = "No plan found"))
                call.respond(ApiResponse(true, plan))
            }

            post("/demo") {
                val profile = DataStore.profiles["demo-student"]!!
                val plan = plannerService.generateStudyPlan(profile, "")
                DataStore.plans["demo-student"] = plan
                DataStore.sessions["demo-student"] = plan.weeklyPlan.sessions.toMutableList()
                call.respond(ApiResponse(true, plan, "Demo plan generated!"))
            }
        }

        // ── Subject analysis ──────────────────────────────────────────────
        post("/analyze") {
            val profile = call.receive<StudentProfile>()
            val insights = plannerService.analyzeSubjects(profile)
            call.respond(ApiResponse(true, insights, "Analysis complete"))
        }

        // ── Session management ────────────────────────────────────────────
        route("/sessions") {
            get("/{studentId}") {
                val studentId = call.parameters["studentId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, ApiResponse<List<StudySession>>(false, message = "Missing ID")
                )
                val sessions = DataStore.getAllSessions(studentId)
                call.respond(ApiResponse(true, sessions))
            }

            patch("/update/{studentId}") {
                val studentId = call.parameters["studentId"] ?: return@patch call.respond(
                    HttpStatusCode.BadRequest, ApiResponse<Boolean>(false, message = "Missing ID")
                )
                val update = call.receive<SessionUpdateRequest>()
                val success = DataStore.updateSession(studentId, update.sessionId, update.completed, update.notes)
                if (success) {
                    call.respond(ApiResponse(true, true, "Session updated"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse(false, false, "Session not found"))
                }
            }
        }

        // ── Progress report ───────────────────────────────────────────────
        get("/progress/{studentId}") {
            val studentId = call.parameters["studentId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, ApiResponse<ProgressReport>(false, message = "Missing ID")
            )
            val profile = DataStore.profiles[studentId]
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<ProgressReport>(false, message = "Profile not found"))
            val sessions = DataStore.getAllSessions(studentId)
            val report = plannerService.generateProgressReport(studentId, sessions, profile.subjects)
            call.respond(ApiResponse(true, report))
        }
    }
}
