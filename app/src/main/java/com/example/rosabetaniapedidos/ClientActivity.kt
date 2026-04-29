package com.example.rosabetaniapedidos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ClientActivity : AppCompatActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** URI local de la imagen seleccionada por el usuario (null = sin imagen) */
    private var selectedImageUri: Uri? = null

    // ── Launcher de galería ────────────────────────────────────────────────
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            showImagePreview(uri)
        }
    }

    // ── Launcher de permiso de almacenamiento (API < 33) ──────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openGallery() else {
            Toast.makeText(this, "Permiso necesario para acceder a la galería", Toast.LENGTH_SHORT).show()
        }
    }

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
        if (user == null) { goToLogin(); return }

        findViewById<TextView>(R.id.tvSessionEmail).text =
            getString(R.string.client_session_label, user.email.orEmpty())

        findViewById<MaterialCardView>(R.id.cardProfile).setOnClickListener { anchor ->
            showProfileMenu(anchor)
        }

        val jobTypes = resources.getStringArray(R.array.job_types)
        val actJobType = findViewById<MaterialAutoCompleteTextView>(R.id.actJobType)
        actJobType.setSimpleItems(jobTypes)
        if (actJobType.text.isNullOrBlank()) actJobType.setText(jobTypes.firstOrNull().orEmpty(), false)

        val paperGrams = resources.getStringArray(R.array.paper_grams)
        val actGramaje = findViewById<MaterialAutoCompleteTextView>(R.id.actGramaje)
        actGramaje.setSimpleItems(paperGrams)
        if (actGramaje.text.isNullOrBlank()) actGramaje.setText(paperGrams.firstOrNull().orEmpty(), false)

        // ── Selector de imagen ─────────────────────────────────────────────
        val cardImagePicker = findViewById<MaterialCardView>(R.id.cardImagePicker)
        val btnChangeImage  = findViewById<MaterialButton>(R.id.btnChangeImage)

        cardImagePicker.setOnClickListener { requestGalleryPermissionAndOpen() }
        btnChangeImage.setOnClickListener  { requestGalleryPermissionAndOpen() }

        // ── Botón de solicitar ─────────────────────────────────────────────
        findViewById<View>(R.id.btnSolicitar).setOnClickListener { submitOrder() }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) goToLogin()
    }

    // ── Galería ────────────────────────────────────────────────────────────

    private fun requestGalleryPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: permiso granular
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Android 12 e inferior
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun showImagePreview(uri: Uri) {
        val layoutNoImage   = findViewById<LinearLayout>(R.id.layoutNoImage)
        val layoutWithImage = findViewById<LinearLayout>(R.id.layoutWithImage)
        val ivPreview       = findViewById<ImageView>(R.id.ivImagePreview)

        layoutNoImage.visibility   = View.GONE
        layoutWithImage.visibility = View.VISIBLE

        Glide.with(this).load(uri).centerCrop().into(ivPreview)
    }

    // ── Perfil ─────────────────────────────────────────────────────────────

    private fun showProfileMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_profile, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_mis_pedidos -> {
                        startActivity(Intent(this@ClientActivity, MisPedidosActivity::class.java))
                        true
                    }
                    R.id.action_logout -> {
                        auth.signOut(); goToLogin(); true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    // ── Envío del pedido ───────────────────────────────────────────────────

    private fun submitOrder() {
        val tilName    = findViewById<TextInputLayout>(R.id.tilJobName)
        val etName     = findViewById<TextInputEditText>(R.id.etJobName)
        val tilLength  = findViewById<TextInputLayout>(R.id.tilLength)
        val etLength   = findViewById<TextInputEditText>(R.id.etLength)
        val tilWidth   = findViewById<TextInputLayout>(R.id.tilWidth)
        val etWidth    = findViewById<TextInputEditText>(R.id.etWidth)
        val tilGramaje = findViewById<TextInputLayout>(R.id.tilGramaje)
        val actGramaje = findViewById<MaterialAutoCompleteTextView>(R.id.actGramaje)

        tilName.error = null; tilLength.error = null
        tilWidth.error = null; tilGramaje.error = null

        val name      = etName.text?.toString()?.trim().orEmpty()
        val lengthStr = etLength.text?.toString()?.trim().orEmpty()
        val widthStr  = etWidth.text?.toString()?.trim().orEmpty()
        val gramaje   = actGramaje.text?.toString()?.trim().orEmpty()

        var ok = true
        if (name.isEmpty())      { tilName.error = getString(R.string.error_required);    ok = false }
        if (lengthStr.isEmpty()) { tilLength.error = getString(R.string.error_required);  ok = false }
        if (widthStr.isEmpty())  { tilWidth.error = getString(R.string.error_required);   ok = false }
        if (gramaje.isEmpty())   { tilGramaje.error = getString(R.string.error_required); ok = false }
        if (!ok) return

        val actJobType  = findViewById<MaterialAutoCompleteTextView>(R.id.actJobType)
        val tipoTrabajo = actJobType.text?.toString()?.trim().orEmpty()

        val etQuantity  = findViewById<TextInputEditText>(R.id.etQuantity)
        val cantidad    = etQuantity.text?.toString()?.toIntOrNull() ?: 100

        val etObservation = findViewById<TextInputEditText>(R.id.etObservation)
        val observaciones = etObservation.text?.toString()?.trim().orEmpty()

        val chipGroupMaterial = findViewById<ChipGroup>(R.id.chipGroupMaterial)
        val selectedChipId    = chipGroupMaterial.checkedChipId
        val material = if (selectedChipId != View.NO_ID)
            findViewById<Chip>(selectedChipId).text.toString() else ""

        val email  = auth.currentUser?.email.orEmpty()
        val length = lengthStr.toDoubleOrNull() ?: 0.0
        val width  = widthStr.toDoubleOrNull() ?: 0.0

        val pedido = Pedido(
            cliente_email  = email,
            nombre_trabajo = name,
            tipo_trabajo   = tipoTrabajo,
            cantidad       = cantidad,
            largo_cm       = length,
            ancho_cm       = width,
            observaciones  = observaciones,
            material       = material,
            gramaje        = gramaje
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
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
        )
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val tvConfirmJobName = dialogView.findViewById<TextView>(R.id.tvConfirmJobName)
        val tvConfirmJobType = dialogView.findViewById<TextView>(R.id.tvConfirmJobType)
        val btnDialogEdit    = dialogView.findViewById<View>(R.id.btnDialogEdit)
        val btnDialogConfirm = dialogView.findViewById<View>(R.id.btnDialogConfirm)

        tvConfirmJobName.text = pedido.nombre_trabajo
        tvConfirmJobType.text = pedido.tipo_trabajo

        btnDialogEdit.setOnClickListener {
            dialog.dismiss()
            btnSolicitar.isEnabled = true
        }

        btnDialogConfirm.setOnClickListener {
            dialog.dismiss()
            btnDialogConfirm.isEnabled = false
            btnDialogEdit.isEnabled    = false

            // Si el usuario seleccionó imagen, primero subirla a Cloudinary
            val imageUri = selectedImageUri
            if (imageUri != null) {
                uploadImageAndSavePedido(pedido, imageUri, btnSolicitar, etName, etLength, etWidth, etObservation)
            } else {
                savePedidoToFirestore(pedido, btnSolicitar, etName, etLength, etWidth, etObservation)
            }
        }

        dialog.show()
    }

    /**
     * Sube la imagen a Cloudinary en background y luego guarda el pedido
     * en Firestore con la URL resultante.
     */
    private fun uploadImageAndSavePedido(
        pedido: Pedido,
        imageUri: Uri,
        btnSolicitar: View,
        etName: TextInputEditText,
        etLength: TextInputEditText,
        etWidth: TextInputEditText,
        etObservation: TextInputEditText
    ) {
        Toast.makeText(this, "Subiendo imagen…", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val url = CloudinaryUploader.uploadImage(this@ClientActivity, imageUri)

            if (url != null) {
                pedido.imagen_url = url
                savePedidoToFirestore(pedido, btnSolicitar, etName, etLength, etWidth, etObservation)
            } else {
                Toast.makeText(this@ClientActivity, "Error al subir la imagen. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                btnSolicitar.isEnabled = true
            }
        }
    }

    private fun savePedidoToFirestore(
        pedido: Pedido,
        btnSolicitar: View,
        etName: TextInputEditText,
        etLength: TextInputEditText,
        etWidth: TextInputEditText,
        etObservation: TextInputEditText
    ) {
        db.collection("pedidos").add(pedido)
            .addOnSuccessListener {
                Toast.makeText(this, "Pedido registrado exitosamente", Toast.LENGTH_LONG).show()
                btnSolicitar.isEnabled = true
                // Limpiar formulario
                etName.text = null; etLength.text = null
                etWidth.text = null; etObservation.text = null
                // Limpiar imagen seleccionada
                selectedImageUri = null
                val layoutNoImage   = findViewById<LinearLayout>(R.id.layoutNoImage)
                val layoutWithImage = findViewById<LinearLayout>(R.id.layoutWithImage)
                layoutNoImage.visibility   = View.VISIBLE
                layoutWithImage.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar pedido", Toast.LENGTH_LONG).show()
                btnSolicitar.isEnabled = true
            }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
