package com.studyplanner

import com.studyplanner.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureCORS()
    configureStatusPages()
    configureCallLogging()
    configureRouting()
}
