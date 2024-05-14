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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.anysync.ui.components.EditSource
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(modifier: Modifier = Modifier) {
    var sources by remember {
        mutableStateOf(
            listOf<com.example.anysync.data.Source>(
                com.example.anysync.data.Source(
                    "test",
                    Environment.getExternalStorageDirectory().absolutePath + "/tmp",
                    "192.168.68.132:5060",
                ),
                com.example.anysync.data.Source(
                    "testcp",
                    Environment.getExternalStorageDirectory().absolutePath + "/tmp",
                    "192.168.68.132:5060",
                ),
            ),
        )
    }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var editingSource by remember {
        mutableStateOf<com.example.anysync.data.Source?>(
            null,
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "add source")
            }
        },
        modifier = modifier.padding(8.dp),
    ) { contentPadding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (source in sources) {
                Source(source = source)
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = {
                    editingSource = null
                    showBottomSheet = false
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                EditSource(source = editingSource) {
                    sources += it
                    editingSource = null

                    showBottomSheet = false
                }
            }
        }
    }
}

