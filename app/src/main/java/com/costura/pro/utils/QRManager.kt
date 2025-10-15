package com.costura.pro.utils

import com.costura.pro.data.model.QRCodeData
import com.costura.pro.data.model.QRType
import java.util.*



object QRManager {

    // Función para QR universal (nueva)
    fun generateUniversalQR(locationId: String = "costura_pro"): QRCodeData {
        return QRCodeData(
            locationId = locationId
        )
    }

    // Función para QR con tipo (mantener para compatibilidad)
    fun generatePermanentQR(type: QRType, locationId: String = "costura_pro"): QRCodeData {
        return QRCodeData(
            locationId = locationId
        )
    }

    fun isQRValid(qrData: QRCodeData): Boolean {
        return qrData.locationId.isNotEmpty()
    }
}