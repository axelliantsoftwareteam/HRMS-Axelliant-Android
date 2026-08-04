package com.axelliant.hris.model

import android.graphics.drawable.Drawable

//data class Modules(val id:Int, val name:String,val description: String, val color: Int, val drawable:Drawable?)
data class Modules(val id:Int, val name:String,val description: String, val color: Drawable?, val drawable:Drawable?, val progressValue: Int? = null, val progressMax: Int? = 0)