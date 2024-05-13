package com.example.anysync.data

data class Source(
    val name: String,
    val path: String,
    val host: String,
)

fun Source.url() = "$host/$name"
