package com.example.anysync.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.anysync.R
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("name") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("host") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painter = painterResource(R.drawable.folder_rounded_48),
                    contentDescription = "select directory",
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = path ?: "path not selected",
                    color = if (path == null) Color.Red else Color.Unspecified
                )
            }
            Button(
                onClick = {
                    pathPickerLauncher.launch(null)
                },
            ) {
                Text(text = "select")
            }
        }

        Button(
            enabled = !(name.isEmpty() || path == null || host.isEmpty()),
            onClick = {
                onConfirm(
                    Source(
                        name,
                        path!!,
                        host,
                        id = source?.id ?: 0,
                    ),
                )
            },
            modifier = Modifier.align(Alignment.End),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "confirm")
                Text(text = "confirm")
            }
        }
    }
}
