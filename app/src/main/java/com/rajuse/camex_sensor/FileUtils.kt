package com.rajuse.camex_sensor

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class FileUtils {
    companion object {
        fun createVideoFile(context: Context): File {
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            val outputDir =
                if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
            return File(
                outputDir,
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
            )
        }
    }
}