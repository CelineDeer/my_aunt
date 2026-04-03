package com.myaunt.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.myaunt.app.MainActivity
import com.myaunt.app.R
import com.myaunt.app.data.PeriodRepository
import java.time.format.DateTimeFormatter
import java.util.Locale

object PeriodWidgetUpdater {

    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINESE)

    fun buildRemoteViews(context: Context): RemoteViews {
        val appContext = context.applicationContext
        val repo = PeriodRepository(appContext)
        val rv = RemoteViews(appContext.packageName, R.layout.widget_period_cycle)
        val pink = ContextCompat.getColor(appContext, R.color.md_primary)

        val days = repo.getDaysSinceLastPeriod()
        if (days == null) {
            rv.setTextViewText(R.id.widget_days, "--")
            rv.setInt(R.id.widget_days, "setTextColor", pink)
            rv.setViewVisibility(R.id.widget_days_unit, android.view.View.GONE)
            rv.setTextViewText(R.id.widget_label, "还没开始记录")
            rv.setTextViewText(R.id.widget_last, "轻点打开\n记一笔")
            rv.setViewVisibility(R.id.widget_forecast, android.view.View.GONE)
        } else {
            rv.setTextViewText(R.id.widget_days, days.toString())
            rv.setInt(R.id.widget_days, "setTextColor", pink)
            rv.setViewVisibility(R.id.widget_days_unit, android.view.View.VISIBLE)
            rv.setTextViewText(R.id.widget_days_unit, "天")
            rv.setTextViewText(R.id.widget_label, "距离上次姨妈已经")
            val last = repo.getAllPeriods().first()
            rv.setTextViewText(R.id.widget_last, "上次 ${last.format(dateFormatter)}")
            val avg = repo.getAverageCycle()
            if (avg != null) {
                val nextDays = avg.toInt() - days
                rv.setViewVisibility(R.id.widget_forecast, android.view.View.VISIBLE)
                if (nextDays > 0) {
                    rv.setTextViewText(R.id.widget_forecast, "预计还有 ${nextDays} 天")
                } else {
                    rv.setTextViewText(R.id.widget_forecast, "这两天可能会来")
                }
            } else {
                rv.setViewVisibility(R.id.widget_forecast, android.view.View.GONE)
            }
        }

        val launch = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            appContext,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        rv.setOnClickPendingIntent(R.id.widget_root, pending)
        return rv
    }

    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val mgr = AppWidgetManager.getInstance(appContext)
        val cn = ComponentName(appContext, PeriodCycleAppWidget::class.java)
        val ids = mgr.getAppWidgetIds(cn)
        if (ids.isEmpty()) return
        val views = buildRemoteViews(appContext)
        for (id in ids) {
            mgr.updateAppWidget(id, views)
        }
    }
}
