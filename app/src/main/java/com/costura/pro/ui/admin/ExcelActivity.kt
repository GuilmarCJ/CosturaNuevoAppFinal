package com.costura.pro.ui.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.costura.pro.databinding.ActivityExcelBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExcelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExcelBinding
    private val TAG = "ExcelActivity"

    // Contract para solicitar permisos (método moderno)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            createExcelFile()
        } else {
            showPermissionDeniedDialog()
        }
    }

    // Contract para seleccionar archivo
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { openExcelFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExcelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateExcel.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnLoadExcel.setOnClickListener {
            openExcelFilePicker()
        }
    }

    private fun checkAndRequestPermissions() {
        // Para Android 10+ (API 29+), no se necesitan permisos de almacenamiento para archivos en directorio de la app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // En Android 10+, podemos escribir en el directorio de documentos sin permisos
            createExcelFile()
        } else {
            // Para versiones anteriores, solicitar permisos
            val requiredPermissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            val hasPermissions = requiredPermissions.all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }

            if (hasPermissions) {
                createExcelFile()
            } else {
                // Solicitar permisos
                requestPermissionLauncher.launch(requiredPermissions)
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos necesarios")
            .setMessage("La aplicación necesita permisos de almacenamiento para crear archivos Excel. ¿Desea configurar los permisos manualmente?")
            .setPositiveButton("Configurar") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la configuración", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createExcelFile() {
        var outputStream: FileOutputStream? = null

        try {
            // Crear datos de ejemplo
            val data = listOf(
                arrayOf("1", "Manga", "50", "0.40", "20.00", "25/10/2025", "Pendiente"),
                arrayOf("2", "Cuerpo", "30", "0.60", "18.00", "26/10/2025", "En Proceso"),
                arrayOf("3", "Cuello", "25", "0.35", "8.75", "27/10/2025", "Completado")
            )

            // Crear contenido CSV
            val csvContent = StringBuilder()

            // Encabezados
            csvContent.append("N°,Producto,Cantidad Paquetes,Precio Unitario,Precio Total,Fecha Entrega,Estado\n")

            // Datos
            data.forEach { row ->
                csvContent.append(row.joinToString(","))
                csvContent.append("\n")
            }

            // Guardar archivo
            val fileName = "Lista_Trabajo_${getCurrentDateTime()}.csv"
            val file = File(getExcelDirectory(), fileName)

            outputStream = FileOutputStream(file)
            outputStream.write(csvContent.toString().toByteArray())

            Toast.makeText(this, "Archivo creado: $fileName", Toast.LENGTH_LONG).show()

            // Abrir el archivo
            openExcelFile(file)

        } catch (e: Exception) {
            Log.e(TAG, "Error creando archivo: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                outputStream?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error cerrando recursos: ${e.message}")
            }
        }
    }

    private fun getExcelDirectory(): File {
        // Para Android 10+, usar el directorio de documentos de la app
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CosturaPro").apply {
                if (!exists()) mkdirs()
            }
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "CosturaPro").apply {
                if (!exists()) mkdirs()
            }
        }
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun openExcelFile(file: File) {
        try {
            // Abrir con nuestro editor integrado
            val intent = Intent(this, FileViewerActivity::class.java).apply {
                putExtra("file_path", file.absolutePath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo archivo: ${e.message}", e)
            Toast.makeText(this, "Error abriendo archivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showOpenOptionsDialog(file: File) {
        val options = arrayOf("Abrir en visor integrado", "Abrir con aplicación externa", "Solo guardar")

        AlertDialog.Builder(this)
            .setTitle("Archivo creado exitosamente")
            .setMessage("¿Cómo deseas abrir el archivo ${file.name}?")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Ya se abrió en el visor integrado
                        dialog.dismiss()
                    }
                    1 -> {
                        openWithExternalApp(file)
                    }
                    2 -> {
                        // Solo guardar, no hacer nada adicional
                        dialog.dismiss()
                    }
                }
            }
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openWithExternalApp(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Abrir con"))
            } else {
                Toast.makeText(this, "No hay aplicaciones para abrir archivos CSV", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error abriendo con app externa", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExcelFilePicker() {
        pickFileLauncher.launch("*/*")
    }

    private fun openExcelFile(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Abrir archivo con"))
            } else {
                Toast.makeText(this, "No hay aplicaciones para abrir este archivo", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error abriendo archivo", Toast.LENGTH_LONG).show()
        }
    }
    private fun showFileLocation(file: File) {
        val locationInfo = """
    📍 ARCHIVO GUARDADO EN:
    
    ${file.absolutePath}
    
    📱 Para encontrarlo:
    1. Abre 'Archivos' en tu celular
    2. Busca la carpeta 'Documents'
    3. Ve a 'CosturaPro'
    4. O busca: ${file.name}
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Ubicación del Archivo")
            .setMessage(locationInfo)
            .setPositiveButton("OK", null)
            .show()
    }

    companion object {
        private const val REQUEST_CODE_PICK_EXCEL = 1001
    }
}