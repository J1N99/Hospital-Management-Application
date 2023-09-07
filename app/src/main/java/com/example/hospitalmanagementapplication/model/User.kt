package com.example.hospitalmanagementapplication.model

data class User(
    val id:String="",
    val firstname:String="",
    val lastname:String="",
    val gender:Boolean,
    val DOB:String="",
    val ic:String="",
    val position:Number=1
)