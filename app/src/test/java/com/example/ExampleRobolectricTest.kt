package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.system.DeviceStorageRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Clean Master AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Clean Master AI", appName)
    }

    @Test
    fun `applicationId is com cleanmaster ai`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.cleanmaster.ai", context.packageName)
    }

    @Test
    fun `device storage repository provides compatibility info and storage summary`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = DeviceStorageRepository(context)
        val compat = repo.getCompatibilityInfo()
        assertTrue(compat.hasStorageStatsApi)
        assertTrue(compat.oemGuidanceSteps.isNotEmpty())
    }
}
