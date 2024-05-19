package com.example.anysync.utils

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.WorkManager

fun getManyWs(
    context: Context,
    source: com.example.anysync.data.Source,
    paths: Array<String>,
): LiveData<Array<String>> {
    val uuid = java.util.UUID.randomUUID().toString()
    val workChunkSize = 10

    val works =
        paths.map {
            com.example.anysync.workers.GetWsWorker.create(source, it, uuid)
        }
    val workIds = works.map { it.id }
    val workChunks = works.chunked(workChunkSize)

    val wm = WorkManager.getInstance(context)
    var workQueue = wm.beginWith(workChunks.first())
    for (workChunk in workChunks.drop(1)) {
        workQueue = workQueue.then(workChunk)
    }
    workQueue.enqueue()

    return wm.getWorkInfosByTagLiveData(uuid).map { workInfos ->
        val completedIds = workInfos.filter { it.state.isFinished }.map { it.id }
        paths.filterIndexed { index, _ -> completedIds.contains(workIds[index]) }.toTypedArray()
    }
}

fun setManyWs(
    context: Context,
    source: com.example.anysync.data.Source,
    paths: Array<String>,
): LiveData<Array<String>> {
    val uuid = java.util.UUID.randomUUID().toString()
    val workChunkSize = 10

    val works =
        paths.map {
            com.example.anysync.workers.SetWsWorker.create(source, it, uuid)
        }
    val workIds = works.map { it.id }
    val workChunks = works.chunked(workChunkSize)

    val wm = WorkManager.getInstance(context)
    var workQueue = wm.beginWith(workChunks.first())
    for (workChunk in workChunks.drop(1)) {
        workQueue = workQueue.then(workChunk)
    }
    workQueue.enqueue()

    return wm.getWorkInfosByTagLiveData(uuid).map { workInfos ->
        val completedIds = workInfos.filter { it.state.isFinished }.map { it.id }
        paths.filterIndexed { index, _ -> completedIds.contains(workIds[index]) }.toTypedArray()
    }
}
