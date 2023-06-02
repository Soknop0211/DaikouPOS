package com.eazy.daikoupos

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.eazy.daikoupos.BaseApp.Companion.connectionType
import com.eazy.daikoupos.databinding.SelectConnectionModeLayoutBinding
import com.eazy.daikoupos.extension.showToast
import com.eazy.daikoupos.utils.payment.Logger

class SelectDeviceModeFragment : DialogFragment() {

    private val bluetoothList = HashMap<String, String>()
    private var bluetoothAddress = ""
    private var mBluetoothAddress = ""
    private lateinit var onCallBackListener: OnCallBackListener
    private lateinit var binding: SelectConnectionModeLayoutBinding
    private var mList : List<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AlertShape)
        isCancelable = false
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SelectConnectionModeLayoutBinding.inflate(inflater, container, false)

        mList = switchBluetooth()
        initAdapter()

        MainActivity.checkConnectionDevice(connectionType)
        if (connectionType.equals("bluetooth", true)) {
            binding.rBluetooth.isChecked = true
        } else if (connectionType.equals("usb", true)) {
            binding.rUSB.isChecked = true
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.rBluetooth.isChecked = true
        }

        binding.rOption.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rBluetooth) {
                connectionType = "bluetooth"
                binding.recyclerView.visibility = View.VISIBLE
            } else if (checkedId == R.id.rUSB) {
                connectionType = "usb"
                binding.recyclerView.visibility = View.GONE
                binding.notFoundBlTv.visibility = View.GONE
            }
            MainActivity.checkConnectionDevice(connectionType)
        }

        binding.txtOk.setOnClickListener {
            if (connectionType == "bluetooth") {
                if (bluetoothAddress != ""){
                    onCallBackListener.onCallBack(bluetoothAddress)
                    dismiss()
                } else {
                    if (mList.isNotEmpty()) {
                        requireContext().showToast("Please select bluetooth address connection .")
                    } else {
                        requireContext().showToast("Not found bluetooth address connection .")
                    }
                }
            } else {
                onCallBackListener.onCallBack("")
                dismiss()
            }
        }

        binding.txtCancel.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    private fun initAdapter() {
        val mAdapter = BluetoothItemAdapter(mBluetoothAddress, mList, object : BluetoothItemAdapter.OnClickCallBackLister {
            @SuppressLint("NotifyDataSetChanged")
            override fun onClickCallBack(bluetooth: String?) {
                bluetoothAddress = bluetoothList[bluetooth] ?: ""
                if (bluetooth != null)  mBluetoothAddress = bluetooth

                initAdapter()
            }
        })

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mAdapter
        }
    }

    @SuppressLint("MissingPermission")
    private fun switchBluetooth() : List<String> {
        val bluetoothManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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

                binding.notFoundBlTv.visibility = if (bluetoothList.size > 0) View.GONE else View.VISIBLE
                binding.recyclerView.visibility = if (bluetoothList.size > 0) View.VISIBLE else View.GONE

            } else {
                requireContext().showToast(R.string.message_bluetooth_disable)
            }
        } else {
            requireContext().showToast(R.string.message_bluetooth_not_support)
        }

        return bluetoothList.keys.toList()
    }

    fun initListener (onCallBackListener: OnCallBackListener) {
        this.onCallBackListener = onCallBackListener
    }

    interface OnCallBackListener {
        fun onCallBack(bluetoothAdd : String)
    }
}