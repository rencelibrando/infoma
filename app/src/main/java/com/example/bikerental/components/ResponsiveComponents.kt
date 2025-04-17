package com.example.bikerental.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusState

/**
 * A responsive text field with consistent styling across the app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    errorMessage: String? = null,
    supportingText: @Composable (() -> Unit)? = null,
    onFocusChanged: ((FocusState) -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onFocusChanged != null) {
                        Modifier.onFocusChanged { onFocusChanged(it) }
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            supportingText = supportingText
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * A responsive button with consistent styling and loading state
 */
@Composable
fun ResponsiveButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text)
        }
    }
}

/**
 * Google logo composable that renders the multicolor "G" logo
 */
@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val width = size.width
        val height = size.height
        
        // Colors from Google's brand guidelines
        val blue = Color(0xFF4285F4)
        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)
        
        // Draw the outer circle/shape
        drawArc(
            color = blue,
            startAngle = -180f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(width, height)
        )
        
        drawArc(
            color = red,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(width, height)
        )
        
        drawArc(
            color = yellow,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(width, height)
        )
        
        drawArc(
            color = green,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(width, height)
        )
        
        // White center
        drawCircle(
            color = Color.White,
            radius = width * 0.33f,
            center = Offset(width / 2, height / 2)
        )
        
        // Draw the "leg" of the G
        val path = Path().apply {
            moveTo(width * 0.75f, height / 2)
            lineTo(width * 0.75f, height * 0.75f)
            lineTo(width * 0.6f, height * 0.75f)
            lineTo(width * 0.6f, height / 2)
            close()
        }
        
        drawPath(
            path = path,
            color = blue
        )
    }
} 