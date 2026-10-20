package com.myra.assistant.wallpaper

import android.service.wallpaper.WallpaperService

/**
 * Official Android WallpaperService for MYRA AI Live Wallpaper.
 * Bound by the Android OS when the user sets MYRA as system Home / Lock screen Live Wallpaper.
 * Instantiates and manages the lifecycle of MyraWallpaperEngine.
 */
class MyraLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return MyraWallpaperEngine(this)
    }

    /**
     * Inner class engine alias ensuring full backward-compatibility for any reflective
     * or inner-class references to MyraLiveWallpaperService.MyraWallpaperInnerEngine.
     */
    inner class MyraWallpaperInnerEngine : MyraWallpaperEngine(this@MyraLiveWallpaperService)
}
