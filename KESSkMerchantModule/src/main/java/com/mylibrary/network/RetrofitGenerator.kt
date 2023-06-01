package com.mylibrary.network

import com.mylibrary.network.Credential.baseUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitGenerator {

    fun createService(mToken: String, deviceId: String): KessMerchantAPI {
        val authToken = String.format("Bearer %s", mToken)
        val httpClient = OkHttpClient.Builder()

        val myInterceptor = MyInterceptor()
        val okHttpClient = OkHttpClient.Builder()
        okHttpClient.addInterceptor(myInterceptor)

        httpClient.addInterceptor { chain: Interceptor.Chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("Authorization", authToken)
                .header("processing-device-id", deviceId)
                .method(original.method(), original.body())
            val request = builder.build()
            chain.proceed(request)
        }
        httpClient.readTimeout(30, TimeUnit.SECONDS).connectTimeout(30, TimeUnit.SECONDS)
        val builder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient.build())
            .baseUrl(baseUrl(true))
        builder.client(httpClient.build())
        val retrofit = builder.build()
        return retrofit.create(KessMerchantAPI::class.java)
    }
}