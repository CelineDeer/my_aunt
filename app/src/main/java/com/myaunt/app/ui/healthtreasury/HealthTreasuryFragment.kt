package com.myaunt.app.ui.healthtreasury

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.myaunt.app.R
import com.myaunt.app.data.ArticleCategory
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.databinding.FragmentHealthTreasuryBinding
import com.myaunt.app.ui.applyStatusBarPaddingTop
import com.myaunt.app.widget.PeriodWidgetUpdater

class HealthTreasuryFragment : Fragment() {

    private var _binding: FragmentHealthTreasuryBinding? = null
    private val binding get() = _binding!!
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var repository: PeriodRepository

    private var selectedCategory: ArticleCategory = ArticleCategory.ALL

    private val exportJsonLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val json = repository.exportDataJson()
        runCatching {
            requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("无法写入文件")
        }.onSuccess {
            Toast.makeText(requireContext(), R.string.export_ok, Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
        }
    }

    private val importJsonLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val text = runCatching {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull() ?: return@registerForActivityResult
        repository.importDataJson(text)
            .onSuccess {
                PeriodWidgetUpdater.updateAll(requireContext())
                Toast.makeText(requireContext(), R.string.import_ok, Toast.LENGTH_SHORT).show()
            }
            .onFailure { e ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.import_fail) + (e.message?.let { "\n$it" } ?: ""),
                    Toast.LENGTH_LONG,
                ).show()
            }
    }

    private val allArticles = HealthTreasuryContent.articles

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHealthTreasuryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PeriodRepository(requireContext())
        binding.root.applyStatusBarPaddingTop()

        setupRecyclerView()
        setupCategoryChips()
        setupSearch()
        setupBackupActions()
        applyFilter()
    }

    private fun setupRecyclerView() {
        articleAdapter = ArticleAdapter()
        binding.recyclerArticles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = articleAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { applyFilter() }
    }

    private fun setupBackupActions() {
        binding.btnExportData.setOnClickListener {
            exportJsonLauncher.launch("my_aunt_backup.json")
        }
        binding.btnImportData.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_data)
                .setMessage(R.string.data_import_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.data_import_choose_file) { _, _ ->
                    importJsonLauncher.launch(arrayOf("application/json", "application/*", "text/*"))
                }
                .show()
        }
    }

    private fun setupCategoryChips() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chipPeriodCare -> ArticleCategory.PERIOD_CARE
                R.id.chipHealthDiet -> ArticleCategory.HEALTH_DIET
                R.id.chipLifestyle -> ArticleCategory.LIFESTYLE
                else -> ArticleCategory.ALL
            }
            applyFilter()
        }

        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            updateChipStyle(binding.chipAll, isChecked)
        }
        binding.chipPeriodCare.setOnCheckedChangeListener { _, isChecked ->
            updateChipStyle(binding.chipPeriodCare, isChecked)
        }
        binding.chipHealthDiet.setOnCheckedChangeListener { _, isChecked ->
            updateChipStyle(binding.chipHealthDiet, isChecked)
        }
        binding.chipLifestyle.setOnCheckedChangeListener { _, isChecked ->
            updateChipStyle(binding.chipLifestyle, isChecked)
        }
    }

    private fun updateChipStyle(chip: com.google.android.material.chip.Chip, isChecked: Boolean) {
        if (isChecked) {
            chip.setChipBackgroundColorResource(R.color.md_primary)
            chip.setTextColor(Color.WHITE)
        } else {
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_chip_outline_text))
        }
    }

    private fun applyFilter() {
        var list = if (selectedCategory == ArticleCategory.ALL) {
            allArticles
        } else {
            allArticles.filter { it.category == selectedCategory }
        }
        val q = binding.etSearch.text?.toString()?.trim().orEmpty()
        if (q.isNotEmpty()) {
            list = list.filter { article ->
                article.title.contains(q, ignoreCase = true) ||
                    article.subtitle.contains(q, ignoreCase = true) ||
                    article.summary.contains(q, ignoreCase = true) ||
                    article.body.contains(q, ignoreCase = true)
            }
        }
        articleAdapter.submitList(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
