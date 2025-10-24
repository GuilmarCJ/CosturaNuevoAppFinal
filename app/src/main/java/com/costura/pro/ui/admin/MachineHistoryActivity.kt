package com.costura.pro.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.costura.pro.databinding.ActivityMachineHistoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MachineHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMachineHistoryBinding
    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var historyAdapter: MachineHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMachineHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadHistory()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = MachineHistoryAdapter(emptyList())
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MachineHistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun loadHistory() {
        showLoading(true)

        scope.launch {
            try {
                val machineRepository = (application as com.costura.pro.CosturaProApp).machineRepository
                val historyFlow = withContext(Dispatchers.IO) {
                    machineRepository.getAllHistory()
                }

                historyFlow.collect { history ->
                    historyAdapter.updateData(history)
                    showLoading(false)
                    updateEmptyState(history.isEmpty())
                }
            } catch (e: Exception) {
                showLoading(false)
                updateEmptyState(true)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateEmptyState(empty: Boolean) {
        binding.tvEmptyHistory.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvHistory.visibility = if (empty) View.GONE else View.VISIBLE
    }
}