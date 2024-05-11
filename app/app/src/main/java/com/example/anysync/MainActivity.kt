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
import com.example.anysync.ui.theme.AnysyncTheme
import kotlinx.coroutines.runBlocking
import java.util.UUID
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

    val filesDownloaded = remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun onSyncAllClick() {
        val workUUID = UUID.randomUUID().toString()
        getManyWs(context, workUUID, missingFiles).observeForever {
            filesDownloaded.value = it
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (filesDownloaded.value != null) {
            val (completed, total) = filesDownloaded.value!!

            if (completed == total) {
                Text("All files downloaded")
            } else {
                Text("Downloaded $completed of $total files")
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
