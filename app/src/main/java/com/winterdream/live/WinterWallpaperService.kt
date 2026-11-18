package com.winterdream.live

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class WinterWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = WinterEngine()

    inner class WinterEngine : Engine() {
        private val thread = HandlerThread("WinterDreamRenderer").apply { start() }
        private val handler = Handler(thread.looper)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private var scene: Bitmap? = null
        private var running = false
        private var width = 1
        private var height = 1
        private var lastTouchX = -1f
        private var lastTouchY = -1f
        private var frame = 0
        private val flakes = ArrayList<Flake>()
        private val snowballs = ArrayList<Snowball>()
        private val random = Random(System.currentTimeMillis())

        private val drawTask = object : Runnable {
            override fun run() {
                if (!running) return
                drawFrame()
                handler.postDelayed(this, 33L)
            }
        }

        init {
            setTouchEventsEnabled(true)
            try {
                scene = BitmapFactory.decodeStream(assets.open("winter_scene.png"))
            } catch (_: Exception) {
                scene = null
            }
            repeat(115) { flakes.add(newFlake(randomY = true)) }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = max(1, w)
            height = max(1, h)
            drawFrame()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            running = visible
            handler.removeCallbacks(drawTask)
            if (visible) handler.post(drawTask)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            running = false
            handler.removeCallbacks(drawTask)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            running = false
            handler.removeCallbacksAndMessages(null)
            thread.quitSafely()
            scene?.recycle()
            scene = null
            super.onDestroy()
        }

        override fun onTouchEvent(event: MotionEvent) {
            val x = event.x
            val y = event.y
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = x
                    lastTouchY = y
                    // A gentle snow puff appears wherever the user touches.
                    repeat(12) {
                        flakes.add(Flake(x, y, random.nextFloat() * 5f - 2.5f,
                            random.nextFloat() * -4f - 0.5f, random.nextFloat() * 3f + 2f,
                            0.82f, random.nextFloat() * 0.8f + 0.4f))
                    }
                    snowballs.add(Snowball(x, y, random.nextFloat() * 8f + 10f, 1f))
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    // Finger movement pushes nearby flakes away.
                    for (flake in flakes) {
                        val ox = flake.x - x
                        val oy = flake.y - y
                        if (ox * ox + oy * oy < 15000f) {
                            flake.vx += dx * 0.035f
                            flake.vy += dy * 0.025f - 0.15f
                        }
                    }
                    lastTouchX = x
                    lastTouchY = y
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    lastTouchX = -1f
                    lastTouchY = -1f
                }
            }
            super.onTouchEvent(event)
        }

        private fun newFlake(randomY: Boolean): Flake {
            return Flake(
                random.nextFloat() * width,
                if (randomY) random.nextFloat() * height else -8f,
                random.nextFloat() * 0.9f - 0.45f,
                random.nextFloat() * 1.5f + 0.7f,
                random.nextFloat() * 2.7f + 1.1f,
                random.nextFloat() * 0.7f + 0.3f,
                random.nextFloat() * 1.4f + 0.3f
            )
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.rgb(8, 17, 42))
                    drawBackground(canvas)
                    drawSnow(canvas)
                    drawSnowballs(canvas)
                }
            } catch (_: Exception) {
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
                }
            }
        }

        private fun drawBackground(canvas: Canvas) {
            val bitmap = scene ?: return
            val scale = max(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
            val dw = bitmap.width * scale
            val dh = bitmap.height * scale
            val left = (width - dw) / 2f
            val top = (height - dh) / 2f
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + dw, top + dh), paint)
            // Slight cool tint keeps the scene calm behind the snow.
            paint.color = Color.argb(18, 10, 25, 60)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.color = Color.WHITE
        }

        private fun drawSnow(canvas: Canvas) {
            val iterator = flakes.iterator()
            while (iterator.hasNext()) {
                val f = iterator.next()
                f.x += f.vx + kotlin.math.sin((frame + f.phase).toDouble()).toFloat() * 0.25f
                f.y += f.vy
                f.vx *= 0.995f
                f.vy = min(4.8f, f.vy + 0.008f)
                if (f.y > height + 12f || f.x < -30f || f.x > width + 30f) {
                    iterator.remove()
                    continue
                }
                paint.color = Color.argb((f.alpha * 255).toInt().coerceIn(0, 255), 255, 255, 255)
                paint.setShadowLayer(if (f.radius > 2f) 5f else 1.5f, 0f, 0f, Color.WHITE)
                canvas.drawCircle(f.x, f.y, f.radius, paint)
                paint.clearShadowLayer()
            }
            while (flakes.size < 115) flakes.add(newFlake(false))
            frame++
        }

        private fun drawSnowballs(canvas: Canvas) {
            val iterator = snowballs.iterator()
            while (iterator.hasNext()) {
                val b = iterator.next()
                b.life -= 0.0035f
                if (b.life <= 0f) {
                    iterator.remove()
                    continue
                }
                paint.color = Color.argb((b.life * 210).toInt().coerceIn(0, 210), 245, 249, 255)
                paint.setShadowLayer(7f, 0f, 2f, Color.argb(100, 210, 230, 255))
                canvas.drawCircle(b.x, b.y, b.radius, paint)
                paint.clearShadowLayer()
                paint.color = Color.argb((b.life * 90).toInt().coerceIn(0, 90), 255, 255, 255)
                canvas.drawCircle(b.x - b.radius * 0.28f, b.y - b.radius * 0.3f,
                    b.radius * 0.22f, paint)
            }
        }

        private data class Flake(
            var x: Float, var y: Float, var vx: Float, var vy: Float,
            val radius: Float, val alpha: Float, val phase: Float
        )
        private data class Snowball(var x: Float, var y: Float, var radius: Float, var life: Float)
    }
}
