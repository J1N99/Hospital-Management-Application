package com.example.hospitalmanagementapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalmanagementapplication.databinding.ActivityAdddepartmentBinding
import com.example.hospitalmanagementapplication.firebase.firestore
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.hospitalmanagementapplication.model.department
import com.example.hospitalmanagementapplication.utils.IntentManager


class AddDepartmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdddepartmentBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imageView: ImageView
    private var itemSelected: Any=""
    private var departmentID = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdddepartmentBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()

        firestore().getUserPosition(this) { position ->
            if (position != null) {
                bottomNavigationView = findViewById(R.id.bottomNavigationView)
                bottomNavigationView.setSelectedItemId(R.id.others);
                IntentManager(this, bottomNavigationView,position)
            }
        }

        departmentID = intent.getStringExtra("departmentID") ?: ""



        if (departmentID != "") {
            binding.button.text = "Save Edit"
            binding.buttonDeleteDepartment.visibility = View.VISIBLE
            firestore().getDepartmentDetails(this, departmentID) { department ->
                if (department != null) {

                    binding.departmentET.setText(department.department)
                } else {
                    Toast.makeText(this, "Hospital is null", Toast.LENGTH_SHORT).show()
                }
            }
        }


        binding.button.setOnClickListener {

            var department = binding.departmentET.text.toString().trim()
            if (department.isEmpty()) {
                Toast.makeText(this, "Please enter all the fields", Toast.LENGTH_SHORT).show()
            } else {
                if (binding.button.text == "Save Edit") {


                    val dataToUpdate = mapOf(
                        "department" to department,

                    )

                    firestore().updateDocument("department", departmentID, dataToUpdate)
                    showDialogSuccessUpdate("The information have updated!")
                } else {
                    val departmentInfo =
                        department("", department)
                    firestore().createDepartment(this, departmentInfo)
                    showDialogSuccessUpdate("The information have created!")
                }
            }
        }

        binding.buttonDeleteDepartment.setOnClickListener {

            AlertDialog.Builder(this)
                .setMessage("Are you sure you want to delete this department?")
                .setPositiveButton("Yes") { _, _ ->
                    firestore().deleteDocument(departmentID, "department",
                        onSuccess = {
                            val intent = Intent(this, AllDepartmentActivity::class.java)
                            startActivity(intent)
                        },
                        onFailure = { e ->
                            Log.w("ERROR", "Error deleting document", e)
                        })
                }
                .setNegativeButton("No", null)
                .show()
        }

    }


    private fun showDialogSuccessUpdate(textShow: String) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_dialog_verification, null)

        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        textViewMessage.text = textShow
        builder.setView(dialogView)
        val dialog = builder.create()

        val buttonDismiss = dialogView.findViewById<Button>(R.id.buttonDismiss)
        var buttonEmail = dialogView.findViewById<Button>(R.id.buttonResentVerification)
        buttonEmail.visibility = View.GONE
        buttonDismiss.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, AllDepartmentActivity::class.java)
            startActivity(intent)
            finish()
        }
        dialog.show()
    }

    override fun onBackPressed() {
        val intent = Intent(this, AllDepartmentActivity::class.java)
        startActivity(intent)
        finish()
    }
}

