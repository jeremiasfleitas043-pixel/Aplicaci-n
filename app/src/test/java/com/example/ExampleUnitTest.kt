package com.example

import com.example.data.system.Formatters
import com.example.data.system.SmartCleanAnalyzer
import com.example.domain.model.InstalledAppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `unavailable bytes always format as No disponible`() {
        assertEquals("No disponible", Formatters.formatBytesOrUnavailable(null))
        assertEquals("No disponible", Formatters.formatBytesOrUnavailable(-1L))
    }

    @Test
    fun `whatsapp and games are flagged as protected apps`() {
        val whatsappReason = SmartCleanAnalyzer.detectProtectionReason(
            packageName = "com.whatsapp",
            isGameCategory = false,
            isSystemApp = false
        )
        assertEquals("Mensajería crítica", whatsappReason)

        val gameReason = SmartCleanAnalyzer.detectProtectionReason(
            packageName = "com.supercell.clashroyale",
            isGameCategory = true,
            isSystemApp = false
        )
        assertEquals("Juego (progreso local)", gameReason)

        val normalReason = SmartCleanAnalyzer.detectProtectionReason(
            packageName = "com.example.calculator",
            isGameCategory = false,
            isSystemApp = false
        )
        assertNull(normalReason)
    }

    @Test
    fun `smart clean recommends cache review for large cache without auto data wipe`() {
        val whatsappWithCache = InstalledAppInfo(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            versionName = "2.25",
            isSystemApp = false,
            isProtectedApp = true,
            protectionReason = "Mensajería crítica",
            appBytes = 120L * 1024L * 1024L,
            dataBytes = 900L * 1024L * 1024L,
            cacheBytes = 80L * 1024L * 1024L,
            totalBytes = 1100L * 1024L * 1024L,
            lastTimeUsedMillis = System.currentTimeMillis()
        )
        val rec = SmartCleanAnalyzer.evaluateApp(whatsappWithCache)
        assertNotNull(rec)
        assertTrue(rec!!.safeToQueueCache)
        assertTrue(rec.reason.contains("nunca datos"))

        val gameLargeDataLowCache = InstalledAppInfo(
            packageName = "com.game.rpg",
            appName = "RPG",
            versionName = "1.0",
            isSystemApp = false,
            isProtectedApp = true,
            protectionReason = "Juego (progreso local)",
            appBytes = 200L * 1024L * 1024L,
            dataBytes = 800L * 1024L * 1024L,
            cacheBytes = 2L * 1024L * 1024L,
            totalBytes = 1002L * 1024L * 1024L,
            lastTimeUsedMillis = System.currentTimeMillis()
        )
        val gameRec = SmartCleanAnalyzer.evaluateApp(gameLargeDataLowCache)
        assertNotNull(gameRec)
        assertFalse(gameRec!!.safeToQueueCache)
    }
}
