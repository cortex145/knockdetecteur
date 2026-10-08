package com.example.cortexknock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.log10

class SpectrumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private var amplitudes = FloatArray(0)
    private var sampleRate = 44100
    private var bufferSize = 1024
    private var knockFreqMin = 5500.0
    private var knockFreqMax = 7500.0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Draw background
        paint.color = Color.BLACK
        canvas.drawRect(0f, 0f, width, height, paint)

        if (amplitudes.isEmpty()) return

        val binWidth = sampleRate.toDouble() / bufferSize
        val startBin = (knockFreqMin / binWidth).toInt().coerceIn(0, amplitudes.size - 1)
        val endBin = (knockFreqMax / binWidth).toInt().coerceIn(0, amplitudes.size - 1)

        // Draw waveform
        paint.color = Color.GREEN
        paint.strokeWidth = 2f

        val pixelWidth = width / amplitudes.size
        for (i in amplitudes.indices) {
            val x = i * pixelWidth
            val magnitude = amplitudes[i]
            val normalizedMagnitude = (log10((magnitude + 1).toDouble()) / 5f).coerceIn(0f, 1f)
            val y = height - (normalizedMagnitude * height)

            if (i == 0) {
                canvas.drawPoint(x, y, paint)
            } else {
                val prevMagnitude = amplitudes[i - 1]
                val prevNormalized = (log10((prevMagnitude + 1).toDouble()) / 5f).coerceIn(0f, 1f)
                val prevY = height - (prevNormalized * height)
                canvas.drawLine(x - pixelWidth, prevY, x, y, paint)
            }
        }

        // Draw knock band
        paint.color = Color.RED
        paint.strokeWidth = 3f
        val startX = startBin * pixelWidth
        val endX = endBin * pixelWidth
        canvas.drawLine(startX, 0f, startX, height, paint)
        canvas.drawLine(endX, 0f, endX, height, paint)
    }

    fun updateSpectrum(amplitudes: FloatArray, sampleRate: Int, bufferSize: Int) {
        this.amplitudes = amplitudes
        this.sampleRate = sampleRate
        this.bufferSize = bufferSize
        invalidate()
    }

    fun setKnockBand(minFreq: Double, maxFreq: Double) {
        this.knockFreqMin = minFreq
        this.knockFreqMax = maxFreq
        invalidate()
    }
}
