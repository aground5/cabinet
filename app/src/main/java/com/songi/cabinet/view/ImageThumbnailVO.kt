package com.songi.cabinet.view

import com.airbnb.lottie.LottieAnimationView
import kotlin.math.abs

class ImageThumbnailVO (var name: String,
                        var absolutePath: String,
                        var isProcessed: Boolean,
                        var view: LottieAnimationView) {
    private var TAG = "ImageThumbnailVO"
}