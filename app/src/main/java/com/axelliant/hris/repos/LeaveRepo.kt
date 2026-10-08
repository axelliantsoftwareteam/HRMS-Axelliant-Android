package com.axelliant.hris.repos

import com.axelliant.hris.network.ApiInterface

/**
 * The Leave screen now calls [ApiInterface] through `LeaveViewModel`. Nothing is left here; the class
 * only stays because `di/SingeltonModules.kt` still declares `factory { LeaveRepo(get()) }`.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class LeaveRepo(apiInterface: ApiInterface)
