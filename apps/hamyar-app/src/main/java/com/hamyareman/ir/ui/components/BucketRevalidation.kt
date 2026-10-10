package com.hamyareman.ir.ui.components

import coil.intercept.Interceptor
import coil.request.ImageResult
import com.hamyareman.ir.ui.study.HmkWebViewClient

/**
 * تصاویر باکت (jpg/png/webp) با `Cache-Control: max-age=0` درخواست می‌شوند: Coil نسخهٔ کش‌شده را
 * نگه می‌دارد ولی هر بار با `If-None-Match`/`If-Modified-Since` از سرور می‌پرسد.
 * ۳۰۴ یعنی همان تصویر (بدون دانلود)، ۲۰۰ یعنی فایل تازه‌ای در همان آدرس آپلود شده.
 * تصاویر غیر باکت (مثلاً فایل‌های محلی) دست‌نخورده می‌مانند.
 */
internal object BucketRevalidateInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        if (request.data.toString().startsWith(HmkWebViewClient.BUCKET_BASE)) {
            return chain.proceed(request.newBuilder().setHeader("Cache-Control", "max-age=0").build())
        }
        return chain.proceed(request)
    }
}
