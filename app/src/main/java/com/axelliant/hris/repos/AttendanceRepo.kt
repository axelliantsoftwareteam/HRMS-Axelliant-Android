package com.axelliant.hris.repos

import com.axelliant.hris.network.ApiInterface

/**
 * The Attendance screen now calls [ApiInterface] through `AttendanceViewModel`. Nothing is left here;
 * the class only stays because `di/SingeltonModules.kt` still declares `factory { AttendanceRepo(get()) }`.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class AttendanceRepo(apiInterface: ApiInterface)
