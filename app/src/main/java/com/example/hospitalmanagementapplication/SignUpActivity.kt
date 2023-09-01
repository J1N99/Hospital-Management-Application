package com.example.hospitalmanagementapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.hospitalmanagementapplication.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth


class SignUpActivity:AppCompatActivity() {
    private lateinit var binding:ActivitySignupBinding
    private lateinit var firebaseAuth:FirebaseAuth
    override fun onCreate(saveInstanceState:Bundle?)
    {
            super.onCreate(saveInstanceState)
            binding= ActivitySignupBinding.inflate(layoutInflater)
            setContentView(binding.root)
            firebaseAuth=FirebaseAuth.getInstance()
            binding.navigationSignIn.setOnClickListener{
                val intent= Intent(this,SignInActivity::class.java )
                startActivity(intent)
            }
            binding.signUp.setOnClickListener{
                val email=binding.emailEt.text.toString()
                val password= binding.passET.text.toString()
                val confirmPassword=binding.confirmPassEt.text.toString()
                if(email.isNotEmpty()&&password.isNotEmpty()&&confirmPassword.isNotEmpty())
                {

                     if(password==(confirmPassword))
                     {
                         if(password.length<8)
                         {
                             Toast.makeText(this,"The password length should longer than 8".toString(),Toast.LENGTH_LONG).show()
                         }
                         else if (!password.any { it.isDigit() } || !password.any { it.isLetter() })
                         {
                             Toast.makeText(this,"The password should include a character and a digit".toString(),Toast.LENGTH_LONG).show()
                         }
                         else {
                             firebaseAuth.createUserWithEmailAndPassword(email, password)
                                 .addOnCompleteListener {

                                     if (it.isSuccessful) {
                                         val intent = Intent(this, SignInActivity::class.java)
                                         startActivity(intent)
                                     } else {

                                         //this is create the exception if error
                                         Toast.makeText(
                                             this,
                                             it.exception.toString(),
                                             Toast.LENGTH_LONG
                                         ).show()
                                     }
                                 }
                         }
                     }else
                     {
                         Toast.makeText(this,"Password and confirm password is not same",Toast.LENGTH_LONG).show()
                     }
                }
                else
                {
                    Toast.makeText(this,"Please fill in all the required field",Toast.LENGTH_LONG).show()
                }
            }
    }
}