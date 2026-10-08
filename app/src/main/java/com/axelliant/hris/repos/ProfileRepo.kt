package com.axelliant.hris.repos

import com.axelliant.hris.config.AppConst
import com.axelliant.hris.model.profile.CertificationCreateRequest
import com.axelliant.hris.model.profile.CertificationCreateResponse
import com.axelliant.hris.model.profile.CertificationListResponse
import com.axelliant.hris.model.profile.ProfileResponse
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult

/** Profile and certification calls; results are parsed by [com.axelliant.hris.network.ApiParser]. */
class ProfileRepo(private val apiInterface: ApiInterface) {

    fun getProfile(onResult: (ApiResult<ProfileResponse>) -> Unit) {
        apiInterface.getProfileOfEmployee(AppConst.authHeader())
            .enqueueResult<ProfileResponse>(ProfileResponse::class.java, onResult)
    }

    fun getCertifications(onResult: (ApiResult<CertificationListResponse>) -> Unit) {
        apiInterface.getEmployeeCertifications(AppConst.authHeader())
            .enqueueResult<CertificationListResponse>(CertificationListResponse::class.java, onResult)
    }

    fun createCertification(
        request: CertificationCreateRequest,
        onResult: (ApiResult<CertificationCreateResponse>) -> Unit
    ) {
        apiInterface.createEmployeeCertification(AppConst.authHeader(), request)
            .enqueueResult<CertificationCreateResponse>(CertificationCreateResponse::class.java, onResult)
    }
}
