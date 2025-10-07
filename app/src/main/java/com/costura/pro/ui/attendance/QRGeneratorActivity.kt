package com.costura.pro.ui.attendance

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.costura.pro.R
import com.costura.pro.databinding.ActivityQrGeneratorBinding
import com.costura.pro.data.model.QRCodeData
import com.costura.pro.data.model.QRType
import com.costura.pro.data.repository.AttendanceRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*

class QRGeneratorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrGeneratorBinding
    private lateinit var attendanceRepository: AttendanceRepository
    private var currentQRType: QRType = QRType.ENTRY
    private var currentQRBitmap: Bitmap? = null
    private var currentUniqueId: String = ""

    companion object {
        private const val TAG = "QRGeneratorActivity"
        private const val QR_CODE_SIZE = 500
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener repository desde la aplicación
        attendanceRepository = (application as com.costura.pro.CosturaProApp).attendanceRepository

        setupUI()
        setupClickListeners()
        generateQRCode()
    }

    private fun setupUI() {
        supportActionBar?.title = "Generador de QR Permanente"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateTimeDisplay()

        // Actualizar textos para QR permanente
        binding.tvExpireTime.text = "PERMANENTE"
        binding.tvExpireTime.setTextColor(ContextCompat.getColor(this, R.color.success_green))
    }

    private fun setupClickListeners() {
        binding.rgQrType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbEntry -> {
                    currentQRType = QRType.ENTRY
                    generateQRCode()
                }
                R.id.rbExit -> {
                    currentQRType = QRType.EXIT
                    generateQRCode()
                }
            }
        }

        binding.btnGenerate.setOnClickListener {
            generateQRCode()
        }

        binding.btnShare.setOnClickListener {
            shareQRCode()
        }
    }

    private fun generateQRCode() {
        showLoading(true)

        Thread {
            try {
                // Generar ID único para este QR
                currentUniqueId = UUID.randomUUID().toString()

                // Crear datos del QR permanente
                val qrData = QRCodeData(
                    type = currentQRType,
                    locationId = "costura_pro",
                    uniqueId = currentUniqueId,
                    isPermanent = true
                )

                val qrContent = qrData.toJsonString()
                Log.d(TAG, "Generando QR con contenido: $qrContent")

                val qrBitmap = generateQRCodeBitmap(qrContent)

                runOnUiThread {
                    showLoading(false)
                    currentQRBitmap = qrBitmap
                    binding.ivQrCode.setImageBitmap(qrBitmap)
                    updateQRTypeUI()
                    updateTimeDisplay()

                    Toast.makeText(this, "Código QR permanente generado", Toast.LENGTH_SHORT).show()

                    // Mostrar en log el contenido generado
                    Log.d(TAG, "✅ QR PERMANENTE GENERADO:")
                    Log.d(TAG, "Tipo: ${qrData.type}")
                    Log.d(TAG, "UniqueId: ${qrData.uniqueId}")
                    Log.d(TAG, "Permanente: ${qrData.isPermanent}")
                    Log.d(TAG, "JSON: $qrContent")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    Log.e(TAG, "❌ Error generando QR", e)
                    Toast.makeText(this, "Error generando código QR", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun generateQRCodeBitmap(text: String): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.MARGIN] = 1
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }

    private fun updateQRTypeUI() {
        val typeText = when (currentQRType) {
            QRType.ENTRY -> "ENTRADA PERMANENTE"
            QRType.EXIT -> "SALIDA PERMANENTE"
        }

        val typeColor = when (currentQRType) {
            QRType.ENTRY -> R.color.success_green
            QRType.EXIT -> R.color.error_red
        }

        binding.tvQrType.text = typeText
        binding.tvQrType.setTextColor(ContextCompat.getColor(this, typeColor))
    }

    private fun updateTimeDisplay() {
        val timeFormat = DateTimeFormat.forPattern("HH:mm:ss")
        binding.tvGeneratedTime.text = timeFormat.print(DateTime())
    }

    private fun shareQRCode() {
        currentQRBitmap?.let { bitmap ->
            try {
                // Información adicional para compartir
                val qrInfo = """
                    Código QR Permanente - Costura Pro
                    Tipo: ${if (currentQRType == QRType.ENTRY) "ENTRADA" else "SALIDA"}
                    ID: $currentUniqueId
                    Generado: ${DateTime().toString("dd/MM/yyyy HH:mm:ss")}
                    
                    Este código QR es permanente y no expira.
                """.trimIndent()

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, qrInfo)
                    putExtra(Intent.EXTRA_SUBJECT, "Código QR Permanente - Costura Pro")
                }

                startActivity(Intent.createChooser(shareIntent, "Compartir código QR"))

            } catch (e: Exception) {
                Toast.makeText(this, "Error compartiendo código QR", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Primero genera un código QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnGenerate.isEnabled = !show
        binding.btnShare.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}