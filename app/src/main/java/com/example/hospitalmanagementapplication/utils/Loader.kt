package com.example.hospitalmanagementapplication.utils

import android.app.Dialog
import android.content.Context
import android.view.Window
import com.example.hospitalmanagementapplication.R

class Loader(context: Context) {

    //TODO Better Design

    private val dialog: Dialog = Dialog(context)

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.activity_loader)
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }
}