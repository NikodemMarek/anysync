package com.example.anysync.workers

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.io.File
import kotlin.Exception

class GetWsWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val PROGRESS_STEP = "progress_step"

        fun createGetTrackWorker(file: com.example.anysync.data.File): WorkRequest =
            OneTimeWorkRequestBuilder<GetWsWorker>()
                .setInputData(
                    workDataOf(
                        "path" to file.path,
                        "mimeType" to file.mimeType,
                    ),
                )
                .build()
    }

    override suspend fun doWork(): Result {
        setProgress(workDataOf(PROGRESS_STEP to 0)) // Work has started

        val path = inputData.getString("path") ?: throw Exception("xxx: GetWsWorker: path is required")
        val mimeType = inputData.getString("mimeType")

        val fileName = path.split("/").last()

        setProgress(workDataOf(PROGRESS_STEP to 1)) // Track data has been parsed and validated

        val tmpFile = File.createTempFile("tomove", "*/*", context.cacheDir)
        downloadFile("", tmpFile)

        setProgress(workDataOf(PROGRESS_STEP to 2)) // Track has been downloaded

        return Result.success()

        val resolver = context.contentResolver

        val newTrackData =
            ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType ?: "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/" + path)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

        val uri = resolver.insert(getColletion(), newTrackData)
        if (uri == null) {
            tmpFile.delete()
            throw Exception("xxx: could not insert into MediaStore")
        }

        setProgress(workDataOf(PROGRESS_STEP to 3)) // Track data has been added to MediaStore

        val mediaFileStream = resolver.openOutputStream(uri)
        if (mediaFileStream == null) {
            tmpFile.delete()
            newTrackData.clear()
            resolver.delete(uri, null, null)
            throw Exception("xxx: GetTrackWorker: could not open output stream")
        }

        tmpFile.inputStream().use { inputStream ->
            inputStream.copyTo(mediaFileStream)
        }

        setProgress(workDataOf(PROGRESS_STEP to 4)) // Track has been moved to MediaStore

        tmpFile.delete()

        newTrackData.clear()
        newTrackData.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, newTrackData, null, null)

        setProgress(workDataOf(PROGRESS_STEP to 5)) // Cleanup has been done

        return Result.success()
    }

    fun downloadFile(
        uri: String,
        file: File,
    ) {
        val client =
            HttpClient {
                install(WebSockets)
            }
        runBlocking {
            client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 5060, path = "/get/$uri") {
                while (true) {
                    val othersMessage = incoming.receive() as? Frame.Text ?: continue
                    println(othersMessage.readText())
                    val myMessage = readlnOrNull()
                    if (myMessage != null) {
                        send(myMessage)
                    }
                }
            }
        }
        client.close()
        println("Connection closed. Goodbye!")
    }

    fun getColletion(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    }
}
