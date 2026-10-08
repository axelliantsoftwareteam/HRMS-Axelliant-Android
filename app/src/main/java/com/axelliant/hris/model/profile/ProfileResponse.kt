package com.axelliant.hris.model.profile

import com.axelliant.hris.model.dashboard.EmployProfile
import com.axelliant.hris.model.payslip.asLenientInt
import com.axelliant.hris.model.payslip.cleaned
import com.axelliant.hris.model.payslip.isTicked
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.Calendar

/**
 * `get_profile_of_employee`. Fields whose shape HRIS varies (lists, the manager, check boxes) are kept
 * as raw JSON and read leniently through the helpers below, so one odd field never hides the profile.
 */
data class ProfileResponse(
    val user_id: String? = null,
    val employee_name: String? = null,
    val father_name: String? = null,
    val person_to_be_contacted: String? = null,
    val date_of_birth: String? = null,
    val gender: String? = null,
    val custom_cnic: String? = null,
    val company: String? = null,
    val custom_employee_code: String? = null,
    val employee_code: String? = null,
    val designation: String? = null,
    val department: String? = null,
    val date_of_joining: String? = null,
    val final_confirmation_date: String? = null,
    val personal_email: String? = null,
    val cell_number: String? = null,
    val emergency_phone_number: String? = null,
    val relation: String? = null,
    val permanent_address: String? = null,
    val image: String? = null,
    val reports_to: JsonElement? = null,
    val emp_skill: JsonElement? = null,
    val education: JsonElement? = null,
    val work_history: JsonElement? = null,
    val health_insurance_no: String? = null,
    val health_insurance_provider: String? = null,
    val custom_registered: Any? = null,
    val custom_life_insurance_registered: Any? = null,
    val custom_eobi_registered: Any? = null,
    val custom_provident_fund_registered: Any? = null,
    val custom_dependents: JsonElement? = null
) {
    /** Code shown on the page: `custom_employee_code`, else `employee_code`. */
    val displayCode: String? get() = custom_employee_code.cleaned() ?: employee_code.cleaned()

    /** True once at least one identifying field arrived, from HRIS or from the dashboard. */
    val hasEmployeeRecord: Boolean
        get() = listOf(employee_name, custom_employee_code, employee_code, designation).any { !it.cleaned().isNullOrEmpty() }

    val managerName: String? get() = reports_to.asObject()?.text("employee_name")

    val healthInsuranceRegistered: Boolean get() = custom_registered.isTicked()
    val lifeInsuranceRegistered: Boolean get() = custom_life_insurance_registered.isTicked()
    val eobiRegistered: Boolean get() = custom_eobi_registered.isTicked()
    val providentFundRegistered: Boolean get() = custom_provident_fund_registered.isTicked()

    /** Skills arrive as strings, one-element arrays or `{skill: "..."}` objects. */
    fun skills(): List<String> = emp_skill.asArray().mapNotNull { element ->
        when {
            element == null || element.isJsonNull -> null
            element.isJsonPrimitive -> element.asString
            element.isJsonArray -> element.asJsonArray.firstOrNull()?.takeIf { it.isJsonPrimitive }?.asString
            element.isJsonObject -> element.asJsonObject.text("skill")
            else -> null
        }.cleaned()
    }

    fun educationRows(): List<ProfileEducation> = education.asArray().mapNotNull { it.asObject() }.map {
        ProfileEducation(
            school = it.text("school_univ"),
            qualification = it.text("qualification"),
            level = it.text("level"),
            year = it.text("year_of_passing")
        )
    }

    fun workHistoryRows(): List<ProfileWork> = work_history.asArray().mapNotNull { it.asObject() }.map {
        ProfileWork(company = it.text("company_name"), designation = it.text("designation"), salary = it.text("salary"))
    }

    /** Dependents without a name are dropped; age is worked out from the birth date when HRIS sends none. */
    fun dependents(): List<ProfileDependent> = custom_dependents.asArray().mapNotNull { it.asObject() }.mapNotNull { row ->
        val name = row.firstText("dependent_name", "full_name") ?: return@mapNotNull null
        val dob = row.firstText("date_of_birth", "dob").orEmpty()
        ProfileDependent(
            name = name,
            dateOfBirth = dob,
            age = row.get("age")?.takeIf { it.isJsonPrimitive }?.asString.asLenientInt() ?: ageFromDateOfBirth(dob),
            relation = row.firstText("relation", "relationship").orEmpty(),
            cnic = row.firstText("cnic", "custom_cnic").orEmpty()
        )
    }

    /** Fills blank text fields from the dashboard employee; the dashboard photo wins (same picture as Home). */
    fun mergedWith(dashboard: ProfileResponse): ProfileResponse {
        fun pick(own: String?, other: String?): String? = own.cleaned()?.let { own } ?: other
        return copy(
            user_id = pick(user_id, dashboard.user_id),
            employee_name = pick(employee_name, dashboard.employee_name),
            father_name = pick(father_name, dashboard.father_name),
            person_to_be_contacted = pick(person_to_be_contacted, dashboard.person_to_be_contacted),
            date_of_birth = pick(date_of_birth, dashboard.date_of_birth),
            gender = pick(gender, dashboard.gender),
            custom_cnic = pick(custom_cnic, dashboard.custom_cnic),
            company = pick(company, dashboard.company),
            custom_employee_code = pick(custom_employee_code, dashboard.custom_employee_code),
            employee_code = pick(employee_code, dashboard.employee_code),
            designation = pick(designation, dashboard.designation),
            department = pick(department, dashboard.department),
            date_of_joining = pick(date_of_joining, dashboard.date_of_joining),
            final_confirmation_date = pick(final_confirmation_date, dashboard.final_confirmation_date),
            personal_email = pick(personal_email, dashboard.personal_email),
            cell_number = pick(cell_number, dashboard.cell_number),
            emergency_phone_number = pick(emergency_phone_number, dashboard.emergency_phone_number),
            relation = pick(relation, dashboard.relation),
            permanent_address = pick(permanent_address, dashboard.permanent_address),
            image = dashboard.image.cleaned() ?: pick(image, null),
            reports_to = reports_to.nullIfJsonNull() ?: dashboard.reports_to,
            emp_skill = emp_skill.nullIfJsonNull() ?: dashboard.emp_skill,
            education = education.nullIfJsonNull() ?: dashboard.education,
            work_history = work_history.nullIfJsonNull() ?: dashboard.work_history
        )
    }

    companion object {
        /** The dashboard employee read as a profile (both come from HRIS with the same field names). */
        fun fromDashboard(employee: EmployProfile?): ProfileResponse {
            if (employee == null) return ProfileResponse()
            val manager = employee.reports_to?.let {
                JsonObject().apply {
                    it.employee_name?.let { v -> addProperty("employee_name", v) }
                    it.user_id?.let { v -> addProperty("user_id", v) }
                    it.name?.let { v -> addProperty("name", v) }
                }
            }
            return ProfileResponse(
                user_id = employee.user_id,
                employee_name = employee.employee_name,
                date_of_birth = employee.date_of_birth,
                gender = employee.gender,
                company = employee.company,
                custom_employee_code = employee.custom_employee_code,
                designation = employee.designation,
                department = employee.department,
                image = employee.image,
                reports_to = manager
            )
        }

        /** Whole years between a "yyyy-MM-dd" birth date and today; null when unreadable or in the future. */
        fun ageFromDateOfBirth(text: String?): Int? {
            val parts = text.orEmpty().take(10).split("-").mapNotNull { it.toIntOrNull() }
            if (parts.size != 3) return null
            val today = Calendar.getInstance()
            var years = today.get(Calendar.YEAR) - parts[0]
            val month = today.get(Calendar.MONTH) + 1
            if (month < parts[1] || (month == parts[1] && today.get(Calendar.DAY_OF_MONTH) < parts[2])) years--
            return years.takeIf { it >= 0 }
        }
    }
}

