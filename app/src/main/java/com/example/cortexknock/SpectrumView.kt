package com.example.cortexknock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SpectrumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint().apply {
        color = Color.CYAN
        strokeWidth = 2f
    }

    private val bandPaint = Paint().apply {
        color = Color.argb(60, 255, 80, 80)
        style = Paint.Style.FILL
    }

    private var amplitudes: FloatArray = FloatArray(0)
    private var sampleRate: Int = 44100
    private var bufferSize: Int = 1024

    private var knockMinHz: Double = 5500.0
    private var knockMaxHz: Double = 7500.0

    fun setKnockBand(minHz: Double, maxHz: Double) {
        knockMinHz = minHz
        knockMaxHz = maxHz
        postInvalidate()
    }

    fun updateSpectrum(spectrum: FloatArray, sampleRate: Int, bufferSize: Int) {
        this.amplitudes = spectrum.copyOf()
        this.sampleRate = sampleRate
        this.bufferSize = bufferSize
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        if (amplitudes.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val n = amplitudes.size
        val binWidth = sampleRate.toDouble() / bufferSize

        // Zone de la bande de frappe
        val startX = ((knockMinHz / (binWidth * n)) * w).toFloat().coerceIn(0f, w)
        val endX = ((knockMaxHz / (binWidth * n)) * w).toFloat().coerceIn(0f, w)
        canvas.drawRect(startX, 0f, endX, h, bandPaint)

        // Barres du spectre
        val maxAmp = amplitudes.maxOrNull() ?: 1f
        if (maxAmp <= 0f) return

        val barWidth = w / n
        for (i in 0 until n) {
            val amp = amplitudes[i] / maxAmp
            val barH = amp * h
            canvas.drawLine(
                i * barWidth,
                h,
                i * barWidth,
                h - barH,
                barPaint
            )
        }
    }
}
