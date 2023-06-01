package com.mylibrary

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.mylibrary.network.KessMerchantAPI
import com.mylibrary.network.MyInterceptor
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mylibrary.network.Credential.baseUrl
import com.mylibrary.network.Credential.daikouUrl
import com.mylibrary.network.Credential.servicePartnerConfirmFunTransfer
import com.mylibrary.network.PreOrderData
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class KESSMerchantService {

    interface OnCallBackListener {
        fun onSuccess(resObj: JsonObject?)
        fun onSuccess(resObj: PreOrderData?)
        fun onFailed(message : String)
    }

    companion object {
        fun submitAccessToken(
            isDev: Boolean,
            grantType: String,
            clientId: String,
            clientSecret: String,
            userName: String,
            password: String,
            onCallBackListener : OnCallBackListener
        ) {
            val myInterceptor = MyInterceptor()
            val okHttpClient = OkHttpClient.Builder()
            okHttpClient.addInterceptor(myInterceptor)
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl(isDev))
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient.build())
                .build()
            val api = retrofit.create(KessMerchantAPI::class.java)
            val jsonObject = JsonObject()
            jsonObject.addProperty("grant_type", grantType)
            jsonObject.addProperty("client_id", clientId)
            jsonObject.addProperty(
                "client_secret",
                clientSecret
            )
            jsonObject.addProperty("username", userName)
            jsonObject.addProperty("password", password)
            api.submitRequestToken(jsonObject)?.enqueue(CustomCallback(object : CustomResponseListener {
                override fun onSuccess(resObj: JsonObject) {
                    onCallBackListener.onSuccess(resObj)
                }

                override fun onError(error: String, resCode: Int) {
                    onCallBackListener.onFailed(error)
                }
            }))
        }

        fun submitCreatePreOrder(
            isDev: Boolean,
            token: String,
            service: String,
            signType: String,
            sellerCode: String,
            currency: String,
            totalAmount: String,
            body: String,
            terminalType: String,
            outTradNo: String,
            secretKey: String,
            deviceId: String,
            onCallBackListener : OnCallBackListener
        ) {
            val myInterceptor = MyInterceptor()
            val okHttpClient = OkHttpClient.Builder()
            okHttpClient.addInterceptor(myInterceptor)
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl(isDev))
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient.build())
                .build()
            val api = retrofit.create(KessMerchantAPI::class.java)
            val jsonObject = JsonObject()
            val hashMap = HashMap<String, Any>()
            val bearerToken = String.format("Bearer %s", token)
            hashMap["Authorization"] = bearerToken
            hashMap["processing-device-id"] = deviceId

            jsonObject.addProperty("service", service)
            jsonObject.addProperty("sign_type", signType)
            jsonObject.addProperty("seller_code", sellerCode)
            jsonObject.addProperty("currency", currency)
            jsonObject.addProperty("total_amount", totalAmount)
            jsonObject.addProperty("body", body)
            jsonObject.addProperty("terminal_type", terminalType)
            jsonObject.addProperty("out_trade_no", outTradNo)

            jsonObject.addProperty(
                "sign",
                makeSign(
                    jsonObject,
                    secretKey
                )
            )

            api.submitCreatePreOrder(jsonObject, hashMap)?.enqueue(CustomCallback(object : CustomResponseListener {
                override fun onSuccess(resObj: JsonObject) {
                    if (resObj.has("data") && !resObj["data"].isJsonNull) {
                        onCallBackListener.onSuccess(onGenerateRespondDataObjWs(resObj, PreOrderData::class.java))
                    } else {
                        onCallBackListener.onFailed(resObj["msg"].asString)
                    }
                }

                override fun onError(error: String, resCode: Int) {
                    onCallBackListener.onFailed(error)
                }
            }))
        }


        private fun makeSign(body: JsonObject, secretKey: String?): String {
            val keys: List<String> = getKSort(body)
            val stringBuilder = StringBuilder()
            for (key in keys) {
                val elm = body[key]
                if (key == "sign" || elm.isJsonArray || elm.isJsonNull) {
                    continue
                }
                if (elm.isJsonObject) {
                    continue
                }
                if (elm.isJsonPrimitive) {
                    val jsonPrimitive = elm.asJsonPrimitive
                    if (jsonPrimitive.isBoolean && !jsonPrimitive.asBoolean) {
                        continue
                    }
                    if (jsonPrimitive.isNumber && jsonPrimitive.asNumber.toDouble() == 0.0) {
                        continue
                    }
                    if (jsonPrimitive.isString && TextUtils.isEmpty(jsonPrimitive.asString)) {
                        continue
                    }
                }
                println(" => $key")
                stringBuilder.append(key).append("=").append(elm.asJsonPrimitive)
                val idx = keys.indexOf(key)
                val isAppendAmp = idx != keys.size - 1
                if (isAppendAmp) {
                    stringBuilder.append("&")
                }
            }

            stringBuilder.append("&key=").append(secretKey)
            //Todo fix string
            val data = stringBuilder.toString().replace("[\"]".toRegex(), "")
            //Todo md5
            Log.d("log_data", "makeSign: $data")
            return md5(data)
        }

        private fun getKSort(jsonObject: JsonObject): List<String> {
            val keySort: MutableList<String> = ArrayList()
            for (entry: Map.Entry<String, JsonElement> in jsonObject.entrySet()) {
                keySort.add(entry.key)
            }

            keySort.sortWith { obj: String, anotherString: String? ->
                obj.compareTo(
                    anotherString!!
                )
            }
            return keySort
        }

        private fun md5(s: String): String {
            val MD5 = "MD5"
            try {
                // Create MD5 Hash
                val digest = MessageDigest.getInstance(MD5)
                digest.update(s.toByteArray())
                val messageDigest = digest.digest()

                // Create Hex String
                val hexString = java.lang.StringBuilder()
                for (aMessageDigest in messageDigest) {
                    val h =
                        java.lang.StringBuilder(Integer.toHexString(0xFF and aMessageDigest.toInt()))
                    while (h.length < 2) h.insert(0, "0")
                    hexString.append(h)
                }
                return hexString.toString()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
            return ""
        }

        fun updateFundTransfer(
            isDev: Boolean,
            token: String,
            partnerTransactionId: String,
            preTransferId: String,
            signType: String,
            methodDesc: String,
            bankInfo: String,
            secretKey: String,
            deviceId: String,
            onCallBackListener: OnCallBackListener
        ) {
            val myInterceptor = MyInterceptor()
            val okHttpClient = OkHttpClient.Builder()
            okHttpClient.addInterceptor(myInterceptor)
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl(isDev))
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient.build())
                .build()

            val api = retrofit.create(KessMerchantAPI::class.java)
            val jsonObject = JsonObject()
            val paymentDetails = JsonObject()
            val hashMap = HashMap<String, Any>()
            val bearerToken = String.format("Bearer %s", token)
            hashMap["Authorization"] = bearerToken
            hashMap["processing-device-id"] = deviceId
            jsonObject.addProperty("service", servicePartnerConfirmFunTransfer)
            jsonObject.addProperty("partner_transaction_id", partnerTransactionId)
            jsonObject.addProperty("pre_transfer_id", preTransferId)
            jsonObject.addProperty("sign_type", signType)
            jsonObject.addProperty("seller_code", deviceId)

            paymentDetails.addProperty("method_desc", methodDesc)
            paymentDetails.addProperty("bank_info", bankInfo)


            jsonObject.add("payment_detail", paymentDetails)
            jsonObject.addProperty(
                "sign",
                makeSign(
                    jsonObject,
                    secretKey
                )
            )

            api.submitCreatePreOrder(jsonObject, hashMap)?.enqueue(CustomCallback(object : CustomResponseListener {
                override fun onSuccess(resObj: JsonObject) {
                    if (resObj.has("data") && !resObj["data"].isJsonNull) {
                        onCallBackListener.onSuccess(
                            onGenerateRespondDataObjWs(
                                resObj,
                                PreOrderData::class.java
                            )
                        )
                    } else {
                        onCallBackListener.onFailed(resObj["msg"].asString)
                    }
                }

                override fun onError(error: String, resCode: Int) {
                    onCallBackListener.onFailed(error)
                }
            }))

        }

        fun backResponsePaymentSuccess(hashMap: HashMap<String, Any>, onCallBackListener : OnCallBackListener) {
            val myInterceptor = MyInterceptor()
            val okHttpClient = OkHttpClient.Builder()
            okHttpClient.addInterceptor(myInterceptor)
            val retrofit = Retrofit.Builder()
                .baseUrl(daikouUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient.build())
                .build()

            val api = retrofit.create(KessMerchantAPI::class.java)
            val jsonObject = JsonObject()
            jsonObject.addProperty("status", hashMap["status"].toString())

            api.submitOrderToWeb(jsonObject, hashMap)?.enqueue(CustomCallback(object : CustomResponseListener {
                override fun onSuccess(resObj: JsonObject) {
                    onCallBackListener.onSuccess(resObj)
                }

                override fun onError(error: String, resCode: Int) {
                    onCallBackListener.onFailed(error)
                }
            }))

        }

        fun <T> onGenerateRespondDataObjWs(resObj: JsonObject, clazz: Class<T>): T {
            // Already check has data in request
            val data = resObj["data"].asJsonObject
            return Gson().fromJson(data, clazz)
        }

    }

}