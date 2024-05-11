package com.example.anysync.workers

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.anysync.workers.GetWsWorker.Companion.ProgressStep.Companion.toInt
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*


import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.Frame.*
import kotlinx.coroutines.*
import java.io.File

class GetWsWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        enum class ProgressStep(val stepNumber: Int) {
            STARTED(0), // Worker has been started
            PARSED(1), // Data has been parsed and validated
            DOWNLOADED(2), // File has been downloaded
            MOVED(3), // File has been moved to external storage
            CLEANED(4), // Cleanup has been done
            COMPLETED(5), // Work has been completed
            ;

            companion object {
                fun fromInt(stepNumber: Int): ProgressStep = entries.first { it.stepNumber == stepNumber }

                fun ProgressStep.toInt(): Int = stepNumber
            }
        }

        const val PROGRESS_STEP = "progress_step"

        fun create(
            path: String,
            uuid: String,
        ): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<GetWsWorker>()
                .setInputData(workDataOf("path" to path))
                .addTag(uuid)
                .build()
    }

    private suspend fun progress(step: ProgressStep) {
        setProgress(workDataOf(PROGRESS_STEP to step.toInt()))
    }

    override suspend fun doWork(): Result {
        progress(ProgressStep.STARTED)

        val path = inputData.getString("path") ?: throw Exception("xxx: GetWsWorker: path is required")
        val fileName = path.split("/").last()

        progress(ProgressStep.PARSED)

        val tmpFile =
            withContext(Dispatchers.IO) {
                File.createTempFile("tomove", "tmp", context.cacheDir)
            }
        try {
            downloadFile(path, tmpFile)
        } catch (e: Exception) {
            throw Exception("could not download file")
        }

        progress(ProgressStep.DOWNLOADED)

        val rootDir = Environment.getExternalStorageDirectory().toString() + "/tmp"
        val relativePath = path.split("/").dropLast(1).joinToString("/") + "/" + fileName
        val outFile = File("$rootDir/$relativePath")

        try {
            outFile.parentFile?.mkdirs()
        } catch (e: Exception) {
            throw Exception("could not create parent directories")
        }
        try {
            withContext(Dispatchers.IO) {
                outFile.createNewFile()
            }
        } catch (e: Exception) {
            throw Exception("could not create file")
        }

        outFile.outputStream().use { outputStream ->
            tmpFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        progress(ProgressStep.MOVED)

        tmpFile.delete()

        progress(ProgressStep.CLEANED)

        progress(ProgressStep.COMPLETED)

        return Result.success()
    }

    private suspend fun downloadFile(
        uri: String,
        file: File,
    ) {
        val client = HttpClient(CIO).config { install(WebSockets) }
        client.ws(host = "192.168.68.132", port = 5060, path = "get?path=$uri") {
            for (frame in incoming) {
                if (frame !is Binary) {
                    throw Exception("unexpected frame type")
                } else {
                    file.appendBytes(frame.data)
                }
            }
        }
        client.close()
    }
}
