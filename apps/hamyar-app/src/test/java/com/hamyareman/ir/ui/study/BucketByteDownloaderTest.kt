package com.hamyareman.ir.ui.study

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class BucketByteDownloaderTest {
    @Test fun interruptedDownloadResumesWithIfRangeWithoutMixingVersions() {
        val expected = ByteArray(16_384) { (it % 251).toByte() }
        val requests = mutableListOf<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/file") { exchange ->
            val range = exchange.requestHeaders.getFirst("Range").orEmpty()
            requests += range
            exchange.responseHeaders.add("ETag", "\"v1\"")
            if (range.isBlank()) {
                exchange.sendResponseHeaders(200, expected.size.toLong())
                runCatching { exchange.responseBody.write(expected, 0, 512); exchange.responseBody.flush() }
                exchange.close()
            } else {
                assertEquals("\"v1\"", exchange.requestHeaders.getFirst("If-Range"))
                val start = range.removePrefix("bytes=").substringBefore('-').toInt()
                exchange.responseHeaders.add("Content-Range", "bytes $start-16383/16384")
                exchange.sendResponseHeaders(206, (expected.size - start).toLong())
                exchange.responseBody.use { it.write(expected, start, expected.size - start) }
            }
        }
        server.start()
        val directory = Files.createTempDirectory("bucket-resume").toFile()
        try {
            val target = directory.resolve("cache")
            val url = "http://127.0.0.1:" + server.address.port + "/file"
            val result = BucketByteDownloader.download(url, target, validate = { it.length() == expected.size.toLong() })
            assertArrayEquals(expected, target.readBytes())
            assertEquals(expected.size.toLong(), result.length)
            assertTrue(requests.any { it == "bytes=512-" })
            assertFalse(directory.resolve("cache.part").exists())
        } finally { server.stop(0); directory.deleteRecursively() }
    }

    @Test fun failedReplacementPreservesPreviousCacheAndClearsTransferUi() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/file") { it.sendResponseHeaders(503, -1); it.close() }
        server.start()
        val directory = Files.createTempDirectory("bucket-preserve").toFile()
        try {
            val target = directory.resolve("cache").apply { writeText("previous version") }
            try {
                BucketByteDownloader.download("http://127.0.0.1:" + server.address.port + "/file", target, validate = { true })
                fail("Expected download failure")
            } catch (_: IOException) { }
            assertEquals("previous version", target.readText())
            assertTrue(BucketTransfer.states.value.isEmpty())
        } finally { server.stop(0); directory.deleteRecursively() }
    }

    @Test fun invalidContentIsDownloadedOnceAndPreservesThePreviousCache() {
        var requests = 0
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/file") { exchange ->
            requests++
            val data = "unsupported envelope".toByteArray()
            exchange.sendResponseHeaders(200, data.size.toLong())
            exchange.responseBody.use { it.write(data) }
        }
        server.start()
        val directory = Files.createTempDirectory("bucket-invalid").toFile()
        try {
            val target = directory.resolve("cache").apply { writeText("previous") }
            try {
                BucketByteDownloader.download("http://127.0.0.1:" + server.address.port + "/file", target, silent = true, validate = { false })
                fail("Invalid content must fail")
            } catch (_: BucketByteDownloader.InvalidContentException) { }
            assertEquals(1, requests)
            assertEquals("previous", target.readText())
        } finally { server.stop(0); directory.deleteRecursively() }
    }
    @Test fun forbiddenResponseIsNotClassifiedAsMissing() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/file") { it.sendResponseHeaders(403, -1); it.close() }
        server.start()
        val directory = Files.createTempDirectory("bucket-forbidden").toFile()
        try {
            try {
                BucketByteDownloader.download("http://127.0.0.1:" + server.address.port + "/file", directory.resolve("cache"), validate = { true })
                fail("Expected access failure")
            } catch (error: IOException) { assertFalse(error is StudyPdfCache.RemoteMissingException) }
        } finally { server.stop(0); directory.deleteRecursively() }
    }
}
