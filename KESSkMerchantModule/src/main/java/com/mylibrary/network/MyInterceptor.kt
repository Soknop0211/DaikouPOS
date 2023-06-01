package com.mylibrary.network

import okhttp3.Interceptor
import okhttp3.Response

class MyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
        try {
            builder.addHeader("Content-Type", "application/json")
           // builder.addHeader("X-Parse-REST-API-Key", "kesschat_dev")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val requestBuilder = builder.build()
        return chain.proceed(requestBuilder)
    }
}