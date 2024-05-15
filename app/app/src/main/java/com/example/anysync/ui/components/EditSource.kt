package com.example.anysync.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.anysync.R
import com.example.anysync.data.Source

@Composable
fun EditSource(
    source: MutableState<Source>,
) {
    val pathPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
            source.value = source.value.copy(path = uri.toString())
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = source.value.name,
            onValueChange = { source.value = source.value.copy(name = it) },
            label = { Text("name") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = source.value.host,
            onValueChange = { source.value = source.value.copy(host = it) },
            label = { Text("host") },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { pathPickerLauncher.launch(null) },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.folder_rounded_48),
                    contentDescription = "select directory",
                    modifier = Modifier.size(24.dp),
                    tint = if (source.value.path.isEmpty()) Color.Red else Color.Black,
                )
                Text(
                    text = source.value.path.ifEmpty { "path not selected" },
                    color = if (source.value.path.isEmpty()) Color.Red else Color.Black,
                )
            }
        }
    }
}
