package com.parsfilo.contentapp.update

import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyResolverTest {
    private fun config(
        min: Long = 1,
        latest: Long = 1,
        mode: String = "none",
    ): RemoteUpdateConfig =
        RemoteUpdateConfig(
            minSupportedVersionCode = min,
            latestVersionCode = latest,
            updateMode = mode,
            title = "Title",
            message = "Message",
            updateButton = "Update",
            laterButton = "Later",
        )

    @Test
    fun `current below min supported resolves hard`() {
        val policy = resolveUpdatePolicy(currentVersionCode = 4, cfg = config(min = 5, latest = 99))
        assertTrue(policy is UpdatePolicy.Hard)
    }

    @Test
    fun `current below latest resolves soft`() {
        val policy = resolveUpdatePolicy(currentVersionCode = 5, cfg = config(min = 5, latest = 6))
        assertTrue(policy is UpdatePolicy.Soft)
    }

    @Test
    fun `current at or above latest resolves none`() {
        val policy = resolveUpdatePolicy(currentVersionCode = 6, cfg = config(min = 5, latest = 6))
        assertTrue(policy is UpdatePolicy.None)
    }

    @Test
    fun `hard mode overrides to hard`() {
        val policy =
            resolveUpdatePolicy(
                currentVersionCode = 10,
                cfg = config(min = 5, latest = 10, mode = "hard"),
            )
        assertTrue(policy is UpdatePolicy.Hard)
    }

    @Test
    fun `soft mode overrides to soft`() {
        val policy =
            resolveUpdatePolicy(
                currentVersionCode = 10,
                cfg = config(min = 5, latest = 10, mode = "soft"),
            )
        assertTrue(policy is UpdatePolicy.Soft)
    }

    @Test
    fun `min supported hard remains highest priority even when mode soft`() {
        val policy =
            resolveUpdatePolicy(
                currentVersionCode = 4,
                cfg = config(min = 5, latest = 99, mode = "soft"),
            )
        assertTrue(policy is UpdatePolicy.Hard)
    }

    @Test
    fun `invalid mode falls back to version rules`() {
        val policy =
            resolveUpdatePolicy(
                currentVersionCode = 10,
                cfg = config(min = 5, latest = 10, mode = "???"),
            )
        assertTrue(policy is UpdatePolicy.None)
    }
}
