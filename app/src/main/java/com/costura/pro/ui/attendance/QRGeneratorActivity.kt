package com.costura.pro.ui.attendance

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
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
    private var currentQRBitmap: Bitmap? = null

    companion object {
        private const val TAG = "QRGeneratorActivity"
        private const val QR_CODE_SIZE = 500
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        attendanceRepository = (application as com.costura.pro.CosturaProApp).attendanceRepository

        setupUI()
        setupClickListeners()
        generateQRCode()
    }

    private fun setupUI() {
        supportActionBar?.title = "Generador de QR Universal"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateTimeDisplay()

        binding.tvExpireTime.text = "UNIVERSAL - ENTRADA/SALIDA"
        binding.tvQrInfo.text = "Este código QR sirve para ENTRADA y SALIDA automáticamente"

        binding.rgQrType.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.btnGenerate.setOnClickListener {
            generateQRCode()
        }

        binding.btnShare.setOnClickListener {
            shareQRCode()
        }

        binding.btnSaveToGallery.setOnClickListener {
            saveQRToGallery()
        }
    }

    private fun generateQRCode() {
        showLoading(true)

        Thread {
            try {
                val qrData = QRCodeData(
                    locationId = "costura_pro"
                )

                val qrContent = qrData.toJsonString()
                Log.d(TAG, "Generando QR UNIVERSAL con contenido: $qrContent")

                val qrBitmap = generateQRCodeBitmap(qrContent)

                runOnUiThread {
                    showLoading(false)
                    currentQRBitmap = qrBitmap
                    binding.ivQrCode.setImageBitmap(qrBitmap)
                    updateQRInfoUI()
                    updateTimeDisplay()

                    Toast.makeText(this, "✅ Código QR universal generado", Toast.LENGTH_SHORT).show()

                    Log.d(TAG, "✅ QR UNIVERSAL GENERADO:")
                    Log.d(TAG, "Location: ${qrData.locationId}")
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

    private fun updateQRInfoUI() {
        binding.tvQrType.text = "QR UNIVERSAL"
        binding.tvQrType.setTextColor(ContextCompat.getColor(this, R.color.primary_color))

        binding.tvInstructions.text = "Los trabajadores escanearán este QR para registrar ENTRADA o SALIDA automáticamente"
    }

    private fun updateTimeDisplay() {
        val timeFormat = DateTimeFormat.forPattern("HH:mm:ss")
        val dateFormat = DateTimeFormat.forPattern("dd/MM/yyyy")
        binding.tvGeneratedTime.text = "${dateFormat.print(DateTime())} ${timeFormat.print(DateTime())}"
    }

    // ui/attendance/QRGeneratorActivity.kt - corregir las funciones
    private fun shareQRCode() {
        currentQRBitmap?.let { _ ->  // Cambiar 'bitmap' por '_'
            try {
                val qrInfo = """
                📋 CÓDIGO QR UNIVERSAL - COSTURA PRO
                
                🔹 Tipo: UNIVERSAL (Entrada/Salida)
                🔹 Ubicación: Costura Pro
                🔹 Generado: ${DateTime().toString("dd/MM/yyyy HH:mm:ss")}
                🔹 Estado: PERMANENTE Y REUTILIZABLE
                
                📝 INSTRUCCIONES:
                • Imprimir este código QR
                • Pegarlo en la entrada/salida
                • Los trabajadores lo escanean diariamente
                • El sistema decide automáticamente si es entrada o salida
                
                💡 Funciona para ENTRADA y SALIDA automáticamente
            """.trimIndent()

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, qrInfo)
                    putExtra(Intent.EXTRA_SUBJECT, "Código QR Universal - Costura Pro")
                }

                startActivity(Intent.createChooser(shareIntent, "Compartir código QR"))

            } catch (e: Exception) {
                Toast.makeText(this, "Error compartiendo código QR", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Primero genera un código QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveQRToGallery() {
        currentQRBitmap?.let { _ ->  // Cambiar 'bitmap' por '_'
            try {
                Toast.makeText(this, "Función de guardar en galería próximamente", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error guardando QR", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Primero genera un código QR", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnGenerate.isEnabled = !show
        binding.btnShare.isEnabled = !show
        binding.btnSaveToGallery.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}