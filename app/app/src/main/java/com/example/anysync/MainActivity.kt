package com.example.anysync

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.anysync.ui.components.Source
import com.example.anysync.ui.theme.AnysyncTheme

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
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Source(
            source =
                com.example.anysync.data.Source(
                    "test",
                    Environment.getExternalStorageDirectory().absolutePath + "/tmp",
                    "192.168.68.132:5060",
                ),
        )
        Source(
            source =
                com.example.anysync.data.Source(
                    "testcp",
                    Environment.getExternalStorageDirectory().absolutePath + "/tmp",
                    "192.168.68.132:5060",
                ),
        )
    }
}
