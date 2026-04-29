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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminProduccionActivity : AppCompatActivity() {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: ProduccionAdapter

    /** Estado Firestore que corresponde a cada tab */
    private val tabEstados = listOf("Confirmado", "En proceso", "Terminado")

    /** Tab seleccionado actualmente (0 = Pendiente, 1 = En proceso, 2 = Terminado) */
    private var tabActual = 0

    /** Cache completo de pedidos de producción */
    private var todosPedidos: List<Pedido> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_produccion)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(R.color.rb_surface)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminProduccionRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Botón de volver
        findViewById<MaterialCardView>(R.id.cardBack).setOnClickListener { finish() }

        // RecyclerView + Adapter
        val rv = findViewById<RecyclerView>(R.id.rvProduccion)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = ProduccionAdapter(emptyList()) { pedido ->
            showCambiarEstadoDialog(pedido)
        }
        rv.adapter = adapter

        // Tabs: Pendiente / En proceso / Terminado
        val tabs = findViewById<TabLayout>(R.id.tabsProduccion)
        tabs.addTab(tabs.newTab().setText("Pendiente"))
        tabs.addTab(tabs.newTab().setText("En proceso"))
        tabs.addTab(tabs.newTab().setText("Terminado"))

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tabActual = tab?.position ?: 0
                filtrarYMostrar()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Escuchar Firestore en tiempo real
        fetchPedidosProduccion()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Firestore: trae todos los pedidos que ya fueron confirmados/en producción
    // ─────────────────────────────────────────────────────────────────────────
    private fun fetchPedidosProduccion() {
        db.collection("pedidos")
            .whereIn("estado", listOf("Confirmado", "En proceso", "Terminado"))
            .orderBy("fecha_creacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    todosPedidos = snapshot.documents.mapNotNull { it.toObject(Pedido::class.java) }
                    filtrarYMostrar()
                }
            }
    }

    private fun filtrarYMostrar() {
        val estadoFiltro = tabEstados[tabActual]
        val filtrados = todosPedidos.filter { it.estado == estadoFiltro }
        adapter.updateData(filtrados)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Diálogo para cambiar estado manualmente
    // ─────────────────────────────────────────────────────────────────────────
    private fun showCambiarEstadoDialog(pedido: Pedido) {
        if (pedido.id.isEmpty()) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_cambiar_estado, null)
        val tvNombre       = dialogView.findViewById<TextView>(R.id.tvDialogNombre)
        val cardEnProceso  = dialogView.findViewById<MaterialCardView>(R.id.cardEnProceso)
        val cardTerminado  = dialogView.findViewById<MaterialCardView>(R.id.cardTerminado)
        val btnCancelar    = dialogView.findViewById<android.view.View>(R.id.btnCancelarEstado)

        tvNombre.text = pedido.nombre_trabajo

        val dialog = MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
        )
            .setView(dialogView)
            .setCancelable(true)
            .create()

        fun actualizarEstado(nuevoEstado: String) {
            cardEnProceso.isEnabled = false
            cardTerminado.isEnabled = false

            db.collection("pedidos").document(pedido.id)
                .update("estado", nuevoEstado)
                .addOnSuccessListener {
                    Toast.makeText(this, "Estado actualizado a: $nuevoEstado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    cardEnProceso.isEnabled = true
                    cardTerminado.isEnabled = true
                }
        }

        // Ocultar opción "En proceso" si ya lo está
        if (pedido.estado == "En proceso") {
            cardEnProceso.alpha = 0.4f
            cardEnProceso.isEnabled = false
        }

        cardEnProceso.setOnClickListener  { actualizarEstado("En proceso") }
        cardTerminado.setOnClickListener  { actualizarEstado("Terminado") }
        btnCancelar.setOnClickListener    { dialog.dismiss() }

        dialog.show()
    }
}
