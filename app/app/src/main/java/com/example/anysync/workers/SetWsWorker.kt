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
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame.Binary
import io.ktor.websocket.close
import java.io.File

class SetWsWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    companion object {
        enum class ProgressStep {
            STARTED, // Worker has been started
            PARSED, // Data has been parsed and validated
            SENT, // File has been sent
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
            OneTimeWorkRequestBuilder<SetWsWorker>()
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
        println(source)
        val path =
            inputData.getString("path") ?: throw Exception("xxx: GetWsWorker: path is required")

        if (source.actions != Actions.SET && source.actions != Actions.GET_SET) {
            throw Exception("xxx: SetWsWorker: actions do not allow SET")
        }

        progress(ProgressStep.PARSED)

        val inFile = File(source.path, path)

        try {
            sendFile(source, path, inFile)
        } catch (e: Exception) {
            throw Exception("could not send file")
        }

        progress(ProgressStep.SENT)

        progress(ProgressStep.COMPLETED)

        return Result.success()
    }

    private suspend fun sendFile(
        source: Source,
        path: String,
        inFile: File,
    ) {
        val client = HttpClient(CIO).config { install(WebSockets) }
        client.ws(urlString = "ws://${source.url()}/set/$path") {
            inFile.inputStream().buffered().use {
                while (true) {
                    val buffer = ByteArray(1024)
                    val bytesRead = it.read(buffer)
                    if (bytesRead <= 0) break

                    send(Binary(true, buffer.copyOf(bytesRead)))
                }
            }
            close(CloseReason(CloseReason.Codes.NORMAL, "EOF"))
        }
        client.close()
    }
}
