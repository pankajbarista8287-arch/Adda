package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.localization.AppLang
import com.example.localization.LocalAddaStrings
import com.example.model.UserProfile
import com.example.ui.components.AddaBrandLogo
import com.example.ui.components.AddaCosmicBackground
import com.example.ui.components.UserAvatarWithFrame
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalPurpleAccent
import com.example.ui.theme.RoyalPurpleDeep
import com.example.ui.theme.RoyalPurpleSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val activity = context as? Activity
    if (activity == null) {
        onUnauthenticated()
        return
    }
    scope.launch {
        try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for Credential Manager")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google Sign-In failed")
        }
    }
}

@Composable
fun AuthScreen(
    currentLang: AppLang,
    skipAutoSignIn: Boolean = false,
    onToggleLang: (AppLang) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val strings = LocalAddaStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(skipAutoSignIn) {
        if (!skipAutoSignIn) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = onAuthSuccess,
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    AddaCosmicBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AddaBrandLogo()
                    OutlinedButton(
                        onClick = { onToggleLang(if (currentLang == AppLang.HI) AppLang.EN else AppLang.HI) },
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.Translate, contentDescription = strings.languageLabel, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (currentLang == AppLang.HI) "EN" else "हिन्दी", color = NeonCyan)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Image(
                    painter = painterResource(id = R.drawable.img_adda_hero),
                    contentDescription = "ADDA Stage",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = strings.splashSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonCyan,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(22.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RoyalPurpleSurface.copy(alpha = 0.88f)),
                    border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                errorMsg = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMsg = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalGold,
                                contentColor = ObsidianVoid
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = ObsidianVoid,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Login, contentDescription = strings.signInWithGoogle)
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = strings.signInWithGoogle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!errorMsg.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                color = CrimsonRed.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CrimsonRed)
                            ) {
                                Text(
                                    text = errorMsg.orEmpty(),
                                    color = CrimsonRed,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Text(
                                text = strings.verifyOtpNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.googleSecurityBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileEditorSheetContent(
    profile: UserProfile,
    currentPhone: String,
    onSave: (username: String, bio: String, age: Long, gender: String, avatarUrl: String?, phone: String) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAddaStrings.current
    var username by remember(profile.username) { mutableStateOf(profile.username) }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio) }
    var ageText by remember(profile.age) { mutableStateOf(profile.age.toString()) }
    var gender by remember(profile.gender) { mutableStateOf(profile.gender) }
    var phone by remember(currentPhone) { mutableStateOf(currentPhone) }
    var pickedPhotoUri by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(strings.setupProfileTitle, style = MaterialTheme.typography.headlineSmall, color = RoyalGold)
        Text("ADDA ID: ${profile.addaCode}", style = MaterialTheme.typography.labelSmall, color = NeonCyan)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UserAvatarWithFrame(
                avatarUrl = pickedPhotoUri ?: profile.avatarUrl,
                username = username,
                vipTier = profile.vipTier,
                size = 64.dp,
                verified = profile.verified
            )
            OutlinedButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.testTag("pick_avatar_button")
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = "Pick Photo", tint = NeonCyan)
                Spacer(Modifier.size(6.dp))
                Text("Photo Picker", color = NeonCyan)
            }
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(strings.usernameLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("username_input")
        )
        OutlinedTextField(
            value = ageText,
            onValueChange = { ageText = it.filter(Char::isDigit).take(2) },
            label = { Text(strings.ageLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text(strings.bioLabel) },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter(Char::isDigit).take(10) },
            label = { Text(strings.mobileHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(strings.genderLabel, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MALE" to strings.male, "FEMALE" to strings.female, "OTHER" to strings.other).forEach { (key, label) ->
                FilterChip(
                    selected = gender == key,
                    onClick = { gender = key },
                    label = { Text(label) }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                Text(strings.cancel)
            }
            Button(
                onClick = {
                    val age = ageText.toLongOrNull()?.coerceIn(13L, 100L) ?: 21L
                    onSave(username, bio, age, gender, pickedPhotoUri, phone)
                    onClose()
                },
                modifier = Modifier.weight(1f).testTag("save_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurpleAccent)
            ) {
                Text(strings.saveProfile)
            }
        }
    }
}
