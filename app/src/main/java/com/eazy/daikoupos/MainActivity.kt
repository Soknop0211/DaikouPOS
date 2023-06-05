package com.eazy.daikoupos

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.webkit.*
import cn.pedant.SweetAlert.SweetAlertDialog
import com.eazy.daikoupos.BaseApp.Companion.connectionType
import com.eazy.daikoupos.databinding.ActivityMainBinding
import com.eazy.daikoupos.ecr.ECRHelper
import com.eazy.daikoupos.extension.validateSt
import com.eazy.daikoupos.model.ResponseData
import com.eazy.daikoupos.utils.ShowAlertDialog
import com.eazy.daikoupos.utils.SunmiPrintHelper
import com.eazy.daikoupos.utils.Utils
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mylibrary.KESSMerchantService
import com.mylibrary.network.Credential.apiSecretKey
import com.mylibrary.network.Credential.clientId
import com.mylibrary.network.Credential.clientSecret
import com.mylibrary.network.Credential.currency
import com.mylibrary.network.Credential.grantType
import com.mylibrary.network.Credential.methodDesc
import com.mylibrary.network.Credential.password
import com.mylibrary.network.Credential.sellerCode
import com.mylibrary.network.Credential.serviceType
import com.mylibrary.network.Credential.signType
import com.mylibrary.network.Credential.terminalType
import com.mylibrary.network.Credential.tillId
import com.mylibrary.network.Credential.userName
import com.mylibrary.network.PreOrderData
import com.pos.connection.bridge.binder.ECRConstant
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors


class MainActivity : BaseActivity() {

    private var getLinkUrl = "https://pos.daikou.asia"
    private var webView: WebView? = null

    private var bluetoothAddress = ""
    private var mAccessToken = ""
    private var mPreOrderToken = ""
    private var mPreOutTradeNo = ""
    private var mPreTransactionId = ""

    private var timerStringOnReceivePOS = arrayOf("")
    private var isSetTimer = booleanArrayOf(false)

    companion object {
        private const val JAVASCRIPT_OBJ = "Android"
        private var mode = ""
        fun checkConnectionDevice(connectionType : String) {
            if (BaseApp.connected) {
                return
            }

            when (connectionType) {
                "bluetooth" -> {
                    mode = ECRConstant.Mode.Bluetooth
                }
                "wifi" -> {
                    mode = ECRConstant.Mode.WIFI
                }
                "usb" -> {
                    mode = ECRConstant.Mode.RS232
                }
            }
        }
    }

    private var bitmap: Bitmap? = null

    private val binding : ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()

        initAction()

