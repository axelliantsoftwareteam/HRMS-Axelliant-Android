package com.axelliant.hris.model

import okhttp3.MultipartBody

class ImagePath{
    var file: MultipartBody.Part?=null
    var docname: String?=null
    var is_private: Int?=0
    var folder: String?=null
    var doctype: String?=null
}
