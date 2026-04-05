package com.example.rosabetaniapedidos

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Pedido(
    val cliente_email: String = "",
    val nombre_trabajo: String = "",
    val tipo_trabajo: String = "",
    val cantidad: Int = 0,
    val largo_cm: Double = 0.0,
    val ancho_cm: Double = 0.0,
    val observaciones: String = "",
    val material: String = "",
    val gramaje: String = "",
    val estado: String = "Pendiente",
    @ServerTimestamp
    val fecha_creacion: Timestamp? = null
)