        initECR()

    }

    private fun initView() {
        webView = binding.webViewUrl

        val options = BitmapFactory.Options()
        options.inTargetDensity = 200
        options.inDensity = 200
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initAction() {
        webView!!.settings.apply {
            javaScriptEnabled = true
            useWideViewPort = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        webView!!.webViewClient = webViewClient
        webView!!.apply {
            setNetworkAvailable(true)
            addJavascriptInterface(JavaScriptInterface(), JAVASCRIPT_OBJ)
            loadUrl(getLinkUrl)
        }
    }

    inner class JavaScriptInterface {
        @JavascriptInterface
        fun updateFromAndroidPrint(data: String) {
            Utils.logDebug("jeeeeeeeeeeeeeeeeeeee", data)
            if (!TextUtils.isEmpty(data) && data != "") {
                val results: ResponseData? = Gson().fromJson(data, ResponseData::class.java)
                if (results != null && !TextUtils.isEmpty(results.mType)) {
                    when (results.mType) {
                        // Alert from web no item select
                        "alert" -> {
                            if (results.mData?.msg != null) {
                                val mDialog = ShowAlertDialog.newInstance("alert", results.mData.msg)
                                mDialog.show(supportFragmentManager, "mFragment")
                            }
                        }
                        "card_payment" -> {
                            if (!BaseApp.connected) {
                                alertPopup("no_connection", "alert", "Please connect device mode first !")
                                return
                            }
                            results.mData?.mTotalPayment?.let { submitAccessToken(it) }
                        }
                        "print_invoice" -> {
                            results.mData?.mBase64?.let { displayBitmap(it) }
                        }
                        "connection" -> {
                            val mFragment = SelectDeviceModeFragment()
                            mFragment.initListener(object : SelectDeviceModeFragment.OnCallBackListener {
                                override fun onCallBack(bluetoothAdd: String) {
                                    bluetoothAddress = bluetoothAdd
                                    if (connectionType == "bluetooth") {
                                        if (bluetoothAddress != ""){
                                            connectWithSATHAPANA("", "")
                                        } else {
                                            showToast("Please select bluetooth address connection .")
                                        }
                                    } else {
                                        connectWithSATHAPANA("", "")
                                    }
                                }

                                override fun onCallBack(ip: String, port: String) {
                                    connectWithSATHAPANA(ip, port)
                                }

                            })
                            mFragment.show(supportFragmentManager, "mFragment")
                        }
                    }
                }
            }
        }
    }

    private fun injectJavaScriptFunction() {
        webView!!.loadUrl("javascript: window.WebPayJSBride.invoke = function(data) { $JAVASCRIPT_OBJ.updateFromAndroidPrint(data) }")
    }

    private val webViewClient = object : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            injectJavaScriptFunction()
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
        }

    }

    // Printer
    private fun displayBitmap(imgBase64: String?) {
        bitmap = imgBase64?.let { it1 -> Utils.passImageToAndroid(it1) }

        // Init printer D2
        val sunmiPrintHelper = SunmiPrintHelper.getInstance()
        sunmiPrintHelper.printBitmap(bitmap, 0)
        if (sunmiPrintHelper.sunmiPrinterService != null) {
            sunmiPrintHelper.sunmiPrinterService.lineWrap(0, null)
            sunmiPrintHelper.sunmiPrinterService.lineWrap(1, null)
            sunmiPrintHelper.sunmiPrinterService.lineWrap(0, null)
        }
        sunmiPrintHelper.cutPaper()
        sunmiPrintHelper.feedPaper()
    }

    @SuppressLint("MissingPermission")
    private fun connectWithSATHAPANA(ip: String, port: String) {
        if (!BaseApp.connected){
            val bundle = Bundle()
            bundle.putString(ECRConstant.Configuration.MODE, mode)
            bundle.putString(ECRConstant.Configuration.TYPE, ECRConstant.Type.SLAVE)

            if (mode == ECRConstant.Mode.Bluetooth) {
                Utils.logDebug(BaseApp.TAG, "bluetoothAddress: $bluetoothAddress")
                bundle.putString(ECRConstant.Configuration.BLUETOOTH_MAC_ADDRESS, bluetoothAddress)
            }
            if (mode == ECRConstant.Mode.WIFI) {
                bundle.putInt(ECRConstant.Configuration.WIFI_PORT, port.toInt())
                bundle.putString(ECRConstant.Configuration.WIFI_ADDRESS, ip)
            }

            val ecrHelper = ECRHelper
            ecrHelper.connect(bundle)
        } else {
            showToast(resources.getString(R.string.message_select_connect))
        }
    }

    private fun initECR() {
        ECRHelper.onBindSuccess = {
            ECRHelper.registerECRListener()
        }
        ECRHelper.onBindFailure = {
            runOnUiThread { showConnectStatus() }
            showToast(R.string.message_bind_ecr_service)
        }
        ECRHelper.onECRConnected = {
            runOnUiThread {
               showToast(mode + " "+ resources.getString(R.string.connected))
                showConnectStatus()
            }
        }
        ECRHelper.onECRDisconnected = { code, message ->
            when(code){
                -7006 -> { }
            }
            runOnUiThread { showConnectStatus() }
            showToast("$message ($code)")
        }
        ECRHelper.onSendSuccess = {
            // Sent To POS
        }
        ECRHelper.onSendFailure = { code, message ->
            showToast("$message ($code)")
            runOnUiThread { showConnectStatus() }
        }
        ECRHelper.onECRReceive = { bytes ->
            this.runOnUiThread {
                val s = String(bytes, StandardCharsets.UTF_8)
                timerStringOnReceivePOS[0] += s
                if (!isSetTimer[0]) {
                    isSetTimer[0] = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        onCallResponsePaymentToWeb(timerStringOnReceivePOS[0])
                    }, 3000)
                }
            }
        }

        ECRHelper.bindECRService()
    }

    private fun showConnectStatus() {
        val text = if (BaseApp.connected) getString(R.string.connected) else getString(R.string.disconnected)
        Utils.logDebug(BaseApp.TAG, text)
    }

    private fun sendPaymentToP2(totalAmount : String, mPreTransactionId: String) {
        if (!BaseApp.connected) {
            showToast(resources.getString(R.string.please_select_device_mode))
            return
        }

        Executors.newCachedThreadPool().execute {
            val text = "CMD:C200|AMT:${totalAmount}|CCY:${currency}|TRXID:${mPreTransactionId}|TILLID:${tillId}" // test : text will get from web invoice
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            Utils.logDebug(BaseApp.TAG, "size: $bytes.size")
            ECRHelper.send(bytes)
        }
    }

    private fun onCallResponsePaymentToWeb(text : String) {
        timerStringOnReceivePOS = arrayOf("")
        isSetTimer = booleanArrayOf(false)

        when {
            text.contains("RESCODE:000") -> {
                updateFundTransfer(mPreOrderToken, text)
            }
            text.contains("RESCODE:099") -> {
                alertPopup("payment_error", "Error", "Payment Error !")
            }
            text.contains("RESCODE:098") -> {
                alertPopup("payment_error", "Error", "Payment Error !")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            finish()
        }
    }

    private fun submitAccessToken(mTotalAmount : String) {
        KESSMerchantService.submitAccessToken(
            true,
            grantType,
            clientId,
            clientSecret,
            userName,
            password,
            object : KESSMerchantService.OnCallBackListener {
                override fun onSuccess(resObj: JsonObject?) {
                    if(resObj == null) return

                    mAccessToken = resObj.validateSt("access_token")
                    mPreOutTradeNo = UUID.randomUUID().toString()

                    createPreOrder(mAccessToken, mPreOutTradeNo, mTotalAmount)
                }

                override fun onSuccess(resObj: PreOrderData?) {}

                override fun onFailed(message: String) {
                    this@MainActivity.showToast(message)
                    alertPopup("payment_error", "Error", "Payment Error !")
                }

            }
        )
    }

    private fun createPreOrder(mToken : String, outTradNo: String, mTotalAmount : String) {
        KESSMerchantService.submitCreatePreOrder(
            true,
            mToken,
            serviceType,
            signType,
            sellerCode,
            currency,
            mTotalAmount,
            "Create preOrder fro payment",
            terminalType,
            outTradNo,
            apiSecretKey,
            sellerCode,
            object : KESSMerchantService.OnCallBackListener {
                override fun onSuccess(resObj: JsonObject?) {}

                override fun onSuccess(resObj: PreOrderData?) {
                    if(resObj == null) return

                    if (resObj.token != null) mPreOrderToken = resObj.token!!

                    // Sent POS
                    sendPaymentToP2(mTotalAmount, mPreOutTradeNo)
                }

                override fun onFailed(message: String) {
                    this@MainActivity.showToast(message)
                    alertPopup("payment_error", "Error", "Payment Error !")
                }

            }
        )
    }

    private fun updateFundTransfer(mToken : String, bankInfo : String) {
        val paymentTransactionId = methodDesc + "-" + sellerCode + "_" + mToken

        KESSMerchantService.updateFundTransfer(
            true,
            mAccessToken,
            paymentTransactionId,
            mToken,
            signType,
            methodDesc,
            bankInfo,
            apiSecretKey,
            sellerCode,
            object : KESSMerchantService.OnCallBackListener {
                override fun onSuccess(resObj: JsonObject?) {}

                override fun onSuccess(resObj: PreOrderData?) {
                    if (resObj?.transactionId != null)   mPreTransactionId = resObj.transactionId!!
                    alertPopup("payment_success", "Success", "Payment Successfully !")
                }

                override fun onFailed(message: String) {
                    this@MainActivity.showToast(message)
                }

            }
        )
    }

    private fun alertPopup(status : String, title : String, message : String) {
        val mDialog = ShowAlertDialog.newInstance(title, message)
        mDialog.initListener(object : ShowAlertDialog.OnCallBackListener {
            override fun onCallBack(bluetoothAdd: String) {
                val jsonObject = JsonObject()
                jsonObject.addProperty("status", status)
                val jsonSt = jsonObject.toString()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    webView?.evaluateJavascript("javascript: GetNotificationFromAndroid($jsonSt)") { data ->
                        Utils.logDebug("Jjeeeeeeeeeeeee", "")
                    }
                } else {
                    webView?.loadUrl("javascript:GetNotificationFromAndroid('Javascript function in webview')")
                }
            }

        })
        mDialog.show(supportFragmentManager, "mFragment")
    }

}