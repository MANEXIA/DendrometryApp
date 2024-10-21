package com.example.myappkotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View

class CrosshairView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 5f
    }

    private var centerX: Float = 0f
    private var centerY: Float = 0f

    // Handler to delay updates
    private val handler = Handler(Looper.getMainLooper())
    private var isUpdateScheduled = false
    private var lastUpdateTime: Long = 0

    // Minimum time between updates (in milliseconds)
    private val updateDelay: Long = 30 // Adjust this value as needed (50ms = 20 frames per second)

    // Update position method with throttling
    fun updatePosition(x: Float, y: Float) {
        val currentTime = System.currentTimeMillis()

        // Only update if the delay period has passed
        if (currentTime - lastUpdateTime >= updateDelay && !isUpdateScheduled) {
            centerX = x
            centerY = y
            scheduleUpdate()
        }
    }

    // Schedule the actual view invalidation with a delay
    private fun scheduleUpdate() {
        isUpdateScheduled = true
        handler.postDelayed({
            invalidate() // Redraw the view
            lastUpdateTime = System.currentTimeMillis()
            isUpdateScheduled = false
        }, updateDelay)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        val crosshairSize = 25 // Adjust this value to change the size of the crosshair

        // Draw crosshair
        canvas.drawLine(centerX - crosshairSize, centerY, centerX + crosshairSize, centerY, paint) // Horizontal line
        canvas.drawLine(centerX, centerY - crosshairSize, centerX, centerY + crosshairSize, paint) // Vertical line
    }
}
