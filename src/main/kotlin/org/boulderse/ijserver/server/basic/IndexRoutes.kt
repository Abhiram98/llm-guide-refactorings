package org.boulderse.ijserver.server.basic

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

class IndexRoutes(
    private val routing: Routing,
) {
    fun install() {
        routing.get("/") {
            call.respondText("Hello, world!", ContentType.Text.Html)
        }
    }
}
