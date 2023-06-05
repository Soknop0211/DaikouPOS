package com.eazy.daikoupos.utils

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.eazy.daikoupos.SelectDeviceModeFragment
import com.eazy.daikoupos.databinding.FragmentShowAlertDialogBinding


class ShowAlertDialog : DialogFragment() {

    private var title: String? = null
    private var message: String? = null
    private var onCallBackListener: OnCallBackListener ? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString("title")
            message = it.getString("message")
        }

        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentShowAlertDialogBinding.inflate(inflater, container, false)

        binding.msg.text = message
        binding.header.text = title

        binding.txtOk.setOnClickListener {
            if (onCallBackListener != null) {
                onCallBackListener!!.onCallBack("")
            }
            dismiss()
        }

        if (dialog != null && dialog!!.window != null) {
            dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setWidthPercent(40)
    }

    companion object {
        @JvmStatic
        fun newInstance(title: String, message: String) =
            ShowAlertDialog().apply {
                arguments = Bundle().apply {
                    putString("title", title)
                    putString("message", message)
                }
            }
    }

    fun initListener (onCallBackListener: OnCallBackListener) {
        this.onCallBackListener = onCallBackListener
    }

    interface OnCallBackListener {
        fun onCallBack(bluetoothAdd : String)
    }
}

fun DialogFragment.setWidthPercent(percentage: Int) {
    val percent = percentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * percent
    dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
}