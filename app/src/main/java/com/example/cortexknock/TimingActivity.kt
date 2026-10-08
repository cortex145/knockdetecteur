package com.example.cortexknock

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TimingActivity : AppCompatActivity() {

    companion object {
        private const val PREFS = "cortex_timing_prefs"
        private const val KEY_ANGLE = "saved_angle"
        private const val KEY_DIRECTION = "saved_direction"
        private const val KEY_HISTORY = "feel_history"
    }

    private lateinit var timingView: TimingView
    private lateinit var tvAngle: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvHistory: TextView

    private var currentAngle = 20
    private var currentCw = true
    private var lastWarningPlayed = false

    private val toneGenerator: ToneGenerator by lazy {
        ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timing)

        timingView = findViewById(R.id.timingView)
        tvAngle = findViewById(R.id.tvAngle)
        tvInfo = findViewById(R.id.tvInfo)
        tvHistory = findViewById(R.id.tvHistory)

        val sbAngle = findViewById<SeekBar>(R.id.sbAngle)
        sbAngle.max = 35

        // Charger le réglage sauvegardé
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentAngle = prefs.getInt(KEY_ANGLE, 20)
        currentCw = prefs.getBoolean(KEY_DIRECTION, true)

        sbAngle.progress = currentAngle
        timingView.setAngle(currentAngle)
        timingView.setDirection(currentCw)

        updateUi()

        sbAngle.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentAngle = progress
                timingView.setAngle(progress)
                updateUi()

                // ✅ Son d'alerte si avance > 30°
                if (progress > 30 && !lastWarningPlayed) {
                    playWarningTone()
                    lastWarningPlayed = true
                } else if (progress <= 30) {
                    lastWarningPlayed = false
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        findViewById<Button>(R.id.btnCw).setOnClickListener {
            currentCw = true
            timingView.setDirection(true)
            updateUi()
            playClickTone()
        }

        findViewById<Button>(R.id.btnCcw).setOnClickListener {
            currentCw = false
            timingView.setDirection(false)
            updateUi()
            playClickTone()
        }

        // ✅ Sauvegarde
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.edit()
                .putInt(KEY_ANGLE, currentAngle)
                .putBoolean(KEY_DIRECTION, currentCw)
                .apply()
            Toast.makeText(
                this,
                getString(R.string.timing_saved, currentAngle),
                Toast.LENGTH_SHORT
            ).show()
            playClickTone()
        }

        // ✅ Boutons de ressenti
        findViewById<Button>(R.id.btnBien).setOnClickListener {
            saveFeel("Bien", currentAngle)
        }
        findViewById<Button>(R.id.btnMoyen).setOnClickListener {
            saveFeel("Moyen", currentAngle)
        }
        findViewById<Button>(R.id.btnMax).setOnClickListener {
            saveFeel("Max", currentAngle)
        }

        // ✅ Historique
        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            showHistory()
        }
        findViewById<Button>(R.id.btnClearHistory).setOnClickListener {
            prefs.edit().remove(KEY_HISTORY).apply()
            tvHistory.text = getString(R.string.feel_empty)
            Toast.makeText(this, getString(R.string.feel_cleared), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUi() {
        tvAngle.text = getString(R.string.timing_angle, currentAngle)
        tvInfo.text = when {
            currentAngle < 10 -> "⚠️ Avance faible — moteur peu performant"
            currentAngle in 10..20 -> "✅ Avance modérée — usage enduro / bas régime"
            currentAngle in 21..30 -> "✅ Avance sportive — usage cross / mi-régime"
            else -> "⚠️ Avance élevée — risque de cliquetis !"
        }
    }

    private fun saveFeel(label: String, angle: Int) {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val timestamp = System.currentTimeMillis()
        val entry = "$timestamp|$angle|$label"

        val old = prefs.getString(KEY_HISTORY, "") ?: ""
        val new = if (old.isEmpty()) entry else "$old\n$entry"
        prefs.edit().putString(KEY_HISTORY, new).apply()

        Toast.makeText(
            this,
            getString(R.string.feel_saved, label, angle),
            Toast.LENGTH_SHORT
        ).show()
        playClickTone()
        showHistory()
    }

    private fun showHistory() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        if (raw.isEmpty()) {
            tvHistory.text = getString(R.string.feel_empty)
            return
        }

        val lines = raw.split("\n").filter { it.isNotEmpty() }
        val sb = StringBuilder()
        lines.reversed().forEach { line ->
            val parts = line.split("|")
            if (parts.size == 3) {
                val angle = parts[1]
                val label = parts[2]
                sb.append("• $angle° → $label\n")
            }
        }
        tvHistory.text = sb.toString().trim()
    }

    private fun playClickTone() {
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (_: Throwable) {}
    }

    private fun playWarningTone() {
        try {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { toneGenerator.release() } catch (_: Throwable) {}
    }
}
