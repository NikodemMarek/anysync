package com.example.anysync.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.anysync.data.Source
import com.example.anysync.data.url
import com.example.anysync.workers.GetWsWorker.Companion.ProgressStep.Companion.toInt
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.Frame.Binary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GetWsWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
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
                fun fromInt(stepNumber: Int): ProgressStep =
                    entries.first { it.stepNumber == stepNumber }

                fun ProgressStep.toInt(): Int = stepNumber
            }
        }

        const val PROGRESS_STEP = "progress_step"

        fun create(
            source: Source,
            path: String,
            uuid: String,
        ): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<GetWsWorker>()
                .setInputData(
                    workDataOf(
                        "source-name" to source.name,
                        "source-path" to source.path,
                        "source-host" to source.host,
                        "path" to path,
                    ),
                )
                .addTag(uuid)
                .build()
    }

    private suspend fun progress(step: ProgressStep) {
        setProgress(workDataOf(PROGRESS_STEP to step.toInt()))
    }

    override suspend fun doWork(): Result {
        progress(ProgressStep.STARTED)

        val source =
            Source(
                inputData.getString("source-name")
                    ?: throw Exception("xxx: GetWsWorker: name is required"),
                inputData.getString("source-path")
                    ?: throw Exception("xxx: GetWsWorker: path is required"),
                inputData.getString("source-host")
                    ?: throw Exception("xxx: GetWsWorker: host is required"),
            )
        val path =
            inputData.getString("path") ?: throw Exception("xxx: GetWsWorker: path is required")

        val fileName = path.split("/").last()
        val relativePath = path.split("/").dropLast(1).joinToString("/") + "/" + fileName

        progress(ProgressStep.PARSED)

        val tmpFile =
            withContext(Dispatchers.IO) {
                File.createTempFile("tomove", "tmp", context.cacheDir)
            }
        try {
            downloadFile(source, path, tmpFile)
        } catch (e: Exception) {
            throw Exception("could not download file")
        }

        progress(ProgressStep.DOWNLOADED)

        val outFile = File("${source.path}/$relativePath")

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
        source: Source,
        path: String,
        outFile: File,
    ) {
        val client = HttpClient(CIO).config { install(WebSockets) }
        client.ws(urlString = "ws://${source.url()}/get/$path") {
            for (frame in incoming) {
                if (frame !is Binary) {
                    throw Exception("unexpected frame type")
                } else {
                    outFile.appendBytes(frame.data)
                }
            }
        }
        client.close()
    }
}
