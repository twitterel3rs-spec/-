package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApkInfo(
    val exists: Boolean,
    val filePath: String,
    val fileSizeBytes: Long,
    val fileSizeFormatted: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val lastModifiedFormatted: String
)

object ApkManager {

    private const val APK_FILE_NAME = "Sennara_Restaurant_POS_v1.0.apk"

    /**
     * Inspects the running application's actual installed APK file.
     * Android stores the running base APK at context.applicationInfo.sourceDir.
     */
    fun getInstalledApkInfo(context: Context): ApkInfo {
        return try {
            val sourceDir = context.applicationInfo.sourceDir
            val apkFile = if (sourceDir != null) File(sourceDir) else null

            if (apkFile != null && apkFile.exists() && apkFile.canRead()) {
                val sizeBytes = apkFile.length()
                val sizeFormatted = formatFileSize(sizeBytes)
                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                val lastModified = dateFormat.format(Date(apkFile.lastModified()))

                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }

                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toLong()
                }

                ApkInfo(
                    exists = true,
                    filePath = apkFile.absolutePath,
                    fileSizeBytes = sizeBytes,
                    fileSizeFormatted = sizeFormatted,
                    packageName = context.packageName,
                    versionName = pInfo.versionName ?: "1.0",
                    versionCode = vCode,
                    lastModifiedFormatted = lastModified
                )
            } else {
                ApkInfo(
                    exists = false,
                    filePath = sourceDir ?: "غير متوفر",
                    fileSizeBytes = 0L,
                    fileSizeFormatted = "0 ميجابايت",
                    packageName = context.packageName,
                    versionName = "1.0",
                    versionCode = 1L,
                    lastModifiedFormatted = "-"
                )
            }
        } catch (e: Exception) {
            ApkInfo(
                exists = false,
                filePath = "خطأ في قراءة ملف التطبيق: ${e.message}",
                fileSizeBytes = 0L,
                fileSizeFormatted = "غير متاح",
                packageName = context.packageName,
                versionName = "1.0",
                versionCode = 1L,
                lastModifiedFormatted = "-"
            )
        }
    }

    /**
     * Exports the real APK binary to the device's public Downloads directory.
     * Uses MediaStore.Downloads on Android 10+ (API 29+) or external storage on older versions.
     */
    fun exportApkToDownloads(context: Context): Result<String> {
        return try {
            val sourceDir = context.applicationInfo.sourceDir
                ?: return Result.failure(IllegalStateException("تعذر العثور على ملف APK المثبت للتطبيق"))
            val sourceFile = File(sourceDir)
            if (!sourceFile.exists() || !sourceFile.canRead()) {
                return Result.failure(IllegalStateException("ملف الـ APK غير قابل للقراءة من المسار: $sourceDir"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, APK_FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(IllegalStateException("فشل إنشاء مدخل ملف الـ APK في مجلد التنزيلات"))

                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return Result.failure(IllegalStateException("تعذر فتح تيار الكتابة في مجلد التنزيلات"))

                Result.success("تم حفظ ملف APK الفعلي بنجاح باسم:\n$APK_FILE_NAME\nفي مجلد التنزيلات (Downloads)")
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val destFile = File(downloadsDir, APK_FILE_NAME)
                FileInputStream(sourceFile).use { inStream ->
                    FileOutputStream(destFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                Result.success("تم حفظ ملف APK الفعلي بنجاح في:\n${destFile.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Prepares the real APK file and opens Android's native share sheet (FileProvider).
     * This transmits the genuine binary .apk file so the recipient can install it directly.
     */
    fun shareRealApkFile(context: Context): Result<Intent> {
        return try {
            val sourceDir = context.applicationInfo.sourceDir
                ?: return Result.failure(IllegalStateException("تعذر العثور على ملف APK المثبت للتطبيق"))
            val sourceFile = File(sourceDir)
            if (!sourceFile.exists()) {
                return Result.failure(IllegalStateException("ملف الـ APK الأصلي غير موجود"))
            }

            val cacheApkDir = File(context.cacheDir, "apks").apply { mkdirs() }
            val sharedApkFile = File(cacheApkDir, APK_FILE_NAME)

            // Copy the source APK to cache to be accessible by FileProvider
            FileInputStream(sourceFile).use { inStream ->
                FileOutputStream(sharedApkFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                sharedApkFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "تطبيق صنارة وشبكة موبايل - نسخة APK أصلية")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "إرسال ملف الـ APK الفعلي لتثبيته على الهاتف")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Result.success(chooser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatFileSize(sizeBytes: Long): String {
        val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f ميجابايت", mb)
    }
}
