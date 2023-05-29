package com.eazy.daikoupos.utils.payment

import android.app.Activity
import android.content.Intent
import android.util.Log

inline fun <reified T : Activity> Activity.openActivity() {
    val intent = Intent(this, T::class.java)
    startActivity(intent)
}

inline fun <T> anyExecute(receiver: T ? , block: T.() -> Unit) {
    if (receiver == null) {
        Log.e("ktx", "The depend on call is null")
    } else {
        receiver.block()
    }
}

val String.empty: Boolean
    get() = isBlank() || isEmpty()

val String.valid: Boolean
    get() = isNotEmpty() && isNotBlank()