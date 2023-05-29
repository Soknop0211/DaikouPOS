package com.eazy.daikoupos

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.RadioGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.eazy.daikoupos.databinding.ActivityMainBinding
import com.eazy.daikoupos.ecr.ECRHelper
import com.eazy.daikoupos.utils.SunmiPrintHelper
import com.eazy.daikoupos.utils.Utils
import com.eazy.daikoupos.utils.payment.Logger
import com.pos.connection.bridge.binder.ECRConstant
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class MainActivity : BaseActivity() {

    private var getLinkUrl = "https://pos.daikou.asia"
    private var webView: WebView? = null
    private var mode = ""

    companion object {
        private const val JAVASCRIPT_OBJ = "Android"
    }

    private var bitmap: Bitmap? = null

    private val binding : ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()

        initAction()

        // Init  Connect P2 for payment
        checkConnectionDevice(connectionType)

        initECR()

        binding.btnClick.setOnClickListener {
            sendPaymentToP2("card")
        }
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
                displayBitmap(data)
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

    private fun displayBitmap(imgBase64: String?) {
        bitmap = imgBase64?.let { it1 -> Utils.passImageToAndroid(it1) }

        // Init printer D2
        val sunmiPrintHelper = SunmiPrintHelper.getInstance()
        sunmiPrintHelper.printBitmap(bitmap, 0)
        if (sunmiPrintHelper.sunmiPrinterService != null) {
            sunmiPrintHelper.sunmiPrinterService.lineWrap(1, null)
            sunmiPrintHelper.sunmiPrinterService.lineWrap(0, null)
            sunmiPrintHelper.sunmiPrinterService.cutPaper( null)
        }
        sunmiPrintHelper.feedPaper()
    }

    private fun connectWithSATHAPANA() {
        if (!BaseApp.connected){
            val bundle = Bundle()
            bundle.putString(ECRConstant.Configuration.MODE, mode)
            bundle.putString(ECRConstant.Configuration.TYPE, if (BaseApp.server) ECRConstant.Type.MASTER else ECRConstant.Type.SLAVE)
            if (mode == ECRConstant.Mode.Bluetooth) {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                    val bluetoothDevice = bluetoothAdapter.bondedDevices
                    for (device in bluetoothDevice) {
                        if (device.address == "00:11:22:33:44:55") continue
                        bundle.putString(ECRConstant.Configuration.BLUETOOTH_MAC_ADDRESS, device.address)
                    }
                } else {
                    showToast(R.string.message_bluetooth_not_support)
                }
            }
            val ecrHelper = ECRHelper
            ecrHelper.connect(bundle)
        }
    }

    private fun initECR() {
        ECRHelper.onBindSuccess = {
            connectWithSATHAPANA()

            ECRHelper.registerECRListener()
        }
        ECRHelper.onBindFailure = {
            runOnUiThread { showConnectStatus() }
            showToast(R.string.message_bind_ecr_service)
        }
        ECRHelper.onECRConnected = {
            runOnUiThread { showConnectStatus() }
        }
        ECRHelper.onECRDisconnected = { code, message ->
            when(code){
                -7006 -> {

                }
            }
            runOnUiThread { showConnectStatus() }
            showToast("$message ($code)")
        }
        ECRHelper.onSendSuccess = {

        }
        ECRHelper.onSendFailure = { code, message ->
            showToast("$message ($code)")
            runOnUiThread { showConnectStatus() }
        }
        ECRHelper.onECRReceive = { bytes ->
            val text = String(bytes, StandardCharsets.UTF_8)
            Utils.logDebug(BaseApp.TAG, text)
        }

        ECRHelper.onBindSuccess

        ECRHelper.bindECRService()
    }

    private fun showConnectStatus() {
        val text = if (BaseApp.connected) getString(R.string.connected) else getString(R.string.disconnected)
        Utils.logDebug(BaseApp.TAG, text)
    }

    private fun showDialog(bundle: Bundle) {
//        val view = View.inflate(baseContext, R.layout.dialog_content, null)
//        val ip = view.findViewById<EditText>(R.id.ip)
//        val port = view.findViewById<EditText>(R.id.port)
//        if (BaseApp.server) {
//            ip.visibility = View.GONE
//        }
//        AlertDialog.Builder(this)
//            .setTitle("Input Data")
//            .setView(view)
//            .setPositiveButton(
//                "Confirm"
//            ) { _, _ ->
//                if (BaseApp.server) {
//                    if (TextUtils.isEmpty(port.text.toString())) {
//                        showToast("Please input port")
//                        return@setPositiveButton
//                    }
//                } else {
//                    if (TextUtils.isEmpty(port.text.toString()) || TextUtils.isEmpty(ip.text.toString())) {
//                        showToast("Please input port and ip")
//                        return@setPositiveButton
//                    }
//                }
//                bundle.putInt(
//                    ECRConstant.Configuration.WIFI_PORT,
//                    port.text.toString().toInt()
//                )
//                bundle.putString(ECRConstant.Configuration.WIFI_ADDRESS, ip.text.toString())
//                binding.connectText.text = getString(R.string.connecting)
//                binding.connectText.setOnClickListener(null)
//                binding.connectText.alpha = 0.5f
//                ECRHelper.connect(bundle)
//            }.show()

    }

    private fun checkConnectionDevice(action : String) {
        if (BaseApp.connected) {
            showToast(R.string.message_select_connect)
            return
        }

        when (action) {
            "bluetooth" -> {
                mode = ECRConstant.Mode.Bluetooth
            }
            "wifi" -> {
                mode = ECRConstant.Mode.WIFI
            }
            "usb" -> {
                mode = ECRConstant.Mode.USB
            }
        }
    }

    private fun sendPaymentToP2(action : String) {
        if (!BaseApp.connected) {
            return
        }

        Executors.newCachedThreadPool().execute {
            val text = if (action == "card") "CMD:C200|AMT:10.39|CCY:USD|TRXID:234567654|TILLID:65434567898" else "CMD:C100|AMT:10.39|CCY:USD|TRXID:5678987|TILLID:5678976"
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            Logger.e(BaseApp.TAG, "size: $bytes.size")
            ECRHelper.send(bytes)
        }
    }
}