package com.example.hospitalmanagementapplication.utils
import android.app.Dialog
import android.content.Context
import android.view.Window
import com.example.hospitalmanagementapplication.R

class Loader(context: Context) {

    // Initialize the Dialog with the custom layout
    private val dialog: Dialog = Dialog(context).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_loader)
        setCancelable(false)
    }

    // Show the dialog
    fun show() {
        dialog.show()
    }

    // Dismiss the dialog
    fun dismiss() {
        dialog.dismiss()
    }
}
