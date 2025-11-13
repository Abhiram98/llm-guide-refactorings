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
            val changes =
                getChanges()
                    .map {
                        val afterPath =
                            it.afterRevision
                                ?.file
                                ?.path
                                ?.removePrefix("${projectCallBack().basePath}/")
                        if (afterPath == null) {
                            null
                        } else {
                            val beforeStr = it.beforeRevision?.content
                            val afterStr = it.afterRevision?.content
                            afterPath to Pair(beforeStr, afterStr)
                        }
                    }.filterNotNull()
                    .toMap()
            call.respond(HttpStatusCode.OK, Json.encodeToString(changes))
        }

        routing.get("/vcs/renamed_files") {
            val project = projectCallBack()
            val renamedFiles =
                getChanges()
                    .map { change ->
                        if (change.isRenamed) {
                            val beforePath =
                                change.beforeRevision
                                    ?.file
                                    ?.path
                                    ?.removePrefix("${project.basePath}/")
                            val afterPath =
                                change.afterRevision
                                    ?.file
                                    ?.path
                                    ?.removePrefix("${project.basePath}/")
                            if (beforePath != null && afterPath != null) {
                                beforePath to afterPath
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }.filterNotNull()
                    .toMap()

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
        return getChanges(projectCallBack())
    }
    companion object {
        fun getChanges(project: Project): Collection<Change> {
            val manager: ChangeListManager = ChangeListManager.getInstance(project)
            return manager.allChanges
        }
    }
}
