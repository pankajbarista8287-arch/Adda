package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Handles real-time Android microphone audio capture (AudioRecord), speakerphone routing,
 * real-time speaking amplitude telemetry (0..100), and synthesized gift/level-up sound effects.
 * For cross-device WebRTC / Agora audio relay, configure AGORA_APP_ID per GUIDE_ADDA_SETUP.md.
 */
class VoiceAudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _speakingLevel = MutableStateFlow(0)
    val speakingLevel: StateFlow<Int> = _speakingLevel.asStateFlow()

    private val _speakerphoneEnabled = MutableStateFlow(true)
    val speakerphoneEnabled: StateFlow<Boolean> = _speakerphoneEnabled.asStateFlow()

    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var toneGenerator: ToneGenerator? = null

    @SuppressLint("MissingPermission")
    fun startMicCapture(scope: CoroutineScope) {
        if (_isCapturing.value) return
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        try {
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                Log.w(TAG, "AudioRecord failed to initialize")
                return
            }
            audioRecord = recorder
            configureVoiceRouting(_speakerphoneEnabled.value)
            recorder.startRecording()
            _isCapturing.value = true

            recordJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(minBuf / 2)
                while (isActive && _isCapturing.value) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sumSquares = 0.0
                        for (i in 0 until read) {
                            val s = buffer[i].toDouble()
                            sumSquares += s * s
                        }
                        val rms = sqrt(sumSquares / read)
                        val normalized = ((rms / 2500.0) * 100.0).toInt().coerceIn(0, 100)
                        _speakingLevel.value = normalized
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "RECORD_AUDIO permission missing: ${e.message}")
            stopMicCapture()
        } catch (e: Exception) {
            Log.e(TAG, "Audio capture error: ${e.message}", e)
            stopMicCapture()
        }
    }

    fun stopMicCapture() {
        _isCapturing.value = false
        _speakingLevel.value = 0
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
    }

    fun toggleSpeakerphone() {
        val next = !_speakerphoneEnabled.value
        _speakerphoneEnabled.value = next
        configureVoiceRouting(next)
    }

    private fun configureVoiceRouting(speakerOn: Boolean) {
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = speakerOn
        } catch (e: Exception) {
            Log.w(TAG, "Audio routing update skipped: ${e.message}")
        }
    }

    fun playGiftSound(animationType: String, enabled: Boolean) {
        if (!enabled) return
        try {
            val gen = toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 85).also {
                toneGenerator = it
            }
            when (animationType) {
                "FULLSCREEN_ROYAL" -> {
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    gen.startTone(ToneGenerator.TONE_DTMF_1, 140)
                    handler.postDelayed({ runCatching { gen.startTone(ToneGenerator.TONE_DTMF_5, 140) } }, 150L)
                    handler.postDelayed({ runCatching { gen.startTone(ToneGenerator.TONE_DTMF_9, 160) } }, 310L)
                    handler.postDelayed({ runCatching { gen.startTone(ToneGenerator.TONE_PROP_ACK, 340) } }, 490L)
                }
                "ORBIT_3D" -> {
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    gen.startTone(ToneGenerator.TONE_PROP_BEEP, 130)
                    handler.postDelayed({ runCatching { gen.startTone(ToneGenerator.TONE_PROP_BEEP2, 220) } }, 150L)
                }
                else -> gen.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
            }
        } catch (_: Exception) {
        }
    }

    fun playLevelUpSound(enabled: Boolean) {
        if (!enabled) return
        try {
            val gen = toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 80).also {
                toneGenerator = it
            }
            gen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 320)
        } catch (_: Exception) {
        }
    }

    fun release() {
        stopMicCapture()
        try {
            toneGenerator?.release()
        } catch (_: Exception) {
        }
        toneGenerator = null
        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val TAG = "VoiceAudioEngine"
    }
}
