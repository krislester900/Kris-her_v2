package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlaybackState
import com.example.audio.NewSchoolSoundEffectManager
import com.example.audio.VoiceAudioManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel dédié à la gestion de l'audio (enregistrement de notes vocales,
 * lecture, effets sonores).
 *
 * Extrait de ChatViewModel (P1-3b) pour séparer les responsabilités :
 * ChatViewModel gère les conversations/messages, AudioManagerViewModel gère l'audio.
 *
 * L'envoi effectif d'un message vocal (Firestore) reste du ressort de
 * ChatViewModel, qui branchera sur [onAudioReadyToSend].
 */
class AudioManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val voiceAudioManager = VoiceAudioManager(application)
    private val soundEffectManager = NewSchoolSoundEffectManager(application)

    val audioPlaybackState: StateFlow<AudioPlaybackState> = voiceAudioManager.playbackState
    val isSoundEffectsEnabled: StateFlow<Boolean> = soundEffectManager.isSoundEnabled

    // État d'enregistrement
    val isRecordingAudio = MutableStateFlow(false)
    val isRecordingPaused = MutableStateFlow(false)
    val recordingDurationSec = MutableStateFlow(0)
    val recordingAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    private var recordTimerJob: Job? = null

    /// Callback appelé quand un enregistrement est prêt à être envoyé.
    /// Le ChatViewModel y branche sa propre logique d'envoi (sendMessage).
    var onAudioReadyToSend: ((String, Int, String?) -> Unit)? = null

    // ------------------------------------------------------------------
    //  Enregistrement
    // ------------------------------------------------------------------

    fun startAudioRecording() {
        voiceAudioManager.stopAudio()
        voiceAudioManager.startRecording()
        isRecordingAudio.value = true
        isRecordingPaused.value = false
        recordingDurationSec.value = 0
        recordingAmplitudes.value = emptyList()
        recordTimerJob?.cancel()
        recordTimerJob = viewModelScope.launch {
            var tickCount = 0
            while (isRecordingAudio.value) {
                delay(100)
                if (!isRecordingPaused.value) {
                    val amp = voiceAudioManager.getLiveAmplitude()
                    val currentList = recordingAmplitudes.value.toMutableList()
                    currentList.add(amp)
                    if (currentList.size > 28) currentList.removeAt(0)
                    recordingAmplitudes.value = currentList

                    tickCount++
                    if (tickCount % 10 == 0) {
                        recordingDurationSec.value += 1
                    }
                }
            }
        }
    }

    fun togglePauseRecording() {
        if (!isRecordingAudio.value) return
        if (isRecordingPaused.value) {
            voiceAudioManager.resumeRecording()
            isRecordingPaused.value = false
        } else {
            voiceAudioManager.pauseRecording()
            isRecordingPaused.value = true
        }
    }

    /**
     * Arrête l'enregistrement et retourne (filePath, durationSec).
     * L'appelant (ChatViewModel) est responsable d'envoyer le message via Firestore.
     */
    fun stopRecording(): Pair<String?, Int> {
        val timerDuration = recordingDurationSec.value
        recordTimerJob?.cancel()
        isRecordingAudio.value = false
        isRecordingPaused.value = false
        val (filePath, fileDuration) = voiceAudioManager.stopRecording()
        val finalDuration = if (fileDuration > 0) fileDuration else timerDuration.coerceAtLeast(1)
        recordingDurationSec.value = 0
        recordingAmplitudes.value = emptyList()
        return Pair(filePath, finalDuration)
    }

    fun cancelAudioRecording() {
        voiceAudioManager.cancelRecording()
        recordTimerJob?.cancel()
        isRecordingAudio.value = false
        isRecordingPaused.value = false
        recordingDurationSec.value = 0
        recordingAmplitudes.value = emptyList()
    }

    // ------------------------------------------------------------------
    //  Lecture
    // ------------------------------------------------------------------

    fun playAudio(messageId: Long, mediaUri: String?, durationSec: Int) {
        voiceAudioManager.playAudio(messageId, mediaUri, durationSec)
    }

    // ------------------------------------------------------------------
    //  Effets sonores
    // ------------------------------------------------------------------

    fun toggleSoundEffects() {
        soundEffectManager.toggleSound()
    }

    fun playSentSoundEffect() {
        soundEffectManager.playSentSound()
    }

    fun playReceivedSoundEffect() {
        soundEffectManager.playReceivedSound()
    }

    fun stopAudio() {
        voiceAudioManager.stopAudio()
    }

    fun release() {
        soundEffectManager.release()
    }

    override fun onCleared() {
        super.onCleared()
        voiceAudioManager.stopAudio()
        soundEffectManager.release()
    }

    private fun formatSeconds(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "%d:%02d".format(m, s)
    }
}
