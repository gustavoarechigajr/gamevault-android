package com.gamevault.android.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamevault.android.ui.theme.GVRed

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    vm: LoginViewModel = viewModel(),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val state by vm.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.init(context) }
    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onLoginSuccess() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF06091A), Color(0xFF0C1228))))
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "GameVault",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1).sp,
            )
            Text(
                text = "Sign in to your library",
                fontSize = 14.sp,
                color = Color(0xFF7090B8),
            )

            Spacer(Modifier.height(8.dp))

            if (!state.urlSaved) {
                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = vm::onServerUrlChange,
                    label = { Text("Server URL") },
                    placeholder = { Text("https://yourserver.duckdns.org") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = gvTextFieldColors(),
                )
            }

            OutlinedTextField(
                value = state.username,
                onValueChange = vm::onUsernameChange,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(12.dp),
                colors = gvTextFieldColors(),
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = vm::onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); vm.login(context) }),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF7090B8),
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = gvTextFieldColors(),
            )

            // Remember me
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Remember me", color = Color(0xFF7090B8), fontSize = 14.sp)
                Switch(
                    checked = state.rememberMe,
                    onCheckedChange = vm::onRememberMeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GVRed,
                        uncheckedThumbColor = Color(0xFF7090B8),
                        uncheckedTrackColor = Color(0xFF1C2A44),
                    ),
                )
            }

            if (state.error.isNotBlank()) {
                Text(state.error, color = GVRed, fontSize = 13.sp)
            }

            Button(
                onClick = { focusManager.clearFocus(); vm.login(context) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GVRed),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Sign In", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun gvTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GVRed,
    unfocusedBorderColor = Color(0xFF1C2A44),
    focusedLabelColor = GVRed,
    unfocusedLabelColor = Color(0xFF7090B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color(0xFFDCE8FF),
    cursorColor = GVRed,
    unfocusedContainerColor = Color(0xFF0C1228),
    focusedContainerColor = Color(0xFF111B35),
)
