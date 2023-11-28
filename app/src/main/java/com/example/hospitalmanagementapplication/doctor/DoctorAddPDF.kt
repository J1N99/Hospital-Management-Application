package com.example.hospitalmanagementapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.hospitalmanagementapplication.databinding.ActivityAddpdfBinding
import com.example.hospitalmanagementapplication.doctor.DoctorHomeActivity
import com.example.hospitalmanagementapplication.doctor.DoctorViewAppointment
import com.example.hospitalmanagementapplication.firebase.firestore
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.hospitalmanagementapplication.model.Illness
import com.example.hospitalmanagementapplication.model.Medicine
import com.example.hospitalmanagementapplication.model.PDFInfo
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.example.hospitalmanagementapplication.utils.Loader
import com.google.android.material.chip.Chip
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class DoctorAddPDF : AppCompatActivity() {
    private lateinit var binding: ActivityAddpdfBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var itemSelected: Any
    private val userSelectedTags = mutableListOf<String>()
    private val medicineSelectedTags = mutableListOf<String>()
    private lateinit var progressDialog: Loader


    /* pass data from other page*/
    private var appointmentID = ""
    private var doctorId = ""
    private var patientID = ""

    /*End*/
    /* Initial data */
    private var hospitalName = ""
    private var hospitalAddress = ""
    private var doctorName = ""
    private var doctorEmail = ""
    private var patientName = ""
    private var patientIC = ""
    private var patientGender = true
    private var illnessName = ""
    private var medicinePDF = ""
    private var actionPDF = ""
    private var PDFDocumentID = ""
    private var pdfFileName = ""
    private var documentID = ""
    /* End */

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityAddpdfBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView, position)
            }
        }

        appointmentID = intent.getStringExtra("appointmentId") ?: ""
        doctorId = intent.getStringExtra("doctorId") ?: ""
        patientID = intent.getStringExtra("patientID") ?: ""
        firebaseAuth = FirebaseAuth.getInstance()





        initializedData()
        initializedMedicineData()
        initializedPDFData()
        firestore().checkPDFandDisplay(this, appointmentID) { pdfInfoList ->
            if (pdfInfoList.isNotEmpty()) {
                val firstPdfInfo = pdfInfoList[0] //cause only call one data to show it
                binding.actionET.setText(firstPdfInfo.action)
                documentID = firstPdfInfo.documentID


                val illness = firstPdfInfo.illness
                val seperateIllness = illness.split(",")
                for (soloIllness in seperateIllness) {
                    userSelectedTags.add(soloIllness)
                    val chip = Chip(this)
                    chip.text = soloIllness
                    chip.isCloseIconVisible = true
                    chip.setOnCloseIconClickListener {
                        // Remove the chip when the close icon is clicked
                        binding.chipGroup.removeView(chip)

                    }

                    // Add the chip to a ChipGroup (assuming you have a ChipGroup named 'binding.chipGroup')
                    binding.chipGroup.addView(chip)
                }

                val medicine = firstPdfInfo.medicine
                val seperateMedicine = medicine.split(",")
                for (soloMedicine in seperateMedicine) {
                    medicineSelectedTags.add(soloMedicine)
                    val chip = Chip(this)
                    chip.text = soloMedicine
                    chip.isCloseIconVisible = true
                    chip.setOnCloseIconClickListener {
                        // Remove the chip when the close icon is clicked
                        binding.chipGroupMedicine.removeView(chip)
                    }

                    // Add the chip to a ChipGroup (assuming you have a ChipGroup named 'binding.chipGroup')
                    binding.chipGroupMedicine.addView(chip)
                }

                binding.button2.visibility = View.VISIBLE
                binding.button.text = "Edit Details"
            }
        }



        binding.button2.setOnClickListener {

            progressDialog = Loader(this@DoctorAddPDF)
            progressDialog.show()
            firestore().checkPDFandDisplay(this, appointmentID) { pdfInfoList ->
                if (pdfInfoList.isNotEmpty()) {
                    val firstPdfInfo = pdfInfoList[0] //cause only call one data to show it
                    var pdfInfoCompleted = false
                    Log.e("Test", firstPdfInfo.illness)
                    medicinePDF = firstPdfInfo.medicine
                    actionPDF = firstPdfInfo.action
                    illnessName = firstPdfInfo.illness
                    pdfInfoCompleted = true
                    if (pdfInfoCompleted) {
                        pdfFileName = firstPdfInfo.PDFName
                        createPdf()
                    }
                }


            }
        }

        binding.button.setOnClickListener {

            var illness = updateChipsToCommaSeparatedString()
            var medicine = updateMedicineChipsToCommaSeparatedString()
            var action = binding.actionET.text.toString().trim()
            if (illness.isEmpty() || (medicine.isEmpty() && action.isEmpty())) {
                Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
            } else {
                progressDialog = Loader(this@DoctorAddPDF)
                progressDialog.show()
                // Format the current date and time
                val currentDateTime =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                // Combine user ID, current date/time, and file extension to create the image file name
                pdfFileName = "$patientID-$currentDateTime.pdf"

                val pdf =
                    PDFInfo("", illness, medicine, action, patientID, appointmentID, pdfFileName)


                if (binding.button.text == "Edit Details") {
                    val dataToUpdate = mapOf(
                        "illness" to illness,
                        "medicine" to medicine,
                        "action" to action,
                        "patientID" to patientID,
                        "appointmentID" to appointmentID,
                        "pdfFileName" to pdfFileName

                    )
                    firestore().updateDocument("pdfinfo", documentID, dataToUpdate)
                    firestore().getPDFInfo(this, documentID) { pdfInfo ->
                        var pdfInfoCompleted = false
                        if (pdfInfo != null) {
                            medicinePDF = pdfInfo.medicine
                            actionPDF = pdfInfo.action
                            illnessName = pdfInfo.illness
                            pdfInfoCompleted = true
                            if (pdfInfoCompleted) {
                                createPdf()
                            }
                        }
                    }
                } else {
                    firestore().createPDFInfo(this, pdf) { documentId ->
                        if (documentId != null) {
                            PDFDocumentID = documentId

                            firestore().getPDFInfo(this, PDFDocumentID) { pdfInfo ->
                                var pdfInfoCompleted = false
                                if (pdfInfo != null) {
                                    if (illness != null) {
                                        medicinePDF = pdfInfo.medicine
                                        actionPDF = pdfInfo.action
                                        illnessName = pdfInfo.illness
                                        pdfInfoCompleted = true
                                        if (pdfInfoCompleted) {
                                            createPdf()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


        }



        binding.autoCompleteTextView.setOnItemClickListener { adapterView, view, position, id ->
            val selectedOption = binding.autoCompleteTextView.text.toString()
            if (selectedOption.isNotEmpty()) {
                // Add the selected option to the user-selected tags list
                userSelectedTags.add(selectedOption)

                // Create a Chip for the selected option
                val chip = Chip(this)
                chip.text = selectedOption
                chip.isCloseIconVisible = true
                chip.setOnCloseIconClickListener {
                    // Remove the selected tag when the close icon is clicked
                    binding.chipGroup.removeView(chip)
                    userSelectedTags.remove(selectedOption)
                }
                binding.chipGroup.addView(chip)
                binding.autoCompleteTextView.text = null // Clear the AutoCompleteTextView
            }
        }


        binding.autoCompleteMedicineET.setOnItemClickListener { adapterView, view, position, id ->
            val selectedOption = binding.autoCompleteMedicineET.text.toString()
            if (selectedOption.isNotEmpty()) {
                // Add the selected option to the user-selected tags list
                medicineSelectedTags.add(selectedOption)

                // Create a Chip for the selected option
                val chip = Chip(this)
                chip.text = selectedOption
                chip.isCloseIconVisible = true
                chip.setOnCloseIconClickListener {
                    // Remove the selected tag when the close icon is clicked
                    binding.chipGroupMedicine.removeView(chip)
                    medicineSelectedTags.remove(selectedOption)
                }
                binding.chipGroupMedicine.addView(chip)
                binding.autoCompleteMedicineET.text = null // Clear the AutoCompleteTextView
            }
        }


    }

    private fun initializedPDFData() {
        //get hospital info

        Log.e("Id", doctorId)
        firestore().getDoctorInfo(this@DoctorAddPDF, doctorId ?: "") { doctorInfo ->
            if (doctorInfo != null) {
                val hospitalID = doctorInfo.hospital
                Log.e("hospitalID", hospitalID)
                firestore().getHospitalDetails(this, hospitalID) { hospital ->
                    if (hospital != null) {
                        hospitalName = hospital.hospital
                        hospitalAddress = hospital.address
                    }
                }
            }
        }
        //get doctor details
        firestore().getOtherUserDetails(this, doctorId) { user ->
            if (user != null) {
                doctorName = user.firstname + " " + user.lastname
                doctorEmail = user.email
            }
        }


        //get patient details
        firestore().getOtherUserDetails(this, patientID) { user ->
            if (user != null) {
                patientName = user.firstname + "" + user.lastname
                patientIC = user.ic
                patientGender = user.gender
            }
        }


    }

    private fun initializedMedicineData() {
        val autoComplete: AutoCompleteTextView = findViewById(R.id.autoCompleteMedicineET)
        val allMedicine: MutableList<Medicine> =
            mutableListOf() // Use MutableList to allow modification

        // Initialize an empty adapter for now
        val adapter = ArrayAdapter(this, R.layout.list_private_government, listOf<String>())
        autoComplete.setAdapter(adapter)



        firestore().getAllMedicine { fetchedMedicine ->
            // Populate the allIllness list with data from Firestore
            allMedicine.clear() // Clear the list to remove any existing data
            allMedicine.addAll(fetchedMedicine)

            // Create the adapter with all illnesses
            val initialAdapter = ArrayAdapter(
                this,
                R.layout.list_private_government,
                allMedicine.map { it.medicineName })

            // Set the adapter for the AutoCompleteTextView
            autoComplete.setAdapter(initialAdapter)
        }

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // Not used in this case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter the list based on user input
                val filteredIllnesses =
                    allMedicine.filter {
                        it.medicineName.contains(
                            s.toString(),
                            ignoreCase = true
                        )
                    }

                if (filteredIllnesses.isEmpty()) {
                    // No results found, clear the text
                    autoComplete.text = null
                }

                // Update the adapter with filtered results
                val filteredAdapter = ArrayAdapter(
                    this@DoctorAddPDF,
                    R.layout.list_private_government,
                    filteredIllnesses.map { it.medicineName })
                autoComplete.setAdapter(filteredAdapter)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not used in this case
            }
        })
    }




    private fun initializedData() {
        val autoComplete: AutoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        val allIllness: MutableList<Illness> =
            mutableListOf() // Use MutableList to allow modification

        // Initialize an empty adapter for now
        val adapter = ArrayAdapter(this, R.layout.list_private_government, listOf<String>())
        autoComplete.setAdapter(adapter)



        firestore().getAllIllness { fetchedIllnesses ->
            // Populate the allIllness list with data from Firestore
            allIllness.clear() // Clear the list to remove any existing data
            allIllness.addAll(fetchedIllnesses)

            // Create the adapter with all illnesses
            val initialAdapter = ArrayAdapter(
                this,
                R.layout.list_private_government,
                allIllness.map { it.illnessName })

            // Set the adapter for the AutoCompleteTextView
            autoComplete.setAdapter(initialAdapter)
        }

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // Not used in this case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter the list based on user input
                val filteredIllnesses =
                    allIllness.filter {
                        it.illnessName.contains(
                            s.toString(),
                            ignoreCase = true
                        )
                    }

                if (filteredIllnesses.isEmpty()) {
                    // No results found, clear the text
                    autoComplete.text = null
                }

                // Update the adapter with filtered results
                val filteredAdapter = ArrayAdapter(
                    this@DoctorAddPDF,
                    R.layout.list_private_government,
                    filteredIllnesses.map { it.illnessName })
                autoComplete.setAdapter(filteredAdapter)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not used in this case
            }
        })
    }

    private fun createPdf() {

        if (isExternalStorageWritable()) {
            val pdfFile =
                File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), pdfFileName)

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
                headerCell1.borderWidthRight = 0f // Remove left border
                headerCell1.setPadding(10f)
                val headerText = Paragraph(
                    "Appointment Report",
                    Font(Font.FontFamily.HELVETICA, 15f, Font.BOLD, BaseColor.BLACK)
                )
                headerText.add(Chunk.NEWLINE)
                headerText.add(
                    Phrase(
                        "Power By Vantist",
                        Font(Font.FontFamily.HELVETICA, 8f, Font.BOLD, BaseColor.BLACK)
                    )
                )
                headerText.add(Chunk.NEWLINE)



                headerText.add(
                    Phrase(
                        hospitalName,
                        Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK)
                    )
                )
                headerText.add(Chunk.NEWLINE)
                headerText.add(
                    Phrase(
                        hospitalAddress,
                        Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK)
                    )
                )
                headerText.add(Chunk.NEWLINE)

                headerText.add(
                    Phrase(
                        "Dr $doctorName",
                        Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK)
                    )
                )
                headerText.add(Chunk.NEWLINE)
                headerText.add(
                    Phrase(
                        doctorEmail,
                        Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK)
                    )
                )
                headerCell1.addElement(headerText)


                val headerCell2 = PdfPCell()
                headerCell2.horizontalAlignment = Element.ALIGN_LEFT
                headerCell2.verticalAlignment = Element.ALIGN_MIDDLE
                headerCell2.setPadding(10f)
                headerCell2.borderWidthLeft = 0f // Remove left border
                //headerCell2.backgroundColor = BaseColor(12, 173, 128) // Background color same as headerCell1


                // Load the image from drawable resources
                val imageBitmap: Bitmap =
                    BitmapFactory.decodeResource(resources, R.drawable.logo)

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

                val contentCell = PdfPCell(
                    Paragraph(
                        "Patient Details\n" +
                                "$patientName\n" +
                                "No IC: $patientIC\n" +
                                "Gender:" + if (patientGender) "Male" else "Female"
                    )
                )
                contentCell.setPadding(10f)

                contentTable.addCell(contentCell)

                // Add the content table to the document
                document.add(contentTable)

                // Create a table for the item list
                val itemListTable = PdfPTable(3)
                itemListTable.widthPercentage = 100f
                itemListTable.setWidths(floatArrayOf(1f, 2f, 2f))



                // Add table headers
                itemListTable.addCell(
                    Phrase(
                        "Illness",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD)
                    )
                )
                itemListTable.addCell(
                    Phrase(
                        "Medicine",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD)
                    )
                )
                itemListTable.addCell(
                    Phrase(
                        "Action",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD)
                    )
                )

                // Item List
                val illnessCell =
                    PdfPCell(Phrase(illnessName, FontFactory.getFont(FontFactory.HELVETICA)))
                illnessCell.minimumHeight = 400f // Adjust the minimum height as needed
                itemListTable.addCell(illnessCell)



                val allMedicine = medicinePDF
                val itemsMedicine = allMedicine.split(",").map { it.trim() }
                val medicineList = mutableListOf<Medicine?>()

                runBlocking {
                    val deferredMedicines = itemsMedicine.map { item ->
                        async(Dispatchers.IO) {
                            firestore().getIllnessByName(this@DoctorAddPDF, item)
                        }
                    }
                    medicineList.addAll(deferredMedicines.awaitAll())
                }

                val concatenatedMedicineInfoList = medicineList.map { medicine ->
                    "${medicine?.medicineName ?: ""} - ${medicine?.medicationTime ?: ""}"
                }
                val resultMedicineName = concatenatedMedicineInfoList.joinToString("\n")





                val medicineCell =
                    PdfPCell(Phrase(resultMedicineName, FontFactory.getFont(FontFactory.HELVETICA)))
                medicineCell.minimumHeight = 400f // Adjust the minimum height as needed
                itemListTable.addCell(medicineCell)

                val actionCell =
                    PdfPCell(Phrase(actionPDF, FontFactory.getFont(FontFactory.HELVETICA)))
                actionCell.minimumHeight = 400f // Adjust the minimum height as needed
                itemListTable.addCell(actionCell)

                val thankYouCell = PdfPCell(
                    Phrase(
                        "Thank you for your appointment.", FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD
                        )
                    )
                )
                thankYouCell.colspan = 3
                itemListTable.addCell(thankYouCell)

                val termsCell = PdfPCell(
                    Phrase(
                        "Terms and conditions: This is an automated generated PDF, signature is not needed.",
                        FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD
                        )
                    )
                )
                termsCell.colspan = 3
                itemListTable.addCell(termsCell)

                // Add the item list table to the document
                document.add(itemListTable)


                // Close the document
                document.close()


                Toast.makeText(this, "PDF created successfully", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
                openPdf(pdfFileName)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error creating PDF", Toast.LENGTH_SHORT).show()
            }

        } else {
            Toast.makeText(this, "External storage not writable", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdf(pdfFileName: String) {
        val pdfFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), pdfFileName)

        if (pdfFile.exists()) {
            try {
                val pdfUri = FileProvider.getUriForFile(
                    this,
                    "com.example.hospitalmanagementapplication.fileprovider",
                    pdfFile
                )

                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(pdfUri, "application/pdf")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Create a chooser dialog to let the user pick an app
                val chooser = Intent.createChooser(intent, "Open PDF with...")

                if (chooser.resolveActivity(packageManager) != null) {
                    startActivity(chooser)
                    finish()
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

    // Function to update the comma-separated string based on userSelectedTags
    fun updateChipsToCommaSeparatedString(): String {
        return userSelectedTags.joinToString(", ")
    }
    fun updateMedicineChipsToCommaSeparatedString(): String {
        return medicineSelectedTags.joinToString(", ")
    }
}

