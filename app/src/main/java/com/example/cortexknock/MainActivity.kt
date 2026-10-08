package com.example.cortexknock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
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

    private var dispatcher: AudioDispatcher? = null
    @Volatile private var isRunning = false
    private var dispatcherThread: Thread? = null

    private val sampleRate = 44100
    private val bufferSize = 1024
    private val overlap = 512

    @Volatile private var knockFreqMin = 5500.0
    @Volatile private var knockFreqMax = 7500.0
    @Volatile private var thresholdFactor = 3.0
    @Volatile private var knockCooldownMs = 150L

    private var noiseFloor = 0.0
    private var knockCounter = 0
    private var lastKnockTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: démarrage de l'appli")

        try {
            setContentView(R.layout.activity_main)
        } catch (e: Throwable) {
            Log.e(TAG, "Erreur setContentView", e)
            throw e
        }

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
            isRunning = true
            tvStatus.text = getString(R.string.listening)
            btnStart.isEnabled = false
            btnStop.isEnabled = true

            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)
            dispatcher?.addAudioProcessor(FFTProcessor())
            dispatcherThread = Thread(dispatcher, "AudioDispatcher")
            dispatcherThread?.start()
            Log.d(TAG, "startListening: dispatcher démarré")
        } catch (e: Throwable) {
            Log.e(TAG, "startListening: erreur", e)
            isRunning = false
            tvStatus.text = "Erreur : ${e.message}"
            btnStart.isEnabled = true
            btnStop.isEnabled = false
        }
    }

    private fun stopListening() {
        isRunning = false
        try {
            dispatcher?.stop()
        } catch (e: Throwable) {
            Log.e(TAG, "stopListening: erreur stop", e)
        }
        dispatcher = null
        dispatcherThread?.join(500)
        dispatcherThread = null
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
