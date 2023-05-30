package com.eazy.daikoupos.model

import com.google.gson.annotations.SerializedName

data class ResponseData(
    @SerializedName("type")
    val mType: String? = null,
    @SerializedName("data")
    val mData: Data? = null
) : java.io.Serializable

data class Data (@SerializedName("base64")
                 val mBase64: String? = null,
                 @SerializedName("total_payment")
                 val mTotalPayment: String? = null
): java.io.Serializable

