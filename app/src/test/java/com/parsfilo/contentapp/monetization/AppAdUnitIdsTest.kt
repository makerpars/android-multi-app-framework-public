package com.parsfilo.contentapp.monetization

import com.parsfilo.contentapp.feature.ads.AdFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class AppAdUnitIdsTest {
    private val ids =
        AppAdUnitIds.Ids(
            banner = "banner-default",
            interstitial = "interstitial-default",
            native = "native-default",
            rewarded = "rewarded-default",
            rewardedInterstitial = "rewarded-inter-default",
            appOpen = "app-open-default",
        )

    @Test
    fun `placement value wins when present`() {
        val resolved = AppAdUnitIds.placementOrDefault("banner-home", ids, AdFormat.BANNER)
        assertEquals("banner-home", resolved)
    }

    @Test
    fun `falls back to format default when placement missing`() {
        val resolved = AppAdUnitIds.placementOrDefault(null, ids, AdFormat.INTERSTITIAL)
        assertEquals("interstitial-default", resolved)
    }

    @Test
    fun `blank placement falls back to default`() {
        val resolved = AppAdUnitIds.placementOrDefault(" ", ids, AdFormat.APP_OPEN)
        assertEquals("app-open-default", resolved)
    }
}
