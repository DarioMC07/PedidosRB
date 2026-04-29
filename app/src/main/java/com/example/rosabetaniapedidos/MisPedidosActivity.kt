package com.example.rosabetaniapedidos

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MisPedidosActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: PedidoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mis_pedidos)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(R.color.rb_surface)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.misPedidosRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarMisPedidos)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val rvMisPedidos = findViewById<RecyclerView>(R.id.rvMisPedidos)
        rvMisPedidos.layoutManager = LinearLayoutManager(this)

        adapter = PedidoAdapter(emptyList()) { pedido ->
            if (pedido.estado == "Cotizado") {
                showConfirmDialog(pedido)
            } else {
                Toast.makeText(this, "Estado actual: ${pedido.estado}", Toast.LENGTH_SHORT).show()
            }
        }
        rvMisPedidos.adapter = adapter

        fetchMyPedidos()
    }

    private fun fetchMyPedidos() {
        val email = auth.currentUser?.email
        if (email == null) {
            Toast.makeText(this, "No autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("pedidos")
            .whereEqualTo("cliente_email", email)
            .orderBy("fecha_creacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Pedido::class.java) }
                    adapter.updateData(list)
                }
            }
    }

    private fun showConfirmDialog(pedido: Pedido) {
        if (pedido.id.isEmpty() || pedido.precio_cotizado == null) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_client_confirm, null)
        val tvPriceAmount = dialogView.findViewById<TextView>(R.id.tvPriceAmount)
        val btnReject = dialogView.findViewById<android.view.View>(R.id.btnRejectPrice)
        val btnConfirm = dialogView.findViewById<android.view.View>(R.id.btnConfirmPrice)

        tvPriceAmount.text = "BOB ${pedido.precio_cotizado}"

        val dialog = MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .setView(dialogView)
            .create()

        btnReject.setOnClickListener {
            updateStatus(pedido.id, "Rechazado", dialog, btnReject, btnConfirm)
        }

        btnConfirm.setOnClickListener {
            updateStatus(pedido.id, "Confirmado", dialog, btnReject, btnConfirm)
        }

        dialog.show()
    }

    private fun updateStatus(
        id: String,
        newStatus: String,
        dialog: androidx.appcompat.app.AlertDialog,
        btnReject: android.view.View,
        btnConfirm: android.view.View
    ) {
        btnReject.isEnabled = false
        btnConfirm.isEnabled = false

        db.collection("pedidos").document(id)
            .update("estado", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Pedido $newStatus", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                btnReject.isEnabled = true
                btnConfirm.isEnabled = true
            }
    }
}
