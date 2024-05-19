package com.example.anysync.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.anysync.R
import com.example.anysync.data.Actions
import com.example.anysync.getManyWs
import com.example.anysync.missing
import com.example.anysync.missingFiles
import com.example.anysync.setManyWs
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

enum class State {
    LOADING,
    AVAILABLE,
    UNAVAILABLE,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Source(
    source: com.example.anysync.data.Source,
    onLongClick: () -> Unit = {},
) {
    var state by remember { mutableStateOf(State.LOADING) }

    val missingLocalFiles = remember { mutableStateOf(emptyArray<String>()) }
    val missingRemoteFiles = remember { mutableStateOf(emptyArray<String>()) }

    fun refreshMissingFiles() {
        thread {
            runBlocking {
                state = State.LOADING

                try {
                    val (missingLocal, missingRemote) = missingFiles(source)
                    missingLocalFiles.value = missingLocal
                    missingRemoteFiles.value = missingRemote

                    state = State.AVAILABLE
                } catch (e: Exception) {
                    state = State.UNAVAILABLE
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshMissingFiles()
    }

    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onDoubleClick = ::refreshMissingFiles) {
                isExpanded = !isExpanded
            }
            .padding(8.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = source.label, fontWeight = FontWeight.Bold)

                if (state == State.LOADING) {
                    CircularProgressIndicator(strokeCap = StrokeCap.Round)
                    return
                } else if (state == State.UNAVAILABLE) {
                    Text(
                        "source not available",
                        modifier = Modifier.padding(all = 8.dp),
                        color = Color.Red
                    )
                    return
                }

                SourceSyncOptions(source, missingLocalFiles, missingRemoteFiles)
            }

            if (isExpanded) {
                PathsList(missingLocalFiles.value, Color.Green)
                PathsList(missingRemoteFiles.value, Color.Yellow)
            }
        }
    }
}

@Composable
fun PathsList(
    paths: Array<String>,
    color: Color,
) {
    Column {
        paths.forEach { item ->
            Text(text = item, modifier = Modifier.padding(all = 4.dp), color = color)
        }
    }
}

@Composable
fun SourceSyncOptions(
    source: com.example.anysync.data.Source,
    missingLocalPaths: MutableState<Array<String>>,
    missingRemotePaths: MutableState<Array<String>>,
) {
    val context = LocalContext.current

    fun onSyncLocalClick() {
        getManyWs(context, source, missingLocalPaths.value).observeForever {
            missingLocalPaths.value = missing(it, missingLocalPaths.value)
        }
    }

    fun onSyncRemoteClick() {
        setManyWs(context, source, missingRemotePaths.value).observeForever {
            missingRemotePaths.value = missing(it, missingRemotePaths.value)
        }
    }

    fun onSyncBothClick() {
        onSyncLocalClick()
        onSyncRemoteClick()
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (source.actions) {
            Actions.GET -> SyncButton(
                Actions.GET,
                missingLocalPaths.value.size,
                onClick = ::onSyncLocalClick
            )

            Actions.SET -> SyncButton(
                Actions.SET,
                missingRemotePaths.value.size,
                onClick = ::onSyncRemoteClick
            )

            Actions.GET_SET -> {
                SyncButton(
                    Actions.GET,
                    missingLocalPaths.value.size,
                    onClick = ::onSyncLocalClick
                )
                SyncButton(
                    Actions.SET,
                    missingRemotePaths.value.size,
                    onClick = ::onSyncRemoteClick
                )
                SyncButton(
                    Actions.GET_SET,
                    missingLocalPaths.value.size + missingRemotePaths.value.size,
                    onClick = ::onSyncBothClick
                )
            }

            else -> {}
        }
    }
}

@Composable
fun SyncButton(actions: Actions, toSync: Int, onClick: () -> Unit = {}) {
    if (actions == Actions.NONE) return

    IconLabelButton(
        label = "$toSync", onClick = onClick, painter = when (actions) {
            Actions.GET -> painterResource(R.drawable.arrow_downward_rounded_48)
            Actions.SET -> painterResource(R.drawable.arrow_upward_rounded_48)
            Actions.GET_SET -> painterResource(R.drawable.sync_alt_rotated_rounded_48)
            else -> throw IllegalArgumentException("invalid action")
        }, enabled = toSync > 0
    )
}
