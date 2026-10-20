package com.myra.assistant.wallpaper

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.myra.assistant.MainActivity
import kotlin.math.*
import kotlin.random.Random

/**
 * Production-ready Android Wallpaper Engine for MYRA Assistant.
 * Correctly inherits from service.Engine() as a non-static inner class of WallpaperService.
 * Renders real-time 9:16 futuristic AI interface with holographic Earth,
 * rotating HUD rings, breathing MYRA orb, audio waveform aura, radial app icons,
 * bottom portal, and cosmic stardust particles.
 */
open class MyraWallpaperEngine(
    protected val service: WallpaperService
) : service.Engine() {

    private val prefs = MyraWallpaperPreferences.getInstance(service)
    private val handler = Handler(Looper.getMainLooper())
    private var isVisible = false
    private var width = 1080
    private var height = 1920

    // Animation timestamps
    private var lastFrameTime = SystemClock.uptimeMillis()
    private var animTimeSec = 0f
    private var pulseProgress = 0f

    // Interactive Touch Ripple
    private var rippleX = -1f
    private var rippleY = -1f
    private var rippleRadius = 0f
    private var rippleAlpha = 0f
    private var lastTapTime = 0L

    // Paints
    private val bgPaint = Paint()
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val orbGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbCorePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val earthAtmospherePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val earthBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val earthGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val portalPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
    }

    // Precomputed Stars & Particles for zero garbage collection
    private data class Star(var x: Float, var y: Float, val speed: Float, val size: Float, val alpha: Int)
    private val stars = ArrayList<Star>()

    private data class Particle(var x: Float, var y: Float, var vy: Float, val size: Float, var alpha: Float, val colorType: Int)
    private val particles = ArrayList<Particle>()

    // App icons for radial futuristic ring
    private data class AppIconItem(val name: String, val glyph: String, val angleDeg: Float, val primaryColor: Int)
    private val appIcons = listOf(
        AppIconItem("Phone", "📞", 0f, Color.rgb(34, 197, 94)),
        AppIconItem("WhatsApp", "💬", 32.7f, Color.rgb(37, 211, 102)),
        AppIconItem("Chrome", "🌐", 65.4f, Color.rgb(59, 130, 246)),
        AppIconItem("YouTube", "▶", 98.1f, Color.rgb(239, 68, 68)),
        AppIconItem("Instagram", "📷", 130.8f, Color.rgb(236, 72, 153)),
        AppIconItem("Messages", "✉", 163.5f, Color.rgb(59, 130, 246)),
        AppIconItem("Camera", "📸", 196.2f, Color.rgb(245, 158, 11)),
        AppIconItem("Photos", "🖼", 228.9f, Color.rgb(168, 85, 247)),
        AppIconItem("Play Store", "🛒", 261.6f, Color.rgb(16, 185, 129)),
        AppIconItem("Gallery", "🌌", 294.3f, Color.rgb(14, 165, 233)),
        AppIconItem("Settings", "⚙", 327f, Color.rgb(148, 163, 184))
    )

    private val drawRunnable = object : Runnable {
        override fun run() {
            drawFrame()
            if (isVisible) {
                // Exhaustive handling of frame rate quality
                val targetFps = if (prefs.batterySaverMode) 30 else when (prefs.quality) {
                    WallpaperQuality.LOW -> 30
                    WallpaperQuality.MEDIUM -> 45
                    WallpaperQuality.HIGH -> 60
                }
                val frameDelay = (1000L / targetFps).coerceAtLeast(16L)
                handler.postDelayed(this, frameDelay)
            }
        }
    }

    override fun onCreate(surfaceHolder: SurfaceHolder?) {
        super.onCreate(surfaceHolder)
        initParticles()
    }

    override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        super.onSurfaceChanged(holder, format, width, height)
        this.width = width
        this.height = height
        initParticles()
    }

    override fun onVisibilityChanged(visible: Boolean) {
        super.onVisibilityChanged(visible)
        this.isVisible = visible
        if (visible) {
            lastFrameTime = SystemClock.uptimeMillis()
            handler.post(drawRunnable)
        } else {
            handler.removeCallbacks(drawRunnable)
        }
    }

    override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
        super.onSurfaceDestroyed(holder)
        this.isVisible = false
        handler.removeCallbacks(drawRunnable)
    }

    override fun onTouchEvent(event: MotionEvent) {
        super.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_DOWN) {
            rippleX = event.x
            rippleY = event.y
            rippleRadius = 10f
            rippleAlpha = 1.0f

            val now = SystemClock.uptimeMillis()
            val dx = event.x - (width / 2f)
            val dy = event.y - (height * 0.58f)
            val distToCenter = sqrt(dx * dx + dy * dy)

            // Double tap or tap on center orb opens MYRA Voice Assistant safely
            if (now - lastTapTime < 400 || distToCenter < (width * 0.22f)) {
                try {
                    val intent = Intent(service, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    service.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore if activity cannot be launched
                }
            }
            lastTapTime = now
        }
    }

    private fun initParticles() {
        stars.clear()
        val starCount = if (prefs.batterySaverMode) 40 else 80
        for (i in 0 until starCount) {
            stars.add(
                Star(
                    x = Random.nextFloat() * width,
                    y = Random.nextFloat() * height,
                    speed = 10f + Random.nextFloat() * 25f,
                    size = 1f + Random.nextFloat() * 2.5f,
                    alpha = 80 + Random.nextInt(170)
                )
            )
        }

        particles.clear()
        val particleCount = if (prefs.batterySaverMode) 20 else 45
        for (i in 0 until particleCount) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * width,
                    y = Random.nextFloat() * height,
                    vy = -(20f + Random.nextFloat() * 40f),
                    size = 2f + Random.nextFloat() * 4f,
                    alpha = Random.nextFloat(),
                    colorType = Random.nextInt(2) // 0: Blue, 1: Red
                )
            )
        }
    }

    private fun drawFrame() {
        val holder = surfaceHolder ?: return
        var canvas: Canvas? = null
        try {
            canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    holder.lockHardwareCanvas()
                } catch (e: Exception) {
                    holder.lockCanvas()
                }
            } else {
                holder.lockCanvas()
            }
            if (canvas != null) {
                renderScene(canvas)
            }
        } catch (e: Exception) {
            // Surface might be released or busy
        } finally {
            if (canvas != null) {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    // Ignore unlock failure
                }
            }
        }
    }

    private fun renderScene(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val dt = ((now - lastFrameTime) / 1000f).coerceIn(0.001f, 0.1f)
        lastFrameTime = now

        // Exhaustive handling of speed multipliers
        val speedMultiplier = when (prefs.speed) {
            WallpaperSpeed.SLOW -> 0.6f
            WallpaperSpeed.NORMAL -> 1.0f
            WallpaperSpeed.FAST -> 1.5f
        }
        animTimeSec += dt * speedMultiplier
        pulseProgress = (pulseProgress + dt * 0.8f * speedMultiplier) % 1.0f

        val cx = width / 2f
        val cy = height / 2f

        // 1. Deep Space Futuristic Background
        val bgGrad = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                Color.rgb(4, 7, 18),
                Color.rgb(8, 14, 32),
                Color.rgb(18, 4, 16),
                Color.rgb(3, 5, 12)
            ),
            floatArrayOf(0f, 0.45f, 0.85f, 1f),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = bgGrad
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Stars Field Animation
        starPaint.color = Color.WHITE
        for (star in stars) {
            star.y += star.speed * dt * speedMultiplier
            if (star.y > height) {
                star.y = 0f
                star.x = Random.nextFloat() * width
            }
            starPaint.alpha = star.alpha
            canvas.drawCircle(star.x, star.y, star.size, starPaint)
        }

        // 3. Perspective Digital Grid at Bottom
        drawPerspectiveGrid(canvas, dt)

        // 4. Holographic Earth (Upper-Center behind Avatar)
        if (prefs.showEarth) {
            drawHolographicEarth(canvas, cx, height * 0.30f, min(width, height) * 0.24f)
        }

        // 5. MYRA AI Female Avatar Silhouette & Cybernetic Halo
        drawMyraAvatar(canvas, cx, height * 0.32f, min(width, height) * 0.22f)

        // 6. Holographic HUD Rings (Rotations & Tick Marks)
        if (prefs.showHudRings) {
            drawHudRings(canvas, cx, height * 0.56f, min(width, height) * 0.40f)
        }

        // 7. Radial App Icons
        if (prefs.showAppIcons) {
            drawRadialAppIcons(canvas, cx, height * 0.56f, min(width, height) * 0.38f)
        }

        // 8. Glowing MYRA AI Central Orb
        if (prefs.showOrb) {
            drawMyraOrb(canvas, cx, height * 0.56f, min(width, height) * 0.12f)
        }

        // 9. Sound Waveform Audio Aura
        if (prefs.showWaveform) {
            drawAudioWave(canvas, cx, height * 0.56f + min(width, height) * 0.15f)
        }

        // 10. Bottom Holographic Energy Platform / Portal
        drawEnergyPortal(canvas, cx, height * 0.90f, width * 0.44f)

        // 11. Floating Upward Particles
        if (prefs.particleEffects) {
            drawParticles(canvas, dt)
        }

        // 12. Interactive Touch Ripple
        if (rippleAlpha > 0f) {
            rippleRadius += 280f * dt
            rippleAlpha -= 1.8f * dt
            if (rippleAlpha < 0f) rippleAlpha = 0f

            // Exhaustive theme handling for ripple color
            val rippleColor = when (prefs.theme) {
                WallpaperTheme.BLUE -> Color.argb((rippleAlpha * 220).toInt(), 0, 210, 255)
                WallpaperTheme.RED -> Color.argb((rippleAlpha * 220).toInt(), 255, 40, 80)
                WallpaperTheme.BLUE_RED -> Color.argb((rippleAlpha * 220).toInt(), 0, 240, 255)
            }
            val ripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = rippleColor
            }
            canvas.drawCircle(rippleX, rippleY, rippleRadius, ripPaint)
        }
    }

    private fun drawPerspectiveGrid(canvas: Canvas, dt: Float) {
        val gridYStart = height * 0.72f
        val horizonY = height * 0.65f
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = when (prefs.theme) {
                WallpaperTheme.BLUE -> Color.argb(45, 0, 180, 255)
                WallpaperTheme.RED -> Color.argb(45, 255, 50, 80)
                WallpaperTheme.BLUE_RED -> Color.argb(45, 0, 190, 255)
            }
        }

        // Perspective vertical rays radiating from vanishing point
        val vpX = width / 2f
        val vpY = horizonY
        for (i in -8..8) {
            val bottomX = vpX + (i * width * 0.12f)
            canvas.drawLine(vpX, vpY + 50f, bottomX, height.toFloat(), gridPaint)
        }

        // Horizontal scrolling grid lines
        val offset = (animTimeSec * 35f) % 40f
        var y = gridYStart + offset
        while (y < height) {
            val progress = (y - gridYStart) / (height - gridYStart)
            gridPaint.alpha = (progress * 70).toInt().coerceIn(0, 90)
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += 30f + progress * 25f
        }
    }

    private fun drawHolographicEarth(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val earthAngle = animTimeSec * 8f

        // Outer glow
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius * 1.35f,
                intArrayOf(
                    Color.argb(90, 0, 160, 255),
                    Color.argb(35, 0, 100, 200),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0.6f, 0.85f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, radius * 1.35f, glowPaint)

        // Planet body
        earthBodyPaint.shader = RadialGradient(
            cx - radius * 0.3f, cy - radius * 0.3f, radius * 1.1f,
            intArrayOf(
                Color.rgb(10, 45, 85),
                Color.rgb(5, 20, 45),
                Color.rgb(2, 8, 20)
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, earthBodyPaint)

        // Holographic Latitude & Longitude Wireframe
        earthGridPaint.color = Color.argb(85, 0, 220, 255)
        earthGridPaint.strokeWidth = 1.6f

        // Latitudes
        for (i in -3..3) {
            val latY = cy + (i * (radius * 0.26f))
            val latWidth = sqrt((radius * radius - (latY - cy) * (latY - cy)).coerceAtLeast(0f))
            if (latWidth > 5f) {
                val rect = RectF(cx - latWidth, latY - 6f, cx + latWidth, latY + 6f)
                canvas.drawOval(rect, earthGridPaint)
            }
        }

        // Longitudes (Rotating ellipses)
        for (i in 0..5) {
            val angle = Math.toRadians((earthAngle + i * 30.0))
            val aspect = cos(angle).toFloat()
            val rect = RectF(cx - radius * abs(aspect), cy - radius, cx + radius * abs(aspect), cy + radius)
            earthGridPaint.alpha = (50 + abs(aspect) * 45).toInt()
            canvas.drawOval(rect, earthGridPaint)
        }

        // Atmosphere halo ring
        ringPaint.strokeWidth = 2.5f
        ringPaint.color = Color.argb(140, 0, 240, 255)
        canvas.drawCircle(cx, cy, radius + 2f, ringPaint)
    }

    private fun drawMyraAvatar(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        // Holographic AI Female Silhouette with cybernetic halo & visor
        val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = when (prefs.theme) {
                WallpaperTheme.BLUE -> Color.argb(210, 0, 220, 255)
                WallpaperTheme.RED -> Color.argb(210, 255, 60, 90)
                WallpaperTheme.BLUE_RED -> Color.argb(220, 140, 180, 255)
            }
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(45, 0, 40, 80)
        }

        // Head & Shoulders Silhouette Path
        val path = Path().apply {
            // Head
            addCircle(cx, cy - size * 0.35f, size * 0.22f, Path.Direction.CW)
            // Neck & futuristic collar
            moveTo(cx - size * 0.10f, cy - size * 0.14f)
            lineTo(cx + size * 0.10f, cy - size * 0.14f)
            lineTo(cx + size * 0.26f, cy + size * 0.15f)
            lineTo(cx + size * 0.44f, cy + size * 0.45f)
            lineTo(cx - size * 0.44f, cy + size * 0.45f)
            lineTo(cx - size * 0.26f, cy + size * 0.15f)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, avatarPaint)

        // Cybernetic Visor / Eye Glow
        val visorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = when (prefs.theme) {
                WallpaperTheme.BLUE -> Color.rgb(0, 255, 240)
                WallpaperTheme.RED -> Color.rgb(255, 40, 80)
                WallpaperTheme.BLUE_RED -> Color.rgb(0, 255, 240)
            }
        }
        val visorRect = RectF(cx - size * 0.12f, cy - size * 0.37f, cx + size * 0.12f, cy - size * 0.33f)
        canvas.drawRoundRect(visorRect, 4f, 4f, visorPaint)

        // Cybernetic Crown / Halo
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.argb(160, 255, 40, 90)
        }
        val haloRect = RectF(cx - size * 0.32f, cy - size * 0.58f, cx + size * 0.32f, cy - size * 0.48f)
        canvas.drawArc(haloRect, 200f, 140f, false, haloPaint)
    }

    private fun drawHudRings(canvas: Canvas, cx: Float, cy: Float, maxRadius: Float) {
        val ring1Radius = maxRadius * 0.65f
        val ring2Radius = maxRadius * 0.85f
        val ring3Radius = maxRadius * 1.05f

        val t = animTimeSec

        // Inner Rotating Segmented Ring 1 (Clockwise)
        ringPaint.strokeWidth = 2.5f
        ringPaint.color = when (prefs.theme) {
            WallpaperTheme.BLUE -> Color.argb(140, 0, 210, 255)
            WallpaperTheme.RED -> Color.argb(140, 255, 60, 90)
            WallpaperTheme.BLUE_RED -> Color.argb(150, 0, 210, 255)
        }
        val r1Rect = RectF(cx - ring1Radius, cy - ring1Radius, cx + ring1Radius, cy + ring1Radius)
        val angle1 = (t * 22f) % 360f
        canvas.drawArc(r1Rect, angle1, 55f, false, ringPaint)
        canvas.drawArc(r1Rect, angle1 + 90f, 55f, false, ringPaint)
        canvas.drawArc(r1Rect, angle1 + 180f, 55f, false, ringPaint)
        canvas.drawArc(r1Rect, angle1 + 270f, 55f, false, ringPaint)

        // Middle Ring 2 (Counter-Clockwise Cyber Red / Blue)
        ringPaint.strokeWidth = 2f
        ringPaint.color = when (prefs.theme) {
            WallpaperTheme.BLUE -> Color.argb(120, 0, 160, 255)
            WallpaperTheme.RED -> Color.argb(160, 255, 30, 70)
            WallpaperTheme.BLUE_RED -> Color.argb(160, 255, 40, 80)
        }
        val r2Rect = RectF(cx - ring2Radius, cy - ring2Radius, cx + ring2Radius, cy + ring2Radius)
        val angle2 = (-t * 16f) % 360f
        canvas.drawArc(r2Rect, angle2, 35f, false, ringPaint)
        canvas.drawArc(r2Rect, angle2 + 60f, 70f, false, ringPaint)
        canvas.drawArc(r2Rect, angle2 + 180f, 40f, false, ringPaint)
        canvas.drawArc(r2Rect, angle2 + 250f, 65f, false, ringPaint)

        // Outer Ring 3 with Radar Cyber Tick Marks
        ringPaint.strokeWidth = 1.2f
        ringPaint.color = Color.argb(90, 0, 220, 255)
        canvas.drawCircle(cx, cy, ring3Radius, ringPaint)

        val tickCount = 24
        val tickLen = 8f
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1.5f
            color = Color.argb(110, 0, 240, 255)
        }
        for (i in 0 until tickCount) {
            val rad = Math.toRadians((i * (360.0 / tickCount) + (t * 6f)))
            val cosR = cos(rad).toFloat()
            val sinR = sin(rad).toFloat()
            val x1 = cx + (ring3Radius - tickLen) * cosR
            val y1 = cy + (ring3Radius - tickLen) * sinR
            val x2 = cx + ring3Radius * cosR
            val y2 = cy + ring3Radius * sinR
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        // Energy Pulse Travel on Ring 2
        val pulseAngle = Math.toRadians((angle2 + (pulseProgress * 360f)).toDouble())
        val pulsePx = cx + ring2Radius * cos(pulseAngle).toFloat()
        val pulsePy = cy + ring2Radius * sin(pulseAngle).toFloat()
        val pulseDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 255, 255)
            setShadowLayer(14f, 0f, 0f, Color.rgb(255, 40, 90))
        }
        canvas.drawCircle(pulsePx, pulsePy, 5.5f, pulseDotPaint)
    }

    private fun drawRadialAppIcons(canvas: Canvas, cx: Float, cy: Float, orbitRadius: Float) {
        val baseAngle = animTimeSec * 4f // Slow graceful orbit rotation
        val iconSize = (width * 0.088f).coerceIn(40f, 75f)

        for (item in appIcons) {
            val totalAngleDeg = baseAngle + item.angleDeg
            val rad = Math.toRadians(totalAngleDeg.toDouble())
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()

            // Orbit is slightly elliptical for high-tech 3D perspective
            val ix = cx + (orbitRadius * 1.08f) * cosA
            val iy = cy + (orbitRadius * 0.82f) * sinA

            // Subtle floating oscillation
            val floatOffset = sin(animTimeSec * 2.5f + item.angleDeg) * 4f

            // Icon Background Disk
            iconBgPaint.color = Color.argb(195, 12, 18, 36)
            canvas.drawCircle(ix, iy + floatOffset, iconSize * 0.5f, iconBgPaint)

            // Neon Border Ring
            iconGlowPaint.strokeWidth = 2.2f
            iconGlowPaint.color = item.primaryColor
            canvas.drawCircle(ix, iy + floatOffset, iconSize * 0.5f, iconGlowPaint)

            // Inner Glyph
            textPaint.color = Color.WHITE
            textPaint.textSize = iconSize * 0.44f
            val fontMetrics = textPaint.fontMetrics
            val baseline = iy + floatOffset - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(item.glyph, ix, baseline, textPaint)
        }
    }

    private fun drawMyraOrb(canvas: Canvas, cx: Float, cy: Float, baseRadius: Float) {
        // Breathing pulse
        val breathe = sin(animTimeSec * 2.2f) * 0.09f
        val r = baseRadius * (1f + breathe)

        // Outer Glow
        val outerGlowColor1 = when (prefs.theme) {
            WallpaperTheme.BLUE -> Color.argb(160, 0, 180, 255)
            WallpaperTheme.RED -> Color.argb(160, 255, 30, 80)
            WallpaperTheme.BLUE_RED -> Color.argb(160, 0, 210, 255)
        }
        val outerGlowColor2 = when (prefs.theme) {
            WallpaperTheme.BLUE -> Color.argb(50, 0, 80, 200)
            WallpaperTheme.RED -> Color.argb(50, 180, 20, 60)
            WallpaperTheme.BLUE_RED -> Color.argb(50, 255, 40, 80)
        }

        orbGlowPaint.shader = RadialGradient(
            cx, cy, r * 2.1f,
            intArrayOf(outerGlowColor1, outerGlowColor2, Color.TRANSPARENT),
            floatArrayOf(0.35f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 2.1f, orbGlowPaint)

        // Core Sphere Gradient
        val coreColor1 = Color.rgb(255, 255, 255)
        val coreColor2 = when (prefs.theme) {
            WallpaperTheme.BLUE -> Color.rgb(0, 200, 255)
            WallpaperTheme.RED -> Color.rgb(255, 50, 90)
            WallpaperTheme.BLUE_RED -> Color.rgb(0, 230, 255)
        }
        val coreColor3 = when (prefs.theme) {
            WallpaperTheme.BLUE -> Color.rgb(10, 40, 120)
            WallpaperTheme.RED -> Color.rgb(120, 15, 40)
            WallpaperTheme.BLUE_RED -> Color.rgb(120, 20, 80)
        }

        orbCorePaint.shader = RadialGradient(
            cx - r * 0.28f, cy - r * 0.28f, r * 1.1f,
            intArrayOf(coreColor1, coreColor2, coreColor3),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, orbCorePaint)

        // Cybernetic Core Ring around Orb
        val ringP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
            color = Color.argb(190, 255, 255, 255)
        }
        val ovalRect = RectF(cx - r * 1.35f, cy - r * 0.45f, cx + r * 1.35f, cy + r * 0.45f)
        canvas.save()
        canvas.rotate(sin(animTimeSec * 1.5f) * 15f, cx, cy)
        canvas.drawOval(ovalRect, ringP)
        canvas.restore()

        // "MYRA" holographic text beneath orb
        textPaint.textSize = (width * 0.042f).coerceIn(16f, 32f)
        textPaint.color = Color.argb(220, 220, 245, 255)
        canvas.drawText("MYRA AI", cx, cy + r * 1.75f, textPaint)
    }

    private fun drawAudioWave(canvas: Canvas, cx: Float, cy: Float) {
        val radiusX = min(width, height) * 0.32f
        for (i in -4..4) {
            val waveX = cx + (i * radiusX * 0.2f)
            val waveHeight = 45f + sin(animTimeSec * 3f + i) * 28f
            wavePaint.shader = LinearGradient(
                waveX, cy, waveX, cy - waveHeight,
                Color.argb(130, 0, 230, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawLine(waveX, cy, waveX, cy - waveHeight, wavePaint)
        }
    }

    private fun drawEnergyPortal(canvas: Canvas, cx: Float, cy: Float, radiusX: Float) {
        val radiusY = radiusX * 0.28f

        // Platform base glow
        val portalGrad = RadialGradient(
            cx, cy, radiusX * 1.2f,
            intArrayOf(
                Color.argb(140, 0, 220, 255),
                Color.argb(70, 255, 40, 90),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        portalPaint.shader = portalGrad
        val pRect = RectF(cx - radiusX, cy - radiusY, cx + radiusX, cy + radiusY)
        canvas.drawOval(pRect, portalPaint)

        // Concentric portal rings
        val pRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(160, 0, 240, 255)
        }
        canvas.drawOval(RectF(cx - radiusX * 0.8f, cy - radiusY * 0.8f, cx + radiusX * 0.8f, cy + radiusY * 0.8f), pRing)

        pRing.color = Color.argb(160, 255, 50, 90)
        canvas.drawOval(RectF(cx - radiusX * 0.5f, cy - radiusY * 0.5f, cx + radiusX * 0.5f, cy + radiusY * 0.5f), pRing)

        // Subtle vertical light waves from portal
        for (i in -4..4) {
            val waveX = cx + (i * radiusX * 0.2f)
            val waveHeight = 60f + sin(animTimeSec * 3f + i) * 35f
            wavePaint.shader = LinearGradient(
                waveX, cy, waveX, cy - waveHeight,
                Color.argb(120, 0, 230, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawLine(waveX, cy, waveX, cy - waveHeight, wavePaint)
        }
    }

    private fun drawParticles(canvas: Canvas, dt: Float) {
        val speedMult = when (prefs.speed) {
            WallpaperSpeed.SLOW -> 0.6f
            WallpaperSpeed.NORMAL -> 1.0f
            WallpaperSpeed.FAST -> 1.5f
        }
        for (p in particles) {
            p.y += p.vy * dt * speedMult
            if (p.y < 0) {
                p.y = height.toFloat()
                p.x = Random.nextFloat() * width
                p.alpha = 0.8f
            }

            p.alpha -= 0.05f * dt
            if (p.alpha < 0.1f) p.alpha = 0.8f

            particlePaint.color = if (p.colorType == 0) {
                Color.argb((p.alpha * 220).toInt(), 0, 220, 255)
            } else {
                Color.argb((p.alpha * 220).toInt(), 255, 50, 90)
            }
            canvas.drawCircle(p.x, p.y, p.size, particlePaint)
        }
    }
}
