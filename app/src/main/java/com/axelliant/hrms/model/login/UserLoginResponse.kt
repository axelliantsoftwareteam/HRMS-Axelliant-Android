package com.axelliant.hrms.model.login

import com.axelliant.hrms.model.base.Meta

data class UserLoginResponse(
    val access_token : AccessInfo?=null,
    val token:String?=null,
    val meta: Meta
)
class AccessInfo{
    val api_key: String?=null
    val api_sec: String?=null
    val api_secret: String?=null
    val email: String?=null
}
