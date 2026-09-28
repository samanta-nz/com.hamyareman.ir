package com.hamyareman.ir.platform.core.appwrite

import android.content.Context
import io.appwrite.Client
import io.appwrite.Permission
import io.appwrite.Role
import com.hamyareman.ir.platform.core.common.TableIds

/**
 * تنها نقطه‌ی ساخت کلاینت Appwrite برای هر دو اپ.
 *
 * اگر [projectId] خالی باشد (یعنی مقدار آن در local.properties گذاشته نشده) همه‌ی
 * سرویس‌ها «حالت محلی» می‌شوند: اپ کار می‌کند، چیزی به سرور نمی‌فرستد و نقش کاربر
 * از نقش پیش‌فرض هر اپ (در `AppwriteAuthService`) می‌آید. این همان رفتاری است که
 * در README وعده داده شده.
 */
class AppwriteClientProvider(
    context: Context,
    val endpoint: String,
    val projectId: String,
    val databaseId: String = TableIds.DATABASE,
) : BackendConfig {
    private val appContext = context.applicationContext

    override val isConfigured: Boolean get() = projectId.isNotBlank()

    val client: Client by lazy {
        Client(appContext)
            .setEndpoint(endpoint.ifBlank { "https://fra.cloud.appwrite.io/v1" })
            .setProject(projectId.ifBlank { "local-dev" })
    }

    companion object {
        /**
         * دسترسی‌های پیش‌فرض هر سطر: فقط خود کاربر.
         * پدر از راه «پیوند» و نقش `father` در سطح سرور دسترسی می‌گیرد، نه از راه کلاینت.
         */
        fun ownerOnly(userId: String): List<String> = listOf(
            Permission.read(Role.user(userId)),
            Permission.update(Role.user(userId)),
            Permission.delete(Role.user(userId)),
        )

        /** سطری که هم زهرا و هم پدرِ پیوندشده می‌توانند بخوانند (حرف دل، تماس). */
        fun sharedWith(zahraId: String, fatherId: String?): List<String> {
            val readers = listOfNotNull(
                Permission.read(Role.user(zahraId)),
                Permission.update(Role.user(zahraId)),
                fatherId?.let { Permission.read(Role.user(it)) },
            )
            return readers
        }
    }
}
