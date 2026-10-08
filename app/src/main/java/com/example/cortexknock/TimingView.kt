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
import kotlin.math.cos
import kotlin.math.sin

class TimingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_COURSE_MM = 54.0
    }

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
    private val statorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#795548")
        style = Paint.Style.FILL
    }
    private val statorEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val screwPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0BEC5")
        style = Paint.Style.FILL
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
    private val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }
    private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    private var angle = 20f
    private var isCw = true
    private var animatedAngle = 0f

    /** Course réglable via la jauge externe. */
    private var courseMm = DEFAULT_COURSE_MM

    private var sparkFlashAlpha = 0f
    private var sparkFlashedThisCycle = false
    private var lastAnimatedAngle = 0f

    private var pistonPosAtSparkMm = 0.0

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val newAngle = it.animatedValue as Float

            val sparkPosition = if (isCw) (360f - angle) % 360f else angle
            val crossed = crossedValue(lastAnimatedAngle, newAngle, sparkPosition)
            if (crossed && !sparkFlashedThisCycle) {
                sparkFlashAlpha = 1f
                sparkFlashedThisCycle = true
            }

            if (newAngle < lastAnimatedAngle && newAngle < 10f) {
                sparkFlashedThisCycle = false
            }

            lastAnimatedAngle = newAngle
            animatedAngle = newAngle

            if (sparkFlashAlpha > 0f) {
                sparkFlashAlpha -= 0.04f
                if (sparkFlashAlpha < 0f) sparkFlashAlpha = 0f
            }

            invalidate()
        }
    }

    private fun crossedValue(from: Float, to: Float, target: Float): Boolean {
        return if (from <= to) {
            target in from..to
        } else {
            target >= from || target <= to
        }
    }

    init {
        animator.start()
        recalcPistonAtSpark()
    }

    fun setAngle(deg: Int) {
        angle = deg.toFloat()
        recalcPistonAtSpark()
        invalidate()
    }

    fun setCourse(course: Double) {
        courseMm = course
        recalcPistonAtSpark()
        invalidate()
    }

    private fun recalcPistonAtSpark() {
        val rad = Math.toRadians(angle.toDouble())
        pistonPosAtSparkMm = (courseMm / 2.0) * (1.0 - cos(rad))
    }

    fun setClockwise(cw: Boolean) {
        isCw = cw
        sparkFlashedThisCycle = false
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

        val posText = "Piston à l'étincelle : %.1f mm avant PMH".format(pistonPosAtSparkMm)
        canvas.drawText(posText, w / 2, 40f, infoPaint)

        val cx = w / 2
        val cy = h * 0.42f
        val r = minOf(w, h) * 0.20f

        val rotation = if (isCw) animatedAngle else -animatedAngle

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        canvas.drawCircle(cx, cy, r, flywheelPaint)
        canvas.drawCircle(cx, cy, r, flywheelEdgePaint)
        canvas.drawRect(cx - 6f, cy - r, cx + 6f, cy - r + 30f, markPaint)
        canvas.restore()

        val statorRotation = if (isCw) -angle else angle
        canvas.save()
        canvas.rotate(statorRotation, cx, cy)

        val statorR = r * 0.7f
        canvas.drawCircle(cx, cy, statorR, statorPaint)
        canvas.drawCircle(cx, cy, statorR, statorEdgePaint)

        for (i in 0..2) {
            val screwAngle = Math.toRadians((i * 120 - 90).toDouble())
            val sx = cx + (statorR * 0.8f) * cos(screwAngle).toFloat()
            val sy = cy + (statorR * 0.8f) * sin(screwAngle).toFloat()
            canvas.drawCircle(sx, sy, 8f, screwPaint)
        }

        canvas.drawRect(cx - 4f, cy - statorR, cx + 4f, cy - statorR + 25f, statorEdgePaint)
        canvas.restore()

        val tdcR = r + 20f
        canvas.drawArc(
            RectF(cx - tdcR, cy - tdcR, cx + tdcR, cy + tdcR),
            -90f - 10f, 20f, false, tdcPaint
        )
        canvas.drawText("PMH", cx, cy - tdcR - 10f, labelPaint)

        val sparkPosition = if (isCw) (360f - angle) % 360f else angle
        val sparkRad = Math.toRadians((sparkPosition - 90).toDouble())
        val sparkX = cx + (r + 30f) * cos(sparkRad).toFloat()
        val sparkY = cy + (r + 30f) * sin(sparkRad).toFloat()

        if (sparkFlashAlpha > 0f) {
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.YELLOW
                alpha = (sparkFlashAlpha * 200).toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(sparkX, sparkY, 25f * sparkFlashAlpha, haloPaint)
        }

        canvas.drawCircle(sparkX, sparkY, 8f, sparkPaint)
        canvas.drawText("⚡", sparkX, sparkY - 15f, labelPaint)

        val advancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val advanceR = r + 40f
        val sweep = if (isCw) -angle else angle
        canvas.drawArc(
            RectF(cx - advanceR, cy - advanceR, cx + advanceR, cy + advanceR),
            -90f, sweep, false, advancePaint
        )

        canvas.drawText("${angle.toInt()}°", cx, cy + tdcR + 50f, textPaint)

        val pistonCx = w / 2
        val pistonTop = h * 0.68f
        val pistonBottom = h * 0.92f
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

        val angleFromTdc = if (isCw) animatedAngle else (360f - animatedAngle) % 360f
        val radFromTdc = Math.toRadians(angleFromTdc.toDouble())
        val instantPosMm = (courseMm / 2.0) * (1.0 - cos(radFromTdc))

        val strokeVisual = pistonBottom - pistonTop - 60f
        val strokeFactor = (instantPosMm / courseMm).toFloat()
        val pistonY = pistonTop + strokeFactor * strokeVisual

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
        canvas.drawLine(pistonCx, pistonY + 30f, pistonCx, pistonBottom + 20f, rodPaint)

        val dirText = if (isCw) "↻ Moteur horaire  |  Avance antihoraire"
                      else "↺ Moteur antihoraire  |  Avance horaire"
        canvas.drawText(dirText, w / 2, h - 20f, labelPaint)
    }
}
