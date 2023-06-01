package com.eazy.daikoupos.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.StrictMode
import android.util.Base64
import android.util.Log
import com.eazy.daikoupos.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.ByteArrayOutputStream
import java.net.URL

class Utils {
    companion object {
        fun passImageToAndroid(base64Url: String): Bitmap {
            // Decode the base64 string to obtain the image data
            val base64Image: String = base64Url.split(",")[1]
            val imageData: ByteArray = Base64.decode(base64Image, Base64.DEFAULT)

            // For example, you can create a Bitmap from the byte array
            return BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        }

        fun logDebug(logTitle: String?, message: String?) {
            if (BuildConfig.DEBUG) { //logDebug only in debug mode
                Log.d(logTitle, message!!)
            }
        }

        fun convertUrlToBase64(url: String?): String? {
            val newUrl: URL
            val bitmap: Bitmap
            var base64: String? = ""
            try {
                val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
                StrictMode.setThreadPolicy(policy)
                newUrl = URL(url)
                bitmap = BitmapFactory.decodeStream(newUrl.openConnection().getInputStream())
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return base64
        }
    }
}