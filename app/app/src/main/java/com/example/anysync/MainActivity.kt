package com.example.anysync

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import com.example.anysync.ui.theme.AnysyncTheme
import com.example.anysync.workers.GetAllWsWorker
import com.example.anysync.workers.GetAllWsWorker.Companion.ProgressStep.Companion.toInt
import com.example.anysync.workers.GetWsWorker.Companion.PROGRESS_STEP
import com.example.anysync.workers.GetWsWorker.Companion.ProgressStep
import com.example.anysync.workers.GetWsWorker.Companion.ProgressStep.Companion.toInt
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val getPermission = Intent()
                getPermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(getPermission)
            }
        }

        setContent {
            AnysyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Main()
                }
            }
        }
    }
}

@Composable
fun Main(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var missingFiles by remember { mutableStateOf<Array<String>>(arrayOf<String>()) }

    fun refreshMissingFiles() {
        thread {
            runBlocking {
                missingFiles =
                    missingFiles(
                        Environment.getExternalStorageDirectory().absolutePath + "/tmp",
                        "http://192.168.68.132:5060",
                    )
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshMissingFiles()
    }

    val progressStep = remember { mutableStateOf<GetAllWsWorker.Companion.ProgressStep?>(null) }

    fun onSyncAllClick() {
        val getAllWsWork = GetAllWsWorker.create()
        val wm = WorkManager.getInstance(context)
        wm.enqueue(getAllWsWork)
        wm.getWorkInfoByIdLiveData(getAllWsWork.id).observeForever {
            if (it != null) {
                progressStep.value =
                    if (it.state.isFinished) {
                        GetAllWsWorker.Companion.ProgressStep.COMPLETED
                    } else {
                        GetAllWsWorker.Companion.ProgressStep.fromInt(
                            it.progress.getInt(PROGRESS_STEP, GetAllWsWorker.Companion.ProgressStep.STARTED.toInt()),
                        )
                    }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (progressStep.value != null) {
            when (progressStep.value!!) {
                GetAllWsWorker.Companion.ProgressStep.STARTED -> Text("Started")
                GetAllWsWorker.Companion.ProgressStep.COMPLETED -> Text("Completed")
            }
        } else {
            Button(
                onClick = ::onSyncAllClick,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally),
            ) {
                Text("Sync All")
            }
        }

        Column {
            for (file in missingFiles) {
                Text(file, modifier = Modifier, color = Color.Green)
            }
        }
    }
}
