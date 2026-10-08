package com.axelliant.hris.repos

import com.axelliant.hris.network.ApiInterface

/**
 * The request forms now call [ApiInterface] through `RequestViewModel`. Nothing is left here; the
 * class only stays because `di/SingeltonModules.kt` still declares `factory { RequestRepo(get()) }`.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class RequestRepo(apiInterface: ApiInterface)
