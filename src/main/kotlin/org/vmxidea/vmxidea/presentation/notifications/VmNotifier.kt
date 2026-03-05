package org.vmxidea.vmxidea.presentation.notifications

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object VmNotifier {
    fun notifySuccess(project: Project, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("vmxIDEA")
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }

    fun notifyError(project: Project, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("vmxIDEA")
            .createNotification(content, NotificationType.ERROR)
            .notify(project)
    }
}