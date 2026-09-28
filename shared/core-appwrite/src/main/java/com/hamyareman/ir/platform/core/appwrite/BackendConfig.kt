package com.hamyareman.ir.platform.core.appwrite

/**
 * فقط یک پرچم «آیا بک‌اند پیکربندی شده» که در ریپازیتوری‌ها استفاده می‌شود.
 *
 * چرا interface جدا:
 *  - ریپازیتوری‌ها نباید به کل کلاینت Appwrite وابسته باشند تا در تست‌های واحد
 *    بدون Robolectric قابل استفاده باشند.
 *  - [AppwriteClientProvider] خودش اینترفیس را پیاده‌سازی می‌کند.
 */
interface BackendConfig {
    val isConfigured: Boolean
}
