package com.example.skycharacterapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.compose.ui.graphics.toArgb
import java.time.LocalTime

class SkyWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return SkyEngine()
    }

    inner class SkyEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { drawFrame() }
        private var visible = false
        private var characterBitmap: Bitmap? = null
        private var characterSize: Float = 200f
        private var offsetX: Float = 0f
        private var offsetY: Float = 0f
        private var skyPalette: SkyPalette = SkyPalette()
        private var imageUriString: String? = null

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            loadPreferences()
        }

        private fun loadPreferences() {
            val prefs = getSharedPreferences("SkyCharacterApp", MODE_PRIVATE)
            val newUriString = prefs.getString("imageUri", null)
            characterSize = prefs.getFloat("characterSize", 200f)
            offsetX = prefs.getFloat("offsetX", 0f)
            offsetY = prefs.getFloat("offsetY", 0f)
            skyPalette = SkyPalette.loadFromPrefs(prefs)

            if (newUriString != imageUriString) {
                imageUriString = newUriString
                loadBitmap()
            }
        }

        private fun loadBitmap() {
            if (imageUriString == null) {
                characterBitmap = null
                return
            }
            try {
                val uri = Uri.parse(imageUriString)
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                characterBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                characterBitmap = null
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                loadPreferences() // Reload in case user changed it in the app
                drawFrame()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val currentTime = LocalTime.now()
                    val bgColors = getSkyGradientForTime(currentTime, skyPalette).map { it.toArgb() }.toIntArray()
                    
                    val bgPaint = Paint().apply {
                        shader = LinearGradient(
                            0f, 0f, 0f, canvas.height.toFloat(),
                            bgColors,
                            null,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), bgPaint)

                    characterBitmap?.let { bitmap ->
                        val density = resources.displayMetrics.density
                        val sizePx = characterSize * density
                        // Note: offset from Compose is already in raw pixels
                        
                        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        var destWidth = sizePx
                        var destHeight = sizePx
                        
                        if (aspectRatio > 1) {
                            destHeight = sizePx / aspectRatio
                        } else {
                            destWidth = sizePx * aspectRatio
                        }

                        val left = (canvas.width - destWidth) / 2f + offsetX
                        val top = (canvas.height - destHeight) / 2f + offsetY
                        
                        val destRect = RectF(left, top, left + destWidth, top + destHeight)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            isFilterBitmap = true
                        }
                        canvas.drawBitmap(bitmap, null, destRect, paint)
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            handler.removeCallbacks(drawRunnable)
            if (visible) {
                // Schedule next frame in 1 minute
                handler.postDelayed(drawRunnable, 60000)
            }
        }
    }
}
