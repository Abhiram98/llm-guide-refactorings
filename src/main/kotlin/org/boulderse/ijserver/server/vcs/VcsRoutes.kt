package org.boulderse.ijserver.server.vcs

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlinx.serialization.json.Json

class VcsRoutes(
    private val routing: Routing,
    private val projectCallBack: () -> Project,
) {
    fun installRoutes() {
        routing.get("/vcs/changes") {
            call.respond(HttpStatusCode.OK, getChanges().toString())
        }

        routing.get("/vcs/renamed_files") {
            val renamedFiles =
                getChanges()
                    .map { change ->
                        if (change.isRenamed) {
                            change.beforeRevision?.file?.name to change.afterRevision?.file?.name
                        } else {
                            null
                        }
                    }.filterNotNull()
                    .toList()

            call.respond(HttpStatusCode.OK, Json.encodeToString(renamedFiles))
        }

        routing.get("/vcs/changed_files") {
            val changedFiles =
                getChanges()
                    .map { change ->
                        change.afterRevision?.file?.name
                    }.filterNotNull()
                    .toList()
            call.respond(HttpStatusCode.OK, Json.encodeToString(changedFiles))
        }
    }

    fun getChanges(): Collection<Change> {
        val manager: ChangeListManager = ChangeListManager.getInstance(projectCallBack())
        manager.allChanges.forEach { change -> println(change.description) }
        manager.changeLists.forEach { changeList -> println(changeList) }
        return manager.allChanges
    }
}
