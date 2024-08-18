package com.l3s.dronegpt.network

import dji.sampleV5.aircraft.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val BASE_URL = "https://api.openai.com/"
    private const val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY

    private var okHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .addInterceptor {chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Bearer $OPENAI_API_KEY")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }.build()

    private val client = Retrofit
        .Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun getInstance() : Retrofit {
        return client
    }

}