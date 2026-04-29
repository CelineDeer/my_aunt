package com.myaunt.app.ui.vaginaldischarge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.myaunt.app.R
import com.myaunt.app.data.DischargeAmount
import com.myaunt.app.data.DischargeColor
import com.myaunt.app.data.DischargeSensation
import com.myaunt.app.data.DischargeTexture
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.data.VaginalDischargeRecord
import com.myaunt.app.databinding.FragmentVaginalDischargeRecordBinding
import com.myaunt.app.ui.recordbook.RecordBookFragment
import com.myaunt.app.widget.PeriodWidgetUpdater
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class VaginalDischargeRecordFragment : Fragment() {

    companion object {
        private const val ARG_EMBEDDED = "embedded"

        fun newInstance(embedded: Boolean = false) = VaginalDischargeRecordFragment().apply {
            arguments = bundleOf(ARG_EMBEDDED to embedded)
        }
    }

    private var _binding: FragmentVaginalDischargeRecordBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: PeriodRepository
    private val embedded: Boolean by lazy { arguments?.getBoolean(ARG_EMBEDDED) == true }

    private var selectedDate: LocalDate = LocalDate.now()

    private var selectedAmount = DischargeAmount.NONE
    private var selectedColor = DischargeColor.WHITE
    private var selectedTexture = DischargeTexture.WATERY
    private var selectedSensation = DischargeSensation.FRESH

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVaginalDischargeRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PeriodRepository(requireContext())

        setupToolbarMenu()
        setupClickListeners()
        setupCardSelections()
        setupChipGroups()
        loadExistingRecord()
        updateDateDisplay()
    }

    /** 由记录本切换 Tab 后设置要编辑的日期。 */
    fun setRecordDate(date: LocalDate) {
        if (_binding == null || !isAdded) return
        selectedDate = date
        loadExistingRecord()
        updateDateDisplay()
    }

    private fun setupToolbarMenu() {
        if (embedded) {
            binding.toolbar.navigationIcon = null
        } else {
            binding.toolbar.setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
        binding.toolbar.inflateMenu(R.menu.menu_vaginal_discharge)
        binding.toolbar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_medical_summary -> {
                    showMedicalSummaryDialog()
                    true
                }
                R.id.action_history -> {
                    (parentFragment as? RecordBookFragment)?.navigateToDischargeHistoryTab()
                    true
                }
                else -> false
            }
        }
        binding.tvDateHeader.setOnClickListener { showRecordDatePicker() }
        binding.tvDateHeader.isClickable = true
        binding.tvDateHeader.isFocusable = true
        binding.tvDateHeader.text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINESE))
    }

    private fun showMedicalSummaryDialog() {
        val body = repository.generateMedicalSummary(30)
        val scroll = ScrollView(requireContext()).apply {
            setPadding(48, 24, 48, 24)
            addView(
                TextView(requireContext()).apply {
                    text = body
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.md_on_surface))
                },
            )
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.medical_summary_title)
            .setView(scroll)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showRecordDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()
        val selectionMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择日期")
            .setCalendarConstraints(constraints)
            .setSelection(selectionMillis)
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            val picked = Instant.ofEpochMilli(utcMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            if (picked.isAfter(LocalDate.now())) return@addOnPositiveButtonClickListener
            selectedDate = picked
            loadExistingRecord()
            updateDateDisplay()
        }
        picker.show(parentFragmentManager, "discharge_pick_date")
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener { saveRecord() }
    }

    private fun setupCardSelections() {
        val amountCards = mapOf(
            DischargeAmount.NONE to binding.cardAmountNone,
            DischargeAmount.LESS to binding.cardAmountLess,
            DischargeAmount.MEDIUM to binding.cardAmountMedium,
            DischargeAmount.MORE to binding.cardAmountMore,
        )
        amountCards.forEach { (amount, card) ->
            card.setOnClickListener {
                selectedAmount = amount
                updateAmountSelectionState()
            }
        }

        val colorCards = mapOf(
            DischargeColor.TRANSPARENT to binding.cardColorTransparent,
            DischargeColor.WHITE to binding.cardColorWhite,
            DischargeColor.YELLOWISH to binding.cardColorYellowish,
            DischargeColor.BROWN to binding.cardColorBrown,
        )
        colorCards.forEach { (color, card) ->
            card.setOnClickListener {
                selectedColor = color
                updateColorSelectionState()
            }
        }

        val textureCards = mapOf(
            DischargeTexture.WATERY to binding.cardTextureWatery,
            DischargeTexture.STRETCHY to binding.cardTextureStretchy,
            DischargeTexture.THICK to binding.cardTextureThick,
            DischargeTexture.CLUMPY to binding.cardTextureClumpy,
        )
        textureCards.forEach { (texture, card) ->
            card.setOnClickListener {
                selectedTexture = texture
                updateTextureSelectionState()
            }
        }

        updateAmountSelectionState()
        updateColorSelectionState()
        updateTextureSelectionState()
    }

    private fun updateAmountSelectionState() {
        val cards = listOf(
            binding.cardAmountNone, binding.cardAmountLess, binding.cardAmountMedium, binding.cardAmountMore,
        )
        val selected = when (selectedAmount) {
            DischargeAmount.NONE -> binding.cardAmountNone
            DischargeAmount.LESS -> binding.cardAmountLess
            DischargeAmount.MEDIUM -> binding.cardAmountMedium
            DischargeAmount.MORE -> binding.cardAmountMore
        }
        applyCardSelected(cards, selected)
    }

    private fun updateColorSelectionState() {
        val cards = listOf(
            binding.cardColorTransparent, binding.cardColorWhite, binding.cardColorYellowish, binding.cardColorBrown,
        )
        val selected = when (selectedColor) {
            DischargeColor.TRANSPARENT -> binding.cardColorTransparent
            DischargeColor.WHITE -> binding.cardColorWhite
            DischargeColor.YELLOWISH -> binding.cardColorYellowish
            DischargeColor.BROWN -> binding.cardColorBrown
        }
        applyCardSelected(cards, selected)
    }

    private fun updateTextureSelectionState() {
        val cards = listOf(
            binding.cardTextureWatery, binding.cardTextureStretchy, binding.cardTextureThick, binding.cardTextureClumpy,
        )
        val selected = when (selectedTexture) {
            DischargeTexture.WATERY -> binding.cardTextureWatery
            DischargeTexture.STRETCHY -> binding.cardTextureStretchy
            DischargeTexture.THICK -> binding.cardTextureThick
            DischargeTexture.CLUMPY -> binding.cardTextureClumpy
        }
        applyCardSelected(cards, selected)
    }

    private fun applyCardSelected(all: List<com.google.android.material.card.MaterialCardView>, selected: com.google.android.material.card.MaterialCardView) {
        all.forEach { card ->
            if (card == selected) {
                card.strokeColor = resources.getColor(R.color.md_primary, null)
                card.strokeWidth = 4
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(0x33EC5A9A.toInt()))
            } else {
                card.strokeWidth = 0
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(0xFFFFF0F3.toInt()))
            }
        }
    }

    private fun setupChipGroups() {
        binding.chipGroupSensation.setOnCheckedStateChangeListener { group, _ ->
            when (group.checkedChipId) {
                R.id.chipSensationFresh -> selectedSensation = DischargeSensation.FRESH
                R.id.chipSensationItchy -> selectedSensation = DischargeSensation.ITCHY
                R.id.chipSensationOdor -> selectedSensation = DischargeSensation.ODOR
                R.id.chipSensationPain -> selectedSensation = DischargeSensation.PAIN
            }
        }
    }

    private fun updateDateDisplay() {
        binding.tvDateHeader.text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINESE))
    }

    private fun loadExistingRecord() {
        val ex = repository.getVaginalDischargeRecord(selectedDate)
        if (ex != null) {
            selectedAmount = ex.amount
            selectedColor = ex.color
            selectedTexture = ex.texture
            selectedSensation = ex.sensation
            updateAmountSelectionState()
            updateColorSelectionState()
            updateTextureSelectionState()
            when (ex.sensation) {
                DischargeSensation.FRESH -> binding.chipSensationFresh.isChecked = true
                DischargeSensation.ITCHY -> binding.chipSensationItchy.isChecked = true
                DischargeSensation.ODOR -> binding.chipSensationOdor.isChecked = true
                DischargeSensation.PAIN -> binding.chipSensationPain.isChecked = true
            }
            binding.switchAbdominal.isChecked = ex.abdominalDiscomfort
            binding.switchAfterIntercourse.isChecked = ex.afterIntercourse
            binding.etDischargeNotes.setText(ex.notes)
        } else {
            selectedAmount = DischargeAmount.NONE
            selectedColor = DischargeColor.WHITE
            selectedTexture = DischargeTexture.WATERY
            selectedSensation = DischargeSensation.FRESH
            updateAmountSelectionState()
            updateColorSelectionState()
            updateTextureSelectionState()
            binding.chipSensationFresh.isChecked = true
            binding.switchAbdominal.isChecked = false
            binding.switchAfterIntercourse.isChecked = false
            binding.etDischargeNotes.setText("")
        }
    }

    private fun saveRecord() {
        val record = VaginalDischargeRecord(
            date = selectedDate,
            amount = selectedAmount,
            color = selectedColor,
            texture = selectedTexture,
            sensation = selectedSensation,
            abdominalDiscomfort = binding.switchAbdominal.isChecked,
            afterIntercourse = binding.switchAfterIntercourse.isChecked,
            notes = binding.etDischargeNotes.text?.toString()?.trim().orEmpty(),
        )

        repository.saveVaginalDischargeRecord(record)
        if (embedded) {
            Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show()
            (parentFragment as? RecordBookFragment)?.navigateToDischargeHistoryAfterSave()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("保存成功")
                .setMessage("白带记录已保存")
                .setPositiveButton("确定") { _, _ ->
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
