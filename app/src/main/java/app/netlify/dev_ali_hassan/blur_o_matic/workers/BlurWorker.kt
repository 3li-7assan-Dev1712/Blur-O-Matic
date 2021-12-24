package app.netlify.dev_ali_hassan.blur_o_matic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.netlify.dev_ali_hassan.blur_o_matic.KEY_IMAGE_URI


private const val TAG = "BlurWorker"

class BlurWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val appContext = applicationContext
        val recUri = inputData.getString(KEY_IMAGE_URI)

        makeStatusNotification("Blurring image", appContext)

        sleep()
        return try {
            /*  val picture = BitmapFactory.decodeResource(
                  appContext.resources,
                  R.drawable.android_cupcake
              )*/
            if (TextUtils.isEmpty(recUri)) {
                Log.d(TAG, "doWork: Invalid input uri")
                throw IllegalArgumentException("Invalid input uri")
            }
            val contentResolver = appContext.contentResolver

            val picture = BitmapFactory.decodeStream(
                contentResolver.openInputStream(Uri.parse(recUri))
            )
            val blurredPicture = blurBitmap(picture, appContext)
            // get the output (blurred picture) uri and display it in a notification

            val blurredPictureUri = writeBitmapToFile(appContext, blurredPicture)
            makeStatusNotification("Output is $blurredPictureUri", appContext)

            val outputData = workDataOf(KEY_IMAGE_URI to blurredPictureUri.toString())
            Result.success(outputData)
        } catch (throwable: Throwable) {
            Log.d(TAG, "doWork: Error applying blur")
            Result.failure()
        }
    }
}