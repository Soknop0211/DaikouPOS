package com.eazy.daikoupos

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper

import androidx.appcompat.app.AppCompatActivity
import com.eazy.daikoupos.utils.SunmiPrintHelper

open class BaseActivity : AppCompatActivity() {

    var handler: Handler? = null

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

    override fun onDestroy() {
        super.onDestroy()
        if (handler != null) {
            handler!!.removeCallbacksAndMessages(null)
        }

        SunmiPrintHelper.getInstance().deInitSunmiPrinterService(this)
    }

    private fun initPrinterStyle() {
        SunmiPrintHelper.getInstance().initPrinter()
    }

}