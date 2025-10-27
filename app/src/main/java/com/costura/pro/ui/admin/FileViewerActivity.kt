package com.costura.pro.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.costura.pro.databinding.ActivityFileViewerBinding
import com.costura.pro.databinding.DialogEditCellBinding
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import com.costura.pro.R
class FileViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileViewerBinding
    private var currentFile: File? = null
    private var csvData: MutableList<MutableList<String>> = mutableListOf()
    private var headers: MutableList<String> = mutableListOf()
    private lateinit var adapter: CsvAdapter
    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        setupRecyclerView()
        loadFileFromIntent()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (isEditing) {
                showSaveConfirmationDialog()
            } else {
                onBackPressed()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = CsvAdapter(csvData, headers,
            onCellEdit = { row, col, newValue ->
                editCell(row, col, newValue)
            },
            onDeleteRow = { position ->
                deleteRow(position)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FileViewerActivity)
            adapter = this@FileViewerActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnShareFile.setOnClickListener {
            shareFile()
        }

        binding.btnOpenExternal.setOnClickListener {
            openWithExternalApp()
        }

        binding.btnEditMode.setOnClickListener {
            toggleEditMode()
        }

        binding.btnAddRow.setOnClickListener {
            addNewRow()
        }

        binding.btnAddColumn.setOnClickListener {
            addNewColumn()
        }

        binding.btnSaveFile.setOnClickListener {
            saveFile()
        }

        binding.btnSearch.setOnClickListener {
            showSearchDialog()
        }

        binding.btnCalculate.setOnClickListener {
            showCalculationsDialog()
        }

        // Buscador en tiempo real
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterData(s.toString())
            }
        })
    }

    private fun toggleEditMode() {
        isEditing = !isEditing
        updateUI()
        adapter.setEditing(isEditing)
    }

    private fun updateUI() {
        if (isEditing) {
            binding.toolbar.title = "${currentFile?.name ?: "Archivo"} - EDITANDO"
            binding.btnEditMode.text = "MODO VISUAL"
            binding.btnEditMode.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            binding.layoutEditControls.visibility = View.VISIBLE
        } else {
            binding.toolbar.title = currentFile?.name ?: "Visor de Archivos"
            binding.btnEditMode.text = "MODO EDICIÓN"
            binding.btnEditMode.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            binding.layoutEditControls.visibility = View.GONE
        }
    }

    private fun loadFileFromIntent() {
        when {
            intent.hasExtra("file_path") -> {
                val filePath = intent.getStringExtra("file_path")
                currentFile = File(filePath)
                loadFileContent(currentFile!!)
            }
            else -> {
                Toast.makeText(this, "No se pudo cargar el archivo", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadFileContent(file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(this, "El archivo no existe", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            binding.toolbar.title = file.name
            binding.tvFileInfo.text = "Tamaño: ${formatFileSize(file.length())} • Creado: ${getFileDate(file)}"

            // Leer CSV
            csvData.clear()
            file.forEachLine { line ->
                val row = line.split(",").map { it.trim() }.toMutableList()
                csvData.add(row)
            }

            // Si no hay datos, crear estructura básica
            if (csvData.isEmpty()) {
                headers = mutableListOf("Producto", "Cantidad", "Precio", "Total")
                csvData.add(headers.toMutableList())
                // Agregar algunas filas de ejemplo
                csvData.add(mutableListOf("Manga", "50", "0.40", "20.00"))
                csvData.add(mutableListOf("Cuerpo", "30", "0.60", "18.00"))
                csvData.add(mutableListOf("Cuello", "25", "0.35", "8.75"))
            } else {
                headers = if (csvData.isNotEmpty()) {
                    csvData[0].toMutableList()
                } else {
                    mutableListOf("Columna 1", "Columna 2", "Columna 3")
                }
            }

            adapter.updateData(csvData, headers)
            updateCalculations()

        } catch (e: Exception) {
            Toast.makeText(this, "Error leyendo archivo: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun editCell(row: Int, col: Int, newValue: String) {
        if (row < csvData.size && col < csvData[row].size) {
            csvData[row][col] = newValue
            isEditing = true
            updateUI()
            updateCalculations()
        }
    }

    private fun addNewRow() {
        val newRow = MutableList(headers.size) { "" }
        csvData.add(newRow)
        adapter.updateData(csvData, headers)
        isEditing = true
        updateUI()
    }

    private fun addNewColumn() {
        val dialogBinding = DialogEditCellBinding.inflate(LayoutInflater.from(this))
        dialogBinding.etCellValue.hint = "Nombre de la columna"

        AlertDialog.Builder(this)
            .setTitle("Agregar Nueva Columna")
            .setView(dialogBinding.root)
            .setPositiveButton("Agregar") { _, _ ->
                val columnName = dialogBinding.etCellValue.text.toString().trim()
                if (columnName.isNotEmpty()) {
                    headers.add(columnName)
                    csvData.forEach { row ->
                        row.add("")
                    }
                    adapter.updateData(csvData, headers)
                    isEditing = true
                    updateUI()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteRow(position: Int) {
        if (position > 0 && position < csvData.size) { // No eliminar encabezados
            csvData.removeAt(position)
            adapter.updateData(csvData, headers)
            isEditing = true
            updateUI()
        }
    }

    private fun saveFile() {
        currentFile?.let { file ->
            try {
                FileWriter(file).use { writer ->
                    csvData.forEach { row ->
                        writer.write(row.joinToString(","))
                        writer.write("\n")
                    }
                }
                isEditing = false
                updateUI()
                Toast.makeText(this, "Archivo guardado exitosamente", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error guardando archivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(this, "No hay archivo para guardar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSearchDialog() {
        val dialogBinding = DialogEditCellBinding.inflate(LayoutInflater.from(this))
        dialogBinding.etCellValue.hint = "Texto a buscar"

        AlertDialog.Builder(this)
            .setTitle("Buscar en el archivo")
            .setView(dialogBinding.root)
            .setPositiveButton("Buscar") { _, _ ->
                val searchText = dialogBinding.etCellValue.text.toString().trim()
                if (searchText.isNotEmpty()) {
                    filterData(searchText)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun filterData(searchText: String) {
        val filteredData = if (searchText.isEmpty()) {
            csvData
        } else {
            csvData.filter { row ->
                row.any { cell -> cell.contains(searchText, ignoreCase = true) }
            }.toMutableList()
        }
        adapter.updateData(filteredData, headers)
    }

    private fun updateCalculations() {
        if (csvData.size > 1) {
            val calculations = StringBuilder("📊 RESUMEN: ")

            // Calcular suma de columnas numéricas
            headers.forEachIndexed { index, header ->
                val columnValues = csvData.drop(1).mapNotNull { row ->
                    if (index < row.size) row[index].toDoubleOrNull() else null
                }
                if (columnValues.isNotEmpty()) {
                    val sum = columnValues.sum()
                    calculations.append("$header=$${"%.2f".format(sum)} ")
                }
            }

            binding.tvCalculations.text = calculations.toString()
        } else {
            binding.tvCalculations.text = "📊 Agrega datos para ver cálculos"
        }
    }

    private fun showCalculationsDialog() {
        val calculations = StringBuilder("📊 ESTADÍSTICAS DETALLADAS\n\n")

        if (csvData.size > 1) {
            headers.forEachIndexed { index, header ->
                val columnValues = csvData.drop(1).mapNotNull { row ->
                    if (index < row.size) row[index].toDoubleOrNull() else null
                }
                if (columnValues.isNotEmpty()) {
                    val sum = columnValues.sum()
                    val avg = columnValues.average()
                    val max = columnValues.maxOrNull() ?: 0.0
                    val min = columnValues.minOrNull() ?: 0.0
                    calculations.append("$header:\n")
                    calculations.append("  • Suma: $${"%.2f".format(sum)}\n")
                    calculations.append("  • Promedio: $${"%.2f".format(avg)}\n")
                    calculations.append("  • Máximo: $${"%.2f".format(max)}\n")
                    calculations.append("  • Mínimo: $${"%.2f".format(min)}\n")
                    calculations.append("  • Registros: ${columnValues.size}\n\n")
                }
            }
        } else {
            calculations.append("No hay datos suficientes para calcular estadísticas")
        }

        AlertDialog.Builder(this)
            .setTitle("Cálculos y Estadísticas")
            .setMessage(calculations.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSaveConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cambios sin guardar")
            .setMessage("¿Deseas guardar los cambios antes de salir?")
            .setPositiveButton("Guardar") { _, _ ->
                saveFile()
                finish()
            }
            .setNegativeButton("Descartar") { _, _ ->
                finish()
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun shareFile() {
        currentFile?.let { file ->
            try {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.provider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "text/csv"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Compartir archivo"))

            } catch (e: Exception) {
                Toast.makeText(this, "Error compartiendo archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openWithExternalApp() {
        currentFile?.let { file ->
            try {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.provider",
                    file
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/csv")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(intent, "Abrir con"))
            } catch (e: Exception) {
                Toast.makeText(this, "Error abriendo archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    private fun getFileDate(file: File): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
    }

    override fun onBackPressed() {
        if (isEditing) {
            showSaveConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }
}

// Adapter simplificado y funcional
class CsvAdapter(
    private var data: MutableList<MutableList<String>>,
    private var headers: MutableList<String>,
    private val onCellEdit: (Int, Int, String) -> Unit,
    private val onDeleteRow: (Int) -> Unit
) : RecyclerView.Adapter<CsvAdapter.ViewHolder>() {

    private var isEditing = false

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rowContainer: LinearLayout = itemView.findViewById(R.id.rowContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_csv_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.rowContainer.removeAllViews()

        val rowData = data[position]
        val isHeaderRow = position == 0

        for (i in 0 until headers.size) {
            val cellValue = if (i < rowData.size) rowData[i] else ""

            if (isHeaderRow) {
                // Encabezados - siempre visibles como texto
                val headerView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_csv_header, holder.rowContainer, false)
                val textView = headerView.findViewById<TextView>(R.id.tvHeader)
                textView.text = headers[i]
                holder.rowContainer.addView(headerView)
            } else {
                // Celdas de datos - editables en modo edición
                val cellView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_csv_cell, holder.rowContainer, false)
                val textView = cellView.findViewById<TextView>(R.id.tvCell)
                val editText = cellView.findViewById<EditText>(R.id.etCell)

                textView.text = cellValue
                editText.setText(cellValue)

                if (isEditing) {
                    textView.visibility = View.GONE
                    editText.visibility = View.VISIBLE

                    editText.setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            onCellEdit(position, i, editText.text.toString())
                        }
                    }
                } else {
                    textView.visibility = View.VISIBLE
                    editText.visibility = View.GONE
                }

                holder.rowContainer.addView(cellView)
            }
        }

        // Botón eliminar para filas de datos en modo edición
        if (!isHeaderRow && isEditing) {
            val deleteButton = Button(holder.itemView.context).apply {
                text = "X"
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                setOnClickListener {
                    onDeleteRow(position)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 0, 0)
                }
            }
            holder.rowContainer.addView(deleteButton)
        }
    }

    override fun getItemCount(): Int = data.size

    fun updateData(newData: MutableList<MutableList<String>>, newHeaders: MutableList<String>) {
        data = newData
        headers = newHeaders
        notifyDataSetChanged()
    }

    fun setEditing(editing: Boolean) {
        isEditing = editing
        notifyDataSetChanged()
    }
}