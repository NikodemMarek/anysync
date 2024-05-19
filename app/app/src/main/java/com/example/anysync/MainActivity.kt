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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anysync.data.Actions
import com.example.anysync.ui.components.EditSource
import com.example.anysync.ui.components.IconLabelButton
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


enum class EditMode {
    NONE,
    ADD,
    EDIT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(modifier: Modifier = Modifier, vm: MainViewModel = viewModel()) {
    val sources by vm.sources.collectAsStateWithLifecycle(emptyList())

    val sheetState = rememberModalBottomSheetState()

    var editMode by remember { mutableStateOf(EditMode.NONE) }
    val editingSource = remember {
        mutableStateOf(
            com.example.anysync.data.Source(
                "",
                "",
                "",
                "",
                Actions.NONE
            )
        )
    }

    fun resetEdit() {
        editingSource.value = com.example.anysync.data.Source(
            "",
            "",
            "",
            "",
            Actions.NONE
        )
        editMode = EditMode.NONE
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingSource.value = com.example.anysync.data.Source(
                    "",
                    "",
                    "",
                    "",
                    Actions.NONE
                )
                editMode = EditMode.ADD
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
            sources.forEach {
                Card {
                    Source(it) {
                        editingSource.value = it
                        editMode = EditMode.EDIT
                    }
                }
            }
        }

        if (editMode != EditMode.NONE) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = ::resetEdit,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier =
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                ) {
                    EditSource(source = editingSource)

                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (editMode == EditMode.EDIT) {
                            IconLabelButton(
                                label = "delete",
                                onClick = {
                                    vm.removeSource(editingSource.value)
                                    resetEdit()
                                },
                                painter = rememberVectorPainter(Icons.Rounded.Delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        IconLabelButton(
                            label = "confirm",
                            onClick = {
                                when (editMode) {
                                    EditMode.NONE -> {}
                                    EditMode.ADD -> vm.addSource(editingSource.value)
                                    EditMode.EDIT -> vm.updateSource(editingSource.value)
                                }

                                resetEdit()
                            },
                            painter = rememberVectorPainter(Icons.Rounded.Check),
                            enabled = editingSource.value.name.isNotEmpty()
                                    && editingSource.value.host.isNotEmpty()
                                    && editingSource.value.path.isNotEmpty(),
                        )
                    }
                }
            }
        }
    }
}