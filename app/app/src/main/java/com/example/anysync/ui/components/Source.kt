package com.example.anysync.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import com.example.anysync.R
import com.example.anysync.getManyWs
import com.example.anysync.missingFiles
import com.example.anysync.setManyWs
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.concurrent.thread

enum class State {
    LOADING,
    AVAILABLE,
    UNAVAILABLE,
    EMPTY,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Source(source: com.example.anysync.data.Source) {
    val context = LocalContext.current

    var state by remember { mutableStateOf(State.LOADING) }

    var missingLocalFiles by remember { mutableStateOf(arrayOf<String>()) }
    var missingRemoteFiles by remember { mutableStateOf(arrayOf<String>()) }

    fun refreshMissingFiles() {
        thread {
            runBlocking {
                state = State.LOADING

                try {
                    val (missingLocal, missingRemote) = missingFiles(source)
                    missingLocalFiles = missingLocal
                    missingRemoteFiles = missingRemote

                    state =
                        if (missingLocal.isEmpty() && missingRemote.isEmpty()) {
                            State.EMPTY
                        } else {
                            State.AVAILABLE
                        }
                } catch (e: Exception) {
                    state = State.UNAVAILABLE
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
        try {
            getManyWs(context, workUUID, source, missingLocalFiles)
        } catch (e: Exception) {
            println(e)
        }
        try {
            setManyWs(context, workUUID, source, missingRemoteFiles)
        } catch (e: Exception) {
            println(e)
        }

        WorkManager.getInstance(context).getWorkInfosByTagLiveData(workUUID).observeForever { workInfos ->
            val completed = workInfos.count { it.state.isFinished }
            val total = workInfos.size

            filesSynced.value = Pair(completed, total)
        }
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { isExpanded = !isExpanded },
    ) {
        if (state == State.LOADING) {
            Text("loading...", modifier = Modifier.padding(all = 8.dp))
            return@Card
        } else if (state == State.UNAVAILABLE) {
            Text("source not available", modifier = Modifier.padding(all = 8.dp), color = Color.Red)
            return@Card
        } else if (state == State.EMPTY) {
            Text("all files synced", modifier = Modifier.padding(all = 8.dp))
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
                Text(text = source.name, fontWeight = FontWeight.Bold)

                ToSync(missingLocalFiles.size, missingRemoteFiles.size)

                if (filesSynced.value != null) {
                    val (completed, total) = filesSynced.value!!

                    if (completed == total) {
                        Text("all files synced")
                    } else {
                        Text("synced $completed of $total files")
                    }
                } else {
                    Button(
                        onClick = ::onSyncAllClick,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sync_rounded_48),
                            contentDescription = "sync both ways",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            if (isExpanded) {
                PathsList(missingLocalFiles, androidx.compose.ui.graphics.Color.Green)
                PathsList(missingRemoteFiles, androidx.compose.ui.graphics.Color.Yellow)
            }
        }
    }
}

@Composable
fun PathsList(
    paths: Array<String>,
    color: androidx.compose.ui.graphics.Color,
) {
    Column {
        paths.forEach { item ->
            Text(text = item, modifier = Modifier.padding(all = 4.dp), color = color)
        }
    }
}

@Composable
fun ToSync(
    down: Int,
    up: Int,
) {
    if (down == 0 && up == 0) {
        return
    }

    Row {
        if (down > 0) {
            Text(text = "$down")
            Icon(
                painter = painterResource(R.drawable.arrow_downward_rounded_48),
                contentDescription = "$down to download",
                modifier = Modifier.size(24.dp),
            )
        }

        if (up > 0) {
            Text(text = "$up")
            Icon(
                painter = painterResource(R.drawable.arrow_upward_rounded_48),
                contentDescription = "$up to upload",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
