package com.example.anysync.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.anysync.data.Source
import com.example.anysync.data.url
import com.example.anysync.workers.SetWsWorker.Companion.ProgressStep.Companion.toInt
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
        enum class ProgressStep(val stepNumber: Int) {
            STARTED(0), // Worker has been started
            PARSED(1), // Data has been parsed and validated
            SENT(2), // File has been sent
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
            OneTimeWorkRequestBuilder<SetWsWorker>()
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

        progress(ProgressStep.PARSED)

        val inFile = File(source.path, path)

        try {
            sendFile(source, path, inFile)
        } catch (e: Exception) {
            println(e)
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
