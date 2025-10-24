package com.costura.pro.ui.admin

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.costura.pro.databinding.ActivityNotesBinding
import com.costura.pro.databinding.DialogFindReplaceBinding
import com.costura.pro.databinding.DialogNewNoteBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private var currentFile: File? = null
    private var isModified = false
    private var originalContent = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (isModified) {
                showSaveConfirmationDialog()
            } else {
                onBackPressed()
            }
        }
    }

    private fun setupUI() {
        // Configurar movimiento de scroll
        binding.etNoteContent.movementMethod = android.text.method.ScrollingMovementMethod()

        // Contenido de ejemplo si está vacío
        if (binding.etNoteContent.text.isEmpty()) {
            binding.etNoteContent.setText(
                "📝 BLOC DE NOTAS - COSTURA PRO\n\n" +
                        "=== LISTA DE TRABAJO PENDIENTE ===\n" +
                        "✅ Manga - 50 paquetes x S/0.40\n" +
                        "✅ Cuerpo - 30 paquetes x S/0.60\n" +
                        "⏳ Cuello - 25 paquetes x S/0.35\n\n" +
                        "📋 NOTAS IMPORTANTES:\n" +
                        "• Revisar calidad de hilos\n" +
                        "• Pedir más agujas tamaño 14\n" +
                        "• Entregar pedido cliente ABC el viernes\n\n" +
                        "🔔 RECORDATORIOS:\n" +
                        "📅 Reunión con proveedor: 25/10\n" +
                        "💰 Cobrar factura #1234\n" +
                        "📦 Preparar envío para exportación\n\n" +
                        "💡 IDEAS:\n" +
                        "- Implementar nuevo sistema de control\n" +
                        "- Capacitar personal en nuevas técnicas\n" +
                        "- Expandir línea de productos"
            )
        }

        originalContent = binding.etNoteContent.text.toString()

        // Detectar cambios en el texto
        binding.etNoteContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                isModified = originalContent != s.toString()
                updateUI()
            }
        })

        updateUI()
    }

    private fun setupClickListeners() {
        binding.btnNewNote.setOnClickListener {
            if (isModified) {
                showSaveBeforeNewDialog()
            } else {
                createNewNote()
            }
        }

        binding.btnSaveNote.setOnClickListener {
            saveNoteToFile()
        }

        binding.btnLoadNote.setOnClickListener {
            openTextFile()
        }

        binding.btnShareNote.setOnClickListener {
            shareNote()
        }

        binding.btnFormatText.setOnClickListener {
            showFormatOptionsDialog()
        }

        binding.btnFindReplace.setOnClickListener {
            showFindReplaceDialog()
        }

        binding.btnWordCount.setOnClickListener {
            showWordCountDialog()
        }

        binding.btnInsertTemplate.setOnClickListener {
            showTemplateDialog()
        }

        binding.btnExportPdf.setOnClickListener {
            exportToPdf()
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun updateUI() {
        if (isModified) {
            binding.toolbar.title = "${currentFile?.name ?: "NUEVA NOTA"} *"
            binding.tvStatus.text = "Modificado - ${getWordCount()} palabras"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            binding.toolbar.title = currentFile?.name ?: "NUEVA NOTA"
            binding.tvStatus.text = "Guardado - ${getWordCount()} palabras"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        }
    }

    private fun createNewNote() {
        val dialogBinding = DialogNewNoteBinding.inflate(layoutInflater)

        AlertDialog.Builder(this)
            .setTitle("Nueva Nota")
            .setView(dialogBinding.root)
            .setPositiveButton("Crear") { _, _ ->
                val title = dialogBinding.etNoteTitle.text.toString().trim()
                binding.etNoteContent.setText("")
                currentFile = null
                binding.toolbar.title = title.ifEmpty { "NUEVA NOTA" }
                originalContent = ""
                isModified = false
                updateUI()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveNoteToFile() {
        try {
            val content = binding.etNoteContent.text.toString()
            if (content.isEmpty()) {
                Toast.makeText(this, "La nota está vacía", Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = if (currentFile != null) {
                currentFile!!.name
            } else {
                "Nota_${getCurrentDateTime()}.txt"
            }

            val file = File(getNotesDirectory(), fileName)

            FileOutputStream(file).use { outputStream ->
                outputStream.write(content.toByteArray())
            }

            currentFile = file
            originalContent = content
            isModified = false
            binding.toolbar.title = fileName

            Toast.makeText(this, "Nota guardada: $fileName", Toast.LENGTH_LONG).show()
            updateUI()

        } catch (e: Exception) {
            Toast.makeText(this, "Error guardando nota: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openTextFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/plain"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, REQUEST_CODE_PICK_TXT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_TXT && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                loadNoteFromUri(uri)
            }
        }
    }

    private fun loadNoteFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                binding.etNoteContent.setText(content)

                // Obtener nombre del archivo
                val fileName = getFileName(uri)
                currentFile = File(getNotesDirectory(), fileName ?: "nota_cargada.txt")
                originalContent = content
                isModified = false
                binding.toolbar.title = fileName ?: "NOTA CARGADA"

                Toast.makeText(this, "Nota cargada", Toast.LENGTH_SHORT).show()
                updateUI()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error cargando nota", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareNote() {
        val content = binding.etNoteContent.text.toString()
        if (content.isEmpty()) {
            Toast.makeText(this, "La nota está vacía", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Nota Costura Pro")
        intent.putExtra(Intent.EXTRA_TEXT, content)

        startActivity(Intent.createChooser(intent, "Compartir nota"))
    }

    private fun showFormatOptionsDialog() {
        val formats = arrayOf(
            "📋 Lista con viñetas",
            "🔢 Lista numerada",
            "🏷️ Insertar fecha/hora",
            "📊 Insertar tabla simple",
            "🧹 Limpiar formato",
            "🔤 Convertir a mayúsculas",
            "🔡 Convertir a minúsculas"
        )

        AlertDialog.Builder(this)
            .setTitle("Opciones de Formato")
            .setItems(formats) { _, which ->
                when (which) {
                    0 -> insertBulletList()
                    1 -> insertNumberedList()
                    2 -> insertDateTime()
                    3 -> insertSimpleTable()
                    4 -> clearFormatting()
                    5 -> convertToUppercase()
                    6 -> convertToLowercase()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFindReplaceDialog() {
        val dialogBinding = DialogFindReplaceBinding.inflate(layoutInflater)

        AlertDialog.Builder(this)
            .setTitle("Buscar y Reemplazar")
            .setView(dialogBinding.root)
            .setPositiveButton("Reemplazar") { _, _ ->
                val findText = dialogBinding.etFind.text.toString()
                val replaceText = dialogBinding.etReplace.text.toString()
                if (findText.isNotEmpty()) {
                    findAndReplace(findText, replaceText)
                }
            }
            .setNeutralButton("Buscar") { _, _ ->
                val findText = dialogBinding.etFind.text.toString()
                if (findText.isNotEmpty()) {
                    findText(findText)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun findAndReplace(findText: String, replaceText: String) {
        val content = binding.etNoteContent.text.toString()
        val newContent = content.replace(findText, replaceText, ignoreCase = true)
        binding.etNoteContent.setText(newContent)
        Toast.makeText(this, "Reemplazado: $findText → $replaceText", Toast.LENGTH_SHORT).show()
    }

    private fun findText(findText: String) {
        val content = binding.etNoteContent.text.toString()
        val index = content.indexOf(findText, ignoreCase = true)
        if (index >= 0) {
            binding.etNoteContent.setSelection(index, index + findText.length)
            Toast.makeText(this, "Texto encontrado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Texto no encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showWordCountDialog() {
        val content = binding.etNoteContent.text.toString()
        val words = content.split("\\s+".toRegex()).count { it.isNotBlank() }
        val characters = content.length
        val lines = content.split("\n").size
        val paragraphs = content.split("\n\n").count { it.isNotBlank() }

        val stats = """
            📊 ESTADÍSTICAS DEL TEXTO:
            
            📝 Palabras: $words
            🔤 Caracteres: $characters
            📄 Líneas: $lines
            🏷️ Párrafos: $paragraphs
            📏 Caracteres (sin espacios): ${content.replace("\\s".toRegex(), "").length}
            
            💡 Densidad: ${"%.1f".format(words.toDouble() / lines.coerceAtLeast(1))} palabras/línea
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Contador de Palabras")
            .setMessage(stats)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun getWordCount(): Int {
        val content = binding.etNoteContent.text.toString()
        return content.split("\\s+".toRegex()).count { it.isNotBlank() }
    }

    private fun showTemplateDialog() {
        val templates = arrayOf(
            "📋 Lista de Trabajo (Costura)",
            "📅 Agenda Diaria",
            "💰 Presupuesto Mensual",
            "📦 Control de Inventario",
            "👥 Acta de Reunión",
            "🎯 Plan de Objetivos",
            "👨‍💼 Evaluación de Personal",
            "📊 Reporte de Producción"
        )

        AlertDialog.Builder(this)
            .setTitle("Insertar Plantilla")
            .setItems(templates) { _, which ->
                when (which) {
                    0 -> insertWorkTemplate()
                    1 -> insertDailyAgendaTemplate()
                    2 -> insertBudgetTemplate()
                    3 -> insertInventoryTemplate()
                    4 -> insertMeetingTemplate()
                    5 -> insertGoalsTemplate()
                    6 -> insertEmployeeEvaluationTemplate()
                    7 -> insertProductionReportTemplate()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun insertWorkTemplate() {
        val template = """
            === LISTA DE TRABAJO ===
            Fecha: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}
            
            📦 PEDIDOS PENDIENTES:
            • [Producto] - [Cantidad] - [Fecha Entrega]
            • [Producto] - [Cantidad] - [Fecha Entrega]
            
            👥 ASIGNACIONES:
            • [Trabajador]: [Tarea]
            • [Trabajador]: [Tarea]
            
            📋 MATERIALES NECESARIOS:
            • [Material] - [Cantidad]
            • [Material] - [Cantidad]
            
            ⚠️ OBSERVACIONES:
            • 
        """.trimIndent()

        insertAtCursor(template)
    }



    private fun insertEmployeeEvaluationTemplate() {
        val template = """
        === EVALUACIÓN DE PERSONAL ===
        Fecha: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}
        Empleado: 
        Puesto: 
        
        📊 EVALUACIÓN POR COMPETENCIAS:
        • Productividad: /10
        • Calidad: /10
        • Puntualidad: /10
        • Trabajo en equipo: /10
        • Iniciativa: /10
        ──────────────────────────────
        PROMEDIO: /10
        
        ✅ FORTALEZAS:
        • 
        • 
        
        📈 ÁREAS DE MEJORA:
        • 
        • 
        
        🎯 OBJETIVOS PARA EL PRÓXIMO PERÍODO:
        1. 
        2. 
        3. 
        
        💬 COMENTARIOS:
        • 
    """.trimIndent()

        insertAtCursor(template)
    }

    private fun insertProductionReportTemplate() {
        val template = """
        === REPORTE DE PRODUCCIÓN ===
        Período: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}
        
        🏭 PRODUCCIÓN TOTAL:
        | Producto | Meta | Real | % Cumplimiento |
        |----------|------|------|----------------|
        | Manga    |      |      | %              |
        | Cuerpo   |      |      | %              |
        | Cuello   |      |      | %              |
        | Otros    |      |      | %              |
        
        📊 ESTADÍSTICAS:
        • Total unidades producidas: 
        • Promedio diario: 
        • Eficiencia: % 
        • Rechazos:  unidades (%)
        
        🔧 INCIDENTES/PROBLEMAS:
        • 
        • 
        
        💡 SUGERENCIAS DE MEJORA:
        • 
        • 
        
        🎯 PLAN ACCIÓN CORRECTIVA:
        • 
        • 
    """.trimIndent()

        insertAtCursor(template)
    }



    private fun insertDailyAgendaTemplate() {
        val template = """
        === AGENDA DIARIA ===
        ${SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())}
        
        🌅 MAÑANA (6:00 - 12:00):
        • 
        • 
        
        🌞 TARDE (12:00 - 18:00):
        • 
        • 
        
        🌙 NOCHE (18:00 - 22:00):
        • 
        • 
        
        ✅ LOGROS DEL DÍA:
        • 
        
        🎯 OBJETIVOS MAÑANA:
        • 
    """.trimIndent()

        insertAtCursor(template)
    }

    private fun insertBudgetTemplate() {
        val template = """
        === PRESUPUESTO ===
        Mes: ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}
        
        💰 INGRESOS:
        • Venta productos: S/ 
        • Servicios: S/ 
        • Otros: S/ 
        ─────────────────────
        TOTAL INGRESOS: S/ 
        
        💸 GASTOS:
        • Materiales: S/ 
        • Salarios: S/ 
        • Alquiler: S/ 
        • Servicios: S/ 
        • Transporte: S/ 
        ─────────────────────
        TOTAL GASTOS: S/ 
        
        📊 BALANCE:
        • Ganancia Neta: S/ 
        • Margen: % 
        
        📈 OBSERVACIONES:
        • 
    """.trimIndent()

        insertAtCursor(template)
    }

    private fun insertInventoryTemplate() {
        val template = """
        === INVENTARIO ===
        Fecha: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}
        
        📦 PRODUCTOS EN STOCK:
        | Producto | Cantidad | Precio Unit. | Valor Total |
        |----------|----------|--------------|-------------|
        |          |          | S/           | S/          |
        |          |          | S/           | S/          |
        |          |          | S/           | S/          |
        
        🔄 MOVIMIENTOS:
        • Entradas: 
        • Salidas: 
        • Stock Final: 
        
        ⚠️ PRODUCTOS BAJO STOCK:
        • 
        
        📋 OBSERVACIONES:
        • 
    """.trimIndent()

        insertAtCursor(template)
    }

    private fun insertMeetingTemplate() {
        val template = """
        === ACTA DE REUNIÓN ===
        Fecha: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}
        Hora: ${SimpleDateFormat("HH:mm").format(Date())}
        Lugar: 
        
        👥 ASISTENTES:
        • 
        • 
        • 
        
        📋 ORDEN DEL DÍA:
        1. 
        2. 
        3. 
        
        💬 ACUERDOS TOMADOS:
        • 
        • 
        • 
        
        🎯 ACCIONES PENDIENTES:
        | Responsable | Tarea | Fecha Límite |
        |-------------|-------|--------------|
        |             |       |              |
        |             |       |              |
        
        📅 PRÓXIMA REUNIÓN:
        • Fecha: 
        • Hora: 
        • Tema: 
    """.trimIndent()

        insertAtCursor(template)
    }

    private fun insertGoalsTemplate() {
        val template = """
        === PLAN DE OBJETIVOS ===
        Período: ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}
        
        🎯 OBJETIVOS PRINCIPALES:
        1. 
        2. 
        3. 
        
        📊 METAS ESPECÍFICAS:
        • Ventas: S/ 
        • Producción:  unidades
        • Clientes nuevos: 
        • Eficiencia: % 
        
        📅 PLAN DE ACCIÓN:
        | Actividad | Responsable | Fecha | Estado |
        |-----------|-------------|-------|--------|
        |           |             |       |        |
        |           |             |       |        |
        
        📈 SEGUIMIENTO:
        • Semana 1: 
        • Semana 2: 
        • Semana 3: 
        • Semana 4: 
        
        ✅ LOGROS ALCANZADOS:
        • 
        
        🔄 AJUSTES NECESARIOS:
        • 
    """.trimIndent()

        insertAtCursor(template)
    }

    private fun insertAtCursor(text: String) {
        val currentText = binding.etNoteContent.text
        val cursorPosition = binding.etNoteContent.selectionStart
        currentText.insert(cursorPosition, "\n\n$text\n")
        isModified = true
        updateUI()
    }

    private fun insertBulletList() {
        insertAtCursor("\n• ")
    }

    private fun insertNumberedList() {
        insertAtCursor("\n1. ")
    }

    private fun insertDateTime() {
        val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        insertAtCursor(dateTime)
    }

    private fun insertSimpleTable() {
        val table = """
            
            | Producto | Cantidad | Precio | Total |
            |----------|----------|--------|-------|
            |          |          |        |       |
            |          |          |        |       |
        """.trimIndent()

        insertAtCursor(table)
    }

    private fun clearFormatting() {
        val content = binding.etNoteContent.text.toString()
        // Eliminar múltiples espacios y saltos de línea
        val cleaned = content.replace("\\s+".toRegex(), " ").trim()
        binding.etNoteContent.setText(cleaned)
        isModified = true
        updateUI()
    }

    private fun convertToUppercase() {
        val content = binding.etNoteContent.text.toString()
        binding.etNoteContent.setText(content.uppercase())
        isModified = true
        updateUI()
    }

    private fun convertToLowercase() {
        val content = binding.etNoteContent.text.toString()
        binding.etNoteContent.setText(content.lowercase())
        isModified = true
        updateUI()
    }

    private fun exportToPdf() {
        Toast.makeText(this, "Exportar a PDF - Próximamente", Toast.LENGTH_SHORT).show()
    }

    private fun showSettingsDialog() {
        val settings = arrayOf(
            "📏 Cambiar tamaño de texto",
            "🎨 Cambiar tema",
            "💾 Configuración de auto-guardado",
            "🔍 Configuración de búsqueda"
        )

        AlertDialog.Builder(this)
            .setTitle("Configuración")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> showTextSizeDialog()
                    1 -> showThemeDialog()
                    else -> Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showTextSizeDialog() {
        val sizes = arrayOf("Pequeño", "Normal", "Grande", "Muy Grande")

        AlertDialog.Builder(this)
            .setTitle("Tamaño de Texto")
            .setItems(sizes) { _, which ->
                val scale = when (which) {
                    0 -> 0.8f
                    1 -> 1.0f
                    2 -> 1.4f
                    3 -> 1.8f
                    else -> 1.0f
                }
                binding.etNoteContent.textSize = 16f * scale
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Claro", "Oscuro", "Sepia", "Verde")
        Toast.makeText(this, "Temas - Próximamente", Toast.LENGTH_SHORT).show()
    }

    private fun showSaveConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cambios sin guardar")
            .setMessage("¿Deseas guardar los cambios antes de salir?")
            .setPositiveButton("Guardar") { _, _ ->
                saveNoteToFile()
                finish()
            }
            .setNegativeButton("Descartar") { _, _ ->
                finish()
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun showSaveBeforeNewDialog() {
        AlertDialog.Builder(this)
            .setTitle("Guardar cambios")
            .setMessage("¿Deseas guardar la nota actual antes de crear una nueva?")
            .setPositiveButton("Guardar") { _, _ ->
                saveNoteToFile()
                createNewNote()
            }
            .setNegativeButton("Descartar") { _, _ ->
                createNewNote()
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow("_display_name"))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun getNotesDirectory(): File {
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "CosturaPro/Notas")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onBackPressed() {
        if (isModified) {
            showSaveConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val REQUEST_CODE_PICK_TXT = 1002
    }
}