data class ProfileEducation(val school: String?, val qualification: String?, val level: String?, val year: String?)

data class ProfileWork(val company: String?, val designation: String?, val salary: String?)

data class ProfileDependent(
    val name: String,
    val dateOfBirth: String,
    val age: Int?,
    val relation: String,
    val cnic: String
)

// ---- lenient JSON readers -------------------------------------------------------------------------

private fun JsonElement?.nullIfJsonNull(): JsonElement? = this?.takeIf { !it.isJsonNull }

private fun JsonElement?.asObject(): JsonObject? = this?.takeIf { it.isJsonObject }?.asJsonObject

private fun JsonElement?.asArray(): List<JsonElement?> =
    this?.takeIf { it.isJsonArray }?.asJsonArray?.toList().orEmpty()

private fun JsonObject.text(key: String): String? =
    get(key)?.takeIf { it.isJsonPrimitive }?.asString

/** First non-blank text among [keys] (HRIS spells some fields two ways). */
private fun JsonObject.firstText(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { text(it).cleaned() }

// ---- certifications ---------------------------------------------------------------------------------

data class ProfileCertification(
    val name: String? = null,
    val certification_name: String? = null,
    val issuing_authority: String? = null,
    val credential_id: String? = null,
    val issue_date: String? = null,
    val expiry_date: String? = null,
    val renewal_required: Any? = null,
    val credential_url: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val tone: String? = null,
    val days_until_expiry: Any? = null,
    val needs_renewal: Any? = null,
    val is_expired: Any? = null,
    val is_expiring_soon: Any? = null
) {
    val renewalRequired: Boolean get() = renewal_required.isTicked() || renewal_required == true
    val needsRenewal: Boolean get() = needs_renewal.isTicked()
    val isExpired: Boolean get() = is_expired.isTicked()
    val isExpiringSoon: Boolean get() = is_expiring_soon.isTicked()
    val daysUntilExpiry: Int? get() = days_until_expiry.asLenientInt()
}

data class CertificationSummary(
    val total: Int? = 0,
    val active: Int? = 0,
    val expiring_soon: Int? = 0,
    val expired: Int? = 0,
    val no_expiry: Int? = 0,
    val needs_renewal: Int? = 0
)

/** `message.data` of `get_employee_certifications`. */
data class CertificationListResponse(
    val items: List<ProfileCertification?>? = null,
    val summary: CertificationSummary? = null
) {
    fun rows(): List<ProfileCertification> = items.orEmpty().filterNotNull()
}

/** Body of `create_employee_certification`. Unset dates stay null and are therefore not sent. */
data class CertificationCreateRequest(
    val certification_name: String,
    val issuing_authority: String? = null,
    val credential_id: String? = null,
    val issue_date: String? = null,
    val expiry_date: String? = null,
    val renewal_required: Boolean = true,
    val credential_url: String? = null,
    val notes: String? = null
)

/** `message.data` of `create_employee_certification`. */
data class CertificationCreateResponse(
    val item: ProfileCertification? = null,
    val summary: CertificationSummary? = null,
    val status_message: String? = null
)

/** How a certification reads on a card (mirrors the iOS `CertificationDisplay`). */
object CertificationDisplay {
    enum class Tone { RED, AMBER, GREEN, GREY }

    fun title(item: ProfileCertification, fallback: String): String =
        item.certification_name.cleaned() ?: item.name.cleaned() ?: fallback

    /** Colour from HRIS's own tone word, or the status when it sends none. */
    fun tone(item: ProfileCertification): Tone {
        val word = (item.tone.cleaned() ?: item.status.cleaned() ?: "").lowercase()
        return when {
            item.isExpired || word.contains("expired") || word.contains("danger") || word.contains("critical") -> Tone.RED
            item.isExpiringSoon || item.needsRenewal || word.contains("warn") || word.contains("expiring") -> Tone.AMBER
            word.contains("active") || word.contains("success") || word.contains("valid") -> Tone.GREEN
            else -> Tone.GREY
        }
    }

    /** An http(s) link that can be opened, or null. */
    fun credentialLink(item: ProfileCertification): String? {
        val text = item.credential_url.cleaned() ?: return null
        val lower = text.lowercase()
        return text.takeIf { lower.startsWith("http://") || lower.startsWith("https://") }
    }
}
