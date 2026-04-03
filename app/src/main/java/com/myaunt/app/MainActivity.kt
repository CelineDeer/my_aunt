package com.myaunt.app

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.myaunt.app.databinding.ActivityMainBinding
import com.myaunt.app.privacy.PrivacyConsent
import com.myaunt.app.ui.chart.ChartFragment
import com.myaunt.app.ui.home.HomeFragment
import com.myaunt.app.ui.special.SpecialRecordsFragment
import com.myaunt.app.widget.PeriodWidgetUpdater

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 顶部不留白：Fragment 可全屏铺背景（如备注页渐变延伸到状态栏），由各 Fragment 自行加 statusBars padding。
            v.updatePadding(bars.left, 0, bars.right, 0)
            binding.bottomNav.updatePadding(bottom = bars.bottom)
            windowInsets
        }

        setupNavigation()

        binding.root.post { showPrivacyDialogIfNeeded() }
    }

    override fun onResume() {
        super.onResume()
        PeriodWidgetUpdater.updateAll(this)
    }

    /** 供子 Fragment 切换底部导航（如首页跳转图表）。 */
    fun selectBottomNav(@IdRes itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    private fun setupNavigation() {
        val homeFragment = HomeFragment()
        val chartFragment = ChartFragment()
        val specialFragment = SpecialRecordsFragment()

        setCurrentFragment(homeFragment)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    setCurrentFragment(homeFragment)
                    true
                }
                R.id.nav_chart -> {
                    setCurrentFragment(chartFragment)
                    true
                }
                R.id.nav_special -> {
                    setCurrentFragment(specialFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun setCurrentFragment(fragment: Fragment) {
        val fm = supportFragmentManager
        val tx = fm.beginTransaction().setReorderingAllowed(true)
        if (fm.findFragmentById(R.id.fragment_container) != null) {
            tx.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
        }
        tx.replace(R.id.fragment_container, fragment).commit()
    }

    private fun showPrivacyDialogIfNeeded() {
        if (PrivacyConsent.hasAccepted(this)) return

        val body = resources.openRawResource(R.raw.privacy_policy).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val dm = resources.displayMetrics
        val density = dm.density
        val screenH = dm.heightPixels
        // 为标题、简介、双按钮与系统对话框边距预留空间，避免 ScrollView 过高把按钮挤出屏幕
        val reservedPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 280f, dm).toInt()
        val maxScrollCap = (screenH * 0.4f).toInt().coerceAtLeast(1)
        val minScrollPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160f, dm).toInt()
        val maxScrollH = (screenH - reservedPx)
            .coerceAtLeast(minScrollPx)
            .coerceAtMost(maxScrollCap)

        val padPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            16f,
            dm,
        ).toInt()

        val dialogView = layoutInflater.inflate(R.layout.dialog_privacy_policy, null, false)
        val scroll = dialogView.findViewById<ScrollView>(R.id.scroll_privacy_body)
        scroll.layoutParams = (scroll.layoutParams as android.widget.LinearLayout.LayoutParams).apply {
            height = maxScrollH
        }

        val tvBody = dialogView.findViewById<TextView>(R.id.tv_privacy_body)
        tvBody.text = body
        tvBody.setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics), 1f)
        tvBody.setPadding(padPx, padPx, padPx, padPx)

        val linkText = getString(R.string.privacy_policy_link_text)
        val introRaw = getString(R.string.privacy_dialog_intro_template, linkText)
        val introSpannable = SpannableString(introRaw)
        val linkStart = introRaw.indexOf(linkText)
        if (linkStart >= 0) {
            val url = getString(R.string.privacy_policy_url).trim()
            val linkColor = ContextCompat.getColor(this, R.color.md_primary)
            introSpannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        if (url.isNotEmpty()) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.privacy_policy_url_pending,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = true
                        ds.color = linkColor
                        ds.typeface = Typeface.DEFAULT_BOLD
                    }
                },
                linkStart,
                linkStart + linkText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        val tvIntro = dialogView.findViewById<TextView>(R.id.tv_privacy_intro)
        tvIntro.text = introSpannable
        tvIntro.movementMethod = LinkMovementMethod.getInstance()
        tvIntro.highlightColor = ContextCompat.getColor(this, android.R.color.transparent)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btn_privacy_agree).setOnClickListener {
            PrivacyConsent.markAccepted(this)
            dialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.tv_privacy_disagree).setOnClickListener {
            finishAffinity()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
