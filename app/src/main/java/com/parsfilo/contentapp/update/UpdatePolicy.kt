package com.parsfilo.contentapp.update

data class RemoteUpdateConfig(
    val minSupportedVersionCode: Long,
    val latestVersionCode: Long,
    val updateMode: String,
    val title: String,
    val message: String,
    val updateButton: String,
    val laterButton: String,
)

sealed class UpdatePolicy {
    data object None : UpdatePolicy()

    data class Soft(
        val title: String,
        val message: String,
        val updateButton: String,
        val laterButton: String,
    ) : UpdatePolicy()

    data class Hard(
        val title: String,
        val message: String,
        val updateButton: String,
    ) : UpdatePolicy()
}

internal fun resolveUpdatePolicy(
    currentVersionCode: Long,
    cfg: RemoteUpdateConfig,
): UpdatePolicy {
    if (currentVersionCode < cfg.minSupportedVersionCode) {
        return UpdatePolicy.Hard(
            title = cfg.title,
            message = cfg.message,
            updateButton = cfg.updateButton,
        )
    }

    return when (normalizeUpdateMode(cfg.updateMode)) {
        UpdateMode.HARD ->
            UpdatePolicy.Hard(
                title = cfg.title,
                message = cfg.message,
                updateButton = cfg.updateButton,
            )

        UpdateMode.SOFT ->
            UpdatePolicy.Soft(
                title = cfg.title,
                message = cfg.message,
                updateButton = cfg.updateButton,
                laterButton = cfg.laterButton,
            )

        UpdateMode.NONE -> {
            if (currentVersionCode < cfg.latestVersionCode) {
                UpdatePolicy.Soft(
                    title = cfg.title,
                    message = cfg.message,
                    updateButton = cfg.updateButton,
                    laterButton = cfg.laterButton,
                )
            } else {
                UpdatePolicy.None
            }
        }
    }
}

internal enum class UpdateMode {
    NONE,
    SOFT,
    HARD,
}

internal fun normalizeUpdateMode(value: String?): UpdateMode =
    when (value?.trim()?.lowercase()) {
        "hard" -> UpdateMode.HARD
        "soft" -> UpdateMode.SOFT
        else -> UpdateMode.NONE
    }

internal fun coerceRemoteVersionCode(value: Long): Long = value.coerceAtLeast(1L)

internal fun resolveLocalizedRemoteText(
    languageCode: String,
    trValue: String?,
    enValue: String?,
    fallback: String,
): String {
    val preferred = if (languageCode.equals("tr", ignoreCase = true)) trValue else enValue
    val secondary = if (languageCode.equals("tr", ignoreCase = true)) enValue else trValue
    return preferred?.trim().takeUnless { it.isNullOrBlank() }
        ?: secondary?.trim().takeUnless { it.isNullOrBlank() }
        ?: fallback
}

data class UpdateDebugSnapshot(
    val currentVersionCode: Long,
    val config: RemoteUpdateConfig,
    val resolvedPolicy: UpdatePolicy,
) {
    fun toSummaryText(): String =
        "current=$currentVersionCode, min=${config.minSupportedVersionCode}, " +
            "latest=${config.latestVersionCode}, mode=${config.updateMode}, " +
            "policy=${resolvedPolicy::class.simpleName}"
}

internal object RemoteUpdateConfigKeys {
    const val MIN_SUPPORTED_VERSION_CODE = "min_supported_version_code"
    const val LATEST_VERSION_CODE = "latest_version_code"
    const val UPDATE_MODE = "update_mode"
    const val UPDATE_TITLE_TR = "update_title_tr"
    const val UPDATE_MESSAGE_TR = "update_message_tr"
    const val UPDATE_TITLE_EN = "update_title_en"
    const val UPDATE_MESSAGE_EN = "update_message_en"
    const val UPDATE_BUTTON_TR = "update_button_tr"
    const val UPDATE_BUTTON_EN = "update_button_en"
    const val LATER_BUTTON_TR = "later_button_tr"
    const val LATER_BUTTON_EN = "later_button_en"

    val allKeys: List<String> =
        listOf(
            MIN_SUPPORTED_VERSION_CODE,
            LATEST_VERSION_CODE,
            UPDATE_MODE,
            UPDATE_TITLE_TR,
            UPDATE_MESSAGE_TR,
            UPDATE_TITLE_EN,
            UPDATE_MESSAGE_EN,
            UPDATE_BUTTON_TR,
            UPDATE_BUTTON_EN,
            LATER_BUTTON_TR,
            LATER_BUTTON_EN,
        )

    val defaults: Map<String, Any> =
        mapOf(
            MIN_SUPPORTED_VERSION_CODE to 1L,
            LATEST_VERSION_CODE to 1L,
            UPDATE_MODE to "none",
            UPDATE_TITLE_TR to "Güncelleme gerekli",
            UPDATE_MESSAGE_TR to "Devam etmek için lütfen uygulamayı güncelleyin.",
            UPDATE_TITLE_EN to "Update required",
            UPDATE_MESSAGE_EN to "Please update the app to continue.",
            UPDATE_BUTTON_TR to "Güncelle",
            UPDATE_BUTTON_EN to "Update",
            LATER_BUTTON_TR to "Daha sonra",
            LATER_BUTTON_EN to "Later",
        )
}
