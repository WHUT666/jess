package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SequencingMode
import com.example.engine.ArenaJsBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ArenaPilot", appName)
    }

    @Test
    fun `verify sequencing modes are defined`() {
        assertEquals(4, SequencingMode.values().size)
        assertNotNull(SequencingMode.SEQUENTIAL.displayName)
    }

    @Test
    fun `verify bridge instantiates properly`() {
        val bridge = ArenaJsBridge()
        assertNotNull(bridge.events)
    }
}
