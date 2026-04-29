package com.myaunt.app.ui.recordbook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.myaunt.app.R
import com.myaunt.app.databinding.FragmentRecordBookBinding
import com.myaunt.app.ui.applyStatusBarPaddingTop
import com.myaunt.app.ui.special.SpecialRecordsFragment
import com.myaunt.app.ui.vaginaldischarge.VaginalDischargeHistoryFragment
import com.myaunt.app.ui.vaginaldischarge.VaginalDischargeRecordFragment
import java.time.LocalDate

/**
 * 记录本：周期备注、分泌物记录、历史分页。
 */
class RecordBookFragment : Fragment() {

    private var _binding: FragmentRecordBookBinding? = null
    private val binding get() = _binding!!

    private var tabMediator: TabLayoutMediator? = null
    private val refreshHistoryRunnable = Runnable {
        if (_binding == null || !isAdded) return@Runnable
        findHistoryFragment()?.refreshHistoryList()
    }

    companion object {
        private const val TAB_DISCHARGE_HISTORY = 2
    }

    /** 保存分泌物后切换到历史页并刷新列表。 */
    fun navigateToDischargeHistoryAfterSave() {
        val b = _binding ?: return
        b.viewPagerRecordBook.removeCallbacks(refreshHistoryRunnable)
        b.viewPagerRecordBook.setCurrentItem(TAB_DISCHARGE_HISTORY, true)
        // ViewPager2 子 Fragment 可能下一帧才 attach
        b.viewPagerRecordBook.post {
            if (_binding == null || !isAdded) return@post
            childFragmentManager.executePendingTransactions()
            findHistoryFragment()?.refreshHistoryList()
        }
        b.viewPagerRecordBook.postDelayed(refreshHistoryRunnable, 80L)
    }

    private fun findHistoryFragment(): VaginalDischargeHistoryFragment? {
        return childFragmentManager.fragments
            .firstOrNull { it is VaginalDischargeHistoryFragment } as? VaginalDischargeHistoryFragment
    }

    private fun findDischargeRecordFragment(): VaginalDischargeRecordFragment? {
        return childFragmentManager.fragments
            .firstOrNull { it is VaginalDischargeRecordFragment } as? VaginalDischargeRecordFragment
    }

    /** 从历史列表点某条：跳到分泌物页并打开该日期的编辑。 */
    fun openDischargeAtDate(date: LocalDate) {
        val b = _binding ?: return
        b.viewPagerRecordBook.setCurrentItem(1, true)
        fun applyDate() {
            if (_binding == null || !isAdded) return
            childFragmentManager.executePendingTransactions()
            findDischargeRecordFragment()?.setRecordDate(date)
        }
        b.viewPagerRecordBook.post { applyDate() }
        b.viewPagerRecordBook.postDelayed({ applyDate() }, 100L)
    }

    /** 工具栏「历史」：仅切换到分泌物历史页。 */
    fun navigateToDischargeHistoryTab() {
        val b = _binding ?: return
        b.viewPagerRecordBook.setCurrentItem(TAB_DISCHARGE_HISTORY, true)
        b.viewPagerRecordBook.post {
            if (_binding == null || !isAdded) return@post
            childFragmentManager.executePendingTransactions()
            findHistoryFragment()?.refreshHistoryList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecordBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyStatusBarPaddingTop()

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> SpecialRecordsFragment()
                1 -> VaginalDischargeRecordFragment.newInstance(embedded = true)
                2 -> VaginalDischargeHistoryFragment.newInstance(embedded = true)
                else -> error("Invalid tab $position")
            }
        }
        binding.viewPagerRecordBook.offscreenPageLimit = 1
        binding.viewPagerRecordBook.adapter = adapter

        tabMediator?.detach()
        tabMediator = TabLayoutMediator(
            binding.tabLayoutRecordBook,
            binding.viewPagerRecordBook,
        ) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.record_book_tab_period_notes)
                1 -> getString(R.string.record_book_tab_discharge)
                2 -> getString(R.string.record_book_tab_discharge_history)
                else -> null
            }
        }.also { it.attach() }
    }

    override fun onDestroyView() {
        _binding?.viewPagerRecordBook?.removeCallbacks(refreshHistoryRunnable)
        tabMediator?.detach()
        tabMediator = null
        _binding = null
        super.onDestroyView()
    }
}
