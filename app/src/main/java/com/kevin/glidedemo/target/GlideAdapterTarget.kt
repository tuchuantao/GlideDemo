package com.kevin.glidedemo.target

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition

/**
 * 用于处理Glide加载的bitmap被回收
 *
 * Created by Jiwei Yuan on 18-12-5.
 */
class GlideAdapterTarget(private var imageView: ImageView?) : DrawableImageViewTarget(imageView) {

    init {
        if (imageView == null) {
            throw IllegalArgumentException("You must pass in a non null View")
        }
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        if (resource is BitmapDrawable) {
            var bitmap: Bitmap? = resource.bitmap
            if (bitmap != null && bitmap.isRecycled) {
                bitmap = Bitmap.createBitmap(bitmap)
            }
            imageView?.setImageBitmap(bitmap)
            return
        }
        imageView?.setImageDrawable(resource)
    }
}
