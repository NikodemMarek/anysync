package com.example.anysync.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.work.WorkManager
import com.example.anysync.R
import com.example.anysync.data.Source
import com.example.anysync.workers.ScheduledSync
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Scheduler(source: Source) {
    val context = LocalContext.current

    val isScheduled by ScheduledSync.isScheduled(context, source).collectAsState(initial = false)

    val wm = WorkManager.getInstance(context)

    var openAlertDialog by remember { mutableStateOf(false) }
    IconButton(onClick = { openAlertDialog = true }) {
        Icon(
            painter = painterResource(R.drawable.schedule_rounded_48),
            contentDescription = "schedule"
        )
    }

    if (openAlertDialog) {
        var selected by remember { mutableIntStateOf(0) }

        var interval by remember { mutableStateOf("1") }
        var unit by remember { mutableStateOf(TimeUnit.MINUTES) }

        val timePickerState = rememberTimePickerState()

        AlertDialog(
            title = {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SegmentedButton(
                        selected = selected == 0,
                        onClick = { selected = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(text = "interval")
                    }
                    SegmentedButton(
                        selected = selected == 1,
                        onClick = { selected = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(text = "daily")
                    }
                }
            },
            text = {
                when (selected) {
                    0 -> Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = interval,
                            onValueChange = { interval = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                IconButton(onClick = {
                                    unit = when (unit) {
                                        TimeUnit.MINUTES -> TimeUnit.HOURS
                                        TimeUnit.HOURS -> TimeUnit.DAYS
                                        TimeUnit.DAYS -> TimeUnit.MINUTES
                                        else -> TimeUnit.MINUTES
                                    }
                                }) {
                                    Text(
                                        when (unit) {
                                            TimeUnit.MINUTES -> "M"
                                            TimeUnit.HOURS -> "H"
                                            TimeUnit.DAYS -> "D"
                                            else -> throw IllegalStateException()
                                        }
                                    )
                                }
                            },
                        )
                    }

                    1 -> TimePicker(state = timePickerState, modifier = Modifier.fillMaxWidth())
                }
            },
            onDismissRequest = {
                openAlertDialog = false
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (isScheduled) {
                        FilledIconButton(
                            onClick = {
                                ScheduledSync.cancelScheduled(context, source)
                                openAlertDialog = false
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                painter = rememberVectorPainter(Icons.Rounded.Delete),
                                contentDescription = "remove",
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier)
                    }

                    IconLabelButton(
                        enabled = if (selected == 0) (interval.toIntOrNull() ?: 0) > 0 else true,
                        label = if (isScheduled) "reschedule" else "schedule",
                        painter = rememberVectorPainter(Icons.Rounded.Done),
                        onClick = {
                            val work =
                                when (selected) {
                                    0 -> ScheduledSync.scheduleInterval(
                                        context = context,
                                        source = source,
                                        interval = 1,
                                        unit = TimeUnit.MINUTES,
                                    )

                                    1 -> ScheduledSync.scheduleDaily(
                                        context = context,
                                        source = source,
                                        hour = timePickerState.hour,
                                        minute = timePickerState.minute
                                    )

                                    else -> throw IllegalStateException()
                                }

                            wm.enqueue(work)
                            openAlertDialog = false
                        })
                }
            },
        )
    }
}
