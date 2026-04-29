package com.example.rosabetaniapedidos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClientActivity : AppCompatActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_client)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = getColor(R.color.rb_surface)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.clientRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        findViewById<TextView>(R.id.tvSessionEmail).text =
            getString(R.string.client_session_label, user.email.orEmpty())

        findViewById<MaterialCardView>(R.id.cardProfile).setOnClickListener { anchor ->
            showProfileMenu(anchor)
        }

        val jobTypes = resources.getStringArray(R.array.job_types)
        val actJobType = findViewById<MaterialAutoCompleteTextView>(R.id.actJobType)
        actJobType.setSimpleItems(jobTypes)
        if (actJobType.text.isNullOrBlank()) {
            actJobType.setText(jobTypes.firstOrNull().orEmpty(), false)
        }

        val paperGrams = resources.getStringArray(R.array.paper_grams)
        val actGramaje = findViewById<MaterialAutoCompleteTextView>(R.id.actGramaje)
        actGramaje.setSimpleItems(paperGrams)
        if (actGramaje.text.isNullOrBlank()) {
            actGramaje.setText(paperGrams.firstOrNull().orEmpty(), false)
        }

        findViewById<View>(R.id.btnSolicitar).setOnClickListener { submitOrder() }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            goToLogin()
        }
    }

    private fun showProfileMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_profile, menu)
            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_mis_pedidos) {
                    startActivity(Intent(this@ClientActivity, MisPedidosActivity::class.java))
                    true
                } else if (item.itemId == R.id.action_logout) {
                    auth.signOut()
                    goToLogin()
                    true
                } else {
                    false
                }
            }
            show()
        }
    }

    private fun submitOrder() {
        val tilName = findViewById<TextInputLayout>(R.id.tilJobName)
        val etName = findViewById<TextInputEditText>(R.id.etJobName)
        val tilLength = findViewById<TextInputLayout>(R.id.tilLength)
        val etLength = findViewById<TextInputEditText>(R.id.etLength)
        val tilWidth = findViewById<TextInputLayout>(R.id.tilWidth)
        val etWidth = findViewById<TextInputEditText>(R.id.etWidth)
        val tilGramaje = findViewById<TextInputLayout>(R.id.tilGramaje)
        val actGramaje = findViewById<MaterialAutoCompleteTextView>(R.id.actGramaje)

        tilName.error = null
        tilLength.error = null
        tilWidth.error = null
        tilGramaje.error = null

        val name = etName.text?.toString()?.trim().orEmpty()
        val lengthStr = etLength.text?.toString()?.trim().orEmpty()
        val widthStr = etWidth.text?.toString()?.trim().orEmpty()
        val gramaje = actGramaje.text?.toString()?.trim().orEmpty()

        var ok = true
        if (name.isEmpty()) {
            tilName.error = getString(R.string.error_required)
            ok = false
        }
        if (lengthStr.isEmpty()) {
            tilLength.error = getString(R.string.error_required)
            ok = false
        }
        if (widthStr.isEmpty()) {
            tilWidth.error = getString(R.string.error_required)
            ok = false
        }
        if (gramaje.isEmpty()) {
            tilGramaje.error = getString(R.string.error_required)
            ok = false
        }
        if (!ok) {
            return
        }

        val actJobType = findViewById<MaterialAutoCompleteTextView>(R.id.actJobType)
        val tipoTrabajo = actJobType.text?.toString()?.trim().orEmpty()

        val etQuantity = findViewById<TextInputEditText>(R.id.etQuantity)
        val cantidad = etQuantity.text?.toString()?.toIntOrNull() ?: 100

        val etObservation = findViewById<TextInputEditText>(R.id.etObservation)
        val observaciones = etObservation.text?.toString()?.trim().orEmpty()

        val chipGroupMaterial = findViewById<ChipGroup>(R.id.chipGroupMaterial)
        val selectedChipId = chipGroupMaterial.checkedChipId
        val material = if (selectedChipId != View.NO_ID) {
            findViewById<Chip>(selectedChipId).text.toString()
        } else {
            ""
        }

        val email = auth.currentUser?.email.orEmpty()
        val length = lengthStr.toDoubleOrNull() ?: 0.0
        val width = widthStr.toDoubleOrNull() ?: 0.0

        val pedido = Pedido(
            cliente_email = email,
            nombre_trabajo = name,
            tipo_trabajo = tipoTrabajo,
            cantidad = cantidad,
            largo_cm = length,
            ancho_cm = width,
            observaciones = observaciones,
            material = material,
            gramaje = gramaje
        )

        val btnSolicitar = findViewById<View>(R.id.btnSolicitar)
        btnSolicitar.isEnabled = false

        showConfirmDialog(pedido, btnSolicitar, etName, etLength, etWidth, etObservation)
    }

    private fun showConfirmDialog(
        pedido: Pedido, 
        btnSolicitar: View, 
        etName: TextInputEditText,
        etLength: TextInputEditText,
        etWidth: TextInputEditText,
        etObservation: TextInputEditText
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_order, null)
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val tvConfirmJobName = dialogView.findViewById<TextView>(R.id.tvConfirmJobName)
        val tvConfirmJobType = dialogView.findViewById<TextView>(R.id.tvConfirmJobType)
        val btnDialogEdit = dialogView.findViewById<View>(R.id.btnDialogEdit)
        val btnDialogConfirm = dialogView.findViewById<View>(R.id.btnDialogConfirm)

        tvConfirmJobName.text = pedido.nombre_trabajo
        tvConfirmJobType.text = pedido.tipo_trabajo

        btnDialogEdit.setOnClickListener {
            dialog.dismiss()
            btnSolicitar.isEnabled = true
        }

        btnDialogConfirm.setOnClickListener {
            dialog.dismiss()
            
            db.collection("pedidos").add(pedido)
                .addOnSuccessListener {
                    Toast.makeText(this, "Pedido registrado exitosamente", Toast.LENGTH_LONG).show()
                    btnSolicitar.isEnabled = true
                    etName.text = null
                    etLength.text = null
                    etWidth.text = null
                    etObservation.text = null
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al registrar pedido", Toast.LENGTH_LONG).show()
                    btnSolicitar.isEnabled = true
                }
        }

        dialog.show()
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
