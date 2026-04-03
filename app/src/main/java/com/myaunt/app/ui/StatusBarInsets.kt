package com.myaunt.app.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** 根布局背景可延伸到状态栏，内容从状态栏下开始排列。 */
fun View.applyStatusBarPaddingTop() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        v.updatePadding(top = top)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
