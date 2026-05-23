package com.parsfilo.contentapp.monetization

import android.content.Context
import com.parsfilo.contentapp.R
import com.parsfilo.contentapp.feature.ads.AdFormat
import com.parsfilo.contentapp.feature.ads.AdPlacement
import com.parsfilo.contentapp.feature.ads.AdUnitIds
import timber.log.Timber

/**
 * Resolves AdMob unit IDs for the current app/flavor.
 *
 * Why this exists:
 * - CI builds (Azure) don't have local.properties, so feature:ads BuildConfig.ADMOB_* can be empty.
 * - The per-flavor production IDs already live in app/src/<flavor>/res/values/ads.xml.
 *
 * AdMob IDs are not secrets; using resources keeps the build reproducible and flavor-correct.
 */
object AppAdUnitIds {
    @Volatile
    private var rewardedInterstitialFallbackWarningLogged = false
    const val GOOGLE_TEST_PUBLISHER_PREFIX = "ca-app-pub-3940256099942544"

    data class Ids(
        val banner: String,
        val interstitial: String,
        val native: String,
        val rewarded: String,
        val rewardedInterstitial: String,
        val appOpen: String,
    )

    fun resolve(
        context: Context,
        useTestAds: Boolean,
    ): Ids =
        if (useTestAds) {
            Ids(
                banner = AdUnitIds.Test.BANNER,
                interstitial = AdUnitIds.Test.INTERSTITIAL,
                native = AdUnitIds.Test.NATIVE,
                rewarded = AdUnitIds.Test.REWARDED,
                rewardedInterstitial = AdUnitIds.Test.REWARDED_INTERSTITIAL,
                appOpen = AdUnitIds.Test.APP_OPEN,
            )
        } else {
            val rewarded = context.getString(R.string.ad_unit_rewarded)
            val rewardedInterstitial =
                stringByNameOrNull(context, "ad_unit_rewarded_interstitial")
                    ?: rewarded.also {
                        if (!rewardedInterstitialFallbackWarningLogged) {
                            rewardedInterstitialFallbackWarningLogged = true
                            Timber.w(
                                "Missing ad_unit_rewarded_interstitial in ads.xml for %s. Falling back to rewarded unit id.",
                                context.packageName,
                            )
                        }
                    }
            Ids(
                banner = context.getString(R.string.ad_unit_banner),
                interstitial = context.getString(R.string.ad_unit_interstitial),
                native = context.getString(R.string.ad_unit_native),
                rewarded = rewarded,
                rewardedInterstitial = rewardedInterstitial,
                appOpen = context.getString(R.string.ad_unit_open_app),
            )
        }

    fun resolvePlacement(
        context: Context,
        placement: AdPlacement,
        useTestAds: Boolean,
    ): String {
        val ids = resolve(context, useTestAds)
        val placementValue =
            placement.resourceName
                ?.let { stringByNameOrNull(context, it) }
                ?.takeIf { it.isNotBlank() }
        return placementOrDefault(placementValue, ids, placement.format)
    }

    internal fun defaultIdForFormat(
        ids: Ids,
        format: AdFormat,
    ): String =
        when (format) {
            AdFormat.BANNER -> ids.banner
            AdFormat.NATIVE -> ids.native
            AdFormat.INTERSTITIAL -> ids.interstitial
            AdFormat.APP_OPEN -> ids.appOpen
            AdFormat.REWARDED -> ids.rewarded
            AdFormat.REWARDED_INTERSTITIAL -> ids.rewardedInterstitial
        }

    internal fun placementOrDefault(
        placementValue: String?,
        ids: Ids,
        format: AdFormat,
    ): String = placementValue?.takeIf { it.isNotBlank() } ?: defaultIdForFormat(ids, format)

    fun Ids.usesGoogleTestIds(): Boolean =
        listOf(banner, interstitial, native, rewarded, rewardedInterstitial, appOpen)
            .all { it.startsWith(GOOGLE_TEST_PUBLISHER_PREFIX) }

    private fun stringByNameOrNull(
        context: Context,
        name: String,
    ): String? {
        val resId = context.resources.getIdentifier(name, "string", context.packageName)
        if (resId == 0) return null
        return runCatching { context.getString(resId) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
