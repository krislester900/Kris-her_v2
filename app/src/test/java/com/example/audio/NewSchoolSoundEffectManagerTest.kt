package com.example.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewSchoolSoundEffectManagerTest {

    @Test
    fun testSoundEffectsToggleAndPlayback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = NewSchoolSoundEffectManager(context)

        // Default state should be enabled
        assertTrue("Sound effects should be enabled by default", manager.isSoundEnabled.value)

        // Play sent and received sound effect calls should execute gracefully
        manager.playSentSound()
        manager.playReceivedSound()

        // Toggle sound effects off
        manager.toggleSound()
        assertFalse("Sound effects should be disabled after toggle", manager.isSoundEnabled.value)

        // Playing while disabled should be a no-op
        manager.playSentSound()
        manager.playReceivedSound()

        // Re-enable and release
        manager.setSoundEnabled(true)
        assertTrue(manager.isSoundEnabled.value)
        manager.release()
    }
}
