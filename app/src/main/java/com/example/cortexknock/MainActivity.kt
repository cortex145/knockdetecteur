package com.example.cortexknock

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.util.fft.FFT
import kotlin.math.cos

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CortexKnock"
        private const val REQ_AUDIO = 1
    }

    private lateinit var tvStatus: TextView
    private lateinit var tvCounter: TextView
    private lateinit var tvInfo: TextView
    private lateinit var spectrumView: SpectrumView

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private var audioRecord: AudioRecord? = null
    @Volatile private var isRunning = false
    private var audioThread: Thread? = null

    private val sampleRate = 44100
    private val bufferSize = 1024
    private val overlap = 512

    @Volatile private var knockFreqMin = 7500.0
    @Volatile private var knockFreqMax = 8500.0
    @Volatile private var thresholdFactor = 3.0
    @Volatile private var knockCooldownMs = 150L

    private var noiseFloor = 0.0
    private var knockCounter = 0
    private var lastKnockTime = 0L

    private val fftProcessor = FFTProcessor()

    // Format audio réutilisé pour les AudioEvent
    private val audioFormat = TarsosDSPAudioFormat(
        sampleRate.toFloat(),
        16,
        1,
        true,
        false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: démarrage de l'appli")
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvCounter = findViewById(R.id.tvCounter)
        tvInfo = findViewById(R.id.tvInfo)
        spectrumView = findViewById(R.id.spectrumView)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        tvCounter.text = "${getString(R.string.knock_counter)}0"

        btnStart.setOnClickListener { startListening() }
        btnStop.setOnClickListener { stopListening() }
        findViewById<Button>(R.id.btnReset).setOnClickListener {
            knockCounter = 0
            tvCounter.text = "${getString(R.string.knock_counter)}0"
        }

        setupSeekBars()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
        }
    }

    private fun setupSeekBars() {
        val lblSens = findViewById<TextView>(R.id.lblSens)
        findViewById<SeekBar>(R.id.sbSens).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                thresholdFactor = 1.5 + (progress / 100.0) * 8.5
                lblSens.text = "Sensibilité : %.1f".format(thresholdFactor)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val lblFmin = findViewById<TextView>(R.id.lblFmin)
        findViewById<SeekBar>(R.id.sbFmin).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val fmin = 2000.0 + (progress / 100.0) * 8000.0
                if (fmin < knockFreqMax - 200) knockFreqMin = fmin
                lblFmin.text = "Fréquence basse : ${knockFreqMin.toInt()} Hz"
                spectrumView.setKnockBand(knockFreqMin, knockFreqMax)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val lblFmax = findViewById<TextView>(R.id.lblFmax)
        findViewById<SeekBar>(R.id.sbFmax).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val fmax = 3000.0 + (progress / 100.0) * 9000.0
                if (fmax > knockFreqMin + 200) knockFreqMax = fmax
                lblFmax.text = "Fréquence haute : ${knockFreqMax.toInt()} Hz"
                spectrumView.setKnockBand(knockFreqMin, knockFreqMax)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val lblCooldown = findViewById<TextView>(R.id.lblCooldown)
        findViewById<SeekBar>(R.id.sbCooldown).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                knockCooldownMs = (30 + (progress / 100.0) * 470).toLong()
                lblCooldown.text = "Anti-rebond : ${knockCooldownMs} ms"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun startListening() {
        if (isRunning) return
        if (!hasAudioPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
            return
        }

        try {
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val recBufSize = maxOf(minBuf, bufferSize * 4)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                tvStatus.text = "Erreur : micro non initialisé"
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRunning = true
            tvStatus.text = getString(R.string.listening)
            btnStart.isEnabled = false
            btnStop.isEnabled = true

            audioThread = Thread {
                val shortBuffer = ShortArray(bufferSize)
                val floatBuffer = FloatArray(bufferSize)

                while (isRunning) {
                    val read = audioRecord?.read(shortBuffer, 0, bufferSize) ?: 0
                    if (read <= 0) {
                        try { Thread.sleep(10) } catch (_: InterruptedException) {}
                        continue
                    }

                    // Convertir Short → Float (échelle [-1, 1])
                    for (i in 0 until read) {
                        floatBuffer[i] = shortBuffer[i] / 32768.0f
                    }

                    // Construire un AudioEvent compatible TarsosDSP 2.5
                    try {
                        val event = AudioEvent(audioFormat)
                        event.setFloatBuffer(floatBuffer)
                        fftProcessor.process(event)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Erreur traitement audio", e)
                    }
                }

                Log.d(TAG, "Boucle audio terminée")
            }.also { it.name = "AudioLoop"; it.start() }

            Log.d(TAG, "startListening: micro démarré")
        } catch (e: Throwable) {
            Log.e(TAG, "startListening: erreur", e)
            isRunning = false
            tvStatus.text = "Erreur : ${e.message}"
            btnStart.isEnabled = true
            btnStop.isEnabled = false
            audioRecord?.release()
            audioRecord = null
        }
    }

    private fun stopListening() {
        isRunning = false
        try {
            audioRecord?.stop()
        } catch (_: Throwable) {}
        try {
            audioRecord?.release()
        } catch (_: Throwable) {}
        audioRecord = null

        audioThread?.join(500)
        audioThread = null

        tvStatus.text = getString(R.string.stopped)
        btnStart.isEnabled = true
        btnStop.isEnabled = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission micro accordée")
            } else {
                tvStatus.text = "Permission micro refusée"
            }
        }
    }

    inner class FFTProcessor : AudioProcessor {
        private val fft = FFT(bufferSize)
        private val amplitudes = FloatArray(bufferSize / 2)
        private var frameCount = 0

        override fun process(audioEvent: AudioEvent): Boolean {
            val buffer = audioEvent.floatBuffer

            for (i in buffer.indices) {
                val w = 0.5 * (1.0 - cos(2.0 * Math.PI * i / (buffer.size - 1)))
                buffer[i] *= w.toFloat()
            }

            fft.forwardTransform(buffer)
            fft.modulus(buffer, amplitudes)

            val localMin = knockFreqMin
            val localMax = knockFreqMax
            val localThresh = thresholdFactor
            val localCooldown = knockCooldownMs

            val knockEnergy = computeBandEnergy(amplitudes, localMin, localMax)
            val totalEnergy = computeTotalEnergy(amplitudes)

            noiseFloor = if (frameCount < 10) totalEnergy
            else 0.95 * noiseFloor + 0.05 * totalEnergy

            val now = System.currentTimeMillis()
            val knockDetected = knockEnergy > localThresh * noiseFloor &&
                knockEnergy > 0.01 &&
                (now - lastKnockTime) > localCooldown

            if (knockDetected) {
                lastKnockTime = now
                knockCounter++
                runOnUiThread {
                    tvCounter.text = "${getString(R.string.knock_counter)}$knockCounter"
                    tvStatus.text = getString(R.string.knock_detected)
                }
            } else {
                runOnUiThread {
                    if (isRunning) tvStatus.text = getString(R.string.listening)
                    tvInfo.text = "Bruit: %.4f  |  Bande: %.4f".format(noiseFloor, knockEnergy)
                }
            }

            spectrumView.updateSpectrum(amplitudes, sampleRate, bufferSize)

            frameCount++
            return true
        }

        override fun processingFinished() {}

        private fun computeBandEnergy(spectrum: FloatArray, minFreq: Double, maxFreq: Double): Double {
            val binWidth = sampleRate.toDouble() / bufferSize
            val startBin = (minFreq / binWidth).toInt().coerceIn(0, spectrum.size - 1)
            val endBin = (maxFreq / binWidth).toInt().coerceIn(0, spectrum.size - 1)
            var energy = 0.0
            for (i in startBin..endBin) energy += spectrum[i].toDouble() * spectrum[i]
            return energy
        }

        private fun computeTotalEnergy(spectrum: FloatArray): Double {
            var energy = 0.0
            for (v in spectrum) energy += v.toDouble() * v
            return energy
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
    }
}
