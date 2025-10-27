package org.boulderse.ijserver

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.boulderse.ijserver.settings.openSettingsDialog

private fun createNotificationGroup(): NotificationGroup =
    NotificationGroupManager.getInstance().getNotificationGroup("AI notification group")

internal fun showUnauthorizedNotification(project: Project) {
    val notification =
        createNotificationGroup().createNotification(
            LLMBundle.message("notification.unauthorized.title"),
            LLMBundle.message("notification.unauthorized.key.not.provided"),
            NotificationType.WARNING,
        )

    val action = LLMBundle.message("notification.unauthorized.key.not.provided.action")
    notification.addAction(
        NotificationAction.createSimple(action) {
            openSettingsDialog(project)
            notification.expire()
        },
    )
    notification.notify(project)
}

internal fun showAuthorizationFailedNotification(project: Project) {
    val notification =
        createNotificationGroup().createNotification(
            LLMBundle.message("notification.unauthorized.title"),
            LLMBundle.message("notification.unauthorized.key.is.invalid"),
            NotificationType.WARNING,
        )

    val action = LLMBundle.message("notification.unauthorized.key.not.provided.action")
    notification.addAction(
        NotificationAction.createSimple(action) {
            openSettingsDialog(project)
            notification.expire()
        },
    )
    notification.notify(project)
}

internal fun showRequestFailedNotification(
    project: Project,
    message: String,
) {
    createNotificationGroup()
        .createNotification(
            LLMBundle.message("notification.request.failed.title"),
            message,
            NotificationType.WARNING,
        ).notify(project)
}

internal fun showEFNotification(
    project: Project,
    message: String,
    notificationType: NotificationType,
) {
    createNotificationGroup()
        .createNotification(
            LLMBundle.message("notification.extract.function.with.llm.title"),
            message,
            notificationType,
        ).notify(project)
}
