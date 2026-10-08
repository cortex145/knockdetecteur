package com.example.knockdetector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SpectrumView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barPaint = Paint().apply { color = Color.CYAN }
    private val knockPaint = Paint().apply { color = Color.RED }
    private val gridPaint = Paint().apply {
        color = Color.DKGRAY
        strokeWidth = 1f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        isAntiAlias = true
    }

    private var spectrum: FloatArray = FloatArray(0)
    private var sampleRate: Int = 44100
    private var bufferSize: Int = 1024
    @Volatile private var knockMinHz: Double = 5500.0
    @Volatile private var knockMaxHz: Double = 7500.0

    fun updateSpectrum(values: FloatArray, sampleRate: Int, bufferSize: Int) {
        this.spectrum = values.copyOf()
        this.sampleRate = sampleRate
        this.bufferSize = bufferSize
        postInvalidate()
    }

    fun setKnockBand(minHz: Double, maxHz: Double) {
        this.knockMinHz = minHz
        this.knockMaxHz = maxHz
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        val maxFreqDisplay = 12000.0
        val binWidth = sampleRate.toDouble() / bufferSize
        val maxBin = (maxFreqDisplay / binWidth).toInt().coerceAtMost(spectrum.size)

        for (freq in 0..12000 step 1000) {
            val x = (freq / maxFreqDisplay * w).toFloat()
            canvas.drawLine(x, 0f, x, h, gridPaint)
            canvas.drawText("${freq / 1000}k", x + 4f, 28f, textPaint)
        }

        if (spectrum.isEmpty()) return

        var maxVal = 0.0001f
        for (i in 0 until maxBin) {
            if (spectrum[i] > maxVal) maxVal = spectrum[i]
        }

        val barWidth = w / maxBin
        for (i in 0 until maxBin) {
            val freq = i * binWidth
            val amplitude = spectrum[i] / maxVal
            val barHeight = amplitude * h

            val paint = if (freq in knockMinHz..knockMaxHz) knockPaint else barPaint
            canvas.drawRect(
                i * barWidth,
                h - barHeight,
                (i + 1) * barWidth,
                h,
                paint
            )
        }

        val bandPaint = Paint().apply {
            color = Color.RED
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        val xMin = (knockMinHz / maxFreqDisplay * w).toFloat()
        val xMax = (knockMaxHz / maxFreqDisplay * w).toFloat()
        canvas.drawLine(xMin, 0f, xMin, h, bandPaint)
        canvas.drawLine(xMax, 0f, xMax, h, bandPaint)
    }
}
