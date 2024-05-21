package com.example.anysync.ui.components

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.anysync.R
import com.example.anysync.data.Actions
import com.example.anysync.data.Source

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSource(
    source: MutableState<Source>,
) {
    val pathPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
            val splitPath = uri?.path?.split(":") ?: return@rememberLauncherForActivityResult
            val path = "${Environment.getExternalStorageDirectory()}/${splitPath[1]}"
            source.value = source.value.copy(path = path)
        }

    fun toggleAction(action: Actions) {
        source.value =
            source.value.copy(
                actions =
                when (source.value.actions) {
                    Actions.NONE -> action
                    Actions.GET -> if (action == Actions.GET) Actions.NONE else Actions.GET_SET
                    Actions.SET -> if (action == Actions.SET) Actions.NONE else Actions.GET_SET
                    Actions.GET_SET -> if (action == Actions.GET) Actions.SET else Actions.GET
                }
            )
    }

    var labelTouched by remember { mutableStateOf(source.value.label != source.value.name) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = source.value.name,
            onValueChange = {
                source.value = source.value.copy(
                    name = it,
                    label = if (!labelTouched) it else source.value.label
                )
            },
            label = { Text("name") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = if (labelTouched) source.value.label else source.value.name,
            onValueChange = {
                labelTouched = true
                source.value = source.value.copy(label = it)
            },
            label = { Text("label") },
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
                    tint = if (source.value.path.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = source.value.path.ifEmpty { "path not selected" },
                    color = if (source.value.path.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = source.value.actions in arrayOf(Actions.GET, Actions.GET_SET),
                onClick = { toggleAction(Actions.GET) },
                label = { Text("get") },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_downward_rounded_48),
                        contentDescription = "get",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            )
            FilterChip(
                selected = source.value.actions in arrayOf(Actions.SET, Actions.GET_SET),
                onClick = { toggleAction(Actions.SET) },
                label = { Text("set") },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_upward_rounded_48),
                        contentDescription = "set",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            )
        }
    }
}
