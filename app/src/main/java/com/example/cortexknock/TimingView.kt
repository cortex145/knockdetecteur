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

    private var angle = 20f
    private var isCw = true
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

    fun setClockwise(cw: Boolean) {
        isCw = cw
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

        val cx = w / 2
        val cy = h * 0.35f
        val r = minOf(w, h) * 0.22f

        // === Volant magnétique : rotation moteur ===
        val rotation = if (isCw) animatedAngle else -animatedAngle

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        canvas.drawCircle(cx, cy, r, flywheelPaint)
        canvas.drawCircle(cx, cy, r, flywheelEdgePaint)
        canvas.drawRect(cx - 6f, cy - r, cx + 6f, cy - r + 30f, markPaint)
        canvas.restore()

        // === Stator : rotation inverse (avance / retard) ===
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

        // === Cercle PMH fixe ===
        val tdcR = r + 20f
        canvas.drawArc(
            RectF(cx - tdcR, cy - tdcR, cx + tdcR, cy + tdcR),
            -90f - 10f, 20f, false, tdcPaint
        )
        canvas.drawText("PMH", cx, cy - tdcR - 10f, labelPaint)

        // === Arc d'avance ===
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

        // === Flèche de rotation du stator ===
        val arrowRadius = r + 60f
        val arrowStart = -30f
        val arrowEnd = if (isCw) -120f else 60f
        canvas.drawArc(
            RectF(cx - arrowRadius, cy - arrowRadius, cx + arrowRadius, cy + arrowRadius),
            arrowStart, arrowEnd - arrowStart, false, arrowPaint
        )

        val headAngle = Math.toRadians(arrowEnd.toDouble())
        val hx = cx + arrowRadius * cos(headAngle).toFloat()
        val hy = cy + arrowRadius * sin(headAngle).toFloat()
        canvas.drawCircle(hx, hy, 10f, arrowPaint)

        // === Piston synchronisé avec le repère du volant ===
        val pistonCx = w / 2
        val pistonTop = h * 0.62f
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

        // ✅ Position du piston calculée à partir de l'angle RÉEL du repère du volant
        // Le repère pointe en haut à animatedAngle = 0 (ou 360), en bas à 180
        // On utilise la projection verticale du repère : repère en haut => piston en haut
        val markAngleRad = Math.toRadians(animatedAngle.toDouble())
        // verticalFactor : 1 quand repère en haut (cos=1), -1 quand repère en bas (cos=-1)
        val verticalFactor = cos(markAngleRad).toFloat() // +1 en haut, -1 en bas
        // course du piston : 0 (haut) à 1 (bas)
        val strokeFactor = (1f - verticalFactor) / 2f // 0 en haut, 1 en bas
        val stroke = pistonBottom - pistonTop - 60f // hauteur de course réelle
        val pistonY = pistonTop + strokeFactor * stroke

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

        // Texte direction
        val dirText = if (isCw) "↻ Moteur horaire  |  Avance antihoraire"
                      else "↺ Moteur antihoraire  |  Avance horaire"
        canvas.drawText(dirText, w / 2, h - 20f, labelPaint)
    }
}
