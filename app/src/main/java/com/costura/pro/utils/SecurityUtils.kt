package com.costura.pro.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    private const val ALGORITHM = "AES"
    private const val KEY = "Your32ByteSecretKey123456789012" // Cambia esto en producción

    fun encrypt(value: String): String {
        val secretKey = SecretKeySpec(KEY.toByteArray(), ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedValue = cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(encryptedValue, Base64.DEFAULT)
    }

    fun decrypt(value: String): String {
        val secretKey = SecretKeySpec(KEY.toByteArray(), ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedValue = cipher.doFinal(Base64.decode(value, Base64.DEFAULT))
        return String(decryptedValue)
    }

    fun validateAppSignature(context: Context): Boolean {
        try {
            val packageName = context.packageName
            val packageManager = context.packageManager
            val signatures = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            ).signatures

            val messageDigest = MessageDigest.getInstance("SHA")
            signatures.forEach { signature ->
                messageDigest.update(signature.toByteArray())
            }

            val currentSignature = Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT)

            // En desarrollo, puedes retornar true directamente
            // En producción, compara con tu firma real
            return true // Cambia esto en producción

        } catch (e: Exception) {
            return false
        }
    }
}