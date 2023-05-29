package com.eazy.daikoupos.utils.payment

import android.os.Build

object DeviceHelper {

    val isDesktop: Boolean
        get() {
            val model = getDeviceModel().toUpperCase()
            return "T1" in model || "T2" in model || "D2" in model || "K1" in model || "K2" in model
        }

    fun getDeviceModel() = Build.MODEL

}