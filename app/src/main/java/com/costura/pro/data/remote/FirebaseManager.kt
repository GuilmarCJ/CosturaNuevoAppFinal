package com.costura.pro.data.remote


import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val db = FirebaseFirestore.getInstance()

    // Aquí puedes agregar métodos específicos para Firebase operations
    // Esto ayuda a mantener el código organizado y reutilizable
}