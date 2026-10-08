package com.axelliant.hris.model.notification

import com.axelliant.hris.model.payslip.asLenientInt

/** A workflow update in the notification feed. HRIS sends `name` as a number; Gson reads it as text. */
data class WorkflowNotification(
    val name: String? = null,
    val message: String? = null,
    val from_user_name: String? = null,
    val from_user_image: String? = null,
    val created_on: String? = null,
    val reference_document_type: String? = null,
    val reference_document_name: String? = null,
    val action_url: String? = null,
    val read: Any? = null
) {
    /** Anything but an explicit 0 counts as read (same as the iOS app). */
    val isRead: Boolean get() = read.asLenientInt() != 0

    fun asRead(): WorkflowNotification = copy(read = 1)
}

/** `message.data` of `get_notifications`. */
data class NotificationPage(
    val items: List<WorkflowNotification?>? = null,
    val unread_count: Any? = null,
    val total: Any? = null
) {
    fun rows(): List<WorkflowNotification> = items.orEmpty().filterNotNull().filter { !it.name.isNullOrBlank() }
    fun unread(): Int = unread_count.asLenientInt() ?: 0
    fun totalCount(): Int = total.asLenientInt() ?: rows().size
}

/** Which kind of document a notification is about; drives its icon and colour. */
enum class NotificationKind {
    LEAVE, ATTENDANCE, EXPENSE, OTHER;

    companion object {
        fun of(documentType: String?): NotificationKind {
            val text = documentType.orEmpty().lowercase()
            return when {
                text.contains("leave") -> LEAVE
                text.contains("checkin") || text.contains("attendance") -> ATTENDANCE
                text.contains("expense") -> EXPENSE
                else -> OTHER
            }
        }
    }
}
