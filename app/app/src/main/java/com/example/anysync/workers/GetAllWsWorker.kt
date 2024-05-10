package com.example.anysync.workers

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.anysync.missingFiles
import com.example.anysync.workers.GetAllWsWorker.Companion.ProgressStep.Companion.toInt
import com.example.anysync.workers.GetWsWorker.Companion.createGetWsWorkerRequestBuilder

class GetAllWsWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        enum class ProgressStep(val stepNumber: Int) {
            STARTED(0), // Worker has been started
            COMPLETED(1), // Work has been completed
            ;

            companion object {
                fun fromInt(stepNumber: Int): ProgressStep = entries.first { it.stepNumber == stepNumber }

                fun ProgressStep.toInt(): Int = stepNumber
            }
        }

        const val PROGRESS_STEP = "progress_step"

        fun create(): WorkRequest =
            OneTimeWorkRequestBuilder<GetAllWsWorker>()
                .build()
    }

    private suspend fun progress(step: ProgressStep) {
        setProgress(workDataOf(PROGRESS_STEP to step.toInt()))
    }

    override suspend fun doWork(): Result {
        progress(ProgressStep.STARTED)

        val missingPaths =
            missingFiles(Environment.getExternalStorageDirectory().absolutePath + "/tmp", "http://192.168.68.132:5060")

        val workChunks = missingPaths.map { createGetWsWorkerRequestBuilder(it) }.chunked(10)
        var workQueue = WorkManager.getInstance(context).beginWith(workChunks.first())
        for (workChunk in workChunks.drop(1)) {
            workQueue = workQueue.then(workChunk)
        }
        workQueue.enqueue()

        progress(ProgressStep.COMPLETED)

        return Result.success()
    }
}
