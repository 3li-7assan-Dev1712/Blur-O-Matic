package app.netlify.dev_ali_hassan.blur_o_matic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.netlify.dev_ali_hassan.blur_o_matic.KEY_IMAGE_URI
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "SaveImageToFileWorker"

class SaveImageToFileWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:mm:ss z",
        Locale.getDefault()
    )

    override fun doWork(): Result {

        makeStatusNotification("Saving image", applicationContext)
        sleep()

        val resolver = applicationContext.contentResolver

        return try {
            val recUri = inputData.getString(KEY_IMAGE_URI)
            val bitmap = BitmapFactory.decodeStream(
                resolver.openInputStream(Uri.parse(recUri))
            )
            val imageUri = MediaStore.Images.Media.insertImage(
                resolver, bitmap, title, dateFormatter.format(Date())
            )
            if (!imageUri.isNullOrEmpty()) {
                val output = workDataOf(KEY_IMAGE_URI to imageUri)
                Result.success(output)
            } else {
                Log.e(TAG, "doWork: Writing to MediaStore failed")
                Result.failure()
            }

        }catch (exception: Exception) {
            exception.printStackTrace()
            Result.failure()
        }
    }
}