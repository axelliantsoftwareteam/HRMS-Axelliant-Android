package com.axelliant.hris.screens

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ProfItemFieldBinding
import com.axelliant.hris.databinding.ProfItemHealthBinding
import com.axelliant.hris.databinding.ProfItemHeroBinding
import com.axelliant.hris.databinding.ProfItemRowCardBinding
import com.axelliant.hris.databinding.ProfSectionBinding
import com.axelliant.hris.databinding.ProfViewPillBinding
import com.axelliant.hris.model.payslip.cleaned
import com.axelliant.hris.model.profile.ProfileDependent
import com.axelliant.hris.model.profile.ProfileEducation
import com.axelliant.hris.model.profile.ProfileResponse
import com.axelliant.hris.model.profile.ProfileWork
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.setAvatar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

/**
 * Builds the profile page (hero, details, personal, employment, contact, benefits, skills, education,
 * work history) into [container] from a [ProfileResponse]. Mirrors the iOS `ProfileRecordView`.
 */
class ProfileRenderer(private val context: Context, private val container: LinearLayout) {

    private val inflater = LayoutInflater.from(context)
    private val primary = ContextCompat.getColor(context, R.color.ws_primary)
    private val grey = ContextCompat.getColor(context, R.color.ws_grey)
    private val notAvailable = context.getString(R.string.prof_not_available)

    fun render(profile: ProfileResponse) {
        container.removeAllViews()
        heroCard(profile)
        detailsSection(profile)
        personalSection(profile)
        employmentSection(profile)
        contactSection(profile)
        benefitsSection(profile)
        val skillNames = profile.skills()
        if (skillNames.isNotEmpty()) skillsSection(skillNames)
        val educationRows = profile.educationRows()
        if (educationRows.isNotEmpty()) educationSection(educationRows)
        val workRows = profile.workHistoryRows()
        if (workRows.isNotEmpty()) workSection(workRows)
    }

    // ------------------------------------------------------------------------------------ sections

    private fun heroCard(profile: ProfileResponse) {
        val b = ProfItemHeroBinding.inflate(inflater, container, false)
        val name = profile.employee_name.cleaned() ?: context.getString(R.string.prof_default_name)
        b.profAvatar.setAvatar(profile.image, name)
        b.profHeroName.text = name
        b.profHeroDesignation.text = profile.designation.cleaned() ?: context.getString(R.string.prof_default_designation)
        val department = profile.department.cleaned()
        b.profHeroDepartment.visibility = if (department != null) View.VISIBLE else View.GONE
        b.profHeroDepartment.text = department
        container.addView(b.root)
    }

    private fun detailsSection(p: ProfileResponse) {
        val body = section(R.string.prof_sec_details)
        grid(
            body,
            listOf(
                R.string.prof_f_code to p.displayCode,
                R.string.prof_f_company to p.company,
                R.string.prof_f_joined to WsFormat.reversedDay(p.date_of_joining),
                R.string.prof_f_manager to p.managerName
            )
        )
    }

    private fun personalSection(p: ProfileResponse) {
        val body = section(R.string.prof_sec_personal)
        rows(
            body,
            listOf(
                R.string.prof_f_full_name to p.employee_name,
                R.string.prof_f_father to (p.person_to_be_contacted.cleaned() ?: p.father_name),
                R.string.prof_f_dob to WsFormat.reversedDay(p.date_of_birth),
                R.string.prof_f_gender to p.gender,
                R.string.prof_f_cnic to p.custom_cnic,
                R.string.prof_f_company to p.company
            )
        )
    }

    private fun employmentSection(p: ProfileResponse) {
        val body = section(R.string.prof_sec_employment)
        body.addView(label(R.string.prof_f_current_role))
        body.addView(text(R.style.Ws_Text_SemiBold, p.designation.cleaned() ?: notAvailable, 12, top = SdpR.dimen._3sdp))
        p.department.cleaned()?.let {
            body.addView(text(R.style.Ws_Text, it, 11, grey, top = SdpR.dimen._3sdp))
        }
        body.addView(divider(SdpR.dimen._8sdp, SdpR.dimen._8sdp))
        grid(
            body,
            listOf(
                R.string.prof_f_code to p.displayCode,
                R.string.prof_f_joined to WsFormat.reversedDay(p.date_of_joining),
                R.string.prof_f_confirmed to WsFormat.reversedDay(p.final_confirmation_date),
                R.string.prof_f_manager to p.managerName
            )
        )
    }

