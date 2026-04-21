package com.myaunt.app.ui.vaginaldischarge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.myaunt.app.R
import com.myaunt.app.data.*
import com.myaunt.app.databinding.FragmentVaginalDischargeRecordBinding
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class VaginalDischargeRecordFragment : Fragment() {

    private var _binding: FragmentVaginalDischargeRecordBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: PeriodRepository
    
    private var selectedDate: LocalDate = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINESE)
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("E", Locale.CHINESE)
    
    // 当前选择的值
    private var selectedAmount = DischargeAmount.NONE
    private var selectedColor = DischargeColor.WHITE
    private var selectedTexture = DischargeTexture.WATERY
    private var selectedSensation = DischargeSensation.FRESH

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVaginalDischargeRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PeriodRepository(requireContext())
        
        setupToolbar()
        setupClickListeners()
        setupCardSelections()
        setupChipGroups()
        loadExistingRecord()
        updateDateDisplay()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.tvDateHeader.text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINESE))
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveRecord()
        }
    }

    private fun setupCardSelections() {
        // 分泌量
        val amountCards = mapOf(
            DischargeAmount.NONE to binding.cardAmountNone,
            DischargeAmount.LESS to binding.cardAmountLess,
            DischargeAmount.MEDIUM to binding.cardAmountMedium,
            DischargeAmount.MORE to binding.cardAmountMore
        )

        amountCards.forEach { (amount, card) ->
            card.setOnClickListener {
                selectedAmount = amount
                updateAmountSelectionState()
            }
        }

        // 颜色
        val colorCards = mapOf(
            DischargeColor.TRANSPARENT to binding.cardColorTransparent,
            DischargeColor.WHITE to binding.cardColorWhite,
            DischargeColor.YELLOWISH to binding.cardColorYellowish,
            DischargeColor.BROWN to binding.cardColorBrown
        )

        colorCards.forEach { (color, card) ->
            card.setOnClickListener {
                selectedColor = color
                updateColorSelectionState()
            }
        }

        // 质地
        val textureCards = mapOf(
            DischargeTexture.WATERY to binding.cardTextureWatery,
            DischargeTexture.STRETCHY to binding.cardTextureStretchy,
            DischargeTexture.THICK to binding.cardTextureThick,
            DischargeTexture.CLUMPY to binding.cardTextureClumpy
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
        val cards = listOf(binding.cardAmountNone, binding.cardAmountLess, binding.cardAmountMedium, binding.cardAmountMore)
        val selected = when (selectedAmount) {
            DischargeAmount.NONE -> binding.cardAmountNone
            DischargeAmount.LESS -> binding.cardAmountLess
            DischargeAmount.MEDIUM -> binding.cardAmountMedium
            DischargeAmount.MORE -> binding.cardAmountMore
        }

        cards.forEach { card ->
            if (card == selected) {
                card.strokeColor = resources.getColor(R.color.md_primary, null)
                card.strokeWidth = 4
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(0x33EC5A9A.toInt())) // Light pinkish
            } else {
                card.strokeWidth = 0
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(0xFFFFF0F3.toInt()))
            }
        }
    }

    private fun updateColorSelectionState() {
        val cards = listOf(binding.cardColorTransparent, binding.cardColorWhite, binding.cardColorYellowish, binding.cardColorBrown)
        val selected = when (selectedColor) {
            DischargeColor.TRANSPARENT -> binding.cardColorTransparent
            DischargeColor.WHITE -> binding.cardColorWhite
            DischargeColor.YELLOWISH -> binding.cardColorYellowish
            DischargeColor.BROWN -> binding.cardColorBrown
        }

        cards.forEach { card ->
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

    private fun updateTextureSelectionState() {
        val cards = listOf(binding.cardTextureWatery, binding.cardTextureStretchy, binding.cardTextureThick, binding.cardTextureClumpy)
        val selected = when (selectedTexture) {
            DischargeTexture.WATERY -> binding.cardTextureWatery
            DischargeTexture.STRETCHY -> binding.cardTextureStretchy
            DischargeTexture.THICK -> binding.cardTextureThick
            DischargeTexture.CLUMPY -> binding.cardTextureClumpy
        }

        cards.forEach { card ->
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
        // 伴随感觉
        binding.chipGroupSensation.setOnCheckedStateChangeListener { group, checkedIds ->
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
        val existingRecord = repository.getVaginalDischargeRecord(selectedDate)
        if (existingRecord != null) {
            selectedAmount = existingRecord.amount
            selectedColor = existingRecord.color
            selectedTexture = existingRecord.texture
            selectedSensation = existingRecord.sensation
            
            updateAmountSelectionState()
            updateColorSelectionState()
            updateTextureSelectionState()
            
            when (existingRecord.sensation) {
                DischargeSensation.FRESH -> binding.chipSensationFresh.isChecked = true
                DischargeSensation.ITCHY -> binding.chipSensationItchy.isChecked = true
                DischargeSensation.ODOR -> binding.chipSensationOdor.isChecked = true
                DischargeSensation.PAIN -> binding.chipSensationPain.isChecked = true
            }
        }
    }

    private fun saveRecord() {
        val record = VaginalDischargeRecord(
            date = selectedDate,
            amount = selectedAmount,
            color = selectedColor,
            texture = selectedTexture,
            sensation = selectedSensation,
            abdominalDiscomfort = false,
            afterIntercourse = false,
            notes = ""
        )
        
        repository.saveVaginalDischargeRecord(record)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("保存成功")
            .setMessage("白带记录已保存")
            .setPositiveButton("确定") { _, _ ->
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
