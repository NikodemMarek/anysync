package com.example.anysync.utils

import com.example.anysync.data.Source
import com.example.anysync.data.url
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.util.Stack

suspend fun getRemotePaths(sourceUri: String): Array<String> {
    return HttpClient(CIO) { install(ContentNegotiation) { json() } }.get("http://$sourceUri/paths")
        .body<Array<String>>()
}

data class Diff(
    val modifiedServer: Array<String>,
    val removedServer: Array<String>,
    val newServer: Array<String>,
    val modifiedClient: Array<String>,
    val removedClient: Array<String>,
    val newClient: Array<String>
)

data class Pth(val path: String, val modified: Long)

fun getLocalPaths(path: String): Array<Pth> {
    val files = mutableListOf<Pth>()
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
                files.add(
                    Pth(
                        it.absolutePath.substring(cutLen),
                        it.lastModified()
                    )
                )
            }
        }
    }

    return files.toTypedArray()
}

inline fun <reified T> missing(
    has: Array<T>,
    wants: Array<T>,
): Array<T> = wants.filter { it !in has }.toTypedArray()

suspend fun diff(source: Source): Diff {
    val sourceUri = source.url()
    val local = getLocalPaths(source.path)

    val body = HttpRequestBuilder().apply {
        url("http://$sourceUri/diff")
        contentType(ContentType.Application.Json)
        setBody("[${
            local.joinToString(",") { "{\"path\":\"${it.path}\",\"modified\":${it.modified}}" }
        }]")
    }

    val res = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                contentType = ContentType.Application.Json
            )
        }
    }.post(body).bodyAsText()

    // Fucking around with kotlin serialization would take longer
    val value =
        """^.+modified_server.+\[(?<modifiedServer>.*)].+removed_server.+\[(?<removedServer>.*)].+new_server.+\[(?<newServer>.*)].+modified_client.+\[(?<modifiedClient>.*)].+removed_client.+\[(?<removedClient>.*)].+new_client.+\[(?<newClient>.*)].*$""".toRegex(
            RegexOption.DOT_MATCHES_ALL
        ).matchEntire(res.replace("\n", " "))?.groups!!
    val modifiedServer =
        value["modifiedServer"]?.value?.split(",")?.map { it.trim() } ?: emptyList()
    val removedServer = value["removedServer"]?.value?.split(",")?.map { it.trim() } ?: emptyList()
    val newServer = value["newServer"]?.value?.split(",")?.map { it.trim() } ?: emptyList()
    val modifiedClient =
        value["modifiedClient"]?.value?.split(",")?.map { it.trim() } ?: emptyList()
    val removedClient = value["removedClient"]?.value?.split(",")?.map { it.trim() } ?: emptyList()
    val newClient = value["newClient"]?.value?.split(",")?.map { it.trim() } ?: emptyList()

    return Diff(
        modifiedServer.toTypedArray(),
        removedServer.toTypedArray(),
        newServer.toTypedArray(),
        modifiedClient.toTypedArray(),
        removedClient.toTypedArray(),
        newClient.toTypedArray()
    )
}