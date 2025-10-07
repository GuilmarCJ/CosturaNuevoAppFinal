package com.costura.pro.utils

import com.costura.pro.data.model.QRCodeData
import com.costura.pro.data.model.QRType
import java.util.*

object QRManager {

    private val usedQRCodes = mutableSetOf<String>()

    fun generatePermanentQR(type: QRType, locationId: String = "costura_pro"): QRCodeData {
        val uniqueId = UUID.randomUUID().toString()
        return QRCodeData(
            type = type,
            locationId = locationId,
            uniqueId = uniqueId,
            isPermanent = true
        )
    }

    fun isQRValid(qrData: QRCodeData): Boolean {
        // Para QR permanentes, verificar que no se haya usado antes
        if (qrData.isPermanent) {
            return !usedQRCodes.contains(qrData.uniqueId)
        }

        // Para QR temporales, mantener la lógica original
        return true
    }

    fun markQRAsUsed(qrData: QRCodeData) {
        if (qrData.isPermanent) {
            usedQRCodes.add(qrData.uniqueId)
        }
    }

    fun clearUsedQRCodes() {
        usedQRCodes.clear()
    }
}