package com.smartshare.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.smartshare.app.R
import com.smartshare.app.ui.MainActivity

/**
 * PUBLIC_INTERFACE
 * AuthActivity handles secure authentication using Firebase Auth with Google Sign-In.
 * It starts One Tap sign-in flow, authenticates with Firebase, and redirects to MainActivity.
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient

    private val launcher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val data = result.data
        val credential = Identity.getSignInClient(this).getSignInCredentialFromIntent(data)
        val idToken = credential.googleIdToken
        if (idToken != null) {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential)
                .addOnSuccessListener {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Auth failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "No ID token!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)

        findViewById<Button>(R.id.btnSignIn).setOnClickListener { startAuthFlow() }
    }

    private fun startAuthFlow() {
        // If google-services.json isn't configured, default_web_client_id will be blank.
        val clientId = getString(R.string.default_web_client_id)
        if (clientId.isNullOrBlank()) {
            Toast.makeText(this, "Google Sign-In is not configured. Please add google-services.json.", Toast.LENGTH_LONG).show()
            return
        }

        val request = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(clientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        oneTapClient.beginSignIn(request)
            .addOnSuccessListener { result ->
                val requestObj = buildIntentSenderRequest(result.pendingIntent.intentSender)
                launcher.launch(requestObj)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Sign-in failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }



    // Helper for ActivityResultContracts - build an IntentSenderRequest
    private fun buildIntentSenderRequest(sender: android.content.IntentSender): IntentSenderRequest =
        IntentSenderRequest.Builder(sender).build()
}
