package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Manages playback of high-contrast 'New-School' comic-style sound effects
 * for message transmission and reception using Android's MediaPlayer.
 *
 * - Sent Sound: Punchy comic 'woosh-pop' with rapid pitch slide and snappy release.
 * - Received Sound: Stylish dual-tone comic chime with harmonics.
 */
class NewSchoolSoundEffectManager(private val context: Context) {

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled

    private var activePlayer: MediaPlayer? = null

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
    }

    fun toggleSound() {
        _isSoundEnabled.value = !_isSoundEnabled.value
    }

    /**
     * Plays the new-school 'message sent' sound effect using MediaPlayer.
     */
    fun playSentSound() {
        if (!_isSoundEnabled.value) return
        playSound(isSent = true)
    }

    /**
     * Plays the new-school 'message received' sound effect using MediaPlayer.
     */
    fun playReceivedSound() {
        if (!_isSoundEnabled.value) return
        playSound(isSent = false)
    }

    private fun playSound(isSent: Boolean) {
        try {
            // Stop and release any currently playing sound effect
            releaseCurrentPlayer()

            val rawResId = if (isSent) R.raw.sound_message_sent else R.raw.sound_message_received
            var player: MediaPlayer? = null

            try {
                player = MediaPlayer.create(context, rawResId)
            } catch (e: Exception) {
                Log.w(TAG, "MediaPlayer.create with raw resource failed: ${e.message}, falling back to cache file")
            }

            // Fallback to generated WAV if raw resource creation failed
            if (player == null) {
                val fallbackFile = getOrCreateFallbackWav(isSent)
                if (fallbackFile != null && fallbackFile.exists()) {
                    player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                .build()
                        )
                        setDataSource(fallbackFile.absolutePath)
                        prepare()
                    }
                }
            }

            player?.let { mp ->
                activePlayer = mp
                mp.setOnCompletionListener {
                    try {
                        it.release()
                    } catch (_: Exception) {}
                    if (activePlayer == it) {
                        activePlayer = null
                    }
                }
                mp.setOnErrorListener { it, _, _ ->
                    try {
                        it.release()
                    } catch (_: Exception) {}
                    if (activePlayer == it) {
                        activePlayer = null
                    }
                    true
                }
                mp.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound effect (isSent=$isSent): ${e.message}")
        }
    }

    private fun releaseCurrentPlayer() {
        try {
            activePlayer?.stop()
            activePlayer?.release()
        } catch (_: Exception) {}
        activePlayer = null
    }

    fun release() {
        releaseCurrentPlayer()
    }

    private fun getOrCreateFallbackWav(isSent: Boolean): File? {
        return try {
            val fileName = if (isSent) "newschool_sent.wav" else "newschool_received.wav"
            val file = File(context.cacheDir, fileName)
            if (!file.exists()) {
                if (isSent) {
                    generateSentWav(file)
                } else {
                    generateReceivedWav(file)
                }
            }
            file
        } catch (e: Exception) {
            Log.w(TAG, "Error generating fallback WAV: ${e.message}")
            null
        }
    }

    private fun generateSentWav(file: File) {
        val sampleRate = 22050
        val duration = 0.16
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val normT = t / duration
            val freq = 420.0 + 930.0 * Math.pow(normT, 1.8)
            val phase = 2.0 * Math.PI * freq * t
            val env = if (normT < 0.1) normT / 0.1 else Math.exp(-6.0 * (normT - 0.1))
            var sampleVal = (Math.sin(phase) * 0.7 + Math.sin(phase * 2.0) * 0.2 + Math.sin(phase * 3.0) * 0.1) * env
            if (normT < 0.04) {
                sampleVal += Math.sin(2.0 * Math.PI * 1800.0 * t) * (1.0 - normT / 0.04) * 0.25
            }
            buffer[i] = (Math.max(-1.0, Math.min(1.0, sampleVal)) * 32767 * 0.9).toInt().toShort()
        }

        writeWavFile(file, buffer, sampleRate)
    }

    private fun generateReceivedWav(file: File) {
        val sampleRate = 22050
        val duration = 0.28
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sampleVal = 0.0
            if (t < 0.14) {
                val env1 = Math.exp(-12.0 * t)
                sampleVal += (Math.sin(2.0 * Math.PI * 659.25 * t) * 0.7 + Math.sin(2.0 * Math.PI * 1318.5 * t) * 0.2) * env1
            }
            if (t >= 0.09) {
                val t2 = t - 0.09
                val env2 = Math.exp(-9.0 * t2)
                sampleVal += (Math.sin(2.0 * Math.PI * 987.77 * t2) * 0.8 + Math.sin(2.0 * Math.PI * 1975.5 * t2) * 0.25) * env2
            }
            buffer[i] = (Math.max(-1.0, Math.min(1.0, sampleVal)) * 32767 * 0.9).toInt().toShort()
        }

        writeWavFile(file, buffer, sampleRate)
    }

    private fun writeWavFile(file: File, buffer: ShortArray, sampleRate: Int) {
        val numSamples = buffer.size
        val byteBuffer = ByteBuffer.allocate(44 + numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put("RIFF".toByteArray())
        byteBuffer.putInt(36 + numSamples * 2)
        byteBuffer.put("WAVE".toByteArray())
        byteBuffer.put("fmt ".toByteArray())
        byteBuffer.putInt(16) // Subchunk1Size
        byteBuffer.putShort(1) // PCM
        byteBuffer.putShort(1) // Mono
        byteBuffer.putInt(sampleRate)
        byteBuffer.putInt(sampleRate * 2)
        byteBuffer.putShort(2)
        byteBuffer.putShort(16)
        byteBuffer.put("data".toByteArray())
        byteBuffer.putInt(numSamples * 2)
        for (sample in buffer) {
            byteBuffer.putShort(sample)
        }
        FileOutputStream(file).use { it.write(byteBuffer.array()) }
    }

    companion object {
        private const val TAG = "NewSchoolSoundEffects"
    }
}
