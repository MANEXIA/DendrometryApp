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
        color = Color.RED
        strokeWidth = 10f
    }

    private var centerX: Float = 0f
    private var centerY: Float = 0f

    fun updatePosition(x: Float, y: Float) {
        centerX = x
        centerY = y
        invalidate() // Redraw the view with the new position
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        val crosshairSize = 100 // Adjust this value to change the size of the crosshair

        // Draw crosshair with a larger size
        canvas.drawLine(centerX - crosshairSize, centerY, centerX + crosshairSize, centerY, paint) // Horizontal line
        canvas.drawLine(centerX, centerY - crosshairSize, centerX, centerY + crosshairSize, paint) // Vertical line
    }
}
