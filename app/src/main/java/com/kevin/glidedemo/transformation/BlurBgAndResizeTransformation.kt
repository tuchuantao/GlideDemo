package com.kevin.glidedemo.transformation

import android.content.Context
import android.graphics.*
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import jp.wasabeef.glide.transformations.BitmapTransformation
import jp.wasabeef.glide.transformations.internal.FastBlur
import java.security.MessageDigest

/**
 * Create by Kevin-Tu on 2019/3/25.
 */
class BlurBgAndResizeTransformation() : BitmapTransformation() {

    private val VERSION = 1
    private val ID = "com.kevin.glidedemo.transformation.BlurBgAndResizeTransformation.$VERSION"

    private val MAX_RADIUS = 25

    private var radius: Int = MAX_RADIUS

    constructor(radius: Int) : this() {
        this.radius = radius
    }

    override fun transform(
        context: Context,
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val width = toTransform.width
        val height = toTransform.height

        return drawTransformBitmap(pool, toTransform, width, height, outWidth, outHeight)
    }

    private fun drawTransformBitmap(
        pool: BitmapPool,
        toTransform: Bitmap,
        width: Int,
        height: Int,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        var bitmap = pool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        bitmap.setHasAlpha(true)

        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true

        // 绘制模糊背景
        // 1、选取合适拉伸填充的中心区域
        var srcRect = Rect()
        var realHeight = outHeight.toFloat() / outWidth.toFloat() * width
        if (realHeight <= height) {
            srcRect.left = 0
            srcRect.right = width
            srcRect.top = ((height - realHeight) / 2).toInt()
            srcRect.bottom = srcRect.top + realHeight.toInt()
        } else {
            var realWith = outWidth.toFloat() / outHeight.toFloat() * height
            srcRect.left = ((width - realWith) / 2).toInt()
            srcRect.right = srcRect.left + realWith.toInt()
            srcRect.top = 0
            srcRect.bottom = height
        }
        canvas.drawBitmap(toTransform, srcRect, RectF(0F, 0F, outWidth.toFloat(), outHeight.toFloat()), paint)
        // 2、模糊处理
        bitmap = FastBlur.blur(bitmap, radius, true)


        // 将清晰图片绘制在正中心位置
        val dst = RectF()
        if (width < outWidth) {
            dst.left = (outWidth - width) / 2F
            dst.right = dst.left + width
        } else {
            dst.left = 0F
            dst.right = outWidth.toFloat()
        }
        if (height < outHeight) {
            dst.top = (outHeight - height) / 2F
            dst.bottom = dst.top + height
        } else {
            dst.top = 0F
            dst.bottom = outHeight.toFloat()
        }
        canvas.drawBitmap(toTransform, null, dst, paint)

        return bitmap
    }

    override fun toString(): String {
        return "BlurBgAndResizeTransformation(radius=$radius)"
    }

    override fun equals(other: Any?): Boolean {
        return other is BlurBgAndResizeTransformation
    }

    override fun hashCode(): Int {
        return ID.hashCode() + radius * 1000
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((ID + radius).toByteArray(Key.CHARSET))
    }
}