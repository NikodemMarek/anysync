package com.example.anysync.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.anysync.data.Actions
import com.example.anysync.data.Source
import com.example.anysync.utils.diff
import com.example.anysync.utils.getManyWs
import com.example.anysync.utils.setManyWs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class ScheduledSync(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    companion object {
        fun scheduleDaily(context: Context, source: Source, hour: Int, minute: Int): WorkRequest {
            cancelScheduled(context, source)

            val nowInDay = System.currentTimeMillis() % (24 * 60 * 60 * 1000)
            val at = (hour * 60 + minute) * 60 * 1000
            val offset = if (at > nowInDay) at - nowInDay else 24 * 60 * 60 * 1000 - nowInDay + at

            return PeriodicWorkRequestBuilder<ScheduledSync>(
                1, TimeUnit.DAYS,
            )
                .setInitialDelay(offset, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        "source-name" to source.name,
                        "source-path" to source.path,
                        "source-host" to source.host,
                        "source-actions" to source.actions.ordinal,
                    ),
                )
                .addTag("source-${source.id}-scheduled")
                .build()
        }

        fun scheduleInterval(
            context: Context,
            source: Source,
            interval: Long,
            unit: TimeUnit
        ): WorkRequest {
            cancelScheduled(context, source)

            return PeriodicWorkRequestBuilder<ScheduledSync>(
                interval, unit,
            )
                .setInputData(
                    workDataOf(
                        "source-name" to source.name,
                        "source-path" to source.path,
                        "source-host" to source.host,
                        "source-actions" to source.actions.ordinal,
                    ),
                )
                .addTag("source-${source.id}-scheduled")
                .build()
        }

        fun cancelScheduled(context: Context, source: Source) {
            WorkManager.getInstance(context).cancelAllWorkByTag("source-${source.id}-scheduled")
        }

        fun isScheduled(context: Context, source: Source): Flow<Boolean> =
            WorkManager.getInstance(context)
                .getWorkInfosByTagFlow("source-${source.id}-scheduled").map {
                    it.any { s -> s.state == androidx.work.WorkInfo.State.ENQUEUED }
                }
    }

    override suspend fun doWork(): Result {
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

        val (missingLocal, _, missingRemote) = diff(source)
        if ((source.actions == Actions.GET_SET || source.actions == Actions.GET) && missingLocal.isNotEmpty()) {
            getManyWs(context, source, missingLocal)
        }
        if ((source.actions == Actions.GET_SET || source.actions == Actions.SET) && missingRemote.isNotEmpty()) {
            setManyWs(context, source, missingRemote)
        }

        return Result.success()
    }
}