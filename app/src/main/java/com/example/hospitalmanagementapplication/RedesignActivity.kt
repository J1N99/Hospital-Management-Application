package com.example.hospitalmanagementapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream

class RedesignActivity : AppCompatActivity() {

    private val pdfFileName = "sample.pdf"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_redesign)

        // Request storage permission if not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }
    }

    fun createPdf(view: View) {
        if (isExternalStorageWritable()) {
            val pdfFile =
                File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), pdfFileName)

            try {
                val document = Document(PageSize.A4)
                val pdfWriter = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
                document.open()

                // Add content to the PDF
                val paragraph =
                    Paragraph("Hello, this is a sample PDF created with iTextPDF in Android.")
                document.add(paragraph)

                // Close the document
                document.close()

                Toast.makeText(this, "PDF created successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "External storage not writable", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdf(view: View) {
        val pdfFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), pdfFileName)

        if (pdfFile.exists()) {
            try {
                val pdfUri = FileProvider.getUriForFile(
                    this,
                    "com.example.hospitalmanagementapplication.fileprovider",
                    pdfFile
                )

                // Create an intent to open the PDF file
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(pdfUri, "application/pdf")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Check if there is a PDF viewer app installed
                val packageManager = packageManager
                val activities = packageManager.queryIntentActivities(intent, 0)

                if (activities.isNotEmpty()) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }
}
