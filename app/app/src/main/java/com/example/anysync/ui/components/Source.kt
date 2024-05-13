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
import com.example.anysync.data.url
import com.example.anysync.getManyWs
import com.example.anysync.missingFiles
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.concurrent.thread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Source(source: com.example.anysync.data.Source) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var isAvailable by remember { mutableStateOf(false) }

    var missingLocalFiles by remember { mutableStateOf(arrayOf<String>()) }

    fun refreshMissingFiles() {
        thread {
            runBlocking {
                isLoading = true

                try {
                    missingLocalFiles = missingFiles(source)
                    isAvailable = true
                } catch (e: Exception) {
                    isAvailable = false
                } finally {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshMissingFiles()
    }

    val filesSynced = remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun onSyncAllClick() {
        val workUUID = UUID.randomUUID().toString()
        getManyWs(context, workUUID, source, missingLocalFiles).observeForever {
            filesSynced.value = it
            if (it.first == it.second) {
                refreshMissingFiles()
            }
        }
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
    ) {
        if (isLoading) {
            Text("loading...", modifier = Modifier.padding(all = 8.dp))
            return@Card
        }

        if (!isAvailable) {
            Text("source not available", modifier = Modifier.padding(all = 8.dp), color = androidx.compose.ui.graphics.Color.Red)
            return@Card
        }

        Column(
            modifier = Modifier.padding(all = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "${source.name}: ${missingLocalFiles.size} missing")

                if (filesSynced.value != null) {
                    val (completed, total) = filesSynced.value!!

                    if (completed == total) {
                        Text("all files synced")
                    } else {
                        Text("synced $completed of $total files")
                    }
                } else {
                    Button(onClick = ::onSyncAllClick) {
                        Text("sync all")
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
