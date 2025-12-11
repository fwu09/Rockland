package com.example.rockland.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class RockClassifier(context: Context) {

    companion object {
        private const val MODEL_NAME = "rock_finetuned_v1.tflite"
        private const val LABELS_NAME = "rock_finetuned_v1_labels.txt"
        private const val INPUT_SIZE = 224
        private const val NUM_CHANNELS = 3
        private const val TAG = "RockClassifier"
    }

    data class Result(
        val label: String,
        val confidence: Float
    )

    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        interpreter = Interpreter(loadModelFile(context, MODEL_NAME))
        labels = loadLabels(context)
        Log.d(TAG, "Loaded model with ${labels.size} labels")
    }

    fun classify(context: Context, imageUri: Uri): Result? {
        val bitmap = loadBitmapFromUri(context, imageUri) ?: return null
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        val inputBuffer = bitmapToByteBuffer(resized)

        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(inputBuffer, output)

        val scores = output[0]
        var maxIdx = 0
        var maxScore = scores[0]
        for (i in 1 until scores.size) {
            if (scores[i] > maxScore) {
                maxScore = scores[i]
                maxIdx = i
            }
        }
        val label = labels.getOrElse(maxIdx) { "Unknown" }
        return Result(label = label, confidence = maxScore)
    }

    // ------------- helpers -------------

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? =
                context.contentResolver.openInputStream(uri)
            inputStream.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from uri", e)
            null
        }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val bufferSize = 4 * INPUT_SIZE * INPUT_SIZE * NUM_CHANNELS
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        // Model was trained with rescale=1/255, so scale to [0,1]
        var pixelIndex = 0
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val pixel = intValues[pixelIndex++]

                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f

                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }
        return byteBuffer
    }

    private fun loadModelFile(context: Context, name: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(name)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(context: Context): List<String> {
        return context.assets.open(LABELS_NAME).bufferedReader().use { it.readLines() }
    }
}