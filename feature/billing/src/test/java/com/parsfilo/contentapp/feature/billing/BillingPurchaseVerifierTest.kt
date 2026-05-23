package com.parsfilo.contentapp.feature.billing

import com.android.billingclient.api.Purchase
import com.google.common.truth.Truth.assertThat
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingPurchaseVerifierTest {

    private val firebaseAuth = mockk<FirebaseAuth>()
    private val firebaseAppCheck = mockk<FirebaseAppCheck>(relaxed = true)
    private val purchase = mockk<Purchase>(relaxed = true)

    @Test
    fun `blank verification url returns misconfigured`() = runTest {
        val verifier = BillingPurchaseVerifier(
            firebaseAuth = firebaseAuth,
            firebaseAppCheck = firebaseAppCheck,
            verificationUrl = "",
        )

        val result = verifier.verify("com.parsfilo.test", purchase)

        assertThat(result.verified).isFalse()
        assertThat(result.purchaseState).isEqualTo("MISCONFIGURED")
    }

    @Test
    fun `missing auth user returns auth required`() = runTest {
        every { firebaseAuth.currentUser } returns null
        val verifier = BillingPurchaseVerifier(
            firebaseAuth = firebaseAuth,
            firebaseAppCheck = firebaseAppCheck,
            verificationUrl = "https://example.com/verify",
        )

        val result = verifier.verify("com.parsfilo.test", purchase)

        assertThat(result.verified).isFalse()
        assertThat(result.purchaseState).isEqualTo("AUTH_REQUIRED")
    }
}
