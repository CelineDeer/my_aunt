package com.myaunt.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.Locale

/**
 * 从桌面快捷方式进入：只触发系统「固定小组件」界面，不打开应用主界面。
 */
class PinWidgetActivity : Activity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var waitingPinResult = false
    private var pausedAfterPinRequest = false
    private var widgetIdsBeforeRequest: Set<Int> = emptySet()
    private val miuiFallbackRunnable = Runnable {
        if (!isFinishing && waitingPinResult && isXiaomiLike()) {
            waitingPinResult = false
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            finish()
            return
        }
        val mgr = AppWidgetManager.getInstance(this)
        if (!mgr.isRequestPinAppWidgetSupported) {
            finish()
            return
        }
        widgetIdsBeforeRequest = currentWidgetIds()
        waitingPinResult = true
        pausedAfterPinRequest = false
        mgr.requestPinAppWidget(
            ComponentName(this, PeriodCycleAppWidget::class.java),
            null,
            null,
        )
        if (isXiaomiLike()) {
            mainHandler.postDelayed(miuiFallbackRunnable, 1200L)
        } else {
            window.decorView.post { finish() }
        }
    }

    override fun onPause() {
        super.onPause()
        if (waitingPinResult) {
            pausedAfterPinRequest = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isXiaomiLike() || !waitingPinResult || !pausedAfterPinRequest || isFinishing) return
        mainHandler.removeCallbacks(miuiFallbackRunnable)
        if (currentWidgetIds().size > widgetIdsBeforeRequest.size) {
            waitingPinResult = false
            finish()
            return
        }
        waitingPinResult = false
        finish()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(miuiFallbackRunnable)
        super.onDestroy()
    }

    private fun currentWidgetIds(): Set<Int> {
        val mgr = AppWidgetManager.getInstance(this)
        val cn = ComponentName(this, PeriodCycleAppWidget::class.java)
        return mgr.getAppWidgetIds(cn).toSet()
    }

    private fun isXiaomiLike(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
        val brand = Build.BRAND.lowercase(Locale.US)
        return manufacturer.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi")
    }

    companion object {
        const val ACTION_REQUEST_PIN_WIDGET = "com.myaunt.app.action.REQUEST_PIN_WIDGET"
    }
}
