package com.example.rosabetaniapedidos

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            if (auth.currentUser?.email == "admin@admin.com") {
                startActivity(Intent(this, AdminActivity::class.java))
            } else {
                startActivity(Intent(this, ClientActivity::class.java))
            }
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tilUser = findViewById<TextInputLayout>(R.id.tilUser)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etUser = findViewById<TextInputEditText>(R.id.etUser)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvForgot = findViewById<TextView>(R.id.tvForgot)

        btnLogin.setOnClickListener {
            tilUser.error = null
            tilPassword.error = null

            val user = etUser.text?.toString()?.trim().orEmpty()
            val pass = etPassword.text?.toString()?.trim().orEmpty()

            var ok = true
            if (user.isBlank()) {
                tilUser.error = getString(R.string.error_required)
                ok = false
            }
            if (pass.isBlank()) {
                tilPassword.error = getString(R.string.error_required)
                ok = false
            }
            if (!ok) return@setOnClickListener

            btnLogin.isEnabled = false
            val originalText = btnLogin.text
            btnLogin.text = "Entrando..."

            auth.signInWithEmailAndPassword(user, pass)
                .addOnCompleteListener { task ->
                    btnLogin.isEnabled = true
                    btnLogin.text = originalText

                    if (task.isSuccessful) {
                        if (auth.currentUser?.email == "admin@admin.com") {
                            startActivity(Intent(this, AdminActivity::class.java))
                        } else {
                            startActivity(Intent(this, ClientActivity::class.java))
                        }
                        finish()
                    } else {
                        tilPassword.error = task.exception?.localizedMessage ?: "No se pudo iniciar sesión"
                    }
                }
        }

        tvForgot.setOnClickListener {
            tilUser.error = null
            val email = etUser.text?.toString()?.trim().orEmpty()
            if (email.isBlank()) {
                tilUser.error = "Ingresa tu correo para recuperar la contraseña"
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        tilUser.error = null
                        tilPassword.error = null
                        tilPassword.helperText = "Te enviamos un correo para restablecer tu contraseña"
                    } else {
                        tilUser.error = task.exception?.localizedMessage ?: "No se pudo enviar el correo"
                    }
                }
        }
    }
}
