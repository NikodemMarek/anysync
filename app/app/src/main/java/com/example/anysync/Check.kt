package com.example.anysync

import com.example.anysync.data.Source
import com.example.anysync.data.url
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.util.Stack

suspend fun getRemotePaths(sourceUri: String): Array<String> {
    return HttpClient(CIO) { install(ContentNegotiation) { json() } }.get("http://$sourceUri/paths")
        .body<Array<String>>()
}

fun getLocalPaths(path: String): Array<String> {
    val files = mutableListOf<String>()
    val root = File(path)

    val stack = Stack<File>()
    stack.push(root)

    val cutLen = root.absolutePath.length + 1

    while (stack.isNotEmpty()) {
        val dir = stack.pop()

        dir.listFiles()?.forEach {
            if (it.isDirectory) {
                stack.push(it)
            } else {
                files.add(it.absolutePath.substring(cutLen))
            }
        }
    }

    return files.toTypedArray()
}

inline fun <reified T> missing(
    has: Array<T>,
    wants: Array<T>,
): Array<T> = wants.filter { it !in has }.toTypedArray()

suspend fun missingFiles(source: Source): Pair<Array<String>, Array<String>> {
    val local = getLocalPaths(source.path)
    val remote = getRemotePaths(source.url())

    val missingLocal = missing(local, remote)
    val missingRemote = missing(remote, local)

    return Pair(missingLocal, missingRemote)
}
