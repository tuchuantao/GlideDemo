package com.kevin.glidedemo.glideprogress

/**
 * Create by Kevin-Tu on 2019/5/20.
 */
interface ProgressListener {

    fun onProgress(progress: Int)

    fun onCompleted()
}