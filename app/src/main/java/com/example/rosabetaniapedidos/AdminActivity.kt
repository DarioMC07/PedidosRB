package com.example.rosabetaniapedidos

import android.content.Intent
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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit  var adapter: PedidoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(R.color.rb_surface)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Mostrar email del admin en el header
        val tvAdminEmail = findViewById<TextView>(R.id.tvAdminEmail)
        tvAdminEmail.text = auth.currentUser?.email ?: ""

        // Botón de logout en el header (mismo estilo que el cliente)
        val cardAdminLogout = findViewById<MaterialCardView>(R.id.cardAdminLogout)
        cardAdminLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        // Botón de acceso a la pantalla de seguimiento de producción
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnIrProduccion)
            .setOnClickListener {
                startActivity(Intent(this, AdminProduccionActivity::class.java))
            }

        val rvPedidosAdmin = findViewById<RecyclerView>(R.id.rvPedidosAdmin)
        rvPedidosAdmin.layoutManager = LinearLayoutManager(this)

        adapter = PedidoAdapter(emptyList()) { pedido ->
            if (pedido.estado == "Pendiente") {
                showSetPriceDialog(pedido)
            } else {
                Toast.makeText(this, "El pedido ya está en estado: ${pedido.estado}", Toast.LENGTH_SHORT).show()
            }
        }
        rvPedidosAdmin.adapter = adapter

        fetchPedidos()
    }

    private fun fetchPedidos() {
        db.collection("pedidos")
            .orderBy("fecha_creacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error al cargar pedidos: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Pedido::class.java)
                    }
                    adapter.updateData(list)
                }
            }
    }

    private fun showSetPriceDialog(pedido: Pedido) {
        if (pedido.id.isEmpty()) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_set_price, null)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etPrice)
        val btnCancel = dialogView.findViewById<android.view.View>(R.id.btnCancelPrice)
        val btnSave = dialogView.findViewById<android.view.View>(R.id.btnSavePrice)

        val dialog = MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val priceStr = etPrice.text?.toString()?.trim().orEmpty()
            if (priceStr.isEmpty()) {
                etPrice.error = "Ingrese un valor"
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull()
            if (price == null || price <= 0) {
                etPrice.error = "Ingrese un monto válido"
                return@setOnClickListener
            }

            // Update in Firestore
            btnSave.isEnabled = false
            db.collection("pedidos").document(pedido.id)
                .update(
                    mapOf(
                        "precio_cotizado" to price,
                        "estado" to "Cotizado"
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Precio asignado con éxito", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                }
        }

        dialog.show()
    }
}
