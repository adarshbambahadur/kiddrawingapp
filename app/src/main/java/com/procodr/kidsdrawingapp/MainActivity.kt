package com.procodr.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint : ImageButton? = null
    var customProgressDialog: Dialog? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        result ->
        if (result.resultCode == RESULT_OK && result.data!=null) {
            val imageBackground: ImageView = findViewById(R.id.iv_background)

            imageBackground.setImageURI(result.data?.data)
        }
    }
    
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
             permissions.entries.forEach{
                 val permissionName = it.key
                 val isGranted = it.value

                 if (isGranted) {
                     Toast.makeText(
                         this@MainActivity,
                         "Permission granted now you can read the storage files.",
                         Toast.LENGTH_LONG
                     ).show()

                     val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                     openGalleryLauncher.launch(pickIntent)
                 } else {
                     if (permissionName== Manifest.permission.READ_EXTERNAL_STORAGE) {
                         Toast.makeText(
                             this@MainActivity,
                             "Oops you just denied the permission.",
                             Toast.LENGTH_LONG
                         ).show()
                     }
                 }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(10.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        val ib_brush : ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener {
            showBrushChooserDialog()
        }

        val clearCanvas : ImageButton = findViewById(R.id.ib_clearCanvas)
        clearCanvas.setOnClickListener {
            drawingView?.clearCanvas()
        }

        val ibUndo : ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibRedo : ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener {
            drawingView?.onClickRedo(this@MainActivity)
        }

        val ibSave : ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            if(isReadStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)

                    saveBitmapFile(getBitmapForView(flDrawingView))
                }
            }
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {
            requestStoragePermission()
        }

        val ibRemoveImage: ImageButton = findViewById(R.id.ib_removeImage)
        ibRemoveImage.setOnClickListener {
            removeImage()
        }

        val ibHidePaths: ImageButton = findViewById(R.id.ib_hidePaths)
        ibHidePaths.setOnTouchListener {
            _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    drawingView?.hidePaths()
                    false
                }
                MotionEvent.ACTION_UP -> {
                    drawingView?.showPaths()
                    false
                }
                else -> false
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun removeImage() {
        val imageBackground: ImageView = findViewById(R.id.iv_background)

        imageBackground.setImageURI(R.drawable.image.toString().toUri())
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            showRationaleDialog("Kids Drawing App", "Kids Drawing App " + "needs to Access Your External Storage")
        } else {
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
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

    fun paintClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view
        }
    }

    private fun showRationaleDialog(
        title: String,
        message: String
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(message).setPositiveButton("Cancel") {
            dialog, _ -> dialog.dismiss()
        }
        builder.create().show()
    }

    private fun getBitmapForView(view: View) : Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()  + File.separator + "KidsDrawingApp_" + System.currentTimeMillis() / 1000 + ".png")

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(this@MainActivity,
                                "File saved successfully: $result",
                                Toast.LENGTH_SHORT
                                ).show()
                            shareImage(FileProvider.getUriForFile(baseContext,"eu.tutorials.kidsdrawingapp.fileprovider", f))
                        } else {
                            Toast.makeText(this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }

        return result
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.dialog_brush_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(uri: Uri){
        val intent = Intent().apply {
            this.action = Intent.ACTION_SEND
            this.putExtra(Intent.EXTRA_STREAM, uri)
            this.type = "image/png"
        }
        startActivity(Intent.createChooser(intent, "Share image via "))
    }
}