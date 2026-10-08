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

        paint.color = Color.BLACK
        canvas.drawRect(0f, 0f, width, height, paint)

        if (amplitudes.isEmpty()) return

        val binWidth = sampleRate.toDouble() / bufferSize.toDouble()
        val startBin = (knockFreqMin / binWidth).toInt().coerceIn(0, amplitudes.size - 1)
        val endBin = (knockFreqMax / binWidth).toInt().coerceIn(0, amplitudes.size - 1)

        paint.color = Color.GREEN
        paint.strokeWidth = 2f

        val pixelWidth = width / amplitudes.size.toFloat()
        for (i in amplitudes.indices) {
            val x = i * pixelWidth
            val magnitude = amplitudes[i].toDouble()
            val normalizedMagnitude = ((log10(magnitude + 1.0) / log10(10.0)) * 0.2).coerceIn(0.0, 1.0)
            val y = height - (normalizedMagnitude * height).toFloat()

            if (i == 0) {
                canvas.drawPoint(x, y, paint)
            } else {
                val prevMagnitude = amplitudes[i - 1].toDouble()
                val prevNormalized = ((log10(prevMagnitude + 1.0) / log10(10.0)) * 0.2).coerceIn(0.0, 1.0)
                val prevY = height - (prevNormalized * height).toFloat()
                canvas.drawLine(x - pixelWidth, prevY, x, y, paint)
            }
        }

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
