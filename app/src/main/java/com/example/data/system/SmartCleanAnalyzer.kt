package com.example.data.system

import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.SmartRecommendation

object SmartCleanAnalyzer {

    private const val LARGE_CACHE_THRESHOLD_BYTES = 50L * 1024L * 1024L // 50 MB
    private const val MEDIUM_CACHE_THRESHOLD_BYTES = 15L * 1024L * 1024L // 15 MB
    private const val LARGE_DATA_THRESHOLD_BYTES = 400L * 1024L * 1024L // 400 MB
    private const val UNUSED_DAYS_MILLIS = 30L * 24L * 60L * 60L * 1000L // 30 días

    private val protectedMessagingPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.telegram.plus",
        "org.thoughtcrime.securesms",
        "com.facebook.orca",
        "com.discord",
        "com.viber.voip",
        "jp.naver.line.android",
        "com.Slack",
        "com.microsoft.teams"
    )

    private val protectedSecurityAndFinanceKeywords = listOf(
        "authenticator", "authy", "bank", "banco", "bbva", "santander",
        "mercadopago", "paypal", "wallet", "billetera", "clave", "keypass", "bitwarden"
    )

    fun detectProtectionReason(
        packageName: String,
        isGameCategory: Boolean,
        isSystemApp: Boolean
    ): String? {
        val lower = packageName.lowercase()
        if (packageName in protectedMessagingPackages || lower.contains("whatsapp")) {
            return "Mensajería crítica"
        }
        if (isGameCategory) {
            return "Juego (progreso local)"
        }
        if (protectedSecurityAndFinanceKeywords.any { lower.contains(it) }) {
            return "Finanzas / Seguridad"
        }
        if (isSystemApp) {
            return "App del sistema"
        }
        return null
    }

    fun evaluateApp(
        app: InstalledAppInfo,
        nowMillis: Long = System.currentTimeMillis()
    ): SmartRecommendation? {
        val cache = app.cacheBytes
        val data = app.dataBytes
        val lastUsed = app.lastTimeUsedMillis

        if (cache != null && cache >= LARGE_CACHE_THRESHOLD_BYTES) {
            val protectionNote = if (app.isProtectedApp) {
                " (${app.protectionReason}: limpiar solo caché, nunca datos)"
            } else {
                ""
            }
            return SmartRecommendation(
                reason = "Caché elevada de ${Formatters.formatBytes(cache)}. Conviene revisar la caché sin afectar tus archivos$protectionNote.",
                categoryLabel = "Caché alta",
                safeToQueueCache = true
            )
        }

        if (lastUsed != null && lastUsed > 0L && (nowMillis - lastUsed) >= UNUSED_DAYS_MILLIS && !app.isSystemApp) {
            val cacheText = if (cache != null && cache > 0L) {
                " Tiene ${Formatters.formatBytes(cache)} de caché."
            } else {
                ""
            }
            return SmartRecommendation(
                reason = "Sin uso reciente (${Formatters.formatDaysAgo(lastUsed)}). Puedes revisar su almacenamiento o desinstalarla.$cacheText",
                categoryLabel = "Poco uso",
                safeToQueueCache = (cache == null || cache > 0L)
            )
        }

        if (cache != null && cache >= MEDIUM_CACHE_THRESHOLD_BYTES) {
            return SmartRecommendation(
                reason = "Caché moderada (${Formatters.formatBytes(cache)}) apta para revisión segura.",
                categoryLabel = "Caché revisable",
                safeToQueueCache = true
            )
        }

        if (data != null && data >= LARGE_DATA_THRESHOLD_BYTES) {
            val safetyMsg = if (app.isProtectedApp) {
                "App protegida (${app.protectionReason}): NO borres sus datos sin copia de seguridad previa."
            } else {
                "Alto volumen de datos locales (${Formatters.formatBytes(data)}). Revísala manualmente antes de tomar acciones."
            }
            return SmartRecommendation(
                reason = safetyMsg,
                categoryLabel = "Datos elevados",
                safeToQueueCache = false
            )
        }

        return null
    }
}
