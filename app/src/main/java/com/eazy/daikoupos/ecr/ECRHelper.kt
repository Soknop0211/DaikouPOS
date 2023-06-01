package com.eazy.daikoupos.ecr

import android.os.Bundle
import com.eazy.daikoupos.BaseApp
import com.eazy.daikoupos.utils.Utils
import com.eazy.daikoupos.utils.payment.Logger
import com.eazy.daikoupos.utils.payment.anyExecute
import com.pos.connection.bridge.ECRConnection
import com.pos.connection.bridge.ECRListener
import com.pos.connection.bridge.ECRRequestCallback
import com.pos.connection.bridge.ECRService
import com.pos.hardware.connection.library.ECRServiceKernel
import java.nio.charset.StandardCharsets

object ECRHelper {

    private var ecrService: ECRService ? = null

    var onBindSuccess: () -> Unit = { }
    var onBindFailure: () -> Unit = { }

    var onECRConnected: () -> Unit = { }
    var onECRDisconnected: (Int, String) -> Unit = { _, _ -> }

    var onSendSuccess: () -> Unit = { }
    var onSendFailure: (Int, String) -> Unit = { _, _ -> }

    var onECRReceive: (bytes: ByteArray) -> Unit = { }

    fun connect(bundle: Bundle) {
        call {
            anyExecute(ecrService)
            {
                connect(bundle, ecrConnection)
            }
        }
    }

    fun disconnect() {
        call { anyExecute(ecrService) { disconnect() } }
    }

    fun registerECRListener() {
        call { anyExecute(ecrService) { register(ecrListener) } }
    }

    fun unregisterECRListener() {
        call { anyExecute(ecrService) { unregister(ecrListener) } }
    }

    fun extensionMethod(bundle: Bundle) {
        call { anyExecute(ecrService) { extensionMethod(bundle) } }
    }

    private val ecrListener = object : ECRListener.Stub() {

        override fun onReceive(byteArray: ByteArray ? ) {
            if (byteArray != null) {
                val string = String(byteArray, StandardCharsets.UTF_8)
                Logger.e(BaseApp.TAG, "onReceive string: $string")
                onECRReceive(byteArray)
            }
        }

    }

    fun send(bytes: ByteArray) {
        try {
            val bridgeService = ecrService
            if (bridgeService != null) {
                bridgeService.send(bytes, requestCallback)
            } else {
                onECRDisconnected(-100, "The bind ECRService failure")
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            onSendFailure(-200, ex.localizedMessage ?: "")
        }
    }

    private val requestCallback = object : ECRRequestCallback.Stub() {

        override fun onSuccess() {
            Logger.e(BaseApp.TAG, "onSuccess")
            onSendSuccess()
        }

        override fun onFailure(code: Int, massage: String ? ) {
            Logger.e(BaseApp.TAG, "onFailure code: $code massage: $massage")
            onSendFailure(code, massage ?: "failure")
        }

    }

    private val ecrConnection = object : ECRConnection.Stub() {

        override fun onConnected() {
            Logger.e(BaseApp.TAG, "onConnected")
            BaseApp.connected = true
            onECRConnected()
        }

        override fun onDisconnected(code: Int, massage: String ? ) {
            Logger.e(BaseApp.TAG, "onDisconnected code: $code massage: $massage")
            BaseApp.connected = false
            onECRDisconnected(code, massage ?: "failure")
        }

    }

    fun bindECRService() {
        ECRServiceKernel.getInstance().bindService(BaseApp.context, connectionCallback)
    }

    private val connectionCallback = object : ECRServiceKernel.ConnectionCallback {

        override fun onServiceConnected() {
            Utils.logDebug(BaseApp.TAG, "onServiceConnected")
            ecrService = ECRServiceKernel.getInstance().ecrService
            onBindSuccess()
        }

        override fun onServiceDisconnected() {
            Logger.e(BaseApp.TAG, "onServiceDisconnected")
            BaseApp.connected = false
            ecrService = null
            onBindFailure()
        }

    }

    private fun call(block: () -> Unit) {
        try {
            val bridgeService = ecrService
            if (bridgeService != null) {
                block()
            } else {
                onECRDisconnected(-100, "The bind ECRService failure")
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            onECRDisconnected(-200, ex.localizedMessage ?: "")
        }
    }

}