package com.kevin.glidedemo

import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.kevin.glidedemo.databinding.ActivityMainBinding
import com.kevin.glidedemo.glidemodule.GlideApp
import com.kevin.glidedemo.glideprogress.ProgressInterceptor
import com.kevin.glidedemo.glideprogress.ProgressListener
import com.kevin.glidedemo.transformation.BlurBgAndResizeTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val imgUrl1 =
        "https://kimg.cdn.video.9ddm.com/kvideo-pic/9FF9AF6B7452D3967A0A211CEEC0F638_1546495631369@base@tag=imgScale&m=0&w=640"
    private val imgUrl2 =
        "https://kimg.cdn.video.9ddm.com/kvideo-pic/17816BF7BAD03C03B2A2ACA858BB4058_1554447948039@base@tag=imgScale&m=0&w=640"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        initView()
    }

    private fun initView() {

        ProgressInterceptor.addListener(imgUrl1, object : ProgressListener {
            override fun onProgress(progress: Int) {
                android.util.Log.v("kevinTu", "imgUrl1 progress: " + progress)
            }

            override fun onCompleted() {
                ProgressInterceptor.removeListener(imgUrl1)
            }
        })

        ProgressInterceptor.addListener(imgUrl2, object : ProgressListener {
            override fun onProgress(progress: Int) {
                android.util.Log.i("kevinTu", "imgUrl2 progress: " + progress)
            }

            override fun onCompleted() {
                ProgressInterceptor.removeListener(imgUrl2)
            }
        })

        initImgOne()
        initImgTwo()
        initImgThree()
        initImgFour()
        initImgFive()
        initImgSix()
    }

    /**
     * 横图显示
     */
    private fun initImgOne() {
        Glide.with(this)
            .load(imgUrl1)
            .apply(RequestOptions.priorityOf(Priority.HIGH))
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
            .apply(RequestOptions.placeholderOf(R.drawable.img_place_holder))
            .apply(RequestOptions().fitCenter())
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false // 标记是否处理onResourceReady事件，返回true，这target将接收不到改事件
                }

            })
            .into(binding.img1)
    }

    private fun initImgTwo() {
        Glide.with(this)
            .load(imgUrl1)
            .apply(RequestOptions.priorityOf(Priority.HIGH))
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
            .apply(RequestOptions.placeholderOf(R.drawable.img_place_holder))
            .apply(RequestOptions().fitCenter())
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return true
                }

            })
            .into(binding.img2)
    }

    /**
     * 横图加全圆角
     */
    private fun initImgThree() {
        GlideApp.with(this)
            .load(imgUrl1)
            .apply(RequestOptions.priorityOf(Priority.HIGH))
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
            .apply(RequestOptions.placeholderOf(R.drawable.img_place_holder))
            .apply(RequestOptions().fitCenter())
            .apply(
                RequestOptions.bitmapTransform(
                    RoundedCornersTransformation(
                        30, 0, RoundedCornersTransformation.CornerType.ALL
                    )
                )
            )
            .into(binding.img3)
    }

    /**
     * 竖图不做任何处理
     */
    private fun initImgFour() {
        GlideApp.with(this)
            .load(imgUrl2)
            .apply(RequestOptions.priorityOf(Priority.HIGH))
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
            .apply(RequestOptions.placeholderOf(R.drawable.img_place_holder))
            .apply(RequestOptions().fitCenter())
            .apply(
                RequestOptions.bitmapTransform(
                    RoundedCornersTransformation(
                        30, 0, RoundedCornersTransformation.CornerType.ALL
                    )
                )
            )
            .into(binding.img4)
    }

    /**
     * 竖图按比例显示在正中间，并加上模糊背景
     */
    private fun initImgFive() {
        GlideApp.with(this)
            .load(imgUrl2)
            .apply(RequestOptions.priorityOf(Priority.HIGH))
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
            .apply(RequestOptions.placeholderOf(R.drawable.img_place_holder))
            .apply(RequestOptions().fitCenter())
            .apply(
                RequestOptions.bitmapTransform(
                    BlurBgAndResizeTransformation(30)
                )
            )
            .into(binding.img5)
    }

    /**
     * 竖图按比例显示在正中间，并加上模糊背景，再加上全圆角
     */
    private fun initImgSix() {
        GlideApp.with(this)
            .load(imgUrl2)
            .apply(RequestOptions.priorityOf(Priority.HIGH))
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
            .apply(RequestOptions.placeholderOf(R.drawable.img_place_holder))
            .apply(RequestOptions().fitCenter())
            .apply(
                RequestOptions.bitmapTransform(
                    MultiTransformation(
                        BlurBgAndResizeTransformation(30),
                        RoundedCornersTransformation(
                            30, 0, RoundedCornersTransformation.CornerType.ALL
                        )
                    )
                )
            )
            .into(binding.img6)
    }
}
