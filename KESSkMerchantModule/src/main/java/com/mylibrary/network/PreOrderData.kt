package com.mylibrary.network

import com.google.gson.annotations.SerializedName

data class PreOrderData(
    val token: String? = null,
    @SerializedName("transaction_id")
    val transactionId: String? = null,
) : java.io.Serializable

