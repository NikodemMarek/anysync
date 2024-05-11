package com.example.anysync.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.anysync.getManyWs
import com.example.anysync.missingFiles
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.concurrent.thread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Source(
    title: String,
    path: String,
    host: String,
) {
    val context = LocalContext.current

    var missingLocalFiles by remember { mutableStateOf<Array<String>>(arrayOf<String>()) }

    fun refreshMissingFiles() {
        thread {
            runBlocking {
                missingLocalFiles = missingFiles(path, host)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshMissingFiles()
    }

    val filesSynced = remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun onSyncAllClick() {
        val workUUID = UUID.randomUUID().toString()
        getManyWs(context, workUUID, missingLocalFiles).observeForever {
            filesSynced.value = it
            if (it.first == it.second) {
                refreshMissingFiles()
            }
        }
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
    ) {
        Column(
            modifier = Modifier.padding(all = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "$title: ${missingLocalFiles.size} missing")

                if (filesSynced.value != null) {
                    val (completed, total) = filesSynced.value!!

                    if (completed == total) {
                        Text("All files synced")
                    } else {
                        Text("Synced $completed of $total files")
                    }
                } else {
                    Button(onClick = ::onSyncAllClick) {
                        Text("Sync All")
                    }
                }
            }

            if (expanded) {
                missingLocalFiles.forEach { item ->
                    Text(text = item, modifier = Modifier.padding(all = 4.dp), color = androidx.compose.ui.graphics.Color.Green)
                }
            }
        }
    }
}
