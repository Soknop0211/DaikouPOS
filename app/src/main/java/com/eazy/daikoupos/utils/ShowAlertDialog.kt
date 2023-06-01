package com.eazy.daikoupos.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.eazy.daikoupos.databinding.FragmentShowAlertDialogBinding


class ShowAlertDialog : DialogFragment() {

    private var message: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            message = it.getString("message")
        }

        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentShowAlertDialogBinding.inflate(inflater, container, false)

        binding.msg.text = message

        binding.txtOk.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(message: String) =
            ShowAlertDialog().apply {
                arguments = Bundle().apply {
                    putString("message", message)
                }
            }
    }
}