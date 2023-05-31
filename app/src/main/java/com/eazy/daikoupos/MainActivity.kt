package com.eazy.daikoupos

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.eazy.daikoupos.databinding.ActivityMainBinding
import com.eazy.daikoupos.databinding.SelectConnectionModeLayoutBinding
import com.eazy.daikoupos.ecr.ECRHelper
import com.eazy.daikoupos.model.ResponseData
import com.eazy.daikoupos.utils.SunmiPrintHelper
import com.eazy.daikoupos.utils.Utils
import com.eazy.daikoupos.utils.payment.Logger
import com.google.gson.Gson
import com.pos.connection.bridge.binder.ECRConstant
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors


class MainActivity : BaseActivity() {

    private var getLinkUrl = "https://pos.daikou.asia"
    private var webView: WebView? = null

    private val bluetoothList = HashMap<String, String>()
    private var bluetoothAddress = ""

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
                    mode = ECRConstant.Mode.USB
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
                        "card_payment" -> {
                            results.mData?.mTotalPayment?.let { sendPaymentToP2(it) }
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
                                            connectWithSATHAPANA()
                                        } else {
                                            showToast("Please select bluetooth address connection .")
                                        }
                                    } else {
                                        connectWithSATHAPANA()
                                    }
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

    private fun displayBitmap(imgBase64: String?) {
        bitmap = imgBase64?.let { it1 -> Utils.passImageToAndroid(it1) }

        // Init printer D2
        val sunmiPrintHelper = SunmiPrintHelper.getInstance()
        sunmiPrintHelper.printBitmap(bitmap, 0)
        if (sunmiPrintHelper.sunmiPrinterService != null) {
            sunmiPrintHelper.sunmiPrinterService.lineWrap(0, null)
            sunmiPrintHelper.sunmiPrinterService.lineWrap(1, null)
            sunmiPrintHelper.sunmiPrinterService.lineWrap(0, null)
            sunmiPrintHelper.sunmiPrinterService.cutPaper( null)
        }
        sunmiPrintHelper.feedPaper()
    }

    @SuppressLint("MissingPermission")
    private fun connectWithSATHAPANA() {
        if (!BaseApp.connected){
            val bundle = Bundle()
            bundle.putString(ECRConstant.Configuration.MODE, mode)
            bundle.putString(ECRConstant.Configuration.TYPE, if (BaseApp.server) ECRConstant.Type.MASTER else ECRConstant.Type.SLAVE)

            if (mode == ECRConstant.Mode.Bluetooth) {
                Logger.e(BaseApp.TAG, "bluetoothAddress: $bluetoothAddress")
                bundle.putString(ECRConstant.Configuration.BLUETOOTH_MAC_ADDRESS, bluetoothAddress)
            }

            val ecrHelper = ECRHelper
            ecrHelper.connect(bundle)
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
            onCallResponsePaymentToWeb(text)
        }

        ECRHelper.bindECRService()
    }

    private fun showConnectStatus() {
        val text = if (BaseApp.connected) getString(R.string.connected) else getString(R.string.disconnected)
        Utils.logDebug(BaseApp.TAG, text)
    }

    private fun sendPaymentToP2(totalAmount : String) {
        if (!BaseApp.connected) {
            showToast("Please connect device mode !")
            return
        }

        Executors.newCachedThreadPool().execute {
            val text = "CMD:C200|AMT:${totalAmount}|CCY:USD|TRXID:23874567654|TILLID:654345678987668" // test : text will get from web invoice
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            Logger.e(BaseApp.TAG, "size: $bytes.size")
            ECRHelper.send(bytes)
        }
    }

    private fun onCallResponsePaymentToWeb(text : String) {
        var onResponse = text
        when {
            text.contains("RESCODE:000") -> {
                onResponse = "Payment Successfully !"
            }
            text.contains("RESCODE:099") -> {
                onResponse = "Payment Failed !"
            }
            text.contains("RESCODE:098") -> {
                onResponse = "Payment Error !"
            }
        }

        showToast(onResponse)
    }

    private fun customOption(mContext : Context) {
        val binding: SelectConnectionModeLayoutBinding = SelectConnectionModeLayoutBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(mContext)
        dialog.setView(binding.root)
        dialog.setCancelable(false)
        val alertDialog = dialog.show()

        val mList = switchBluetooth()
        binding.recyclerView.visibility = View.VISIBLE
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(mContext)
            adapter = BluetoothItemAdapter(bluetoothAddress, mList, object : BluetoothItemAdapter.OnClickCallBackLister {
                @SuppressLint("NotifyDataSetChanged")
                override fun onClickCallBack(bluetooth: String?) {
                    bluetoothAddress = bluetoothList[bluetooth] ?: ""
                    binding.recyclerView.adapter!!.notifyDataSetChanged()
                }
            })
        }

        connectionType = "bluetooth"
        checkConnectionDevice(connectionType)
        binding.rOption.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rBluetooth) {
                connectionType = "bluetooth"
                binding.recyclerView.visibility = View.VISIBLE
            } else if (checkedId == R.id.rUSB) {
                connectionType = "usb"
                binding.recyclerView.visibility = View.GONE
            }
            checkConnectionDevice(connectionType)
        }

        binding.txtOk.setOnClickListener {
            if (connectionType == "bluetooth") {
                if (bluetoothAddress != ""){
                    connectWithSATHAPANA()
                    alertDialog.dismiss()
                } else {
                    showToast("Please select device connection")
                }
            } else {
                connectWithSATHAPANA()
                alertDialog.dismiss()
            }
        }

        binding.txtCancel.setOnClickListener {
            alertDialog.dismiss()
        }

    }

    @SuppressLint("MissingPermission")
    private fun switchBluetooth() : List<String> {
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled) {
                bluetoothList.clear()
                val bondedDevices = bluetoothAdapter.bondedDevices
                if (bondedDevices != null && bondedDevices.size > 0) {
                    for (bluetoothDevice in bondedDevices) {
                        val name = bluetoothDevice.name
                        val address = bluetoothDevice.address
                        Logger.e(BaseApp.TAG, "name: $name")
                        Logger.e(BaseApp.TAG, "address: $address")
                        if (address == "00:11:22:33:44:55") continue
                        bluetoothList["$name - $address"] = address
                    }
                }
                if (bluetoothList.size <= 0) {
                    showToast(R.string.message_bluetooth_not_find_bonded)
                }
            } else {
                showToast(R.string.message_bluetooth_disable)
            }
        } else {
            showToast(R.string.message_bluetooth_not_support)
        }
        return bluetoothList.keys.toList()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            finish()
        }
    }

}