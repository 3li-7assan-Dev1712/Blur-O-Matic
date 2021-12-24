package app.netlify.dev_ali_hassan.blur_o_matic.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.netlify.dev_ali_hassan.blur_o_matic.OUTPUT_PATH
import java.io.File

private const val TAG = "Cleanup Worker"
class CleanupWorker (ctx: Context, params: WorkerParameters) : Worker(ctx, params){

    override fun doWork(): Result {
        makeStatusNotification("cleaning up temp files", applicationContext)
        sleep()

        return try {
            val outputDirectory = File(applicationContext.filesDir, OUTPUT_PATH)
            if (outputDirectory.exists()) {
                val entries = outputDirectory.listFiles()
                if (entries != null) {
                    for (entry in entries) {
                        val name = entry.name
                        if (name.isNotEmpty() && name.endsWith(".png")) {
                            val deleted = entry.delete()
                            Log.i(TAG, "Deleted $name - $deleted")
                        }
                    }
                }
            }

            Result.success()
        }catch (throwable: Throwable) {
            Result.failure()
        }
    }
}