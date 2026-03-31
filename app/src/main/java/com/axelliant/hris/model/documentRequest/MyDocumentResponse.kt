package com.axelliant.hris.model.documentRequest

import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.dashboard.FilterModel

data class MyDocumentResponse(
    val employee_form_status:  ArrayList<FilterModel>? = null,
    val employee_form_count: Int? = null,
    val employee_forms:  ArrayList<DocumentForm>? = null,
    val meta: Meta
)