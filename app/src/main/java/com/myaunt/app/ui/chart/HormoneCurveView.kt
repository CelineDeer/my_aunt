package com.myaunt.app.ui.chart

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.myaunt.app.R

class HormoneCurveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val estrogenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val progesteronePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
    }

    private val estrogenFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val progesteroneFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        color = Color.parseColor("#E0E0E0")
    }

    private var animationProgress = 0f
    private var animator: ValueAnimator? = null

    init {
        // 设置颜色
        val estrogenColor = ContextCompat.getColor(context, R.color.md_primary)
        val progesteroneColor = Color.parseColor("#7986CB")

        estrogenPaint.color = estrogenColor
        progesteronePaint.color = progesteroneColor

        // 填充色带透明度
        estrogenFillPaint.color = estrogenColor
        estrogenFillPaint.alpha = 30
        progesteroneFillPaint.color = progesteroneColor
        progesteroneFillPaint.alpha = 30
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paddingLeft = 40f
        val paddingRight = 40f
        val paddingTop = 20f
        val paddingBottom = 40f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // 绘制雌激素曲线（实心粉色）
        drawEstrogenCurve(canvas, paddingLeft, paddingTop, chartWidth, chartHeight)

        // 绘制孕激素曲线（虚线蓝色）
        drawProgesteroneCurve(canvas, paddingLeft, paddingTop, chartWidth, chartHeight)
    }

    private fun drawEstrogenCurve(
        canvas: Canvas,
        paddingLeft: Float,
        paddingTop: Float,
        chartWidth: Float,
        chartHeight: Float
    ) {
        val path = Path()
        val fillPath = Path()

        val points = generateEstrogenPoints(paddingLeft, paddingTop, chartWidth, chartHeight)

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, points[0].y)

            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
                fillPath.lineTo(points[i].x, points[i].y)
            }

            // 绘制填充区域
            fillPath.lineTo(points.last().x, paddingTop + chartHeight)
            fillPath.lineTo(points[0].x, paddingTop + chartHeight)
            fillPath.close()
            canvas.drawPath(fillPath, estrogenFillPaint)

            // 绘制曲线
            canvas.drawPath(path, estrogenPaint)
        }
    }

    private fun drawProgesteroneCurve(
        canvas: Canvas,
        paddingLeft: Float,
        paddingTop: Float,
        chartWidth: Float,
        chartHeight: Float
    ) {
        val path = Path()
        val fillPath = Path()

        val points = generateProgesteronePoints(paddingLeft, paddingTop, chartWidth, chartHeight)

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, points[0].y)

            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
                fillPath.lineTo(points[i].x, points[i].y)
            }

            // 绘制填充区域
            fillPath.lineTo(points.last().x, paddingTop + chartHeight)
            fillPath.lineTo(points[0].x, paddingTop + chartHeight)
            fillPath.close()
            canvas.drawPath(fillPath, progesteroneFillPaint)

            // 绘制虚线曲线
            canvas.drawPath(path, progesteronePaint)
        }
    }

    private fun generateEstrogenPoints(
        paddingLeft: Float,
        paddingTop: Float,
        chartWidth: Float,
        chartHeight: Float
    ): List<PointF> {
        val points = mutableListOf<PointF>()
        val dayCount = 28

        // 关键帧: (day, level)  level范围0~1
        val keyframes = listOf(
            0f to 0.25f,   // 月经开始
            5f to 0.35f,   // 月经结束
            12f to 0.75f,  // 排卵前上升
            14f to 0.92f,  // 排卵高峰
            18f to 0.65f,  // 黄体期下降
            22f to 0.55f,  // 黄体期
            28f to 0.25f   // 下次月经前
        )

        for (day in 0..dayCount) {
            val x = paddingLeft + (day / dayCount.toFloat()) * chartWidth
            val yRatio = smoothInterpolate(keyframes, day.toFloat())
            val y = paddingTop + chartHeight - (yRatio * chartHeight * animationProgress)
            points.add(PointF(x, y))
        }

        return points
    }

    private fun generateProgesteronePoints(
        paddingLeft: Float,
        paddingTop: Float,
        chartWidth: Float,
        chartHeight: Float
    ): List<PointF> {
        val points = mutableListOf<PointF>()
        val dayCount = 28

        // 关键帧: (day, level)
        val keyframes = listOf(
            0f to 0.12f,   // 月经开始
            5f to 0.10f,   // 月经结束
            13f to 0.15f,  // 排卵前低
            16f to 0.35f,  // 排卵后开始上升
            20f to 0.85f,  // 黄体期高峰
            23f to 0.70f,  // 开始下降
            28f to 0.12f   // 下次月经前
        )

        for (day in 0..dayCount) {
            val x = paddingLeft + (day / dayCount.toFloat()) * chartWidth
            val yRatio = smoothInterpolate(keyframes, day.toFloat())
            val y = paddingTop + chartHeight - (yRatio * chartHeight * animationProgress)
            points.add(PointF(x, y))
        }

        return points
    }

    /**
     * 使用余弦插值在关键帧之间平滑过渡
     */
    private fun smoothInterpolate(keyframes: List<Pair<Float, Float>>, x: Float): Float {
        if (x <= keyframes.first().first) return keyframes.first().second
        if (x >= keyframes.last().first) return keyframes.last().second

        for (i in 0 until keyframes.size - 1) {
            val (x0, y0) = keyframes[i]
            val (x1, y1) = keyframes[i + 1]
            if (x in x0..x1) {
                val t = (x - x0) / (x1 - x0)
                // 余弦插值：smoothstep
                val smoothT = (1f - kotlin.math.cos(t * Math.PI.toFloat())) / 2f
                return y0 + (y1 - y0) * smoothT
            }
        }
        return keyframes.last().second
    }

    fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
