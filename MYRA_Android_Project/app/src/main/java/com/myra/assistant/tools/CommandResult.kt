package com.myra.assistant.tools

/**
 * Standard Command Execution Status for MYRA Assistant.
 * Strictly prevents fake success claims when actions fail or are blocked.
 */
enum class CommandStatus {
    SUCCESS,
    FAILED,
    NOT_INSTALLED,
    PERMISSION_REQUIRED,
    NOT_FOUND,
    UNSUPPORTED
}

/**
 * Result model holding actual execution outcome, status code, and honest Hindi voice feedback.
 */
data class CommandResult(
    val success: Boolean,
    val status: CommandStatus,
    val message: String,
    val target: String = "",
    val actionDetail: String = ""
) {
    companion object {
        fun success(message: String, target: String = "", detail: String = "") =
            CommandResult(success = true, status = CommandStatus.SUCCESS, message = message, target = target, actionDetail = detail)

        fun failed(message: String, target: String = "", detail: String = "") =
            CommandResult(success = false, status = CommandStatus.FAILED, message = message, target = target, actionDetail = detail)

        fun notInstalled(message: String, target: String = "", detail: String = "") =
            CommandResult(success = false, status = CommandStatus.NOT_INSTALLED, message = message, target = target, actionDetail = detail)

        fun permissionRequired(message: String, target: String = "", detail: String = "") =
            CommandResult(success = false, status = CommandStatus.PERMISSION_REQUIRED, message = message, target = target, actionDetail = detail)

        fun notFound(message: String, target: String = "", detail: String = "") =
            CommandResult(success = false, status = CommandStatus.NOT_FOUND, message = message, target = target, actionDetail = detail)

        fun unsupported(message: String, target: String = "", detail: String = "") =
            CommandResult(success = false, status = CommandStatus.UNSUPPORTED, message = message, target = target, actionDetail = detail)
    }
}
