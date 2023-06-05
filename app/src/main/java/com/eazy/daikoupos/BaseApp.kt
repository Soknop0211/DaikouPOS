package com.eazy.daikoupos

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.eazy.daikoupos.utils.SunmiPrintHelper
import com.eazy.daikoupos.utils.payment.DeviceHelper

@SuppressLint("StaticFieldLeak")
class BaseApp : Application() {

    companion object {
        const val TAG = "ECRDemo"

        lateinit var app: BaseApp
        lateinit var context: Context

        var server = false // Device Type can have Service(true) and Client(false)
        var connected: Boolean = false
        var isDisConnect = false

        var connectionType = "bluetooth"
    }

    override fun onCreate() {
        super.onCreate()

        // Init  Connect P2 for payment
        app = this
        context = applicationContext
        server = DeviceHelper.isDesktop

        // Init printer D2
        init()
    }

    /**
     * Connect print service through interface library
     */
    private fun init() {
        SunmiPrintHelper.getInstance().initSunmiPrinterService(this)
    }
}