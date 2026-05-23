package com.parsfilo.contentapp.core.firebase

/**
 * Analytics contract:
 * - Event names and param keys are centralized to prevent drift.
 * - Keep names stable (changing names breaks historical reporting).
 *
 * Firebase Analytics constraints (high level):
 * - Event name: <= 40 chars, starts with a letter, [A-Za-z0-9_]
 * - Param key: <= 40 chars, starts with a letter, [A-Za-z0-9_]
 * - User property: <= 24 chars, starts with a letter, [A-Za-z0-9_]
 */
object AnalyticsEventName {
    const val TAB_SELECTED = "tab_selected"

    const val VERSE_READ = "verse_read"
    const val DISPLAY_MODE_CHANGED = "display_mode_changed"

    const val AUDIO_PLAY = "audio_play"
    const val AUDIO_PAUSE = "audio_pause"
    const val AUDIO_STOP = "audio_stop"
    const val AUDIO_COMPLETE = "audio_complete"

    const val AD_SHOWN = "ad_shown"
    const val AD_CLICKED = "ad_clicked"
    const val AD_FAILED_TO_LOAD = "ad_failed_to_load"
    const val AD_REQUEST_SENT = "ad_request_sent"
    const val AD_LOADED = "ad_loaded"
    const val AD_SUPPRESSED = "ad_suppressed"
    const val AD_FAILED_TO_SHOW = "ad_failed_to_show"
    const val AD_SHOW_BLOCKED = "ad_show_blocked"
    const val AD_SHOW_NOT_LOADED = "ad_show_not_loaded"
    const val AD_PRELOAD_REQUESTED = "ad_preload_requested"
    const val AD_FAILED_TO_SHOW_LEGACY = "ad_failed_to_show_fullscreen"
    const val AD_DISMISSED = "ad_dismissed"
    const val AD_AFTER_ENGAGEMENT = "ad_after_engagement"
    const val AD_IMPRESSION = "ad_impression"
    const val AD_SERVED = "ad_served"
    const val AD_PAID_EVENT = "ad_paid_event"
    const val AD_CLICK = "ad_click"
    const val REWARDED_INTRO_SHOWN = "rewarded_intro_shown"
    const val REWARDED_INTRO_SKIPPED = "rewarded_intro_skipped"
    const val REWARDED_INTRO_CONFIRMED = "rewarded_intro_confirmed"
    const val HISTORY_UNLOCK_REQUESTED = "history_unlock_requested"
    const val HISTORY_UNLOCK_SKIPPED = "history_unlock_skipped"
    const val HISTORY_UNLOCK_SUCCEEDED = "history_unlock_succeeded"
    const val HISTORY_UNLOCK_UNAVAILABLE = "history_unlock_unavailable"

    const val CONSENT_FLOW_STARTED = "consent_flow_started"
    const val CONSENT_GRANTED = "consent_granted"
    const val CONSENT_DENIED = "consent_denied"
    const val CONSENT_NOT_REQUIRED = "consent_not_required"
    const val CONSENT_ERROR = "consent_error"
    const val CONSENT_REFRESHED = "consent_refreshed"
    const val CONSENT_DEBUG_RESULT = "consent_debug_result"
    const val PRIVACY_OPTIONS_OPENED = "privacy_options_opened"
    const val AGE_GATE_COMPLETED = "age_gate_completed"

    const val PUSH_RECEIVED = "push_received"
    const val PUSH_OPEN = "push_open"

    const val NOTIFICATION_OPEN = "notification_open"
    const val NOTIFICATION_MARK_READ = "notification_mark_read"
    const val NOTIFICATION_MARK_UNREAD = "notification_mark_unread"
    const val NOTIFICATIONS_MARK_ALL_READ = "notifications_mark_all_read"
    const val NOTIFICATION_DELETE = "notification_delete"
    const val NOTIFICATIONS_DELETE_ALL = "notifications_delete_all"

    const val PAYWALL_VIEW = "paywall_view"
    const val PURCHASE_START = "purchase_start"
    const val PURCHASE_SUCCESS = "purchase_success"
    const val PURCHASE_FAILED = "purchase_failed"
    const val CONTENT_PLAY_START = "content_play_start"
    const val CONTENT_PLAY_COMPLETE = "content_play_complete"
    const val REWARDED_WATCH_COMPLETE = "rewarded_watch_complete"
    const val REWARDED_WATCH_SKIPPED = "rewarded_watch_skipped"
}

object AnalyticsParamKey {
    const val TAB = "tab"

    const val VERSE_ID = "verse_id"
    const val DISPLAY_MODE = "display_mode"
    const val OLD_MODE = "old_mode"
    const val NEW_MODE = "new_mode"

    const val POSITION_MS = "position_ms"
    const val DURATION_MS = "duration_ms"
    const val TOTAL_DURATION_MS = "total_duration_ms"

    const val AD_TYPE = "ad_type"
    const val AD_FORMAT = "ad_format"
    const val PLACEMENT = "placement"
    const val ROUTE = "route"
    const val AD_UNIT_ID = "ad_unit_id"
    const val ERROR_CODE = "error_code"
    const val ERROR_MESSAGE = "error_message"
    const val FILL_LATENCY_MS = "fill_latency_ms"
    const val SUPPRESS_REASON = "suppress_reason"
    const val BACKOFF_ATTEMPT = "backoff_attempt"
    const val BACKOFF_NEXT_MS = "backoff_next_ms"
    const val ADAPTER_NAME = "adapter_name"
    const val IS_LOADING = "is_loading"
    const val SHOW_TRIGGER = "show_trigger"
    const val TIME_SINCE_LOAD_MS = "time_since_load_ms"
    const val PRELOAD_REASON = "preload_reason"
    const val CONSENT_STATUS = "consent_status"
    const val CONSENT_TRIGGER = "consent_trigger"
    const val UMP_FORM_SHOWN = "ump_form_shown"
    const val UMP_RESULT = "ump_result"
    const val REQUEST_GEO = "request_geo"
    const val CAN_REQUEST_ADS = "can_request_ads"
    const val PRIVACY_OPTIONS_REQUIRED = "privacy_options_req"
    const val AGE_GATE_RESULT = "age_gate_result"
    const val SESSION_CONTENT_TYPE = "session_content_type"
    const val SESSION_VERSE_COUNT = "session_verse_count"
    const val SESSION_AUDIO_PLAYED = "session_audio_played"
    const val USER_TENURE_DAYS = "user_tenure_days"
    const val WATCH_DURATION_S = "watch_duration_s"
    const val REWARD_MINUTES_EARNED = "reward_minutes_earned"
    const val TOTAL_WATCH_COUNT = "total_watch_count"
    const val SESSION_DURATION_S = "session_duration_s"
    const val VERSE_COUNT_BEFORE_AD = "verse_count_before_ad"
    const val AGE_BUCKET = "age_bucket"
    const val PRIVACY_STATE = "privacy_state"
    const val PREMIUM_STATE = "premium_state"

    const val PUSH_TYPE = "push_type"
    const val PACKAGE_NAME = "package_name"

    const val PLAN_ID = "plan_id"
    const val REASON = "reason"
}

object AnalyticsUserPropertyKey {
    const val FLAVOR = "flavor"
    const val BUILD_TYPE = "build_type"
    const val APP_LANG = "app_lang"
    const val TZ = "tz"
    const val CONSENT_STATUS = "consent_status"
    const val IS_PREMIUM = "is_premium"
    const val AGE_GATE_STATUS = "age_gate_status"
}
