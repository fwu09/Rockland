package com.example.rockland.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream

private fun uriToBase64Jpeg(context: Context, uri: Uri): String {
    val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }

    // Resize to reduce size
    val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

    val baos = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 70, baos)

    val bytes = baos.toByteArray()
    if (bytes.isEmpty()) throw IllegalStateException("Bitmap compression failed")

    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}


