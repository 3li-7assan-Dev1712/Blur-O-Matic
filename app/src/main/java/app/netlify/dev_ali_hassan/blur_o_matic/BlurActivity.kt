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

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import app.netlify.dev_ali_hassan.blur_o_matic.databinding.ActivityBlurBinding
import com.google.android.material.snackbar.Snackbar
import java.util.jar.Manifest


class BlurActivity : AppCompatActivity() {

    private val requestPerimssionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {isGranted ->
            if (isGranted) {
                Snackbar.make(binding.root, "permission granted", Snackbar.LENGTH_SHORT)
                    .show()
                blurImage()
            } else {
                Snackbar.make(binding.root, "you need to grant the permission first", Snackbar.LENGTH_LONG)
                    .show()
            }
        }

    private val viewModel: BlurViewModel by viewModels {
        BlurViewModel.BlurViewModelFactory(
            application
        )
    }
    private lateinit var binding: ActivityBlurBinding

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlurBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.outputWorkInfos.observe(this, workInfoObserver())
        binding.goButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                blurImage()
            } else
                requestStoragePermission()

        }
        binding.seeFileButton.setOnClickListener {
            viewModel.outputUri?.let {
                val actionView = Intent(Intent.ACTION_VIEW, it)
                actionView.resolveActivity(packageManager)?.run {
                    startActivity(actionView)
                }
            }
        }
        binding.cancelButton.setOnClickListener {
            viewModel.cancelWork()
        }
    }

    private fun blurImage() {
        viewModel.applyBlur(blurLevel)
    }

    /**
     * Shows and hides views for when the Activity is processing an image
     */
    private fun workInfoObserver(): Observer<List<WorkInfo>> {
        return Observer {listOfWorkInfo ->
            // if the list is null or empty there's no image is processing
            if (listOfWorkInfo.isNullOrEmpty())
                return@Observer

            // we have one output status
            val workInfo = listOfWorkInfo[0]
            if (workInfo.state.isFinished) {
                showWorkFinished()

                // get the output uri
                val outputImageUri = workInfo.outputData.getString(KEY_IMAGE_URI)
                if (!outputImageUri.isNullOrEmpty()) {
                    binding.seeFileButton.visibility = View.VISIBLE
                    Toast.makeText(applicationContext, "Done", Toast.LENGTH_SHORT).show()
                } else
                    Toast.makeText(applicationContext, "There's an error!", Toast.LENGTH_SHORT).show()
            }
            else
                showWorkInProgress()
        }
    }
    private fun showWorkInProgress() {
        with(binding) {
            progressBar.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            goButton.visibility = View.GONE
            seeFileButton.visibility = View.GONE
        }
    }

    /**
     * Shows and hides views for when the Activity is done processing an image
     */
    private fun showWorkFinished() {
        with(binding) {
            progressBar.visibility = View.GONE
            cancelButton.visibility = View.GONE
            goButton.visibility = View.VISIBLE
        }
    }

    private val blurLevel: Int
        get() =
            when (binding.radioBlurGroup.checkedRadioButtonId) {
                R.id.radio_blur_lv_1 -> 1
                R.id.radio_blur_lv_2 -> 2
                R.id.radio_blur_lv_3 -> 3
                else -> 1
            }

    private fun requestStoragePermission() {
        requestPerimssionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
