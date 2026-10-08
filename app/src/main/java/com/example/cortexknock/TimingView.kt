package com.example.cortexknock

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class TimingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E")
        style = Paint.Style.FILL
    }
    private val flywheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F51B5")
        style = Paint.Style.FILL
    }
    private val flywheelEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val tdcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F44336")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val pistonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0BEC5")
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    private var angle = 20f
    private var clockwise = true
    private var animatedAngle = 0f

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            animatedAngle = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        animator.start()
    }

    fun setAngle(deg: Int) {
        angle = deg.toFloat()
        invalidate()
    }

    fun setDirection(cw: Boolean) {
        clockwise = cw
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val flywheelCx = w / 2
        val flywheelCy = h * 0.35f
        val flywheelR = minOf(w, h) * 0.22f

        val rotation = if (clockwise) animatedAngle else -animatedAngle

        canvas.save()
        canvas.rotate(rotation, flywheelCx, flywheelCy)
        canvas.drawCircle(flywheelCx, flywheelCy, flywheelR, flywheelPaint)
        canvas.drawCircle(flywheelCx, flywheelCy, flywheelR, flywheelEdgePaint)

        val markPaint2 = Paint(markPaint).apply { color = Color.WHITE }
        canvas.drawRect(
            flywheelCx - 6f,
            flywheelCy - flywheelR,
            flywheelCx + 6f,
            flywheelCy - flywheelR + 30f,
            markPaint2
        )

        val markPaint3 = Paint(markPaint).apply { color = Color.parseColor("#FFD54F") }
        canvas.drawRect(
            flywheelCx - 4f,
            flywheelCy + flywheelR - 30f,
            flywheelCx + 4f,
            flywheelCy + flywheelR,
            markPaint3
        )
        canvas.restore()

        val tdcR = flywheelR + 20f
        canvas.drawArc(
            RectF(flywheelCx - tdcR, flywheelCy - tdcR, flywheelCx + tdcR, flywheelCy + tdcR),
            -90f - 10f,
            20f,
            false,
            tdcPaint
        )

        canvas.drawText("PMH", flywheelCx, flywheelCy - tdcR - 10f, labelPaint)

        val advancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val advanceR = flywheelR + 40f
        val sweep = if (clockwise) angle else -angle
        canvas.drawArc(
            RectF(flywheelCx - advanceR, flywheelCy - advanceR, flywheelCx + advanceR, flywheelCy + advanceR),
            -90f,
            sweep,
            false,
            advancePaint
        )

        canvas.drawText("${angle.toInt()}°", flywheelCx, flywheelCy + tdcR + 50f, textPaint)

        val pistonCx = w / 2
        val pistonTop = h * 0.65f
        val pistonBottom = h * 0.9f
        val pistonWidth = w * 0.3f

        val cylinderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#455A64")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val cylinderRect = RectF(
            pistonCx - pistonWidth / 2 - 20f,
            pistonTop - 30f,
            pistonCx + pistonWidth / 2 + 20f,
            pistonBottom + 20f
        )
        canvas.drawRoundRect(cylinderRect, 10f, 10f, cylinderPaint)

        val angleRad = Math.toRadians(angle.toDouble())
        val pistonOffset = (kotlin.math.cos(angleRad) * 30).toFloat()
        val pistonY = pistonTop + pistonOffset

        val pistonRect = RectF(
            pistonCx - pistonWidth / 2,
            pistonY,
            pistonCx + pistonWidth / 2,
            pistonY + 60f
        )
        canvas.drawRoundRect(pistonRect, 8f, 8f, pistonPaint)

        val rodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0BEC5")
            strokeWidth = 8f
        }
        canvas.drawLine(pistonCx, pistonY + 30f, pistonCx, pistonBottom, rodPaint)

        val dirText = if (clockwise) "↻ Horaire (avance)" else "↺ Antihoraire (retard)"
        canvas.drawText(dirText, w / 2, h - 20f, labelPaint)
    }
}
