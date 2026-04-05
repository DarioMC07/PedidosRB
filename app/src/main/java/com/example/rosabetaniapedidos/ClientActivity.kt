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
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ClientActivity : AppCompatActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

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
                if (item.itemId == R.id.action_logout) {
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
        val length = etLength.text?.toString()?.trim().orEmpty()
        val width = etWidth.text?.toString()?.trim().orEmpty()
        val gramaje = actGramaje.text?.toString()?.trim().orEmpty()

        var ok = true
        if (name.isEmpty()) {
            tilName.error = getString(R.string.error_required)
            ok = false
        }
        if (length.isEmpty()) {
            tilLength.error = getString(R.string.error_required)
            ok = false
        }
        if (width.isEmpty()) {
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

        Toast.makeText(this, R.string.client_submit_placeholder, Toast.LENGTH_LONG).show()
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
