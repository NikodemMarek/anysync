package com.example.anysync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.anysync.ui.theme.AnysyncTheme
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            Text(
                text = file,
                modifier = modifier,
            )
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
    println(body)
    return gson.fromJson(body, Array<String>::class.java)
}
