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
import android.widget.Button
import android.widget.GridLayout
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
import androidx.fragment.app.FragmentManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.myaunt.app.databinding.ActivityMainBinding
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.privacy.PrivacyConsent
import com.myaunt.app.ui.chart.ChartFragment
import com.myaunt.app.ui.healthtreasury.HealthTreasuryFragment
import com.myaunt.app.ui.home.HomeFragment
import com.myaunt.app.ui.history.PeriodHistoryFragment
import com.myaunt.app.ui.recordbook.RecordBookFragment
import com.myaunt.app.widget.PeriodWidgetUpdater
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository by lazy { PeriodRepository(this) }
    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINESE)

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
            binding.bottomNavContainer.updatePadding(bottom = bars.bottom)
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

    /** 打开历史记录进行编辑，由 HomeFragment 等调用。 */
    fun openPeriodHistoryForEdit(date: LocalDate) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
            .replace(R.id.fragment_container, PeriodHistoryFragment.newInstanceForEdit(date))
            .addToBackStack("period_history_edit")
            .commit()
    }

    /** 编辑选定的周期记录。 */
    fun editSelectedPeriod(date: LocalDate) {
        (supportFragmentManager.findFragmentById(R.id.fragment_container) as?
                com.myaunt.app.ui.history.PeriodHistoryFragment)?.prepareEditMode(date)
    }

    /** 从首页等进入：月经开始日历史列表。 */
    fun openPeriodHistory() {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
            .replace(R.id.fragment_container, PeriodHistoryFragment())
            .addToBackStack("period_history")
            .commit()
    }

    private fun setupNavigation() {
        val homeFragment = HomeFragment()
        val chartFragment = ChartFragment()
        val healthTreasuryFragment = HealthTreasuryFragment()
        val recordsFragment = RecordBookFragment()

        setCurrentFragment(homeFragment)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    setCurrentFragment(homeFragment)
                    true
                }
                R.id.nav_list -> {
                    setCurrentFragment(chartFragment)
                    true
                }
                R.id.nav_add -> {
                    // 中间为“动作按钮占位”，不作为可选中的目的地。
                    false
                }
                R.id.nav_info -> {
                    setCurrentFragment(healthTreasuryFragment)
                    true
                }
                R.id.nav_record -> {
                    setCurrentFragment(recordsFragment)
                    true
                }
                else -> false
            }
        }

        // 中央添加按钮点击
        binding.centerAddButton.setOnClickListener {
            showRecordDatePicker()
        }
    }

    private fun showRecordDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("记录：来月经的日期")
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            val picked = Instant.ofEpochMilli(utcMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            if (picked.isAfter(LocalDate.now())) return@addOnPositiveButtonClickListener
            showRecordConfirmDialog(picked)
        }
        picker.show(supportFragmentManager, "record_date")
    }

    private fun showRecordConfirmDialog(selectedDate: LocalDate) {
        val message = if (selectedDate == LocalDate.now()) {
            "确认今天开始了吗？"
        } else {
            "确认 ${selectedDate.format(dateFormatter)} 开始了吗？"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("来姨妈了？")
            .setMessage(message)
            .setNegativeButton("取消", null)
            .setPositiveButton("确认记录") { _, _ ->
                tryRecordDate(selectedDate)
            }
            .show()
    }

    private fun tryRecordDate(recordDate: LocalDate) {
        if (repository.getAllPeriods().contains(recordDate)) {
            showAlreadyExistsDialog(recordDate)
            return
        }

        val abnormalGap = repository.getAbnormalGapForNewRecord(recordDate)
        if (abnormalGap == null) {
            repository.addPeriod(recordDate)
            PeriodWidgetUpdater.updateAll(this)
            notifyRecordChanged()
            return
        }

        showAbnormalCycleDialog(abnormalGap.days, recordDate)
    }

    private fun showAlreadyExistsDialog(existingDate: LocalDate) {
        val message = "${existingDate.format(dateFormatter)} 已经记过一次，"
        MaterialAlertDialogBuilder(this)
            .setTitle("该日期已有记录")
            .setMessage(message + "是否要修改这条记录？")
            .setNegativeButton("取消") { _, _ ->
                // 取消，不做任何操作
            }
            .setPositiveButton("修改") { _, _ ->
                openPeriodHistoryForEdit(existingDate)
            }
            .setNeutralButton("删除") { _, _ ->
                deleteAndRecord(existingDate)
            }
            .show()
    }

    private fun deleteAndRecord(existingDate: LocalDate) {
        val oldReason = repository.getSpecialReason(existingDate)
        repository.removePeriod(existingDate)

        MaterialAlertDialogBuilder(this)
            .setTitle("删除并重新记录")
            .setMessage("已删除${existingDate.format(dateFormatter)}的旧记录，现在可以重新记录。")
            .setPositiveButton("开始记录") { _, _ ->
                showRecordConfirmDialog(existingDate)
            }
            .show()
    }

    private fun showAbnormalCycleDialog(
        cycleLength: Long,
        recordDate: LocalDate,
    ) {
        val whenText = when {
            recordDate == LocalDate.now() -> "本次"
            else -> "为 ${recordDate.format(dateFormatter)} 补录时"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("周期提醒")
            .setMessage("$whenText 间隔为 $cycleLength 天，超出常见 20-40 天范围。\n周期可能不太规律，仍要记录吗？")
            .setNegativeButton("取消", null)
            .setNeutralButton("写备注") { _, _ ->
                showSpecialReasonDialog(cycleLength, recordDate)
            }
            .setPositiveButton("直接记录") { _, _ ->
                repository.addPeriod(recordDate)
                PeriodWidgetUpdater.updateAll(this)
                notifyRecordChanged()
            }
            .show()
    }

    private fun showSpecialReasonDialog(
        cycleLength: Long,
        recordDate: LocalDate,
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_special_reason, null, false)
        val gridChips = dialogView.findViewById<GridLayout>(R.id.grid_reason_chips)
        val noteInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_special_note)
        val noteLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layout_special_note)

        dialogView.findViewById<TextView>(R.id.tv_dialog_subtitle).text = recordDate.format(dateFormatter)
        dialogView.findViewById<TextView>(R.id.tv_gap_card_body).text =
            "本次间隔为 ${cycleLength} 天，超出常见的 20–40 天范围。\n勾选标签（可多选）或写一句备注，帮助以后更懂自己的周期。"

        fun collectTags(): List<String> {
            val out = mutableListOf<String>()
            for (i in 0 until gridChips.childCount) {
                val child = gridChips.getChildAt(i)
                if (child is Chip && child.isChecked) out.add(child.text.toString())
            }
            return out
        }

        fun buildReason(tags: List<String>, note: String): String {
            return buildString {
                append("周期${cycleLength}天")
                if (tags.isNotEmpty()) {
                    append(" · ")
                    append(tags.joinToString("、"))
                }
                if (note.isNotEmpty()) {
                    append(" · ")
                    append(note)
                }
            }
        }

        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.92f).toInt(),
            )
            setGravity(android.view.Gravity.BOTTOM)
        }
        @Suppress("DEPRECATION")
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        fun performSave() {
            val tags = collectTags()
            val note = noteInput.text?.toString()?.trim().orEmpty()
            if (tags.isEmpty() && note.isEmpty()) {
                noteLayout.error = "请至少选一个标签，或填写补充说明"
                return
            }
            noteLayout.error = null
            repository.addPeriodWithSpecialReason(recordDate, buildReason(tags, note))
            PeriodWidgetUpdater.updateAll(this)
            dialog.dismiss()
            notifyRecordChanged()
        }

        dialogView.findViewById<View>(R.id.btn_dialog_close).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<TextView>(R.id.btn_dialog_save_top).setOnClickListener { performSave() }
        dialogView.findViewById<Button>(R.id.btn_save_record).setOnClickListener { performSave() }
        dialogView.findViewById<TextView>(R.id.tv_skip_record).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_skip_title)
                .setMessage(R.string.confirm_skip_message)
                .setNegativeButton(R.string.special_dialog_save_record, null)
                .setPositiveButton(R.string.special_dialog_skip) { _, _ ->
                    repository.addPeriodWithSpecialReason(recordDate, "未填写")
                    PeriodWidgetUpdater.updateAll(this)
                    dialog.dismiss()
                    notifyRecordChanged()
                }
                .show()
        }

        dialog.show()
    }

    private fun notifyRecordChanged() {
        Toast.makeText(this, "记录成功", Toast.LENGTH_SHORT).show()
        (supportFragmentManager.findFragmentById(R.id.fragment_container) as? HomeFragment)?.refreshAfterRecord()
    }

    private fun setCurrentFragment(fragment: Fragment) {
        val fm = supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
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
