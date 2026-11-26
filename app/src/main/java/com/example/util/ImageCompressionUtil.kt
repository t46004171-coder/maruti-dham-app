package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageCompressionUtil {

    /**
     * Decodes and compresses an image picked via Android Photo Picker to a lightweight
     * JPEG (max 640px) stored inside app-private filesDir/menu_photos to prevent memory lag.
     */
    suspend fun compressPickedImage(
        context: Context,
        sourceUri: Uri,
        fileNamePrefix: String = "item"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }

            val maxDim = 640
            var sampleSize = 1
            while ((bounds.outWidth / sampleSize) > maxDim * 2 || (bounds.outHeight / sampleSize) > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read image"))

            val ratio = minOf(
                maxDim.toFloat() / decoded.width.coerceAtLeast(1),
                maxDim.toFloat() / decoded.height.coerceAtLeast(1),
                1f
            )
            val targetWidth = (decoded.width * ratio).toInt().coerceAtLeast(1)
            val targetHeight = (decoded.height * ratio).toInt().coerceAtLeast(1)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
            } else {
                decoded
            }

            val outDir = File(context.filesDir, "menu_photos").apply { mkdirs() }
            val cleanPrefix = fileNamePrefix.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "item" }
            val outFile = File(outDir, "${cleanPrefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 78, fos)
            }
            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()

            Result.success(Uri.fromFile(outFile).toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
