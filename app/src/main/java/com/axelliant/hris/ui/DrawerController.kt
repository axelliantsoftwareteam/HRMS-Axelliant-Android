package com.axelliant.hris.ui

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.axelliant.hris.R
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.ItemDrawerRowBinding
import com.axelliant.hris.databinding.ItemDrawerSectionBinding
import com.axelliant.hris.databinding.ViewDrawerBinding
import com.axelliant.hris.model.dashboard.EmployProfile
import com.axelliant.hris.navigation.AppNavigator

/**
 * Builds and updates the left navigation drawer: profile header, EMPLOYEE section, MANAGER section
 * (managers only) and the Sign Out footer. The drawer itself is a native [DrawerLayout].
 */
class DrawerController(
    private val drawerLayout: DrawerLayout,
    private val binding: ViewDrawerBinding,
    private val onSignOut: () -> Unit
) {

    private class Item(
        @StringRes val title: Int,
        @DrawableRes val icon: Int,
        val showsUnread: Boolean = false,
        val action: () -> Unit
    )

    private val inflater = LayoutInflater.from(binding.root.context)
    private val context = binding.root.context
    private var isManager = AppConst.FORCE_MANAGER_FOR_TESTING
    private var unreadBadge: ItemDrawerRowBinding? = null
    private var unread = 0

    private val employeeItems = listOf(
        Item(R.string.drawer_attendance, R.drawable.ws_ic_attendance) { AppNavigator.navigateToMyAttendanceDetail() },
        Item(R.string.drawer_leave, R.drawable.ws_ic_description) { AppNavigator.navigateToLeaves() },
        Item(R.string.drawer_requests, R.drawable.ws_ic_help) { AppNavigator.navigateToRequestsOverview() },
        Item(R.string.drawer_expenses, R.drawable.ws_ic_credit_card) { AppNavigator.navigateToExpense() },
        Item(R.string.drawer_payslips, R.drawable.ws_ic_payments) { AppNavigator.navigateToPayslips() },
        Item(R.string.drawer_certifications, R.drawable.ws_ic_certificate) { AppNavigator.navigateToCertifications() },
        Item(R.string.drawer_notifications, R.drawable.ws_ic_bell, showsUnread = true) { AppNavigator.navigateToNotifications() }
    )

    private val managerItems = listOf(
        Item(R.string.drawer_approvals, R.drawable.ws_ic_verified) { AppNavigator.navigateToApprovals() },
        Item(R.string.drawer_expense_approvals, R.drawable.ws_ic_receipt) { AppNavigator.navigateToExpenseApprovals() },
        Item(R.string.drawer_team_activity, R.drawable.ws_ic_bar_chart) { AppNavigator.navigateToTeamActivity() },
        Item(R.string.drawer_team_calendar, R.drawable.ws_ic_calendar_month) { AppNavigator.navigateToTeamCalendar() },
        Item(R.string.drawer_organogram, R.drawable.ws_ic_account_tree) { AppNavigator.navigateToOrganogram() }
    )

    init {
        binding.drawerHeader.setOnClickListener { go { AppNavigator.navigateToProfile() } }
        binding.drawerSignOut.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            onSignOut()
        }
        rebuild()
    }

    /** Fills the header and shows the MANAGER section only for managers. */
    fun bind(profile: EmployProfile?) {
        val name = profile?.employee_name?.takeIf { it.isNotBlank() }
        binding.drawerName.text = name ?: context.getString(R.string.drawer_default_name)
        binding.drawerDesignation.text =
            profile?.designation?.takeIf { it.isNotBlank() } ?: context.getString(R.string.drawer_default_designation)
        val code = profile?.custom_employee_code
        binding.drawerEmpId.visibility = if (code.isNullOrBlank()) View.GONE else View.VISIBLE
        if (!code.isNullOrBlank()) binding.drawerEmpId.text = context.getString(R.string.drawer_emp_id, code)
        binding.drawerAvatar.setAvatar(profile?.image, name)

        val manager = AppConst.FORCE_MANAGER_FOR_TESTING || profile?.is_manager == true
        if (manager != isManager) {
            isManager = manager
            rebuild()
        }
    }

    /** Updates the red unread pill on the Notifications row ("99+" above 99, hidden at 0). */
    fun setUnread(count: Int) {
        unread = count
        unreadBadge?.rowBadge?.let {
            it.visibility = if (count > 0) View.VISIBLE else View.GONE
            it.text = if (count > 99) "99+" else count.toString()
        }
    }

    private fun rebuild() {
        val container = binding.drawerItems
        container.removeAllViews()
        unreadBadge = null
        addSection(R.string.drawer_section_employee)
        employeeItems.forEach { addRow(it) }
        if (isManager) {
            addSection(R.string.drawer_section_manager)
            managerItems.forEach { addRow(it) }
        }
        setUnread(unread)
    }

    private fun addSection(@StringRes title: Int) {
        val section = ItemDrawerSectionBinding.inflate(inflater, binding.drawerItems, false)
        section.sectionTitle.setText(title)
        binding.drawerItems.addView(section.root)
    }

    private fun addRow(item: Item) {
        val row = ItemDrawerRowBinding.inflate(inflater, binding.drawerItems, false)
        row.rowIcon.setImageResource(item.icon)
        row.rowTitle.setText(item.title)
        row.root.setOnClickListener { go(item.action) }
        if (item.showsUnread) unreadBadge = row
        binding.drawerItems.addView(row.root)
    }

    private fun go(action: () -> Unit) {
        drawerLayout.closeDrawer(GravityCompat.START)
        action()
    }
}
