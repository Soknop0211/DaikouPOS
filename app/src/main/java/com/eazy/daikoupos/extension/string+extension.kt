package com.eazy.daikoupos.extension

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.gson.Gson
import com.google.gson.JsonObject

fun Context.showToast(msg : String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.showToast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}
fun JsonObject.validateSt(key: String): String {
    if (this.has(key)) {
        if (!this[key].isJsonNull) {
            return this[key].asString
        }
    }
    return ""
}