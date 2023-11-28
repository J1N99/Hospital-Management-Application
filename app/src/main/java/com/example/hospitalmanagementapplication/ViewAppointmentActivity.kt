package com.example.hospitalmanagementapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.hospitalmanagementapplication.databinding.ActivityViewappointmentBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.example.hospitalmanagementapplication.utils.IntentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hospitalmanagementapplication.model.Appointment
import com.example.hospitalmanagementapplication.model.Medicine
import com.example.hospitalmanagementapplication.utils.Loader
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream


class ViewAppointmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewappointmentBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var appointmentAdapter: AppointmentAdapter
    private lateinit var progressDialog: Loader

    private var appointmentID = ""
    private var doctorId = ""
    private var patientID = ""
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewappointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView, position)
            }
        }

        firebaseAuth = FirebaseAuth.getInstance()


        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        appointmentAdapter = AppointmentAdapter()
        recyclerView.adapter = appointmentAdapter

        firestore().getAndDisplayAppointments { appointments ->
            appointmentAdapter.setAppointments(appointments)
        }


    }

    // Define your ViewHolder class for the RecyclerView
    private inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Initialize your views here (e.g., TextViews, ImageView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val doctorTextView: TextView = itemView.findViewById(R.id.doctorTextView)
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val cancelAppointment: TextView = itemView.findViewById(R.id.cancelAppointment)
        val viewPDF: TextView = itemView.findViewById(R.id.viewPDFAppointment)
    }

    // Define your Adapter class for the RecyclerView
    private inner class AppointmentAdapter : RecyclerView.Adapter<AppointmentViewHolder>() {
        private var appointments: List<Appointment> = emptyList()

        fun setAppointments(appointments: List<Appointment>) {
            this.appointments = appointments
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.appointment_card_view, parent, false)
            return AppointmentViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
            val appointment = appointments[position]
            // Bind data to the ViewHolder views here
            holder.dateTextView.text = "Date: ${appointment.dateAppointment}"
            holder.timeTextView.text = "Time: ${appointment.time}"

            val dateFormat =
                DateTimeFormatter.ofPattern("yyyy-MM-dd") // Define the format of your date string
            val dateString = appointment.dateAppointment

            val currentDate = LocalDate.now()
            val date = LocalDate.parse(dateString, dateFormat)

            val daysUntilDate = ChronoUnit.DAYS.between(currentDate, date)

            if (daysUntilDate <= 0) {
                // if over date then the close appointment Date gone
                holder.cancelAppointment.visibility = View.GONE

                firestore().checkPDFandDisplay(
                    this@ViewAppointmentActivity,
                    appointment.documentID ?: ""
                ) { pdfInfoList ->
                    //if got pdf
                    if (pdfInfoList.isNotEmpty()) {
                        holder.viewPDF.visibility = View.VISIBLE
                    }
                }
            } else {
                // if over date then the close appointment Date gone
                holder.cancelAppointment.visibility = View.VISIBLE

                firestore().checkPDFandDisplay(
                    this@ViewAppointmentActivity,
                    appointment.documentID ?: ""
                ) { pdfInfoList ->
                    //if got pdf
                    if (pdfInfoList.isNotEmpty()) {
                        holder.viewPDF.visibility = View.GONE
                    }
                }
            }

            holder.cancelAppointment.setOnClickListener {
                showConfirmationDialog(appointment.documentID ?: "") { confirmed ->
                    if (confirmed) {
                        Log.e("Deleted", "Success")
                    }
                }


            }

            holder.viewPDF.setOnClickListener {
                progressDialog = Loader(this@ViewAppointmentActivity)
                progressDialog.show()
                appointmentID = appointment.documentID ?: ""
                doctorId = appointment.doctorId ?: ""
                patientID = appointment.userID ?: ""

                var callbackCount = 0

                // Callback for getDoctorInfo
                firestore().getDoctorInfo(
                    this@ViewAppointmentActivity,
                    doctorId ?: ""
                ) { doctorInfo ->
                    if (doctorInfo != null) {
                        val hospitalID = doctorInfo.hospital
                        Log.e("hospitalID", hospitalID)
                        firestore().getHospitalDetails(
                            this@ViewAppointmentActivity,
                            hospitalID
                        ) { hospital ->
                            if (hospital != null) {
                                hospitalName = hospital.hospital
                                hospitalAddress = hospital.address
                            }
                            callbackCount++
                            checkCallbacks(callbackCount)
                        }
                    }
                }

                // Callback for getOtherUserDetails (doctor)
                firestore().getOtherUserDetails(
                    this@ViewAppointmentActivity,
                    doctorId ?: ""
                ) { user ->
                    if (user != null) {
                        doctorName = user.firstname + " " + user.lastname
                        doctorEmail = user.email
                    }
                    callbackCount++
                    checkCallbacks(callbackCount)
                }

                // Callback for getOtherUserDetails (patient)
                firestore().getOtherUserDetails(
                    this@ViewAppointmentActivity,
                    patientID ?: ""
                ) { user ->
                    if (user != null) {
                        patientName = user.firstname + "" + user.lastname
                        patientIC = user.ic
                        patientGender = user.gender
                    }
                    callbackCount++
                    checkCallbacks(callbackCount)
                }

                // Callback for checkPDFandDisplay
                firestore().checkPDFandDisplay(
                    this@ViewAppointmentActivity,
                    appointmentID ?: ""
                ) { pdfInfoList ->
                    if (pdfInfoList.isNotEmpty()) {
                        val firstPdfInfo = pdfInfoList[0]
                        illnessName = firstPdfInfo.illness
                        medicinePDF = firstPdfInfo.medicine
                        actionPDF = firstPdfInfo.action
                        pdfFileName = firstPdfInfo.PDFName
                    }
                    callbackCount++
                    checkCallbacks(callbackCount)
                }
            }


            val doctorId = appointment.doctorId
            firestore().getOtherUserDetails(
                this@ViewAppointmentActivity,
                appointment.doctorId ?: ""
            ) { user ->
                if (user != null) {
                    holder.doctorTextView.text = "DR " + user.firstname + " " + user.lastname
                }
            }

            firestore().getDoctorInfo(this@ViewAppointmentActivity, doctorId ?: "") { doctorInfo ->
                if (doctorInfo != null) {

                    val imagefile = doctorInfo.profileImageUri
                    val imagePath = "doctorProfileImages/$imagefile"
                    // Initialize Firebase Storage
                    val storage = Firebase.storage

                    // Reference to the image in Firebase Storage (replace "your-image-path" with the actual path)
                    val storageRef = storage.reference.child(imagePath)

                    // Determine the file extension based on the MIME type
                    val fileExtension =
                        getFileExtension(imagefile!!.toUri()) // Pass the URL of the downloaded image
                    val localFile = File.createTempFile("images", ".$fileExtension")

                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Handle the successful download here
                        // You can set the retrieved image to the specific ImageView using Glide
                        val downloadedUri = uri.toString() // The URL to the downloaded image
                        loadAndDisplayImage(
                            downloadedUri,
                            holder.imageView
                        ) // Modify this line as needed
                    }.addOnFailureListener { e ->
                        // Handle any errors that occurred during the download
                        // e.g., handle network errors or file not found errors
                    }
                } else {
                    Log.d("Fail", "Fail")
                    Toast.makeText(this@ViewAppointmentActivity, "Fail", Toast.LENGTH_SHORT)
                }
            }
        }

        override fun getItemCount(): Int {
            return appointments.size
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        val contentResolver = contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun loadAndDisplayImage(imageUrl: String, imageView: ImageView) {
        Glide.with(this)
            .load(imageUrl)
            .into(imageView) // Assuming 'imageView' is the target ImageView where you want to display the image
    }

    fun checkCallbacks(callbackCount: Int) {
        if (callbackCount == 4) { // Adjust the count based on the number of callbacks
            createPdf()
        }
    }
    private fun createPdf() {

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
                            firestore().getIllnessByName(this@ViewAppointmentActivity, item)
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
                openPdf()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error creating PDF", Toast.LENGTH_SHORT).show()
            }

        } else {
            Toast.makeText(this, "External storage not writable", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdf() {
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


    private fun showConfirmationDialog(appointmentID: String, callback: (Boolean) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Cancel Appointment")
        builder.setMessage("Do you want to cancel the appointment?")

        builder.setPositiveButton("Yes") { _, _ ->
            firestore().deleteDocument(appointmentID ?: "", "appointments",
                onSuccess = {
                    // Create an Intent to restart the current activity
                    val intent = intent
                    finish() // Finish the current activity
                    startActivity(intent) // Start a new instance of the current activity
                },
                onFailure = { e ->
                    Toast.makeText(this, "ERROR:Error deleting document+$e", Toast.LENGTH_SHORT)
                })
            callback(true)
        }

        builder.setNegativeButton("No") { _, _ ->
            callback(false)
        }

        builder.setCancelable(false)
        builder.show()
    }


}

