package com.costura.pro.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class AppSignatureHelper(context: Context) {

    private val context: Context = context

    fun getAppSignatures(): ArrayList<String> {
        val appCodes = ArrayList<String>()

        try {
            val packageName = context.packageName
            val packageManager = context.packageManager
            val signatures = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            ).signatures

            signatures.forEach { signature ->
                val hash = hash(packageName, signature.toCharsString())
                if (hash != null) {
                    appCodes.add(String.format("%s", hash))
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppSignature", "Unable to find package to obtain signature.", e)
        }

        return appCodes
    }

    private fun hash(packageName: String, signature: String): String? {
        val appInfo = "$packageName $signature"
        try {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(appInfo.toByteArray())
            var hashSignature = messageDigest.digest()

            hashSignature = java.util.Arrays.copyOfRange(hashSignature, 0, 9)
            var base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING or Base64.NO_WRAP)
            base64Hash = base64Hash.substring(0, 11)

            Log.d("AppSignature", "\\n${base64Hash}\\n")
            return base64Hash
        } catch (e: NoSuchAlgorithmException) {
            Log.e("AppSignature", "No Such Algorithm.", e)
        }

        return null
    }
}