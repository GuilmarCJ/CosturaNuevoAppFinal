package com.costura.pro.ui.attendance
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.costura.pro.databinding.ActivityQrScannerBinding
import com.costura.pro.utils.AppPreferences
import com.costura.pro.data.model.QRCodeData
import com.costura.pro.data.model.QRType
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import androidx.lifecycle.lifecycleScope
import org.joda.time.DateTime
import kotlinx.coroutines.launch

class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var preferences: AppPreferences
    private lateinit var barcodeView: DecoratedBarcodeView

    private var isProcessing = false

    companion object {
        private const val TAG = "QRScannerActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)
        setupUI()
        setupClickListeners()

        updateScanTypeUI()

        barcodeView = binding.barcodeScanner

        if (allPermissionsGranted()) {
            startScanner()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    private fun setupUI() {
        supportActionBar?.hide()
        startScanLineAnimation()
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun updateScanTypeUI() {
        binding.tvScanType.text = "REGISTRO AUTOMÁTICO"
        binding.tvScanInstruction.text = "Escanea el código QR para registrar entrada o salida automáticamente"
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startScanner()
            } else {
                Toast.makeText(this, "Se necesita permiso de cámara para escanear QR", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startScanner() {
        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (!isProcessing) {
                    isProcessing = true
                    val qrContent = result.text
                    handleScannedQRCode(qrContent)
                }
            }
        }

        barcodeView.decodeSingle(callback)
    }

    private fun handleScannedQRCode(qrContent: String) {
        Log.d(TAG, "QR Code escaneado: $qrContent")

        runOnUiThread {
            try {
                val qrData = QRCodeData.fromJsonString(qrContent)

                if (qrData != null) {
                    Log.d(TAG, "✅ QR universal parseado correctamente")
                    Log.d(TAG, "   - LocationId: ${qrData.locationId}")

                    registerAttendanceAutomatically()
                } else {
                    Log.e(TAG, "❌ No se pudo parsear el QR: $qrContent")
                    showErrorMessage("Código QR inválido o formato incorrecto")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error procesando código QR", e)
                showErrorMessage("Error procesando código QR: ${e.message}")
            }
        }
    }

    private fun registerAttendanceAutomatically() {
        val workerId = preferences.userId
        val workerName = preferences.username ?: "Trabajador"

        if (workerId != null) {
            showLoading(true)

            val attendanceRepository = (application as com.costura.pro.CosturaProApp).attendanceRepository

            lifecycleScope.launch {
                try {
                    // LÓGICA AUTOMÁTICA: decidir si es entrada o salida
                    val today = DateTime().toString("yyyy-MM-dd")
                    val existingRecord = attendanceRepository.getAttendanceByWorkerAndDate(workerId, today)

                    val (success, actionType) = if (existingRecord == null || existingRecord.exitTime != null) {
                        // No tiene entrada hoy o ya tiene salida → REGISTRAR ENTRADA
                        Log.d(TAG, "📝 Registrando ENTRADA automática para $workerName...")
                        val result = attendanceRepository.registerEntry(workerId, workerName)
                        Pair(result, "entrada")
                    } else {
                        // Tiene entrada sin salida → REGISTRAR SALIDA
                        Log.d(TAG, "📝 Registrando SALIDA automática para $workerName...")
                        val result = attendanceRepository.registerExit(workerId)
                        Pair(result, "salida")
                    }

                    runOnUiThread {
                        showLoading(false)
                        if (success) {
                            Log.d(TAG, "✅ $actionType registrada automáticamente")

                            showSuccessMessage("✅ $actionType registrada automáticamente")

                            val resultIntent = Intent().apply {
                                putExtra("REGISTRATION_SUCCESS", true)
                                putExtra("ACTION_TYPE", actionType)
                                putExtra("WORKER_NAME", workerName)
                            }
                            setResult(RESULT_OK, resultIntent)

                            binding.root.postDelayed({
                                finish()
                            }, 1500)
                        } else {
                            Log.e(TAG, "❌ Error registrando $actionType")
                            showErrorMessage("Error registrando $actionType. Verifica tu estado actual.")
                            resetScanner()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Excepción registrando asistencia", e)
                    runOnUiThread {
                        showLoading(false)
                        showErrorMessage("Error de conexión: ${e.message}")
                        resetScanner()
                    }
                }
            }
        } else {
            showErrorMessage("Error: Usuario no identificado")
            resetScanner()
        }
    }

    private fun resetScanner() {
        isProcessing = false
        binding.barcodeScanner.postDelayed({
            if (!isFinishing) {
                startScanner()
            }
        }, 3000)
    }

    private fun startScanLineAnimation() {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 0f,
            Animation.RELATIVE_TO_PARENT, 1f
        )
        animation.duration = 2000
        animation.repeatCount = Animation.INFINITE
        animation.repeatMode = Animation.REVERSE

        binding.scanLine.startAnimation(animation)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnCancel.isEnabled = !show

        if (show) {
            binding.tvStatus.text = "Procesando..."
        } else {
            binding.tvStatus.text = "Listo para escanear"
        }
    }

    private fun showSuccessMessage(message: String) {
        binding.tvStatus.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorMessage(message: String) {
        binding.tvStatus.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}