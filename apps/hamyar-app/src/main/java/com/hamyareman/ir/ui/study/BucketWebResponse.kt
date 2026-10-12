package com.hamyareman.ir.ui.study

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URLConnection

/** Streaming cached media with seek support; no full MP3/PDF/image allocation in RAM. */
internal fun bucketRawResponse(file: File, path: String, range: String?): WebResourceResponse {
    val length = file.length()
    val mime = URLConnection.guessContentTypeFromName(path) ?: "application/octet-stream"
    val headers = mutableMapOf("Cache-Control" to "no-store", "Accept-Ranges" to "bytes", "Access-Control-Allow-Origin" to "*")
    val match = range?.let { Regex("^bytes=(\\d*)-(\\d*)$").matchEntire(it) }
    var start = 0L
    var end = length - 1
    if (match != null) {
        val first = match.groupValues[1].toLongOrNull()
        val last = match.groupValues[2].toLongOrNull()
        if (first != null) { start = first; end = minOf(last ?: end, end) }
        else if (last != null) start = (length - last).coerceAtLeast(0)
        if (start > end || start >= length) {
            headers["Content-Range"] = "bytes */$length"
            return WebResourceResponse(mime, null, 416, "Range Not Satisfiable", headers, ByteArrayInputStream(ByteArray(0)))
        }
        headers["Content-Range"] = "bytes $start-$end/$length"
    }
    val count = (end - start + 1).coerceAtLeast(0)
    headers["Content-Length"] = count.toString()
    val fileStream = FileInputStream(file).apply { channel.position(start) }
    val stream = object : InputStream() {
        var remaining = count
        override fun read(): Int { if (remaining <= 0) return -1; val n = fileStream.read(); if (n >= 0) remaining--; return n }
        override fun read(buffer: ByteArray, offset: Int, size: Int): Int {
            if (remaining <= 0) return -1
            val read = fileStream.read(buffer, offset, minOf(size.toLong(), remaining).toInt())
            if (read > 0) remaining -= read
            return read
        }
        override fun close() = fileStream.close()
    }
    return WebResourceResponse(mime, null, if (match != null) 206 else 200, if (match != null) "Partial Content" else "OK", headers, stream)
}
