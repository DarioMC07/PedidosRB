package com.example.rosabetaniapedidos

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Pedido(
    @DocumentId
    var id: String = "",
    var cliente_email: String = "",
    var nombre_trabajo: String = "",
    var tipo_trabajo: String = "",
    var cantidad: Int = 0,
    var largo_cm: Double = 0.0,
    var ancho_cm: Double = 0.0,
    var observaciones: String = "",
    var material: String = "",
    var gramaje: String = "",
    var estado: String = "Pendiente",
    var precio_cotizado: Double? = null,
    var imagen_url: String? = null,
    @ServerTimestamp
    var fecha_creacion: Timestamp? = null
)
