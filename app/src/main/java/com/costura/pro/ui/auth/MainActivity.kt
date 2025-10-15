package com.costura.pro.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.costura.pro.databinding.ActivityMainBinding
import com.costura.pro.utils.AppPreferences
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)

        // Check if user is already logged in
        if (preferences.isLoggedIn) {
            redirectToDashboard()
            return
        }

        setupAnimations()
        setupButtonAnimations()
        setupClickListeners()
    }

    private fun setupAnimations() {
        // Referencias a las vistas - USANDO BINDING
        val ivLogo = binding.ivLogo
        val tvTitle = binding.tvTitle
        val tvSubtitle = binding.tvSubtitle
        val divider = binding.divider
        val cardLogin = binding.cardLogin
        val ivUser = binding.ivUser
        val cardAdmin = binding.cardAdmin
        val tvFooter = binding.tvFooter

        // Configurar estado inicial invisible
        setViewsInvisible(ivLogo, tvTitle, tvSubtitle, divider, cardLogin, cardAdmin, tvFooter)

        // Animación secuencial con delays
        ivLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setStartDelay(300)
            .setInterpolator(BounceInterpolator())
            .withEndAction {
                // Logo animado → Título
                tvTitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        // Título → Subtítulo
                        tvSubtitle.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(500)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .withEndAction {
                                // Subtítulo → Línea divisora
                                divider.animate()
                                    .alpha(1f)
                                    .scaleX(1f)
                                    .setDuration(400)
                                    .withEndAction {
                                        // Línea → Tarjeta login
                                        animateLoginCard(cardLogin, ivUser)
                                    }
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()

        // Footer aparece último
        tvFooter.animate()
            .alpha(1f)
            .setStartDelay(2000)
            .setDuration(800)
            .start()

        // Card admin aparece después del login
        cardAdmin.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(1800)
            .setDuration(600)
            .start()
    }

    private fun setViewsInvisible(vararg views: View) {
        views.forEach { view ->
            view.alpha = 0f
            view.translationY = 30f
        }

        // Configuraciones especiales
        binding.ivLogo.apply {
            scaleX = 0.8f
            scaleY = 0.8f
        }

        binding.divider.apply {
            scaleX = 0f
        }
    }

    private fun animateLoginCard(card: MaterialCardView, userIcon: ImageView) {
        // Animación de la tarjeta
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // Animación del icono de usuario
                userIcon.animate()
                    .rotationBy(360f)
                    .setDuration(800)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun setupButtonAnimations() {
        val btnLogin = binding.btnLogin

        // Animación hover (solo API 21+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            btnLogin.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(50).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                false
            }
        }
    }

    private fun performLoginAnimation() {
        val btnLogin = binding.btnLogin

        // Animación de carga en el botón
        btnLogin.text = "CARGANDO..."
        btnLogin.isEnabled = false

        // Simular proceso de login
        Handler(Looper.getMainLooper()).postDelayed({
            btnLogin.text = "INICIAR SESIÓN"
            btnLogin.isEnabled = true

            // Efecto de éxito
            btnLogin.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .withEndAction {
                    btnLogin.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()

            Toast.makeText(this, "Login exitoso!", Toast.LENGTH_SHORT).show()

            // Aquí puedes redirigir al login real
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }, 2000)
    }
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun redirectToDashboard() {
        val intent = when (preferences.userRole) {
            "ADMIN" -> Intent(this, com.costura.pro.ui.admin.AdminDashboardActivity::class.java)
            "WORKER" -> Intent(this, com.costura.pro.ui.worker.WorkerDashboardActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}