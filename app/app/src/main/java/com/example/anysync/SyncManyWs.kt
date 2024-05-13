package com.example.anysync

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.WorkManager

fun getManyWs(
    context: Context,
    uuid: String,
    source: com.example.anysync.data.Source,
    paths: Array<String>,
) {
    val workChunkSize = 10

    val workChunks =
        paths.map {
            com.example.anysync.workers.GetWsWorker.create(source, it, uuid)
        }.chunked(workChunkSize)
    if (workChunks.isEmpty()) {
        throw Exception("no missing files")
    }

    val wm = WorkManager.getInstance(context)
    var workQueue = wm.beginWith(workChunks.first())
    for (workChunk in workChunks.drop(1)) {
        workQueue = workQueue.then(workChunk)
    }
    workQueue.enqueue()
}

fun setManyWs(
    context: Context,
    uuid: String,
    source: com.example.anysync.data.Source,
    paths: Array<String>,
) {
    val workChunkSize = 10

    val workChunks =
        paths.map {
            com.example.anysync.workers.SetWsWorker.create(source, it, uuid)
        }.chunked(workChunkSize)
    if (workChunks.isEmpty()) {
        throw Exception("no missing files")
    }

    val wm = WorkManager.getInstance(context)
    var workQueue = wm.beginWith(workChunks.first())
    for (workChunk in workChunks.drop(1)) {
        workQueue = workQueue.then(workChunk)
    }
    workQueue.enqueue()
}
