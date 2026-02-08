// Screen responsible for the login form in the UI layer.
package com.example.rockland.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rockland.R
import com.example.rockland.ui.theme.BackgroundLight
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark

// Validates email input with Android's email pattern.
private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBackClick: () -> Unit = {},
    onLoginClick: (email: String, password: String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {},
    onClearError: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }

    // Email validation
    val isEmailValid = email.isEmpty() || isValidEmail(email)
    val showEmailError = emailTouched && email.isNotEmpty() && !isEmailValid

    // Form validation
    val isFormValid = email.isNotBlank() && isEmailValid && password.isNotBlank()

    // Clear error when user starts typing
    LaunchedEffect(email, password) {
        if (errorMessage != null) {
            onClearError()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(BackgroundLight, BackgroundLight)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextDark
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Rock1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error message display with retry hint
            if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please check your credentials and try again",
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailTouched = true
                },
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                isError = showEmailError,
                supportingText = {
                    if (showEmailError) {
                        Text(
                            text = "Please enter a valid email address",
                            color = Color.Red
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (showEmailError) Color.Red else Rock1,
                    unfocusedBorderColor = if (showEmailError) Color.Red else TextDark.copy(
                        alpha = 0.5f
                    ),
                    focusedLabelColor = if (showEmailError) Color.Red else Rock1,
                    unfocusedLabelColor = if (showEmailError) Color.Red else TextDark.copy(
                        alpha = 0.5f
                    ),
                    cursorColor = Rock1
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Rock1,
                    unfocusedBorderColor = TextDark.copy(alpha = 0.5f),
                    focusedLabelColor = Rock1,
                    unfocusedLabelColor = TextDark.copy(alpha = 0.5f),
                    cursorColor = Rock1
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Client-side validation before sending to server
                    when {
                        email.isBlank() -> onShowMessage("Email is required.")
                        !isValidEmail(email) -> onShowMessage("Please enter a valid email address.")
                        password.isBlank() -> onShowMessage("Password is required.")
                        else -> onLoginClick(email.trim(), password)
                    }
                },
                enabled = !isLoading && isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Rock3,
                    contentColor = TextDark
                )
            ) {
                Text(
                    text = if (isLoading) "Logging in..." else stringResource(R.string.login),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.no_account),
                    color = TextDark.copy(alpha = 0.7f)
                )
                TextButton(onClick = onRegisterClick) {
                    Text(
                        text = stringResource(R.string.register),
                        color = Rock1,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isLoading) {
            // Block UI while auth is in progress to avoid double taps / navigation glitches.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Rock1)
            }
        }
    }
}

