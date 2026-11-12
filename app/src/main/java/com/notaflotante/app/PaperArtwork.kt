package com.notaflotante.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.withSave

// Shared artwork for Compose previews/cards and the native home-screen notes.
// Everything is painted inside the existing bounds: no extra views or touch targets.
internal class PaperArtwork(
    private val color: Int,
    private val density: Float,
    private val folded: Boolean = true,
    private val shadowDepth: Float = 4f,
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paper = Path()
    private val fold = Path()
    private val crease = Path()
    private val tape = Path()
    private var paperShader: Shader? = null
    private var foldShader: Shader? = null
    private var width = -1f
    private var height = -1f
    private var tapeCenterY = 0f
    var alpha: Int = 255
    var colorFilter: ColorFilter? = null

    fun draw(canvas: Canvas, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        if (this.width != width || this.height != height) updateGeometry(width, height)
        val pixelDensity = density
        canvas.withSave {
            if (alpha < 255) saveLayerAlpha(0f, 0f, width, height, alpha)
            paint.colorFilter = colorFilter

            if (shadowDepth > 0f) withSave {
                val offset = shadowDepth.coerceAtMost(4f) * pixelDensity
                translate(offset * .55f, offset)
                fill(this, paper, 0x383A3828)
            }
            fill(this, paper, color, paperShader)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = .6f * pixelDensity
            paint.color = 0x183A3024
            drawPath(paper, paint)

            if (folded) {
                fill(this, fold, color, foldShader)
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = .7f * pixelDensity
                paint.color = 0x183A3024
                drawPath(crease, paint)
            }

            withSave {
                rotate(-3f, width / 2f, tapeCenterY)
                translate(.5f * pixelDensity, pixelDensity)
                fill(this, tape, 0x14745B2D)
                translate(-.5f * pixelDensity, -pixelDensity)
                fill(this, tape, 0x88C4AA6D.toInt())
            }
        }
    }

    private fun fill(canvas: Canvas, path: Path, color: Int, shader: Shader? = null) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.shader = shader
        canvas.drawPath(path, paint)
    }

    private fun updateGeometry(width: Float, height: Float) {
        this.width = width
        this.height = height
        val right = (width - 4f * density).coerceAtLeast(width * .5f)
        val top = (6f * density).coerceAtMost(height * .1f)
        val bottom = (height - 6f * density).coerceAtLeast(top + 1f)
        val curl = if (folded) minOf(34f * density, right * .18f, (bottom - top) * .2f) else 0f

        paper.reset()
        paper.moveTo(0f, top)
        paper.lineTo(right, top)
        paper.lineTo(right, bottom - curl * .58f)
        paper.quadTo(right - curl * .35f, bottom - curl * .12f, right - curl, bottom)
        paper.lineTo(0f, bottom)
        paper.close()
        paperShader = LinearGradient(0f, top, 0f, bottom,
            intArrayOf(tint(color, Color.WHITE, .06f), color, tint(color, 0xFF3A3024.toInt(), .025f)),
            floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP)

        fold.reset()
        fold.moveTo(right - curl, bottom)
        fold.cubicTo(right - curl * .72f, bottom - curl * .22f,
            right - curl * .56f, bottom - curl * .66f, right - curl * .44f, bottom - curl * .82f)
        fold.quadTo(right - curl * .22f, bottom - curl * .66f, right, bottom - curl * .58f)
        fold.quadTo(right - curl * .35f, bottom - curl * .12f, right - curl, bottom)
        fold.close()
        // Keep the folded underside in the chosen paper color, rather than a white triangle.
        foldShader = if (curl > 0f) LinearGradient(right - curl, bottom, right, bottom - curl,
            tint(color, 0xFF806432.toInt(), .23f), tint(color, Color.WHITE, .1f), Shader.TileMode.CLAMP) else null
        crease.reset()
        crease.moveTo(right - curl, bottom)
        crease.cubicTo(right - curl * .72f, bottom - curl * .22f,
            right - curl * .56f, bottom - curl * .66f, right - curl * .44f, bottom - curl * .82f)

        val tapeWidth = minOf(66f * density, right * .34f)
        val tapeHeight = minOf(14f * density, height * .27f)
        val left = (right - tapeWidth) / 2f
        val tapeTop = 1.5f * density
        tapeCenterY = tapeTop + tapeHeight / 2f
        tape.reset()
        tape.moveTo(left + density, tapeTop)
        tape.lineTo(left + tapeWidth * .14f, tapeTop + density * .6f)
        tape.lineTo(left + tapeWidth * .26f, tapeTop)
        tape.lineTo(left + tapeWidth * .39f, tapeTop + density)
        tape.lineTo(left + tapeWidth * .56f, tapeTop + density * .4f)
        tape.lineTo(left + tapeWidth * .74f, tapeTop + density)
        tape.lineTo(left + tapeWidth, tapeTop + density * .4f)
        tape.lineTo(left + tapeWidth - density, tapeTop + tapeHeight * .45f)
        tape.lineTo(left + tapeWidth, tapeTop + tapeHeight)
        tape.lineTo(left + tapeWidth * .78f, tapeTop + tapeHeight - density * .6f)
        tape.lineTo(left + tapeWidth * .61f, tapeTop + tapeHeight)
        tape.lineTo(left + tapeWidth * .43f, tapeTop + tapeHeight - density)
        tape.lineTo(left + tapeWidth * .25f, tapeTop + tapeHeight - density * .4f)
        tape.lineTo(left, tapeTop + tapeHeight)
        tape.lineTo(left + density * .6f, tapeTop + tapeHeight * .5f)
        tape.close()
    }

    private fun tint(from: Int, to: Int, amount: Float): Int = Color.rgb(
        (Color.red(from) + (Color.red(to) - Color.red(from)) * amount).toInt(),
        (Color.green(from) + (Color.green(to) - Color.green(from)) * amount).toInt(),
        (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount).toInt(),
    )
}

internal class PaperDrawable(color: Int, density: Float, folded: Boolean) : Drawable() {
    private val artwork = PaperArtwork(color, density, folded)

    override fun draw(canvas: Canvas) {
        canvas.withSave {
            translate(bounds.left.toFloat(), bounds.top.toFloat())
            artwork.draw(this, bounds.width().toFloat(), bounds.height().toFloat())
        }
    }

    override fun setAlpha(alpha: Int) { artwork.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { artwork.colorFilter = colorFilter; invalidateSelf() }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
