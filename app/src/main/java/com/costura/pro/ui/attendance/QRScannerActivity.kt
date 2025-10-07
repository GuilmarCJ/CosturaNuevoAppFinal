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
import com.costura.pro.utils.QRManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var preferences: AppPreferences
    private lateinit var barcodeView: DecoratedBarcodeView

    private var scanType: String = "ENTRY"
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

        // Obtener tipo de escaneo
        scanType = intent.getStringExtra("SCAN_TYPE") ?: "ENTRY"
        updateScanTypeUI()

        // Inicializar el escáner
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
        binding.tvScanType.text = when (scanType) {
            "ENTRY" -> "REGISTRAR ENTRADA"
            "EXIT" -> "REGISTRAR SALIDA"
            else -> "ENTRADA"
        }

        binding.tvScanInstruction.text = when (scanType) {
            "ENTRY" -> "Escanea el código QR para registrar tu entrada"
            "EXIT" -> "Escanea el código QR para registrar tu salida"
            else -> "Escanea el código QR"
        }
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
        Log.d(TAG, "QR Code scanned: $qrContent")

        runOnUiThread {
            try {
                // Parsear el contenido del QR
                val qrData = com.costura.pro.data.model.QRCodeData.fromJsonString(qrContent)

                if (qrData != null) {
                    Log.d(TAG, "✅ QR parseado correctamente:")
                    Log.d(TAG, "   - Tipo: ${qrData.type}")
                    Log.d(TAG, "   - Permanente: ${qrData.isPermanent}")
                    Log.d(TAG, "   - UniqueId: ${qrData.uniqueId}")
                    Log.d(TAG, "   - LocationId: ${qrData.locationId}")

                    // Verificar que el tipo de QR coincida con la acción
                    val isValidType = when (scanType) {
                        "ENTRY" -> qrData.type == com.costura.pro.data.model.QRType.ENTRY
                        "EXIT" -> qrData.type == com.costura.pro.data.model.QRType.EXIT
                        else -> false
                    }

                    if (isValidType) {
                        if (qrData.isPermanent) {
                            Log.d(TAG, "✅ QR permanente detectado - procediendo con validación")
                            // QR permanente - usar el nuevo sistema de validación
                            if (validatePermanentQR(qrData)) {
                                registerAttendance(qrData)
                            } else {
                                resetScanner()
                            }
                        } else {
                            Log.w(TAG, "❌ QR temporal detectado")
                            // QR temporal - mostrar información detallada
                            Toast.makeText(this, "Código QR temporal detectado. Genere un nuevo código QR permanente.", Toast.LENGTH_LONG).show()
                            resetScanner()
                        }
                    } else {
                        Log.w(TAG, "❌ Tipo de QR incorrecto. Esperado: $scanType, Recibido: ${qrData.type}")
                        Toast.makeText(this, "Código QR incorrecto para esta acción. Esperado: $scanType", Toast.LENGTH_LONG).show()
                        resetScanner()
                    }
                } else {
                    Log.e(TAG, "❌ No se pudo parsear el QR: $qrContent")
                    Toast.makeText(this, "Código QR inválido o formato incorrecto", Toast.LENGTH_LONG).show()
                    resetScanner()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error procesando código QR", e)
                Toast.makeText(this, "Error procesando código QR: ${e.message}", Toast.LENGTH_LONG).show()
                resetScanner()
            }
        }
    }

    private fun resetScanner() {
        isProcessing = false
        // Reiniciar el escáner después de un breve delay
        binding.barcodeScanner.postDelayed({
            if (!isFinishing) {
                startScanner()
            }
        }, 2000)
    }

    private fun registerAttendance(qrData: com.costura.pro.data.model.QRCodeData) {
        val workerId = preferences.userId
        val workerName = preferences.username ?: "Trabajador"

        if (workerId != null) {
            showLoading(true)

            // Aquí integrarías con el repository real
            // Por ahora simulamos el registro
            Thread {
                try {
                    // Marcar el QR como usado
                    QRManager.markQRAsUsed(qrData)

                    // Simular procesamiento
                    Thread.sleep(1500)

                    runOnUiThread {
                        showLoading(false)
                        Toast.makeText(this, "Asistencia registrada exitosamente", Toast.LENGTH_SHORT).show()

                        val resultIntent = Intent().apply {
                            putExtra("REGISTRATION_SUCCESS", true)
                            putExtra("SCAN_TYPE", scanType)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                } catch (e: InterruptedException) {
                    runOnUiThread {
                        showLoading(false)
                        Toast.makeText(this, "Error registrando asistencia", Toast.LENGTH_SHORT).show()
                        resetScanner()
                    }
                }
            }.start()
        } else {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            resetScanner()
        }
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
    }

    private fun validatePermanentQR(qrData: com.costura.pro.data.model.QRCodeData): Boolean {
        // Verificar que el QR sea válido según el manager
        if (!QRManager.isQRValid(qrData)) {
            Toast.makeText(this, "Este código QR ya ha sido utilizado", Toast.LENGTH_LONG).show()
            return false
        }

        // Aquí puedes añadir más validaciones según tu negocio
        // Por ejemplo, verificar locationId, tipo de usuario, etc.

        // Verificar que sea un QR permanente
        if (!qrData.isPermanent) {
            Toast.makeText(this, "Este código QR no es permanente", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }
}