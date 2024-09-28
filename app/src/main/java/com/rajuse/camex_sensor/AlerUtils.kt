package com.rajuse.camex_sensor

import android.content.Context
import androidx.appcompat.app.AlertDialog

class AlerUtils {
    companion object{

        public fun showAlert(message: String, context:Context) {
            AlertDialog.Builder(context)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .show()
        }
    }
}