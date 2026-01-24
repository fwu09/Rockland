// Screen handling the registration form in the UI layer.
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rockland.R
import com.example.rockland.ui.theme.BackgroundLight
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.RocklandTheme
import com.example.rockland.ui.theme.TextDark
import com.example.rockland.ui.theme.TextLight

// Email validation helper function
private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBackClick: () -> Unit = {},
    onRegisterClick: (email: String, password: String, firstName: String, lastName: String) -> Unit = { _, _, _, _ -> },
    onLoginClick: () -> Unit = {},
    onClearError: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }

    // Email validation
    val isEmailValid = email.isEmpty() || isValidEmail(email)
    val showEmailError = emailTouched && email.isNotEmpty() && !isEmailValid

    // Check if passwords match (only when both are not empty)
    val passwordsMatch =
        password.isEmpty() || confirmPassword.isEmpty() || password == confirmPassword

    // Password length validation
    val isPasswordLongEnough = password.isEmpty() || password.length >= 6

    // All fields validation
    val isFormValid = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            isEmailValid &&
            password.isNotBlank() &&
            isPasswordLongEnough &&
            passwordsMatch

    // Clear error when user starts typing again
    LaunchedEffect(firstName, lastName, email, password, confirmPassword) {
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
                        text = "Please check your information and try again",
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(stringResource(R.string.first_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text(stringResource(R.string.last_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
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
                    unfocusedBorderColor = if (showEmailError) Color.Red else TextDark.copy(alpha = 0.5f),
                    focusedLabelColor = if (showEmailError) Color.Red else Rock1,
                    unfocusedLabelColor = if (showEmailError) Color.Red else TextDark.copy(alpha = 0.5f),
                    cursorColor = Rock1
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                isError = password.isNotEmpty() && !isPasswordLongEnough,
                supportingText = {
                    if (password.isNotEmpty() && !isPasswordLongEnough) {
                        Text(
                            text = "Password must be at least 6 characters",
                            color = Color.Red
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
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
                    focusedBorderColor = if (password.isNotEmpty() && !isPasswordLongEnough) Color.Red else Rock1,
                    unfocusedBorderColor = if (password.isNotEmpty() && !isPasswordLongEnough) Color.Red else TextDark.copy(
                        alpha = 0.5f
                    ),
                    focusedLabelColor = if (password.isNotEmpty() && !isPasswordLongEnough) Color.Red else Rock1,
                    unfocusedLabelColor = if (password.isNotEmpty() && !isPasswordLongEnough) Color.Red else TextDark.copy(
                        alpha = 0.5f
                    ),
                    cursorColor = Rock1
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.confirm_password)) },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                isError = !passwordsMatch,
                supportingText = {
                    if (!passwordsMatch) {
                        Text(
                            text = "Passwords do not match",
                            color = Color.Red
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (passwordsMatch) Rock1 else Color.Red,
                    unfocusedBorderColor = if (passwordsMatch) TextDark.copy(alpha = 0.5f) else Color.Red,
                    focusedLabelColor = if (passwordsMatch) Rock1 else Color.Red,
                    unfocusedLabelColor = if (passwordsMatch) TextDark.copy(alpha = 0.5f) else Color.Red,
                    cursorColor = Rock1
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Client-side validation before sending to server
                    when {
                        firstName.isBlank() || lastName.isBlank() -> {
                            // Fields will show their individual errors
                        }

                        !isValidEmail(email) -> {
                            // Email error will be shown
                        }

                        password.length < 6 -> {
                            onShowMessage("Password must be at least 6 characters.")
                        }

                        password != confirmPassword -> {
                            onShowMessage("Passwords do not match.")
                        }

                        else -> {
                            // All validations passed, proceed with registration
                            onRegisterClick(
                                email.trim(),
                                password,
                                firstName.trim(),
                                lastName.trim()
                            )
                        }
                    }
                },
                enabled = !isLoading && isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Rock1,
                    contentColor = TextLight
                )
            ) {
                Text(
                    text = if (isLoading) "Creating Account..." else stringResource(R.string.register),
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
                    text = stringResource(R.string.have_account),
                    color = TextDark.copy(alpha = 0.7f)
                )
                TextButton(onClick = onLoginClick) {
                    Text(
                        text = stringResource(R.string.login),
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

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RocklandTheme {
        RegisterScreen()
    }
}
