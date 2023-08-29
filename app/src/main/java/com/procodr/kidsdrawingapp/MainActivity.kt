package com.procodr.kidsdrawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val ib_brush : ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener {
            showBrushChooserDialog()
        }
    }

    private fun showBrushChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")

        val tinyButton: ImageButton = brushDialog.findViewById(R.id.ib_tiny_brush)
        changeBrushSize(2f, tinyButton, brushDialog)

        val xSmallButton: ImageButton = brushDialog.findViewById(R.id.ib_xSmall_brush)
        changeBrushSize(4f, xSmallButton, brushDialog)

        val smallButton: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        changeBrushSize(6f, smallButton, brushDialog)

        val mediumButton: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        changeBrushSize(10f, mediumButton, brushDialog)

        val largeButton: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        changeBrushSize(15f, largeButton, brushDialog)

        val xLargeButton: ImageButton = brushDialog.findViewById(R.id.ib_xLarge_brush)
        changeBrushSize(20f, xLargeButton, brushDialog)

        val humongousButton: ImageButton = brushDialog.findViewById(R.id.ib_humongous_brush)
        changeBrushSize(30f, humongousButton, brushDialog)

        brushDialog.show()
    }

    private fun changeBrushSize(brushSize: Float,button:ImageButton, brushDialog: Dialog) {
        button.setOnClickListener {
            drawingView?.setSizeForBrush(brushSize)

            brushDialog.dismiss()
        }
    }
}