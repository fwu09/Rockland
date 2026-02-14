package com.example.rockland.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlin.math.max
import kotlin.math.sqrt

object ImageValidationUtil {

    private const val MAX_BYTES: Long = 20L * 1024L * 1024L

    const val TYPE_SIZE_ERROR = "Upload failed. The image must be a JPEG or PNG and under 20MB."
    const val QUALITY_ERROR =
        "Image quality is too low for accurate identification. Please select a clearer photo."

    sealed class Result {
        data object Ok : Result()
        data class Error(val message: String) : Result()
    }

    //// Validate (1) mime type and (2) size. ////
    fun validateTypeAndSize(context: Context, uri: Uri): Result {
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
        val typeOk = mime == "image/jpeg" || mime == "image/png" || mime == "image/jpg"

        val sizeBytes = getFileSizeBytes(context, uri)
        val sizeOk = (sizeBytes != null && sizeBytes in 1..MAX_BYTES)

        return if (typeOk && sizeOk) Result.Ok else Result.Error(TYPE_SIZE_ERROR)
    }

    //// Validate type+size AND basic quality gates for scanner (blur + too-dark/low-contrast) ////
    fun validateForIdentification(context: Context, uri: Uri): Result {
        val basic = validateTypeAndSize(context, uri)
        if (basic is Result.Error) return basic

        val bmp = decodeDownsampledBitmap(context, uri, targetMaxSide = 256)
            ?: return Result.Error(TYPE_SIZE_ERROR)

        val tooDarkOrFlat = isTooDarkOrLowContrast(bmp)
        val tooBlurry = isTooBlurry(bmp)

        return if (tooDarkOrFlat || tooBlurry) Result.Error(QUALITY_ERROR) else Result.Ok
    }

    private fun getFileSizeBytes(context: Context, uri: Uri): Long? {
        // 1) Try OpenableColumns.SIZE
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (idx >= 0 && !cursor.isNull(idx)) return cursor.getLong(idx)
                    }
                }
        }
        // 2) Fallback: AssetFileDescriptor length
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?.use { afd ->
                    val len = afd.length
                    if (len > 0) len else null
                }
        }.getOrNull()
    }

    private fun decodeDownsampledBitmap(
        context: Context,
        uri: Uri,
        targetMaxSide: Int
    ): Bitmap? {
        // First decode bounds
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, opts)
            }
        }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

        // Compute inSampleSize
        val maxSide = max(opts.outWidth, opts.outHeight)
        var sample = 1
        while (maxSide / sample > targetMaxSide) sample *= 2

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = max(1, sample)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            }
        }.getOrNull()
    }

    //// Reject images that are very dark or nearly flat (low contrast). ////
    private fun isTooDarkOrLowContrast(bmp: Bitmap): Boolean {
        val w = bmp.width
        val h = bmp.height
        val n = w * h
        if (n <= 0) return true

        var sum = 0.0
        var sumSq = 0.0

        // Luma approximation: 0.2126R + 0.7152G + 0.0722B
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = bmp.getPixel(x, y)
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = (c) and 0xFF
                val l = 0.2126 * r + 0.7152 * g + 0.0722 * b
                sum += l
                sumSq += l * l
            }
        }

        val mean = sum / n
        val variance = max(0.0, (sumSq / n) - (mean * mean))
        val std = sqrt(variance)

        // Thresholds tuned for small downsampled images:
        // - mean < ~45 => too dark
        // - std < ~22 => too flat / bad lighting
        return mean < 45.0 || std < 22.0
    }

    //// Reject very blurry images using variance of Laplacian on grayscale. ////
    private fun isTooBlurry(bmp: Bitmap): Boolean {
        val w = bmp.width
        val h = bmp.height
        if (w < 8 || h < 8) return true

        // Convert to grayscale array [0..255]
        val gray = IntArray(w * h)
        var idx = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = bmp.getPixel(x, y)
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = (c) and 0xFF
                val l = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
                gray[idx++] = l
            }
        }

        // Laplacian kernel:
        //  0  1  0
        //  1 -4  1
        //  0  1  0
        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until h - 1) {
            val row = y * w
            val rowUp = (y - 1) * w
            val rowDown = (y + 1) * w
            for (x in 1 until w - 1) {
                val center = gray[row + x]
                val lap =
                    gray[rowUp + x] +
                            gray[row + (x - 1)] -
                            4 * center +
                            gray[row + (x + 1)] +
                            gray[rowDown + x]

                val v = lap.toDouble()
                sum += v
                sumSq += v * v
                count++
            }
        }

        if (count <= 0) return true

        val mean = sum / count
        val variance = max(0.0, (sumSq / count) - (mean * mean))

        // Lower variance => blurrier
        // Typical: sharp images higher; blurry lower.
        return variance < 120.0
    }
}
