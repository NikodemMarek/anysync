package com.example.anysync.workers

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.anysync.workers.GetWsWorker.Companion.ProgressStep.Companion.toInt
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.websocket.*
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

        fun createGetWsWorker(path: String): WorkRequest =
            OneTimeWorkRequestBuilder<GetWsWorker>()
                .setInputData(workDataOf("path" to path))
                .build()
    }

    override suspend fun doWork(): Result {
        setProgress(workDataOf(PROGRESS_STEP to ProgressStep.STARTED.toInt()))

        val path = inputData.getString("path") ?: throw Exception("xxx: GetWsWorker: path is required")
        val fileName = path.split("/").last()

        setProgress(workDataOf(PROGRESS_STEP to ProgressStep.PARSED.toInt()))

        val tmpFile = withContext(Dispatchers.IO) {
            File.createTempFile("tomove", "tmp", context.cacheDir)
        }
        try {
            downloadFile(path, tmpFile)
        } catch (e: Exception) {
            throw Exception("could not download file")
        }

        setProgress(workDataOf(PROGRESS_STEP to ProgressStep.DOWNLOADED.toInt()))

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

        setProgress(workDataOf(PROGRESS_STEP to ProgressStep.MOVED.toInt()))

        tmpFile.delete()

        setProgress(workDataOf(PROGRESS_STEP to ProgressStep.CLEANED.toInt()))

        setProgress(workDataOf(PROGRESS_STEP to ProgressStep.COMPLETED.toInt()))

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
