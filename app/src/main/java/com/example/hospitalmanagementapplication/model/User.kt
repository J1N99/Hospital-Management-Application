package com.example.hospitalmanagementapplication.model

data class User(
    val id:String="",
    val firstname:String="",
    val lastname:String="",
    val gender:Boolean=false,
    val dob:String="",
    val ic:String="",
    val position:Int=5
)