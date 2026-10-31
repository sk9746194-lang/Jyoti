package com.myra.assistant.tools

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

/**
 * Real Android App Launcher using PackageManager, Hardware Intents, and Category Resolvers.
 * Verifies installation before launch and returns an honest CommandResult.
 */
class AppLauncher(private val context: Context) {

    private val tag = "MYRA_AppLauncher"

    // Primary package IDs for known popular apps
    private val knownAppPackages = mapOf(
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "youtube" to listOf("com.google.android.youtube"),
        "youtube_music" to listOf("com.google.android.apps.youtube.music"),
        "chrome" to listOf("com.android.chrome", "org.chromium.chrome"),
        "spotify" to listOf("com.spotify.music"),
        "camera" to listOf(
            "com.google.android.GoogleCamera",
            "com.sec.android.app.camera",
            "com.android.camera",
            "com.android.camera2",
            "com.oneplus.camera",
            "com.oppo.camera"
        ),
        "settings" to listOf("com.android.settings"),
        "gallery" to listOf(
            "com.google.android.apps.photos",
            "com.sec.android.gallery3d",
            "com.android.gallery3d",
            "com.miui.gallery",
            "com.oneplus.gallery"
        ),
        "phone" to listOf(
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.dialer",
            "com.android.phone"
        ),
        "calculator" to listOf(
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.sec.android.app.popupcalculator"
        ),
        "maps" to listOf("com.google.android.apps.maps"),
        "instagram" to listOf("com.instagram.android", "com.instagram.lite"),
        "telegram" to listOf("org.telegram.messenger", "org.thunderdog.challegram"),
        "facebook" to listOf("com.facebook.katana", "com.facebook.lite", "com.facebook.android"),
        "twitter" to listOf("com.twitter.android"),
        "gmail" to listOf("com.google.android.gm")
    )

    /**
     * Attempts to launch an app by target key or generic name.
     * Returns an honest CommandResult (SUCCESS, NOT_INSTALLED, FAILED, etc.)
     */
    fun launchAppByName(name: String): CommandResult {
        val query = name.lowercase().trim()
        val pm = context.packageManager

        Log.d(tag, "MYRA_ACTION: OPEN_APP, target='$query'")

        // 1. Camera special hardware intent
        if (query == "camera" || query == "कैमरा") {
            return launchCamera()
        }

        // 2. Settings special standard intent
        if (query == "settings" || query == "setting" || query == "सेटिंग्स") {
            return launchSettings()
        }

        // 3. Phone / Dialer special standard intent
        if (query == "phone" || query == "dialer" || query == "फ़ोन" || query == "डायलर") {
            return launchDialer()
        }

        // 4. Gallery intent
        if (query == "gallery" || query == "photos" || query == "गैलरी") {
            return launchGallery()
        }

        // 5. YouTube Music explicit query
        if (query == "youtube music" || query == "yt music" || query == "youtube_music" || query == "यूट्यूब म्यूजिक") {
            return launchExplicitPackage("com.google.android.apps.youtube.music", "YouTube Music")
        }

        // 6. Check in known packages map
        val candidatePackages = knownAppPackages[query]
        if (!candidatePackages.isNullOrEmpty()) {
            for (pkg in candidatePackages) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    return try {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        Log.d(tag, "MYRA_RESULT: SUCCESS, launched package=$pkg")
                        CommandResult.success("${formatAppName(query)} खोल दिया।", target = query, detail = pkg)
                    } catch (e: Exception) {
                        Log.e(tag, "MYRA_RESULT: FAILED to launch $pkg", e)
                        CommandResult.failed("${formatAppName(query)} खोलने में त्रुटि हुई: ${e.message}", target = query, detail = e.message ?: "")
                    }
                }
            }

            // 6b. Fallback intent resolvers for YouTube, Chrome, etc.
            if (query == "youtube") {
                try {
                    val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        setPackage("com.google.android.youtube")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (ytIntent.resolveActivity(pm) != null) {
                        context.startActivity(ytIntent)
                        return CommandResult.success("YouTube खोल दिया।", target = query)
                    }
                } catch (e: Exception) {}
            }

            if (query == "chrome" || query == "browser") {
                return launchDefaultBrowser()
            }

