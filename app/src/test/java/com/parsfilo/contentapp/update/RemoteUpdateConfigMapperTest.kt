package com.parsfilo.contentapp.update

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteUpdateConfigMapperTest {
    @Test
    fun `tr locale prefers turkish text`() {
        val result =
            resolveLocalizedRemoteText(
                languageCode = "tr",
                trValue = "Türkçe",
                enValue = "English",
                fallback = "Fallback",
            )

        assertEquals("Türkçe", result)
    }

    @Test
    fun `non-tr locale prefers english text`() {
        val result =
            resolveLocalizedRemoteText(
                languageCode = "en",
                trValue = "Türkçe",
                enValue = "English",
                fallback = "Fallback",
            )

        assertEquals("English", result)
    }

    @Test
    fun `blank remote values fall back safely`() {
        val result =
            resolveLocalizedRemoteText(
                languageCode = "tr",
                trValue = "   ",
                enValue = "",
                fallback = "Fallback",
            )

        assertEquals("Fallback", result)
    }

    @Test
    fun `negative remote version code is coerced to one`() {
        assertEquals(1L, coerceRemoteVersionCode(-5))
    }
}
