package com.eazy.daikoupos

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes

import androidx.appcompat.app.AppCompatActivity
import com.eazy.daikoupos.ecr.ECRHelper
import com.eazy.daikoupos.utils.SunmiPrintHelper

open class BaseActivity : AppCompatActivity() {

    var handler: Handler? = null
    var connectionType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = Handler(Looper.getMainLooper())

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        val mActionBar = supportActionBar
        if (mActionBar != null) {
            supportActionBar!!.hide()
        }

        initPrinterStyle()
    }

    fun showToast(text: String) {
        runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }

    fun showToast(@StringRes resId: Int) {
        runOnUiThread { Toast.makeText(this, resId, Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (handler != null) {
            handler!!.removeCallbacksAndMessages(null)
        }

        SunmiPrintHelper.getInstance().deInitSunmiPrinterService(this)
    }

    private fun initPrinterStyle() {
        SunmiPrintHelper.getInstance().initPrinter()

        ECRHelper.disconnect()
    }

}