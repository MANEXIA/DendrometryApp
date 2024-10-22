package com.example.myappkotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CrosshairView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.CYAN
        strokeWidth = 5f
    }

    private var centerX: Float = 0f
    private var centerY: Float = 0f

    // Minimum time between updates (in milliseconds)
    private val updateDelay: Long = 30 // 30ms delay, adjust for smoothness
    private var lastUpdateTime: Long = 0

    // Update position method with throttling
    fun updatePosition(x: Float, y: Float) {
        val currentTime = System.currentTimeMillis()

        // Throttle updates based on time delay
        if (currentTime - lastUpdateTime >= updateDelay) {
            centerX = x
            centerY = y
            lastUpdateTime = currentTime
            postInvalidateOnAnimation() // Sync with display refresh rate
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val crosshairSize = 25f // Crosshair size

        // Draw crosshair
        canvas.drawLine(centerX - crosshairSize, centerY, centerX + crosshairSize, centerY, paint) // Horizontal line
        canvas.drawLine(centerX, centerY - crosshairSize, centerX, centerY + crosshairSize, paint) // Vertical line
    }
}
