package com.example.evenly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.example.evenly.api.ApiRepository
import com.example.evenly.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(baseContext)

        binding.createAccount.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        lifecycleScope.launch {
                            val currentUser = it.result!!.user!!
                            try {
                                val result = ApiRepository.auth.getUser(currentUser.uid)
                                result.fold(
                                        onSuccess = { response ->
                                            if (response.user.name.isNullOrBlank()) {
                                                // User doesn't have a name set, go to
                                                // NameCollection
                                                val intent =
                                                        Intent(
                                                                this@Login,
                                                                NameCollection::class.java
                                                        )
                                                startActivity(intent)
                                            } else {
                                                // User has a name, go to MainActivity
                                                val intent =
                                                        Intent(this@Login, MainActivity::class.java)
                                                intent.putExtra("user_name", response.user.name)
                                                intent.putExtra("user_id", response.user.id)
                                                startActivity(intent)
                                            }
                                        },
                                        onFailure = { exception ->
                                            Log.e("Login", "Failed to get user info", exception)
                                            // If we can't get user info, assume they need to set
                                            // their name
                                            val intent =
                                                    Intent(this@Login, NameCollection::class.java)
                                            startActivity(intent)
                                        }
                                )
                            } catch (e: Exception) {
                                Log.e("Login", "Exception during user info fetch", e)
                                // If there's an exception, assume they need to set their name
                                val intent = Intent(this@Login, NameCollection::class.java)
                                startActivity(intent)
                            }
                        }
                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Empty fields are not allowed.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnGoogleSignin.setOnClickListener {
            val signInWithGoogleOption =
                    GetSignInWithGoogleOption.Builder(getString(R.string.client_id)).build()

            val request =
                    GetCredentialRequest.Builder()
                            .addCredentialOption(signInWithGoogleOption)
                            .build()

            lifecycleScope.launch {
                try {
                    val result =
                            credentialManager.getCredential(
                                    request = request,
                                    context = baseContext
                            )
                    handleSignIn(result.credential)
                } catch (e: Exception) {
                    Log.e(TAG, "Google sign-in failed", e)
                    Toast.makeText(
                                    this@Login,
                                    "Google sign-in failed: ${e.message}",
                                    Toast.LENGTH_LONG
                            )
                            .show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }
    private fun handleSignIn(result: Credential) {
        // Handle the successfully returned credential.
        when (result) {
            is CustomCredential -> {
                if (result.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(result.data)

                        firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    // Catch any unrecognized credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                }
            }
            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential:success")
                val user = firebaseAuth.currentUser
                lifecycleScope.launch {
                    try {
                        // First try to get the user from the backend
                        val getUserResult = ApiRepository.auth.getUser(user!!.uid)
                        getUserResult.fold(
                                onSuccess = { response ->
                                    // User exists, check if they have a name
                                    if (response.user.name.isNullOrBlank()) {
                                        // User doesn't have a name set, go to NameCollection
                                        val intent = Intent(this@Login, NameCollection::class.java)
                                        startActivity(intent)
                                    } else {
                                        // User has a name, go to MainActivity with user ID
                                        val intent = Intent(this@Login, MainActivity::class.java)
                                        intent.putExtra("user_name", response.user.name)
                                        intent.putExtra("user_id", response.user.id)
                                        startActivity(intent)
                                    }
                                },
                                onFailure = { exception ->
                                    Log.d(TAG, "User not found in database, creating new user")
                                    // User doesn't exist in database, create them
                                    val registerResult =
                                            ApiRepository.auth.register(user.email!!, user.uid)
                                    registerResult.fold(
                                            onSuccess = { registerResponse ->
                                                Log.d(TAG, "User created successfully in database")
                                                // New user created, go to NameCollection to set
                                                // their name
                                                val intent =
                                                        Intent(
                                                                this@Login,
                                                                NameCollection::class.java
                                                        )
                                                startActivity(intent)
                                            },
                                            onFailure = { registerException ->
                                                Log.e(
                                                        TAG,
                                                        "Failed to create user in database",
                                                        registerException
                                                )
                                                Toast.makeText(
                                                                this@Login,
                                                                "Failed to create account: ${registerException.message}",
                                                                Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                            }
                                    )
                                }
                        )
                    } catch (e: Exception) {
                        Log.e("Login", "Exception during user info fetch", e)
                        // If there's an exception, assume they need to set their name
                        val intent = Intent(this@Login, NameCollection::class.java)
                        startActivity(intent)
                    }
                }
            } else {
                // If sign in fails, display a message to the user
                Log.w(TAG, "signInWithCredential:failure", task.exception)
                Toast.makeText(
                                this@Login,
                                "Google sign-in failed: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                        )
                        .show()
            }
        }
    }

    companion object {
        private const val TAG = "GoogleActivity"
    }
}