    private fun contactSection(p: ProfileResponse) {
        val body = section(R.string.prof_sec_contact)
        rows(
            body,
            listOf(
                R.string.prof_f_work_email to p.user_id,
                R.string.prof_f_personal_email to p.personal_email,
                R.string.prof_f_phone to p.cell_number,
                R.string.prof_f_emergency_phone to p.emergency_phone_number,
                R.string.prof_f_relation to p.relation,
                R.string.prof_f_address to p.permanent_address
            )
        )
    }

    private fun benefitsSection(p: ProfileResponse) {
        val body = section(R.string.prof_sec_benefits)
        healthCard(body, p)

        // Dependents: heading with a count capsule, then one card each (or an empty note).
        val dependents = p.dependents()
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            text(R.style.Ws_Text_SemiBold, context.getString(R.string.prof_dependents), 12),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        if (dependents.isNotEmpty()) {
            header.addView(capsule(dependents.size.toString(), primary, 9))
        }
        body.addView(header, topMargin(px(SdpR.dimen._10sdp)))

        if (dependents.isEmpty()) {
            val empty = text(R.style.Ws_Text, context.getString(R.string.prof_no_dependents), 11, grey).apply {
                gravity = Gravity.CENTER_VERTICAL
                compoundDrawablePadding = px(SdpR.dimen._5sdp)
                val pad = px(SdpR.dimen._8sdp)
                setPadding(pad, pad, pad, pad)
                background = tinted(R.drawable.prof_pill_bg, WsStatus.tint(grey, 8))
                setCompoundDrawablesRelative(icon(R.drawable.prof_ic_group, grey, SdpR.dimen._14sdp), null, null, null)
            }
            body.addView(empty, topMargin(px(SdpR.dimen._6sdp)))
        } else {
            dependents.forEach { dependentCard(body, it) }
        }

        // Other schemes.
        body.addView(
            text(R.style.Ws_Text_SemiBold, context.getString(R.string.prof_other_schemes), 12),
            topMargin(px(SdpR.dimen._10sdp))
        )
        schemeTile(body, R.string.prof_life_insurance, R.drawable.prof_ic_heart, p.lifeInsuranceRegistered)
        schemeTile(body, R.string.prof_eobi, R.drawable.prof_ic_bank, p.eobiRegistered)
        schemeTile(body, R.string.prof_provident_fund, R.drawable.ws_ic_payments, p.providentFundRegistered)
    }

    private fun healthCard(body: LinearLayout, p: ProfileResponse) {
        val b = ProfItemHealthBinding.inflate(inflater, body, false)
        tintCircle(b.profHealthCircle, b.profHealthIcon, primary)
        b.profHealthProvider.text = p.health_insurance_provider.cleaned() ?: context.getString(R.string.prof_provider_missing)
        b.profHealthStatus.addView(statusPill(p.healthInsuranceRegistered))
        b.profHealthPolicy.addView(field(b.profHealthPolicy, R.string.prof_f_policy, p.health_insurance_no))
        body.addView(b.root)
    }

    private fun dependentCard(body: LinearLayout, dependent: ProfileDependent) {
        val b = ProfItemRowCardBinding.inflate(inflater, body, false)
        tintCircle(b.profRowCircle, null, primary)
        b.profRowInitials.visibility = View.VISIBLE
        b.profRowInitials.text = dependent.name.split(" ").filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercase() }

        val nameRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        nameRow.addView(
            text(R.style.Ws_Text_SemiBold, dependent.name, 12),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        if (dependent.relation.isNotEmpty()) {
            nameRow.addView(
                capsule(dependent.relation, primary, 9),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginStart = px(SdpR.dimen._5sdp) }
            )
        }
        b.profRowContent.addView(nameRow)

