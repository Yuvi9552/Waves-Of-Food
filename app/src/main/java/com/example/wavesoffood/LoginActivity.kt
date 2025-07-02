package com.example.wavesoffood

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffood.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var email: String
    private lateinit var password: String
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = Firebase.auth
        database = Firebase.database.reference

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.loginbutton.setOnClickListener {
            email = binding.loginemail.text.toString().trim()
            password = binding.loginpassword.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
            } else {
                loginUserAccount(email, password)
            }
        }

        binding.googleloginbutton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }

        binding.donthavebutton.setOnClickListener {
            startActivity(Intent(this, SigninActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        auth.currentUser?.let {
            startActivity(Intent(this, ChooseLocation::class.java))
            finish()
        }
    }

    private fun loginUserAccount(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateUi(auth.currentUser)
            } else {
                Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUi(user: FirebaseUser?) {
        startActivity(Intent(this, ChooseLocation::class.java))
        finish()
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Signed in with Google", Toast.LENGTH_SHORT).show()
                        updateUi(auth.currentUser)
                    } else {
                        Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
