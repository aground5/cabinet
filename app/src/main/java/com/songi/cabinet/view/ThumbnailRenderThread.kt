package com.songi.cabinet.view

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.airbnb.lottie.LottieAnimationView
import com.songi.cabinet.R
import java.io.File
import java.lang.Exception

class ThumbnailRenderThread (list: MutableList<Pair<LottieAnimationView, File>>) : Thread() {
    private var TAG = "ThumbnailRenderThread"

    var list = list; set(value) {
        field = value
        isRendered = BooleanArray(field.size) {false}
    }
    var isRendered = BooleanArray(list.size) {false}

    override fun run() {
        while(true)
        {
            if (isFinished() || interrupted()) {
                Log.e(TAG, "Thread Interrupted.")
                break
            }
            for (i in list.indices) {
                if (!isRendered[i]) {
                    if (interrupted()) {
                        break
                    }
                    if (isVisible(list[i].first)) {
                        val bitmap = BitmapFactory.decodeStream((list[i].second).inputStream())
                        (list[i].first).post(
                            Runnable {
                                list[i].first.setImageBitmap(bitmap)
                            }
                        )
                        isRendered[i] = true
                        Log.d(TAG, "${list[i].second.name} rendered.")
                    }
                } else {
                    Log.d(TAG, "${list[i].second.name} is already rendered.")
                }
            }
            try {
                sleep(15)
            } catch (e: InterruptedException) {
                //Log.e(TAG, "Thread Interrupted.")
            }
        }
    }

    fun isVisible(view: View?): Boolean {
        if (view == null) {
            return false
        }
        if (!view.isShown) {
            return false
        }
        val actualPosition = Rect()
        view.getGlobalVisibleRect(actualPosition)
        val screen = Rect(0, 0, Resources.getSystem().getDisplayMetrics().widthPixels, Resources.getSystem().getDisplayMetrics().heightPixels)
        return actualPosition.intersect(screen)
    }

    fun isFinished() : Boolean {
        for (i in isRendered) {
            if (!i) {
                return false
            }
        }
        return true
    }
}