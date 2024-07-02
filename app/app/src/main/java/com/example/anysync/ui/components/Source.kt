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
import com.example.anysync.utils.Diff
import com.example.anysync.utils.diff
import com.example.anysync.utils.getManyWs
import com.example.anysync.utils.setManyWs
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

    val diff = remember {
        mutableStateOf(
            Diff(
                emptyArray(),
                emptyArray(),
                emptyArray(),
                emptyArray(),
                emptyArray(),
                emptyArray()
            )
        )
    }

    fun refreshMissingFiles() {
        thread {
            runBlocking {
                state = State.LOADING

                try {
                    val res = diff(source)
                    diff.value = res

                    state = State.AVAILABLE
                } catch (e: Exception) {
                    state = State.UNAVAILABLE
                    print(e.stackTraceToString())
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Scheduler(source)

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

                SourceSyncOptions(source, diff)
            }

            if (isExpanded) {
                PathsList(diff.value.modifiedServer, Color.Green)
                PathsList(diff.value.removedServer, Color.Red)
                PathsList(diff.value.newServer, Color.Blue)
                PathsList(diff.value.modifiedClient, Color.Green)
                PathsList(diff.value.removedClient, Color.Red)
                PathsList(diff.value.newClient, Color.Blue)
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
    diff: MutableState<Diff>,
) {
    val context = LocalContext.current

    fun onSyncLocalClick() {
        getManyWs(context, source, diff.value.modifiedServer).observeForever {
            diff.value = diff.value.copy(modifiedServer = diff.value.modifiedServer)
        }
        getManyWs(context, source, diff.value.removedServer).observeForever {
            diff.value = diff.value.copy(removedServer = diff.value.removedServer)
        }
        getManyWs(context, source, diff.value.newServer).observeForever {
            diff.value = diff.value.copy(newServer = diff.value.newServer)
        }
    }

    fun onSyncRemoteClick() {
        setManyWs(context, source, diff.value.modifiedClient).observeForever {
            diff.value = diff.value.copy(modifiedClient = diff.value.modifiedClient)
        }
        setManyWs(context, source, diff.value.removedClient).observeForever {
            diff.value = diff.value.copy(removedClient = diff.value.removedClient)
        }
        setManyWs(context, source, diff.value.newClient).observeForever {
            diff.value = diff.value.copy(newClient = diff.value.newClient)
        }
    }

    fun onSyncBothClick() {
        onSyncLocalClick()
        onSyncRemoteClick()
    }

    fun toSyncLocalCount(): Int =
        diff.value.modifiedServer.size + diff.value.newServer.size + diff.value.removedServer.size

    fun toSyncRemoteCount(): Int =
        diff.value.modifiedClient.size + diff.value.newClient.size + diff.value.removedClient.size

    fun toSyncBothCount(): Int = toSyncLocalCount() + toSyncRemoteCount()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (source.actions) {
            Actions.GET -> SyncButton(
                Actions.GET,
                toSyncLocalCount(),
                onClick = ::onSyncLocalClick
            )

            Actions.SET -> SyncButton(
                Actions.SET,
                toSyncRemoteCount(),
                onClick = ::onSyncRemoteClick
            )

            Actions.GET_SET -> {
                SyncButton(
                    Actions.GET,
                    toSyncLocalCount(),
                    onClick = ::onSyncLocalClick
                )
                SyncButton(
                    Actions.SET,
                    toSyncRemoteCount(),
                    onClick = ::onSyncRemoteClick
                )
                SyncButton(
                    Actions.GET_SET,
                    toSyncBothCount(),
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
