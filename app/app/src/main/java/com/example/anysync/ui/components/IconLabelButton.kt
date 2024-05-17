package com.example.anysync.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun IconLabelButton(
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
    painter: Painter,
    tint: Color = Color.White,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(color),
        enabled = enabled,
    ) {
        Icon(
            painter = painter,
            contentDescription = label,
            modifier = Modifier.size(ButtonDefaults.IconSize),
            tint = if (enabled) tint else Color.Gray
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(label, color = if (enabled) tint else Color.Gray)
    }
}