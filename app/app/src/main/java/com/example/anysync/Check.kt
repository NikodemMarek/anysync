package com.example.anysync

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Stack

suspend fun getRemoteFiles(host: String): Array<String> {
    val client = OkHttpClient()
    val request =
        Request.Builder()
            .url("$host/paths")
            .build()

    val gson = Gson()

    val response = client.newCall(request).execute()
    val body = response.body?.string() ?: "[]"
    return gson.fromJson(body, Array<String>::class.java)
}

fun getLocalFiles(path: String): Array<String> {
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

suspend fun missingFiles(
    path: String,
    host: String,
): Array<String> =
    missing(
        getLocalFiles(path),
        getRemoteFiles(host),
    )
