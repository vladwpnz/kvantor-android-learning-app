package com.bambiloff.kvantor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bambiloff.kvantor.ui.theme.KvantorTheme
import com.bambiloff.kvantor.ui.theme.Rubik
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AuthActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            KvantorTheme {
                AuthScreen()
            }
        }
    }

    /**
     * Після успішного входу перевіряємо профіль у Firestore
     * і переходимо на відповідний екран.
     */
    private fun navigateBasedOnUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        val usersRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)

        usersRef.get()
            .addOnSuccessListener { doc ->
                val hasProfile = doc.exists() &&
                    !doc.getString("nickname").isNullOrBlank() &&
                    !doc.getString("avatarName").isNullOrBlank()

                if (!hasProfile) {
                    // Якщо профілю не було — нехай все одно налаштує профіль
                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                } else {
                    // У всіх інших випадках — вибір курсу
                    startActivity(Intent(this, CourseSelectionActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Could not check your profile",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AuthScreen() {
        val activity = this@AuthActivity
        val context = LocalContext.current

        // стани
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        // Snackbar
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        // Налаштування Google Sign-In клієнта
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

        // Launcher для Google-входу
        val googleLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                task.result?.let { acct ->
                    val cred = GoogleAuthProvider.getCredential(acct.idToken, null)
                    auth.signInWithCredential(cred)
                        .addOnCompleteListener { t ->
                            if (t.isSuccessful) {
                                activity.navigateBasedOnUserProfile()
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Google sign-in failed")
                                }
                            }
                        }
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Google sign-in failed")
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = KvBg
        ) { paddingValues ->
            KvGradientBackground(
                modifier = Modifier.padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    KvGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        Text(
                            "KVANTOR",
                            color = KvCyan,
                            style = MaterialTheme.typography.displayMedium,
                            fontFamily = Rubik,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Sign in to continue your learning quest",
                            color = KvMutedText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = Rubik,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email", fontFamily = Rubik) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(color = KvTextColor, fontFamily = Rubik),
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = KvCyan)
                            },
                            colors = KvOutlinedTextFieldColors()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", fontFamily = Rubik) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(color = KvTextColor, fontFamily = Rubik),
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = KvCyan)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible }
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible)
                                            Icons.Filled.Visibility
                                        else
                                            Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = KvMutedText
                                    )
                                }
                            },
                            colors = KvOutlinedTextFieldColors()
                        )

                        KvantorButton(
                            text = "Sign in",
                            onClick = {
                                auth.signInWithEmailAndPassword(email.trim(), password)
                                    .addOnCompleteListener { t ->
                                        if (t.isSuccessful) {
                                            activity.navigateBasedOnUserProfile()
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Incorrect email or password")
                                            }
                                        }
                                    }
                            },
                            leadingIcon = Icons.AutoMirrored.Filled.Login,
                            modifier = Modifier.fillMaxWidth()
                        )

                        KvantorOutlinedButton(
                            text = "Create account",
                            onClick = {
                                context.startActivity(Intent(context, RegisterActivity::class.java))
                            },
                            leadingIcon = Icons.Default.PersonAdd,
                            modifier = Modifier.fillMaxWidth()
                        )

                        KvantorOutlinedButton(
                            text = "Continue with Google",
                            onClick = {
                                googleLauncher.launch(googleClient.signInIntent)
                            },
                            leadingIcon = Icons.Default.AccountCircle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
