package com.axelliant.hris.model.documentRequest

data class SubmitDocument(
    var resource_hour_id_list: ArrayList<String>?=null,
    var status: String?=null
)