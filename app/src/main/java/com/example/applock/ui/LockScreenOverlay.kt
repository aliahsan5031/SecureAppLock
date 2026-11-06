package com.example.applock.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applock.data.LockType
import kotlin.math.sqrt

@Composable
fun LockScreenOverlay(
    targetPackage: String,
    lockType: LockType,
    biometricEnabled: Boolean,
    expectedPin: String,
    expectedPassword: String,
    expectedPattern: String,
    onBiometricRequest: () -> Unit,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (biometricEnabled) {
            onBiometricRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Locked Application",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = targetPackage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (lockType) {
                LockType.PIN -> {
                    PinLockView(
                        expectedPin = expectedPin,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                }
                LockType.PASSWORD -> {
                    PasswordLockView(
                        expectedPassword = expectedPassword,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                }
                LockType.PATTERN -> {
                    PatternLockView(
                        expectedPattern = expectedPattern,
                        isSettingMode = false,
                        onSuccess = onSuccess,
                        onFailure = onFailure,
                        onPatternDrawn = {}
                    )
                }
            }

            if (biometricEnabled) {
                Spacer(modifier = Modifier.height(24.dp))
                IconButton(
                    onClick = onBiometricRequest,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Unlock",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Tap for Fingerprint / Face",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PasswordLockView(
    expectedPassword: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    fun verify() {
        if (password == expectedPassword) {
            isError = false
            onSuccess()
        } else {
            isError = true
            password = ""
            onFailure()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.85f)
    ) {
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                isError = false
            },
            label = { Text("Enter Password") },
            singleLine = true,
            isError = isError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { verify() }),
            modifier = Modifier.fillMaxWidth()
        )

        if (isError) {
            Text(
                text = "Incorrect password! Try again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { verify() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock")
        }
    }
}

@Composable
fun PinLockView(
    expectedPin: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun checkPin(pin: String) {
        if (pin == expectedPin) {
            isError = false
            onSuccess()
        } else {
            isError = true
            enteredPin = ""
            onFailure()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for (i in 0 until expectedPin.length) {
                val filled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isError -> MaterialTheme.colorScheme.error
                                filled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val buttons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        buttons.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                row.forEach { digit ->
                    KeypadButton(
                        text = digit,
                        onClick = {
                            when (digit) {
                                "DEL" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                "" -> Unit
                                else -> {
                                    if (enteredPin.length < expectedPin.length) {
                                        enteredPin += digit
                                        isError = false
                                        if (enteredPin.length == expectedPin.length) {
                                            checkPin(enteredPin)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(
                if (text.isEmpty()) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(enabled = text.isNotEmpty(), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (text == "DEL") {
            Icon(Icons.Default.Backspace, contentDescription = "Delete")
        } else {
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PatternLockView(
    expectedPattern: String,
    isSettingMode: Boolean = false,
    onSuccess: () -> Unit = {},
    onFailure: () -> Unit = {},
    onPatternDrawn: (String) -> Unit = {}
) {
    var selectedDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var isError by remember { mutableStateOf(false) }

    val dotColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val lineColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when {
                isSettingMode -> "Draw desired pattern"
                isError -> "Wrong pattern! Try again"
                else -> "Draw pattern to unlock"
            },
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .size(260.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isError = false
                            selectedDots = emptyList()
                            currentTouchPoint = offset
                            val dotIndex = getDotIndex(offset, size.width.toFloat())
                            if (dotIndex != null) {
                                selectedDots = listOf(dotIndex)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentTouchPoint = change.position
                            val dotIndex = getDotIndex(change.position, size.width.toFloat())
                            if (dotIndex != null && !selectedDots.contains(dotIndex)) {
                                selectedDots = selectedDots + dotIndex
                            }
                        },
                        onDragEnd = {
                            currentTouchPoint = null
                            val patternString = selectedDots.joinToString(",")
                            if (isSettingMode) {
                                onPatternDrawn(patternString)
                            } else {
                                if (patternString == expectedPattern) {
                                    isError = false
                                    onSuccess()
                                } else {
                                    isError = true
                                    onFailure()
                                }
                            }
                        },
                        onDragCancel = {
                            currentTouchPoint = null
                            selectedDots = emptyList()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = size.width / 4f
                val dotCenters = List(9) { index ->
                    val row = index / 3
                    val col = index % 3
                    Offset(step * (col + 1), step * (row + 1))
                }

                for (i in 0 until selectedDots.size - 1) {
                    val p1 = dotCenters[selectedDots[i]]
                    val p2 = dotCenters[selectedDots[i + 1]]
                    drawLine(
                        color = lineColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                if (selectedDots.isNotEmpty() && currentTouchPoint != null) {
                    val lastCenter = dotCenters[selectedDots.last()]
                    drawLine(
                        color = lineColor,
                        start = lastCenter,
                        end = currentTouchPoint!!,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                dotCenters.forEachIndexed { index, center ->
                    val isSelected = selectedDots.contains(index)
                    drawCircle(
                        color = if (isSelected) dotColor else Color.Gray.copy(alpha = 0.4f),
                        radius = if (isSelected) 14.dp.toPx() else 8.dp.toPx(),
                        center = center
                    )
                }
            }
        }
    }
}

private fun getDotIndex(point: Offset, totalSize: Float): Int? {
    val step = totalSize / 4f
    val hitRadius = step * 0.45f

    for (index in 0 until 9) {
        val row = index / 3
        val col = index % 3
        val center = Offset(step * (col + 1), step * (row + 1))
        val distance = sqrt((point.x - center.x) * (point.x - center.x) + (point.y - center.y) * (point.y - center.y))
        if (distance <= hitRadius) {
            return index
        }
    }
    return null
}
