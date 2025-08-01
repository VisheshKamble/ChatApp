package com.example.chatx

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.chatx.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "chatx_prefs"
        private const val KEY_REMEMBER = "remember_me"
        private const val KEY_SAVED_NAME = "saved_name"
        private const val KEY_SAVED_EMAIL = "saved_email"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional fullscreen flags (can be adjusted for modern insets-based handling)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Auto-login if "remember me" was checked and user is still authenticated
        val remembered = prefs.getBoolean(KEY_REMEMBER, false)
        if (remembered && auth.currentUser != null) {
            goToHome()
            return
        }

        // Pre-fill name/email if previously saved (convenience)
        binding.NameText.setText(prefs.getString(KEY_SAVED_NAME, ""))
        binding.emailText.setText(prefs.getString(KEY_SAVED_EMAIL, ""))

        binding.loginButton.setOnClickListener {
            clearFieldErrors()

            val name = binding.NameText.text.toString().trim()
            val email = binding.emailText.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val rememberMeChecked = binding.rememberMe.isChecked

            if (!validateInputs(name, email, password)) return@setOnClickListener

            setLoading(true)

            // Try sign-in first
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    saveUserProfileIfNeeded(email, name)
                    persistRememberState(rememberMeChecked, name, email)
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    goToHome()
                }
                .addOnFailureListener { signInError ->
                    // If login failed, attempt signup
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            saveUserProfileIfNeeded(email, name)
                            persistRememberState(rememberMeChecked, name, email)
                            Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()
                            goToHome()
                        }
                        .addOnFailureListener { signUpError ->
                            Toast.makeText(
                                this,
                                "Authentication failed: ${signUpError.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            setLoading(false)
                        }
                }
        }

        // Insets handling (status bar, nav bar, IME)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = maxOf(systemBars.bottom, ime.bottom)
            binding.main.updatePadding(top = systemBars.top, bottom = bottom)
            insets
        }
    }

    private fun clearFieldErrors() {
        binding.nameLayout.error = null
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
    }

    private fun validateInputs(name: String, email: String, password: String): Boolean {
        var valid = true
        if (name.isEmpty()) {
            binding.nameLayout.error = "Name required"
            valid = false
        }
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email required"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Invalid email"
            valid = false
        }
        if (password.isEmpty()) {
            binding.passwordLayout.error = "Password required"
            valid = false
        } else if (password.length < 6) {
            binding.passwordLayout.error = "At least 6 characters"
            valid = false
        }
        return valid
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.loginButton.isEnabled = false
            binding.loginButton.text = "Please wait..."
        } else {
            binding.loginButton.isEnabled = true
            binding.loginButton.text = "LOGIN"
        }
    }

    private fun persistRememberState(remember: Boolean, name: String, email: String) {
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER, remember)
            if (remember) {
                putString(KEY_SAVED_NAME, name)
                putString(KEY_SAVED_EMAIL, email)
            } else {
                remove(KEY_SAVED_NAME)
                remove(KEY_SAVED_EMAIL)
            }
            apply()
        }
    }

    private fun saveUserProfileIfNeeded(email: String, name: String) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid)
        val userMap = hashMapOf(
            "uid" to uid,
            "email" to email,
            "name" to name
        )
        // Use merge to avoid overwriting any other fields unintentionally
        docRef.set(userMap)
            .addOnFailureListener {
                // Optional: log or silently handle profile sync failure
            }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    /**
     * Call this from HomeActivity (or wherever logout happens) to fully sign the user out
     * and clear the "remember me" persistence.
     */
    fun logout() {
        // clear remember flag
        prefs.edit().putBoolean(KEY_REMEMBER, false).apply()
        // sign out from Firebase
        auth.signOut()
        // redirect back to login
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

