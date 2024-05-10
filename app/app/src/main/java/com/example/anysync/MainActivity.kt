package com.example.anysync

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import com.example.anysync.ui.theme.AnysyncTheme
import com.example.anysync.workers.GetWsWorker.Companion.PROGRESS_STEP
import com.example.anysync.workers.GetWsWorker.Companion.ProgressStep
import com.example.anysync.workers.GetWsWorker.Companion.ProgressStep.Companion.toInt
import com.example.anysync.workers.GetWsWorker.Companion.createGetWsWorker
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
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
    FilesList(modifier = modifier)
}

@Composable
fun FilesList(modifier: Modifier = Modifier) {
    var files by remember { mutableStateOf<Array<String>>(arrayOf<String>()) }
    LaunchedEffect(Unit) {
        thread {
            runBlocking {
                files = getFiles()
            }
        }
    }

    Column(modifier = modifier) {
        for (file in files) {
            FileItem(path = file, modifier = Modifier.padding(8.dp))
        }
    }
}

// write a compose function that displays a file with path and a button to download it
@Composable
fun FileItem(
    path: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val progressStep = remember { mutableStateOf<ProgressStep?>(null) }

    fun onDownloadClick() {
        val getWsWork = createGetWsWorker(path)
        val wm = WorkManager.getInstance(context)
        wm.enqueue(getWsWork)
        wm.getWorkInfoByIdLiveData(getWsWork.id).observeForever {
            if (it != null) {
                progressStep.value =
                    if (it.state.isFinished) {
                        ProgressStep.COMPLETED
                    } else {
                        ProgressStep.fromInt(it.progress.getInt(PROGRESS_STEP, ProgressStep.STARTED.toInt()))
                    }
            }
        }
    }

    Row(modifier = modifier) {
        Text(text = path)
        if (progressStep.value != null) {
            when (progressStep.value!!) {
                ProgressStep.STARTED -> Text("Started")
                ProgressStep.PARSED -> Text("Parsed")
                ProgressStep.DOWNLOADED -> Text("Downloaded")
                ProgressStep.MOVED -> Text("Moved")
                ProgressStep.CLEANED -> Text("Cleaned")
                ProgressStep.COMPLETED -> Text("Completed")
            }
        } else {
            Button(onClick = ::onDownloadClick) {
                Text("Download")
            }
        }
    }
}

suspend fun getFiles(): Array<String> {
    val client = OkHttpClient()
    val request =
        Request.Builder()
            .url("http://192.168.68.132:5060/paths")
            .build()

    val gson = Gson()

    val response = client.newCall(request).execute()
    val body = response.body?.string() ?: "[]"
    return gson.fromJson(body, Array<String>::class.java)
}
