package com.myaunt.app.ui.chart

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import android.view.animation.DecelerateInterpolator
import com.myaunt.app.R
import com.myaunt.app.ui.CycleIntervalBand
import com.myaunt.app.ui.bandForCycleDays
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    data class ChartPoint(
        val periodDate: LocalDate,
        val intervalDays: Long,
    )

    private val colorAbnormalStart = ContextCompat.getColor(context, R.color.md_chart_abnormal_start)
    private val colorAbnormalEnd = ContextCompat.getColor(context, R.color.md_chart_abnormal_end)
    private val colorAbnormalText = ContextCompat.getColor(context, R.color.md_chart_abnormal_text)
    private val colorPurpleStart = ContextCompat.getColor(context, R.color.md_cycle_purple_start)
    private val colorPurpleEnd = ContextCompat.getColor(context, R.color.md_cycle_purple_end)
    private val colorPurpleText = ContextCompat.getColor(context, R.color.md_cycle_purple_text)
    private val colorGreenStart = ContextCompat.getColor(context, R.color.md_cycle_green_start)
    private val colorGreenEnd = ContextCompat.getColor(context, R.color.md_cycle_green_end)
    private val colorGreenText = ContextCompat.getColor(context, R.color.md_cycle_green_text)
    private val colorYellowStart = ContextCompat.getColor(context, R.color.md_cycle_yellow_start)
    private val colorYellowEnd = ContextCompat.getColor(context, R.color.md_cycle_yellow_end)
    private val colorYellowText = ContextCompat.getColor(context, R.color.md_cycle_yellow_text)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 24f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 30f
        isFakeBoldText = true
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
    }
    private val avgLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(15f, 8f), 0f)
    }
    private val avgLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        isFakeBoldText = true
    }

    init {
        labelPaint.color = ContextCompat.getColor(context, R.color.md_chart_label)
        valuePaint.color = ContextCompat.getColor(context, R.color.md_chart_value)
        gridPaint.color = ContextCompat.getColor(context, R.color.md_chart_grid)
        val avg = ContextCompat.getColor(context, R.color.md_chart_avg)
        avgLinePaint.color = avg
        avgLabelPaint.color = avg
    }

    private var points: List<ChartPoint> = emptyList()
    private var animationProgress = 0f
    private val monthFormatter = DateTimeFormatter.ofPattern("M月", Locale.CHINESE)
    private var barAnimator: ValueAnimator? = null

    /** 该柱对应一次月经开始日（即本周期结束的那一天），可用于读取备注 */
    var onBarLongPressListener: ((periodStartDate: LocalDate, intervalDays: Long) -> Unit)? = null

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onLongPress(e: MotionEvent) {
                val index = hitTestBarIndex(e.x, e.y) ?: return
                val point = points.getOrNull(index) ?: return
                onBarLongPressListener?.invoke(point.periodDate, point.intervalDays)
            }
        },
    )

    fun setData(points: List<ChartPoint>) {
        this.points = points
        startAnimation()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun hitTestBarIndex(x: Float, y: Float): Int? {
        if (points.isEmpty() || width == 0 || height == 0) return null
        val paddingLeft = 80f
        val paddingRight = 24f
        val paddingTop = 48f
        val paddingBottom = 110f
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        if (chartWidth <= 0f || chartHeight <= 0f) return null

        val barCount = points.size
        val barSpacing = chartWidth / barCount
        val barWidth = barSpacing * 0.58f

        val labelTop = paddingTop + chartHeight
        val labelBottom = labelTop + 72f
        if (y < paddingTop - 40f || y > labelBottom) return null

        for (i in points.indices) {
            val left = paddingLeft + i * barSpacing + (barSpacing - barWidth) / 2
            val right = left + barWidth
            if (x >= left && x <= right) return i
        }
        return null
    }

    private fun startAnimation() {
        barAnimator?.cancel()
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 420
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            animationProgress = anim.animatedValue as Float
            invalidate()
        }
        barAnimator = animator
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val paddingLeft = 80f
        val paddingRight = 24f
        val paddingTop = 48f
        val paddingBottom = 110f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val intervalValues = points.map { it.intervalDays }
        val maxVal = max(intervalValues.maxOrNull()?.toFloat() ?: 30f, 35f)
        val avg = intervalValues.average().toFloat()

        val barCount = points.size
        val barWidth = (chartWidth / barCount) * 0.58f
        val barSpacing = chartWidth / barCount

        val gridLines = 4
        for (i in 0..gridLines) {
            val y = paddingTop + chartHeight - (i * chartHeight / gridLines)
            canvas.drawLine(paddingLeft, y, paddingLeft + chartWidth, y, gridPaint)
            val label = (maxVal / gridLines * i).toInt().toString()
            labelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, paddingLeft - 8f, y + 8f, labelPaint)
        }

        val avgY = paddingTop + chartHeight - (avg / maxVal * chartHeight)
        canvas.drawLine(paddingLeft, avgY, paddingLeft + chartWidth, avgY, avgLinePaint)
        canvas.drawText("均值 ${avg.toInt()}天", paddingLeft + chartWidth - 100f, avgY - 10f, avgLabelPaint)

        for (i in points.indices) {
            val point = points[i]
            val value = point.intervalDays.toFloat()
            val animatedHeight = (value / maxVal * chartHeight) * animationProgress
            val band = bandForCycleDays(point.intervalDays)

            val left = paddingLeft + i * barSpacing + (barSpacing - barWidth) / 2
            val right = left + barWidth
            val top = paddingTop + chartHeight - animatedHeight
            val bottom = paddingTop + chartHeight

            val (startColor, endColor, valueColor) = when (band) {
                CycleIntervalBand.ABNORMAL ->
                    Triple(colorAbnormalStart, colorAbnormalEnd, colorAbnormalText)
                CycleIntervalBand.PURPLE ->
                    Triple(colorPurpleStart, colorPurpleEnd, colorPurpleText)
                CycleIntervalBand.GREEN ->
                    Triple(colorGreenStart, colorGreenEnd, colorGreenText)
                CycleIntervalBand.YELLOW ->
                    Triple(colorYellowStart, colorYellowEnd, colorYellowText)
            }
            val gradient = LinearGradient(
                left, top, left, bottom,
                startColor,
                endColor,
                Shader.TileMode.CLAMP
            )
            barPaint.shader = gradient

            val rect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, 22f, 22f, barPaint)

            if (animationProgress > 0.8f) {
                valuePaint.color = valueColor
                canvas.drawText(
                    "${point.intervalDays}天",
                    left + barWidth / 2,
                    top - 8f,
                    valuePaint
                )
            }

            val dateLabel = point.periodDate.format(monthFormatter)
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                dateLabel,
                left + barWidth / 2,
                paddingTop + chartHeight + 40f,
                labelPaint
            )
        }
    }
}
