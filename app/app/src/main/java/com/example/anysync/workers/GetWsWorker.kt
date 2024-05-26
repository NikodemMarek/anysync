package com.example.anysync.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.anysync.data.Actions
import com.example.anysync.data.Source
import com.example.anysync.data.url
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.Frame.Binary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder

class GetWsWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    companion object {
        enum class ProgressStep {
            STARTED, // Worker has been started
            PARSED, // Data has been parsed and validated
            DOWNLOADED, // File has been downloaded
            MOVED, // File has been moved to external storage
            CLEANED, // Cleanup has been done
            COMPLETED, // Work has been completed
            ;

            companion object {
                fun fromInt(value: Int) = entries.firstOrNull { it.ordinal == value }
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
                        "source-actions" to source.actions.ordinal,
                        "path" to path,
                    ),
                )
                .addTag(uuid)
                .build()
    }

    private suspend fun progress(step: ProgressStep) {
        setProgress(workDataOf(PROGRESS_STEP to step.ordinal))
    }

    override suspend fun doWork(): Result {
        progress(ProgressStep.STARTED)

        val source =
            Source(
                inputData.getString("source-name")
                    ?: throw Exception("xxx: GetWsWorker: name is required"),
                "",
                inputData.getString("source-path")
                    ?: throw Exception("xxx: GetWsWorker: path is required"),
                inputData.getString("source-host")
                    ?: throw Exception("xxx: GetWsWorker: host is required"),
                inputData.getInt("source-actions", -1).let { Actions.fromInt(it) }
                    ?: throw Exception("xxx: GetWsWorker: actions is required"),
            )
        val path =
            inputData.getString("path") ?: throw Exception("xxx: GetWsWorker: path is required")

        if (source.actions != Actions.GET && source.actions != Actions.GET_SET) {
            throw Exception("xxx: GetWsWorker: actions do not allow GET")
        }

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
        val urlString = "ws://${source.url()}/get/${
            withContext(Dispatchers.IO) {
                URLEncoder.encode(path, "UTF-8")
            }
        }".replace("+", "%20")

        client.ws(urlString = urlString) {
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
