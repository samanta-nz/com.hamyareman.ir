package com.hamyareman.ir.platform.feature.hearttoheart

enum class MessageType { TEXT, VOICE, PHOTO, VIDEO, FILE, CALL_EVENT }

enum class MessageDirection { TO_FATHER, TO_ZAHRA }

/**
 * یک پیام «حرف دل».
 *
 * رسانه‌ها سه نشانی دارند چون سه حالت وجود دارد:
 *  - [mediaPath]: فایل محلی روی همین دستگاه (همیشه نگه داشته می‌شود تا آفلاین دیده شود).
 *  - [mediaRef]: ارجاع اپ‌رایت به‌شکل `bucketId/fileId` (وقتی آپلود موفق شده).
 *  - [mediaUrl]: لینک نمایش که هر بار از Storage گرفته می‌شود و ذخیره نمی‌شود.
 */
data class HeartMessage(
    val id: String,
    val type: MessageType,
    val direction: MessageDirection,
    val text: String,
    val createdAt: Long,
    val mediaPath: String? = null,
    val mediaRef: String? = null,
    val mediaUrl: String? = null,
    val durationMs: Long? = null,
    val synced: Boolean = false,
) {
    val hasMedia: Boolean get() = mediaPath != null || mediaRef != null
}
