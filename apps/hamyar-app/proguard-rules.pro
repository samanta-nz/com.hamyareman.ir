# قواعدِ نگهبان برای نسخه‌ی ریلیزِ R8 (بیلدِ دیباگ از این فایل استفاده نمی‌کند).
#
# چرا این‌ها لازم‌اند:
#  • مدل‌های «Appwrite SDK» با Gson و از راهِ بازتاب (reflection) پر می‌شوند؛ اگر
#    نام/امضای فیلدهایشان حذف شود، پاسخِ سرور بی‌صدا خالی می‌شود.
#  • WebRTC یک کتابخانهٔ بومی است و نامِ کلاس‌هایش از سمتِ C++ صدا زده می‌شود.
#  • OkHttp/Okio قواعدِ همراهِ خودشان را می‌آورند؛ فقط برای ساکت‌کردنِ هشدارهای
#    کلاس‌های اختیاری (Conscrypt/BouncyCastle/OpenJSSE) `-dontwarn` می‌گذاریم.

-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# --- Appwrite SDK (Gson) ---
-keep class io.appwrite.** { *; }
-dontwarn io.appwrite.**
-dontwarn com.google.gson.**

# --- WebRTC (JNI) ---
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# --- شبکه ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
