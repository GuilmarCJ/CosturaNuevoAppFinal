package com.costura.pro.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object NetworkUtils {

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Método moderno para API 23+
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.let {
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                } ?: false
            } else {
                // Método legacy para API 21-22
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            // En caso de error, asumimos que hay conexión para no bloquear la app
            true
        }
    }
}