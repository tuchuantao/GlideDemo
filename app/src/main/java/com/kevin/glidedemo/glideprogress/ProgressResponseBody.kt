package com.kevin.glidedemo.glideprogress

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*

/**
 * Create by Kevin-Tu on 2019/5/20.
 */
class ProgressResponseBody private constructor() : ResponseBody() {

    private var bufferedSource: BufferedSource? = null
    private var responseBody: ResponseBody? = null
    private var listener: ProgressListener? = null

    constructor(url: String, responseBody: ResponseBody?): this() {
        this.responseBody = responseBody
        listener = ProgressInterceptor.LISTENER_MAP[url]
    }


    override fun contentType(): MediaType? {
        return responseBody?.contentType()
    }

    override fun contentLength(): Long {
        return responseBody?.contentLength() ?: 0
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(ProgressSource(responseBody?.source()))
        }
        return bufferedSource!!
    }

    private inner class ProgressSource internal constructor(source: Source?) : ForwardingSource(source) {

        internal var totalBytesRead: Long = 0
        internal var currentProgress: Int = 0

        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            val fullLength = responseBody?.contentLength() ?: 0L
            if (bytesRead == -1L) {
                totalBytesRead = fullLength
            } else {
                totalBytesRead += bytesRead
            }
            val progress = (100f * totalBytesRead / fullLength).toInt()
            android.util.Log.d("kevin", "read()  download progress is $progress")
            if (listener != null && progress != currentProgress) {
                listener!!.onProgress(progress)
            }
            if (listener != null && totalBytesRead == fullLength) {
                listener!!.onCompleted()
                listener = null
            }

            currentProgress = progress
            return bytesRead
        }
    }
}