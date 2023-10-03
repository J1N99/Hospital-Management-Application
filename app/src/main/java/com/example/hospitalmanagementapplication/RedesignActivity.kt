package com.example.hospitalmanagementapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import com.itextpdf.text.*
import java.io.ByteArrayOutputStream
import java.io.IOException

class RedesignActivity : AppCompatActivity() {

    private val pdfFileName = "appointment_report.pdf"

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
            val pdfFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), pdfFileName)

            try {
                val document = Document(PageSize.A4)
                val pdfWriter = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
                document.open()

                // Create a table for the header
                val headerTable = PdfPTable(2)
                headerTable.widthPercentage = 100f
                headerTable.setWidths(floatArrayOf(3f, 1f))

                val headerCell1 = PdfPCell()
                //headerCell1.backgroundColor = BaseColor(12, 173, 128)
                headerCell1.horizontalAlignment = Element.ALIGN_LEFT
                headerCell1.verticalAlignment = Element.ALIGN_MIDDLE
                headerCell1.setPadding(10f)
                headerCell1.borderWidthRight = 0f // Remove left border

                val headerText = Paragraph("Appointment Report", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK))
                headerText.add(Chunk.NEWLINE)
                headerText.add(Phrase("Vantist", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK)))
                headerText.add(Chunk.NEWLINE)
                headerText.add(Phrase("No 25,Lorong Tanjung Aman 12, Taman Tanjung Aman", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK)))
                headerText.add(Chunk.NEWLINE)
                headerText.add(Phrase("12300 Butterworth, Pulau Pinang", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK)))
                headerText.add(Chunk.NEWLINE)
                headerText.add(Phrase("Dr Ang Wei Jin", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK)))
                headerText.add(Chunk.NEWLINE)
                headerText.add(Phrase("01112896803", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK)))
                headerCell1.addElement(headerText)

                val headerCell2 = PdfPCell()
                headerCell2.horizontalAlignment = Element.ALIGN_LEFT
                headerCell2.verticalAlignment = Element.ALIGN_MIDDLE
                headerCell2.setPadding(10f)
                headerCell2.borderWidthLeft = 0f // Remove left border
                //headerCell2.backgroundColor = BaseColor(12, 173, 128) // Background color same as headerCell1


// Load the image from drawable resources
                val imageBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.logo)

// Convert the Bitmap to a byte array
                val byteArrayOutputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val imageByteArray: ByteArray = byteArrayOutputStream.toByteArray()

// Create an iText Image instance from the byte array
                val image = Image.getInstance(imageByteArray)

// Scale the image if needed
                image.scaleToFit(120f, 120f)
                headerCell2.addElement(image)

                headerTable.addCell(headerCell1)
                headerTable.addCell(headerCell2)


                // Add the header table to the document
                document.add(headerTable)

                // Create a table for the content
                val contentTable = PdfPTable(1)
                contentTable.widthPercentage = 100f

                val contentCell = PdfPCell(Paragraph("Patient Name\n" +
                        "Chooi Chee Kean\n" +
                        "No IC: 991125075103\n" +
                        "Gender: Male"))
                contentCell.setPadding(10f)

                contentTable.addCell(contentCell)

                // Add the content table to the document
                document.add(contentTable)

                // Create a table for the item list
                val itemListTable = PdfPTable(4)
                itemListTable.widthPercentage = 100f
                itemListTable.setWidths(floatArrayOf(3f, 1f, 1f, 1f))

                // Add table headers
                itemListTable.addCell(Phrase("Item name", FontFactory.getFont(FontFactory.HELVETICA_BOLD)))
                itemListTable.addCell(Phrase("Price", FontFactory.getFont(FontFactory.HELVETICA_BOLD)))
                itemListTable.addCell(Phrase("Quantity", FontFactory.getFont(FontFactory.HELVETICA_BOLD)))
                itemListTable.addCell(Phrase("Total", FontFactory.getFont(FontFactory.HELVETICA_BOLD)))

                // Add item rows (You will need to loop through your data here)
                // Example:
                // itemListTable.addCell("Item 1")
                // itemListTable.addCell("RM50.00")
                // itemListTable.addCell("2")
                // itemListTable.addCell("RM100.00")

                // Add the grand total row and the thank you message
                val grandTotalCell = PdfPCell(Phrase("Grand total: RM200.00"))
                grandTotalCell.colspan = 4
                itemListTable.addCell(grandTotalCell)

                val thankYouCell = PdfPCell(Phrase("Thank you for your appointment.", FontFactory.getFont(FontFactory.HELVETICA_BOLD)))
                thankYouCell.colspan = 4
                itemListTable.addCell(thankYouCell)

                val termsCell = PdfPCell(Phrase("Terms and conditions: This is an automated generated PDF; signature is not needed.", FontFactory.getFont(FontFactory.HELVETICA_BOLD)))
                termsCell.colspan = 4
                itemListTable.addCell(termsCell)

                // Add the item list table to the document
                document.add(itemListTable)

                // Close the document
                document.close()

                Toast.makeText(this, "PDF created successfully", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error creating PDF", Toast.LENGTH_SHORT).show()
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