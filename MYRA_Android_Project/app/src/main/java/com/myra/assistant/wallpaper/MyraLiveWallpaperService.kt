package com.myra.assistant.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder

/**
 * Android WallpaperService host for the MYRA AURA live wallpaper.
 * The framework Engine is an actual inner class of this service, while the
 * renderer remains a helper that owns the animation and drawing implementation.
 */
class MyraLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = MyraWallpaperInnerEngine()

    inner class MyraWallpaperInnerEngine : Engine() {
        private val renderer = MyraWallpaperEngine(this@MyraLiveWallpaperService)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            renderer.onCreate(surfaceHolder)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            renderer.onSurfaceChanged(holder, format, width, height)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            renderer.onVisibilityChanged(visible)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            renderer.onSurfaceDestroyed(holder)
            super.onSurfaceDestroyed(holder)
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            renderer.onTouchEvent(event)
        }

        override fun onDestroy() {
            renderer.onVisibilityChanged(false)
            super.onDestroy()
        }
    }
}
