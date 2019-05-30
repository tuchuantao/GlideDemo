package com.kevin.glidedemo.glidemodule

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import okhttp3.OkHttpClient
import com.kevin.glidedemo.glideprogress.ProgressInterceptor
import java.io.InputStream


/**
 * Create by Kevin-Tu on 2019/5/20.
 */
@GlideModule
class GlideConfigModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // 更改解码方式，让图片更加清晰，Glide默认的解码格式是RGB_565,
        builder.setDefaultRequestOptions(RequestOptions.formatOf(DecodeFormat.PREFER_ARGB_8888))
        //设置内存大小
        builder.setMemoryCache(LruResourceCache(1024 * 1024 * 100)) // 100M

        //设置图片缓存大小
        builder.setBitmapPool(LruBitmapPool(1024 * 1024 * 50))

        /**
         * 设置磁盘缓存大小
         */
        // 内部缓存目录  data/data/packageName/DiskCacheName
        builder.setDiskCache(
            InternalCacheDiskCacheFactory(context, "GlideDemo", 1024 * 1024 * 100)
        )
        // 外部磁盘SD卡
        /*builder.setDiskCache(
            ExternalPreferredCacheDiskCacheFactory(context, "GlideDemo", 1024 * 1024 * 10)
        )*/
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        //添加拦截器到Glide
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(ProgressInterceptor)
        val okHttpClient = builder.build()

        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient))
    }

    /**
     * 禁用清单解析
     */
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}