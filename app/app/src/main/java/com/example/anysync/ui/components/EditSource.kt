package com.example.anysync.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.anysync.data.Source

@Composable
fun EditSource(
    source: Source? = null,
    onConfirm: (Source) -> Unit,
) {
    var name by remember { mutableStateOf(source?.name ?: "") }
    var path by remember { mutableStateOf<String?>(source?.path) }
    var host by remember { mutableStateOf(source?.host ?: "") }

    val pathPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
            path = uri?.path
        }

    Column {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("name") },
        )
        Text(text = path ?: "")
        Button(onClick = {
            pathPickerLauncher.launch(null)
        }) {
            Text("select path")
        }
        TextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("host") },
        )

        if (name.isEmpty() || path == null || host.isEmpty()) {
            return@Column
        }

        Button(onClick = {
            onConfirm(
                Source(
                    name = name,
                    path = path!!,
                    host = host,
                ),
            )
        }) {
            Text(text = "save")
        }
    }
}