        val birth = WsFormat.reversedDay(dependent.dateOfBirth)
        val details = listOfNotNull(
            birth.takeIf { it.isNotEmpty() }?.let { context.getString(R.string.prof_dependent_dob, it) },
            dependent.age?.let { context.getString(R.string.prof_dependent_age, it) }
        ).joinToString(" · ")
        if (details.isNotEmpty()) {
            b.profRowContent.addView(text(R.style.Ws_Text, details, 10, grey, top = SdpR.dimen._4sdp))
        }
        if (dependent.cnic.isNotEmpty()) {
            b.profRowContent.addView(
                text(R.style.Ws_Text, context.getString(R.string.prof_dependent_cnic, dependent.cnic), 10, grey, top = SdpR.dimen._4sdp)
            )
        }
        body.addView(b.root)
    }

    private fun schemeTile(body: LinearLayout, @StringRes title: Int, @DrawableRes icon: Int, registered: Boolean) {
        val b = ProfItemRowCardBinding.inflate(inflater, body, false)
        tintCircle(b.profRowCircle, b.profRowIcon, if (registered) primary else grey)
        b.profRowIcon.setImageResource(icon)
        b.profRowIcon.visibility = View.VISIBLE
        b.profRowContent.addView(text(R.style.Ws_Text_Medium, context.getString(title), 12))
        b.profRowTrailing.addView(statusPill(registered))
        body.addView(b.root)
    }

    private fun skillsSection(skills: List<String>) {
        val body = section(R.string.prof_sec_skills)
        val group = ChipGroup(context).apply {
            chipSpacingHorizontal = px(SdpR.dimen._5sdp)
            chipSpacingVertical = px(SdpR.dimen._5sdp)
        }
        val font = ResourcesCompat.getFont(context, R.font.poppins_medium)
        skills.forEach { skill ->
            group.addView(Chip(context).apply {
                text = skill
                isCheckable = false
                isClickable = false
                setEnsureMinTouchTargetSize(false)
                chipStrokeWidth = 0f
                chipBackgroundColor = ColorStateList.valueOf(WsStatus.tint(primary, 10))
                setTextColor(primary)
                typeface = font
                setTextSize(TypedValue.COMPLEX_UNIT_PX, ssp(10))
                setRippleColorResource(android.R.color.transparent)
            })
        }
        body.addView(group)
    }

    private fun educationSection(rows: List<ProfileEducation>) {
        val body = section(R.string.prof_sec_education)
        rows.forEachIndexed { index, item ->
            if (index > 0) body.addView(divider(SdpR.dimen._8sdp, SdpR.dimen._8sdp))
            body.addView(text(R.style.Ws_Text_SemiBold, item.school.cleaned() ?: notAvailable, 11))
            val line = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
            }
            line.addView(
                text(R.style.Ws_Text, (item.qualification.cleaned() ?: item.level.cleaned()) ?: notAvailable, 11, grey),
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            item.year.cleaned()?.let {
                line.addView(
                    capsule(it, primary, 9, 10),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        .apply { marginStart = px(SdpR.dimen._5sdp) }
                )
            }
            body.addView(line, topMargin(px(SdpR.dimen._4sdp)))
        }
    }

    private fun workSection(rows: List<ProfileWork>) {
        val body = section(R.string.prof_sec_work)
        rows.forEachIndexed { index, item ->
            if (index > 0) body.addView(divider(SdpR.dimen._8sdp, SdpR.dimen._8sdp))
            body.addView(text(R.style.Ws_Text_SemiBold, item.company.cleaned() ?: notAvailable, 11))
            body.addView(text(R.style.Ws_Text, item.designation.cleaned() ?: notAvailable, 11, grey, top = SdpR.dimen._4sdp))
            item.salary.cleaned()?.let {
                body.addView(text(R.style.Ws_Text, context.getString(R.string.prof_salary, it), 10, grey, top = SdpR.dimen._4sdp))
            }
        }
    }

    // -------------------------------------------------------------------------------------- pieces

    /** Adds a titled section card to the page and returns its body. */
    private fun section(@StringRes title: Int): LinearLayout {
        val b = ProfSectionBinding.inflate(inflater, container, false)
        b.profSectionTitle.setText(title)
        container.addView(b.root)
        return b.profSectionBody
    }

    private fun field(parent: ViewGroup, @StringRes label: Int, value: String?): View {
        val b = ProfItemFieldBinding.inflate(inflater, parent, false)
        b.profFieldLabel.setText(label)
        b.profFieldValue.text = value.cleaned() ?: notAvailable
        return b.root
    }

    /** Fields stacked with a divider between them. */
    private fun rows(body: LinearLayout, fields: List<Pair<Int, String?>>) {
        fields.forEachIndexed { index, (label, value) ->
            if (index > 0) body.addView(divider(SdpR.dimen._5sdp, SdpR.dimen._5sdp))
            body.addView(field(body, label, value))
        }
    }

    /** Fields in two equal columns. */
    private fun grid(body: LinearLayout, fields: List<Pair<Int, String?>>) {
        fields.chunked(2).forEachIndexed { index, pair ->
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEachIndexed { column, (label, value) ->
                val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                if (column > 0) params.marginStart = px(SdpR.dimen._9sdp)
                row.addView(field(row, label, value), params)
            }
            if (pair.size == 1) row.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
            body.addView(row, topMargin(if (index == 0) 0 else px(SdpR.dimen._9sdp)))
        }
    }

    private fun label(@StringRes res: Int): TextView = text(R.style.Ws_Text, context.getString(res), 10, grey)

    private fun divider(@DimenRes topPad: Int, @DimenRes bottomPad: Int): View = View(context).apply {
        setBackgroundColor(ContextCompat.getColor(context, R.color.ws_divider))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1).apply {
            topMargin = px(topPad)
            bottomMargin = px(bottomPad)
        }
    }

    private fun topMargin(px: Int) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px
        }

    /** A TextView in one of the Ws text styles with an explicit ssp size and optional colour. */
    private fun text(style: Int, value: String, sizeSsp: Int, @ColorInt color: Int? = null, @DimenRes top: Int = 0): TextView =
        TextView(context, null, 0, style).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_PX, ssp(sizeSsp))
            color?.let { setTextColor(it) }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                if (top != 0) topMargin = px(top)
            }
        }

    /** Capsule with [value] in [color] on an [alphaPercent] % tint of it. */
    private fun capsule(value: String, @ColorInt color: Int, sizeSsp: Int, alphaPercent: Int = 12): TextView =
        TextView(context, null, 0, R.style.Ws_Text_Medium).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_PX, ssp(sizeSsp))
            setTextColor(color)
            setPadding(px(SdpR.dimen._5sdp), px(SdpR.dimen._2sdp), px(SdpR.dimen._5sdp), px(SdpR.dimen._2sdp))
            background = tinted(R.drawable.prof_pill_bg, WsStatus.tint(color, alphaPercent))
        }

    private fun statusPill(registered: Boolean): View {
        val tint = if (registered) primary else grey
        val pill = ProfViewPillBinding.inflate(inflater, null, false).root
        pill.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        pill.setText(if (registered) R.string.prof_registered else R.string.prof_not_registered)
        pill.setTextColor(tint)
        pill.background?.mutate()?.setTint(WsStatus.tint(tint, 12))
        pill.setCompoundDrawablesRelative(
            icon(if (registered) R.drawable.ws_ic_check_circle else R.drawable.prof_ic_minus_circle, tint, SdpR.dimen._9sdp),
            null, null, null
        )
        return pill
    }

    private fun tinted(@DrawableRes res: Int, @ColorInt color: Int): Drawable? =
        ContextCompat.getDrawable(context, res)?.mutate()?.also { it.setTint(color) }

    private fun icon(@DrawableRes res: Int, @ColorInt tint: Int, @DimenRes size: Int): Drawable {
        val drawable = ContextCompat.getDrawable(context, res)!!.mutate()
        drawable.setTint(tint)
        val s = px(size)
        drawable.setBounds(0, 0, s, s)
        return drawable
    }

    /** Circle at 12 % of [tint] with the glyph in [tint]. */
    private fun tintCircle(circle: View, glyph: ImageView?, @ColorInt tint: Int) {
        circle.background?.mutate()?.setTint(WsStatus.tint(tint, 12))
        glyph?.setColorFilter(tint)
    }

    private fun px(@DimenRes id: Int): Int = context.resources.getDimensionPixelSize(id)

    /** Text size in px for an ssp step (so it scales with the screen like the XML layouts). */
    private fun ssp(size: Int): Float = context.resources.getDimension(
        when (size) {
            9 -> SspR.dimen._8ssp
            10 -> SspR.dimen._9ssp
            11 -> SspR.dimen._10ssp
            else -> SspR.dimen._11ssp
        }
    )
}
