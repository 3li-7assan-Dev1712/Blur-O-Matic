/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.netlify.dev_ali_hassan.blur_o_matic

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import app.netlify.dev_ali_hassan.blur_o_matic.workers.BlurWorker
import app.netlify.dev_ali_hassan.blur_o_matic.workers.CleanupWorker
import app.netlify.dev_ali_hassan.blur_o_matic.workers.SaveImageToFileWorker


class BlurViewModel(application: Application) : ViewModel() {

    private val workManager = WorkManager.getInstance(application)

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null
    internal val outputWorkInfos: LiveData<List<WorkInfo>>

    init {
        imageUri = getImageUri(application.applicationContext)

        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
    }

    internal fun cancelWork() {
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }
    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {

        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresStorageNotLow(true)
            .build()
        // work request to clean up temporary files first
        var continuation = workManager.beginUniqueWork(
            IMAGE_MANIPULATION_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequest.from(CleanupWorker::class.java)
        )

        for (i in 0 until blurLevel) {
            // work request to blur image

            val blurWorkRequest = OneTimeWorkRequestBuilder<BlurWorker>()

            if (i == 0) {
                blurWorkRequest.setInputData(createInputDataForUri())
            }

            continuation = continuation.then(blurWorkRequest.build())

            // work request to save the temp blur image to the filesystem
            val save = OneTimeWorkRequest.Builder(SaveImageToFileWorker::class.java)
                .setConstraints(constraints)
                .addTag(TAG_OUTPUT).build()
            continuation = continuation.then(save)

            // start the chain of work
            continuation.enqueue()
        }
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        imageUri?.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }
        return builder.build()
    }

    private fun getImageUri(context: Context): Uri {
        val resources = context.resources

        val imageUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()

        return imageUri
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    class BlurViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(BlurViewModel::class.java)) {
                BlurViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