            // Known app is not installed on this Android device/emulator
            Log.w(tag, "MYRA_RESULT: NOT_INSTALLED for target=$query")
            return CommandResult.notInstalled("${formatAppName(query)} इस फोन में installed नहीं है।", target = query)
        }

        // 7. Dynamic search in all installed launcher activities
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = pm.queryIntentActivities(mainIntent, 0)
            for (app in resolvedApps) {
                val appLabel = app.loadLabel(pm).toString().lowercase()
                if (appLabel.contains(query) || query.contains(appLabel)) {
                    val pkgName = app.activityInfo.packageName
                    val intent = pm.getLaunchIntentForPackage(pkgName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        Log.d(tag, "MYRA_RESULT: SUCCESS, dynamically matched $appLabel ($pkgName)")
                        return CommandResult.success("${app.loadLabel(pm)} खोल दिया।", target = query, detail = pkgName)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error querying launcher activities", e)
        }

        // Chrome fallback via browser intent
        if (query.contains("chrome") || query.contains("browser")) {
            return launchDefaultBrowser()
        }

        Log.w(tag, "MYRA_RESULT: NOT_FOUND for target=$query")
        return CommandResult.notInstalled("$name इस फोन में नहीं मिला।", target = query)
    }

    /**
     * Real music player launcher.
     * Looks for installed music apps or sends Android standard music intent.
     * Honestly reports that the player is opened and asks what song to play.
     */
    fun launchMusicPlayer(): CommandResult {
        Log.d(tag, "MYRA_ACTION: LAUNCH_MUSIC_PLAYER")
        val pm = context.packageManager

        // Priority list of popular music player apps in India / Global
        val musicApps = listOf(
            Pair("com.spotify.music", "Spotify"),
            Pair("com.google.android.apps.youtube.music", "YouTube Music"),
            Pair("com.jio.media.jiobeats", "JioSaavn"),
            Pair("com.bsbportal.music", "Wynk Music"),
            Pair("com.gaana", "Gaana"),
            Pair("com.sec.android.app.music", "Samsung Music"),
            Pair("com.apple.android.music", "Apple Music")
        )

        for ((pkg, name) in musicApps) {
            if (isPackageInstalled(pm, pkg)) {
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    try {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        Log.d(tag, "MYRA_RESULT: SUCCESS, launched music player $name ($pkg)")
                        return CommandResult.success("$name खोल रही हूँ। कौन सा गाना चलाऊँ?", target = "music", detail = pkg)
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to launch $pkg", e)
                    }
                }
            }
        }

        // Fallback to standard Android MediaStore / Category music intent
        try {
            val musicIntent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (musicIntent.resolveActivity(pm) != null) {
                context.startActivity(musicIntent)
                Log.d(tag, "MYRA_RESULT: SUCCESS, launched default MediaStore music player")
                return CommandResult.success("Music player खोल रही हूँ। कौन सा गाना चलाऊँ?", target = "music")
            }
        } catch (e: Exception) {
            Log.e(tag, "MediaStore music intent failed", e)
        }

        // Fallback to YouTube as universal music source if no dedicated music app
        if (isPackageInstalled(pm, "com.google.android.youtube")) {
            val ytIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
            if (ytIntent != null) {
                ytIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(ytIntent)
                Log.d(tag, "MYRA_RESULT: SUCCESS, launched YouTube for music")
                return CommandResult.success("YouTube खोल रही हूँ। कौन सा गाना चलाऊँ?", target = "youtube_music")
            }
        }

        Log.w(tag, "MYRA_RESULT: NOT_INSTALLED, no music player found")
        return CommandResult.notInstalled("फोन में कोई music app नहीं मिला।", target = "music")
    }

    private fun launchCamera(): CommandResult {
        return try {
            val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (camIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(camIntent)
                Log.d(tag, "MYRA_RESULT: SUCCESS, camera hardware intent launched")
                CommandResult.success("Camera खोल दिया।", target = "camera")
            } else {
                // Fallback to searching known camera packages
                launchCandidatePackages(knownAppPackages["camera"] ?: emptyList(), "Camera")
            }
        } catch (e: Exception) {
            Log.e(tag, "Camera launch failed", e)
            CommandResult.failed("Camera खोलने में समस्या आई।", target = "camera", detail = e.message ?: "")
        }
    }

    private fun launchSettings(): CommandResult {
        return try {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            Log.d(tag, "MYRA_RESULT: SUCCESS, settings launched")
            CommandResult.success("Settings खोल दिया।", target = "settings")
        } catch (e: Exception) {
            Log.e(tag, "Settings launch failed", e)
            CommandResult.failed("Settings खोल नहीं पाया।", target = "settings", detail = e.message ?: "")
        }
    }

    private fun launchDialer(): CommandResult {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            Log.d(tag, "MYRA_RESULT: SUCCESS, dialer launched")
            CommandResult.success("Phone dialer खोल दिया।", target = "phone")
        } catch (e: Exception) {
            Log.e(tag, "Dialer launch failed", e)
            CommandResult.failed("Phone dialer नहीं खुला।", target = "phone", detail = e.message ?: "")
        }
    }

    private fun launchGallery(): CommandResult {
        return try {
            val galleryIntent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (galleryIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(galleryIntent)
                Log.d(tag, "MYRA_RESULT: SUCCESS, gallery intent launched")
                CommandResult.success("Gallery खोल दी।", target = "gallery")
            } else {
                launchCandidatePackages(knownAppPackages["gallery"] ?: emptyList(), "Gallery")
            }
        } catch (e: Exception) {
            Log.e(tag, "Gallery launch failed", e)
            launchCandidatePackages(knownAppPackages["gallery"] ?: emptyList(), "Gallery")
        }
    }

    private fun launchDefaultBrowser(): CommandResult {
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            Log.d(tag, "MYRA_RESULT: SUCCESS, default browser opened")
            CommandResult.success("Chrome खोल दिया।", target = "chrome")
        } catch (e: Exception) {
            Log.e(tag, "Browser launch failed", e)
            CommandResult.notInstalled("Chrome या कोई web browser installed नहीं है।", target = "chrome")
        }
    }

    private fun launchExplicitPackage(packageName: String, displayName: String): CommandResult {
        val pm = context.packageManager
        if (!isPackageInstalled(pm, packageName)) {
            Log.w(tag, "MYRA_RESULT: NOT_INSTALLED for $packageName")
            return CommandResult.notInstalled("$displayName इस फोन में installed नहीं है।", target = displayName)
        }
        val intent = pm.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(tag, "MYRA_RESULT: SUCCESS, $displayName launched")
                CommandResult.success("$displayName खोल दिया।", target = displayName, detail = packageName)
            } catch (e: Exception) {
                Log.e(tag, "Error launching $displayName", e)
                CommandResult.failed("$displayName खोल नहीं पाया।", target = displayName, detail = e.message ?: "")
            }
        } else {
            CommandResult.failed("$displayName खोल नहीं पाया।", target = displayName)
        }
    }

    private fun launchCandidatePackages(candidates: List<String>, displayName: String): CommandResult {
        val pm = context.packageManager
        for (pkg in candidates) {
            if (isPackageInstalled(pm, pkg)) {
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    return try {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        Log.d(tag, "MYRA_RESULT: SUCCESS, launched candidate $pkg for $displayName")
                        CommandResult.success("$displayName खोल दिया।", target = displayName, detail = pkg)
                    } catch (e: Exception) {
                        Log.e(tag, "Error launching $pkg", e)
                        CommandResult.failed("$displayName खोलने में समस्या आई।", target = displayName, detail = e.message ?: "")
                    }
                }
            }
        }
        Log.w(tag, "MYRA_RESULT: NOT_INSTALLED for $displayName")
        return CommandResult.notInstalled("$displayName इस फोन में उपलब्ध नहीं है।", target = displayName)
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun formatAppName(key: String): String {
        return when (key.lowercase()) {
            "whatsapp" -> "WhatsApp"
            "youtube" -> "YouTube"
            "youtube_music", "youtube music" -> "YouTube Music"
            "chrome" -> "Chrome"
            "camera" -> "Camera"
            "settings", "setting" -> "Settings"
            "gallery" -> "Gallery"
            "phone" -> "Phone"
            "calculator" -> "Calculator"
            "maps" -> "Google Maps"
            "spotify" -> "Spotify"
            "instagram" -> "Instagram"
            "telegram" -> "Telegram"
            "facebook" -> "Facebook"
            "twitter" -> "Twitter"
            "gmail" -> "Gmail"
            else -> key.replaceFirstChar { it.uppercase() }
        }
    }
}
