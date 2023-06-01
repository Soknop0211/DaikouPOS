package com.mylibrary.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface KessMerchantAPI {

    @POST("oauth/token")
    fun submitRequestToken(@Body jsonObject: JsonObject): Call<JsonElement>?

    @POST("api/mch/v2/gateway")
    fun submitCreatePreOrder(
        @Body jsonObject: JsonObject,
        @HeaderMap headerMap: HashMap<String, Any>?,
    ): Call<JsonElement>?

    @POST("api/notification/alert")
    fun submitOrderToWeb(
        @Body jsonObject: JsonObject,
        @HeaderMap headerMap: HashMap<String, Any>?,
    ): Call<JsonElement>?

}