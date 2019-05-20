package com.kevin.glidedemo.glideprogress

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Create by Kevin-Tu on 2019/5/20.
 */
object ProgressInterceptor : Interceptor {

    val LISTENER_MAP: MutableMap<String, ProgressListener> = HashMap()

    /**
     * 注册下载监听
     */
    fun addListener(url: String, listener: ProgressListener) {
        LISTENER_MAP[url] = listener
    }

    /**
     * 取消注册下载监听
     */
    fun removeListener(url: String) {
        LISTENER_MAP.remove(url)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val url = request.url().toString()
        val body = response.body()
        return response.newBuilder().body(ProgressResponseBody(url, body)).build()
    }
}