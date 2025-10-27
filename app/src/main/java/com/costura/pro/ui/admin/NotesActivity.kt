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
import com.costura.pro.databinding.DialogSimpleInputBinding
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
            checkSaveBeforeExit()
        }
        binding.toolbar.title = "Bloc de Notas"
    }

    private fun setupUI() {
        // Configurar movimiento de scroll
        binding.etNoteContent.movementMethod = android.text.method.ScrollingMovementMethod()

        // Contenido de ejemplo si está vacío
        if (binding.etNoteContent.text.isEmpty()) {
            binding.etNoteContent.setText(getDefaultTemplate())
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
        // Barra superior - Operaciones básicas
        binding.btnNewNote.setOnClickListener { createNewNote() }
        binding.btnSaveNote.setOnClickListener { saveNoteToFile() }
        binding.btnLoadNote.setOnClickListener { openTextFile() }
        binding.btnShareNote.setOnClickListener { shareNote() }

        // Barra inferior - Funciones avanzadas
        binding.btnFormat.setOnClickListener { showFormatOptions() }
        binding.btnTemplates.setOnClickListener { showTemplates() }
        binding.btnTools.setOnClickListener { showTools() }
    }

    private fun updateUI() {
        val wordCount = getWordCount()
        if (isModified) {
            binding.toolbar.title = "${currentFile?.name ?: "Nueva Nota"} *"
            binding.tvStatus.text = "Modificado - $wordCount palabras"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            binding.toolbar.title = currentFile?.name ?: "Nueva Nota"
            binding.tvStatus.text = "Guardado - $wordCount palabras"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        }
    }

    // === OPERACIONES BÁSICAS ===
    private fun createNewNote() {
        if (isModified) {
            showSaveConfirmation {
                binding.etNoteContent.setText("")
                currentFile = null
                originalContent = ""
                isModified = false
                updateUI()
                Toast.makeText(this, "Nueva nota creada", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.etNoteContent.setText("")
            currentFile = null
            originalContent = ""
            isModified = false
            updateUI()
            Toast.makeText(this, "Nueva nota creada", Toast.LENGTH_SHORT).show()
        }
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

            Toast.makeText(this, "Nota guardada: $fileName", Toast.LENGTH_LONG).show()
            updateUI()

        } catch (e: Exception) {
            Toast.makeText(this, "Error guardando: ${e.message}", Toast.LENGTH_LONG).show()
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
                currentFile = null
                originalContent = content
                isModified = false
                updateUI()
                Toast.makeText(this, "Nota cargada", Toast.LENGTH_SHORT).show()
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

    // === FUNCIONES DE FORMATEO ===
    private fun showFormatOptions() {
        val options = arrayOf("📋 Lista con viñetas", "🔢 Lista numerada", "📅 Insertar fecha", "🧹 Limpiar formato")

        AlertDialog.Builder(this)
            .setTitle("Opciones de Formato")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> insertAtCursor("• ")
                    1 -> insertAtCursor("1. ")
                    2 -> insertAtCursor(SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date()))
                    3 -> clearFormatting()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // === PLANTILLAS ===
    private fun showTemplates() {
        val templates = arrayOf("📝 Lista de Trabajo", "📅 Agenda Diaria", "💰 Presupuesto", "📦 Inventario")

        AlertDialog.Builder(this)
            .setTitle("Insertar Plantilla")
            .setItems(templates) { _, which ->
                when (which) {
                    0 -> insertWorkTemplate()
                    1 -> insertAgendaTemplate()
                    2 -> insertBudgetTemplate()
                    3 -> insertInventoryTemplate()
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
            • Manga - 50 unidades - S/0.40
            • Cuerpo - 30 unidades - S/0.60
            • Cuello - 25 unidades - S/0.35
            
            👥 ASIGNACIONES:
            • María: Coser mangas
            • Juan: Ensamblar cuerpos
            • Pedro: Control calidad
            
            📋 MATERIALES:
            • Hilo negro - 5 carretes
            • Agujas #14 - 2 paquetes
            • Tela algodón - 50 metros
        """.trimIndent()

        insertAtCursor(template)
    }

    private fun insertAgendaTemplate() {
        val template = """
            === AGENDA DIARIA ===
            ${SimpleDateFormat("EEEE, dd MMMM yyyy").format(Date())}
            
            🌅 MAÑANA:
            • Revisar pedidos pendientes
            • Asignar tareas al personal
            
            🌞 TARDE:
            • Control de calidad
            • Empaquetar productos
            
            🌙 NOCHE:
            • Preparar envíos
            • Planificar día siguiente
        """.trimIndent()

        insertAtCursor(template)
    }

    // === HERRAMIENTAS ===
    private fun showTools() {
        val tools = arrayOf("📊 Contar palabras", "🔍 Buscar texto", "📏 Estadísticas")

        AlertDialog.Builder(this)
            .setTitle("Herramientas")
            .setItems(tools) { _, which ->
                when (which) {
                    0 -> showWordCount()
                    1 -> showSearchDialog()
                    2 -> showStatistics()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showWordCount() {
        val words = getWordCount()
        val chars = binding.etNoteContent.text.length
        val lines = binding.etNoteContent.text.split("\n").size

        val message = """
            📊 ESTADÍSTICAS:
            
            📝 Palabras: $words
            🔤 Caracteres: $chars
            📄 Líneas: $lines
            
            💡 Promedio: ${"%.1f".format(words.toDouble() / lines.coerceAtLeast(1))} palabras/línea
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Contador de Palabras")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSearchDialog() {
        val dialogBinding = DialogSimpleInputBinding.inflate(layoutInflater)
        dialogBinding.etInput.hint = "Texto a buscar"

        AlertDialog.Builder(this)
            .setTitle("Buscar Texto")
            .setView(dialogBinding.root)
            .setPositiveButton("Buscar") { _, _ ->
                val searchText = dialogBinding.etInput.text.toString()
                if (searchText.isNotEmpty()) {
                    findText(searchText)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // === MÉTODOS AUXILIARES ===
    private fun insertAtCursor(text: String) {
        val current = binding.etNoteContent.text
        val position = binding.etNoteContent.selectionStart
        current.insert(position, "\n$text")
        isModified = true
        updateUI()
    }

    private fun clearFormatting() {
        val content = binding.etNoteContent.text.toString()
        val cleaned = content.replace("\\s+".toRegex(), " ").trim()
        binding.etNoteContent.setText(cleaned)
        isModified = true
        updateUI()
    }

    private fun findText(text: String) {
        val content = binding.etNoteContent.text.toString()
        val index = content.indexOf(text, ignoreCase = true)
        if (index >= 0) {
            binding.etNoteContent.setSelection(index, index + text.length)
            Toast.makeText(this, "Texto encontrado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Texto no encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWordCount(): Int {
        return binding.etNoteContent.text.split("\\s+".toRegex()).count { it.isNotBlank() }
    }

    private fun showStatistics() {
        val content = binding.etNoteContent.text.toString()
        val words = getWordCount()
        val chars = content.length
        val lines = content.split("\n").size
        val paragraphs = content.split("\n\n").count { it.isNotBlank() }

        val stats = """
            📈 ESTADÍSTICAS DETALLADAS:
            
            📝 Palabras: $words
            🔤 Caracteres: $chars
            📄 Líneas: $lines
            🏷️ Párrafos: $paragraphs
            
            📏 Caracteres (sin espacios): ${content.replace("\\s".toRegex(), "").length}
            💡 Densidad: ${"%.1f".format(words.toDouble() / lines.coerceAtLeast(1))} palabras/línea
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Estadísticas Avanzadas")
            .setMessage(stats)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun checkSaveBeforeExit() {
        if (isModified) {
            showSaveConfirmation { finish() }
        } else {
            finish()
        }
    }

    private fun showSaveConfirmation(onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Guardar cambios")
            .setMessage("¿Deseas guardar los cambios antes de continuar?")
            .setPositiveButton("Guardar") { _, _ ->
                saveNoteToFile()
                onConfirm()
            }
            .setNegativeButton("Descartar") { _, _ -> onConfirm() }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun getDefaultTemplate(): String {
        return """📝 BLOC DE NOTAS - COSTURA PRO

=== LISTA DE TRABAJO ===
• Manga - 50 paquetes x S/0.40
• Cuerpo - 30 paquetes x S/0.60  
• Cuello - 25 paquetes x S/0.35

💡 Toca los botones inferiores para:
🎨 Formatear texto
📋 Insertar plantillas
📊 Usar herramientas"""
    }

    private fun getNotesDirectory(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "CosturaPro/Notas")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("ddMMyyyy_HHmmss").format(Date())
    }

    override fun onBackPressed() {
        checkSaveBeforeExit()
    }

    companion object {
        private const val REQUEST_CODE_PICK_TXT = 1002
    }

    // Métodos de plantillas que faltaban
    private fun insertBudgetTemplate() {
        val template = """
            === PRESUPUESTO MENSUAL ===
            Mes: ${SimpleDateFormat("MMMM yyyy").format(Date())}
            
            💰 INGRESOS:
            • Ventas: S/ 
            • Servicios: S/ 
            
            💸 GASTOS:
            • Materiales: S/ 
            • Salarios: S/ 
            • Alquiler: S/ 
        """.trimIndent()
        insertAtCursor(template)
    }

    private fun insertInventoryTemplate() {
        val template = """
            === INVENTARIO ===
            Fecha: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}
            
            📦 PRODUCTOS:
            • Manga:  unidades
            • Cuerpo:  unidades  
            • Cuello:  unidades
            
            📋 MATERIALES:
            • Tela:  metros
            • Hilo:  carretes
            • Agujas:  unidades
        """.trimIndent()
        insertAtCursor(template)
    }
}