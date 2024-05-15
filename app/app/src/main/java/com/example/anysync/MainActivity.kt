package com.example.anysync

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

        val vm by viewModels<MainViewModel>()
        vm.init(applicationContext)

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
fun Main(modifier: Modifier = Modifier, vm: MainViewModel = viewModel()) {
    val sources by vm.sources.collectAsStateWithLifecycle(emptyList())

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var editingSource by remember {
        mutableStateOf<com.example.anysync.data.Source?>(null)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingSource = null
                showBottomSheet = true
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "add source")
            }
        },
        modifier = modifier.padding(8.dp),
    ) { contentPadding ->
        Column(
            modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sources.forEach { source ->
                Card {
                    Source(source = source) {
                        editingSource = source
                        showBottomSheet = true
                    }
                }
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
                Box(
                    modifier =
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                ) {
                    EditSource(
                        source = editingSource,
                    ) {
                        if (editingSource != null) {
                            vm.updateSource(it)
                        } else {
                            vm.addSource(it)
                        }

                        editingSource = null
                        showBottomSheet = false
                    }
                }
            }
        }
    }
}
