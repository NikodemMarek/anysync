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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.anysync.ui.components.Source
import com.example.anysync.ui.theme.AnysyncTheme
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

    val filesDownloaded = remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Source(
            title = "Source",
            path = Environment.getExternalStorageDirectory().absolutePath + "/tmp",
            host = "http://192.168.68.132:5060",
        )
    }
}
