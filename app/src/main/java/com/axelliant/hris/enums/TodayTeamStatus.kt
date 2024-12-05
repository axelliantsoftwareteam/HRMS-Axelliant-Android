package com.axelliant.hris.enums

enum class TodayTeamStatus (val value: String){
    AllTeamMember("All Team Member"),
    CheckIn("Check In"),
    CheckOut("Check Out"),
    OnLeave("On Leave"),
    InOffice("In Office"),
    WFH("Work From Home"),
    MissedPunch("Absent")
}
