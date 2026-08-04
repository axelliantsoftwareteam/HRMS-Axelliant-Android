package com.axelliant.hris.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import com.axelliant.hris.R
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.axelliant.hris.model.attendance.AttendanceDetail
import com.axelliant.hris.model.dashboard.FilterModel
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import com.microsoft.fluentui.theme.token.controlTokens.CardType
import com.microsoft.fluentui.tokenized.controls.BasicCard

// ---------- Status chip (Present / Weekend / Absent / Leave etc.) ----------

fun attendanceStatusStyle(rawStatus: String): Triple<Color, Color, String> = when {
    rawStatus.contains("present", true) -> Triple(PresentBg, PresentGreen, rawStatus)
    rawStatus.contains("weekend", true) -> Triple(WeekendBg, WeekendPurple, rawStatus)
    rawStatus.contains("leave", true) -> Triple(LeaveBg, LeaveAmber, rawStatus)
    rawStatus.contains("absent", true) -> Triple(AbsentBg, AbsentRed, rawStatus)
    rawStatus.contains("holiday", true) -> Triple(LeaveBg, LeaveAmber, rawStatus)
    else -> Triple(BorderGrey, TextGrey, rawStatus.ifBlank { "—" })
}

@Composable
fun AttendanceStatusChip(rawStatus: String) {
    val (bg, fg, label) = attendanceStatusStyle(rawStatus)
    Row(
        Modifier
            .background(bg, RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._8sdp),
                vertical = dimensionResource(SdpR.dimen._4sdp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(dimensionResource(SdpR.dimen._5sdp))
                .background(fg, RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
        )
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
        Text(
            label,
            color = fg,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------- From / To date row ----------

@Composable
fun AttendanceDateRangeRow(
    startDate: String,
    endDate: String,
    onStartClick: () -> Unit = {},
    onEndClick: () -> Unit = {}
) {
    Row(Modifier.fillMaxWidth()) {
        DateRangeBox(
            label = "From",
            value = startDate,
            modifier = Modifier.weight(1f),
            onClick = onStartClick
        )
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._10sdp)))
        DateRangeBox(
            label = "To",
            value = endDate,
            modifier = Modifier.weight(1f),
            onClick = onEndClick
        )
    }
}

// ---------- Sub filter chips (All / Work From Home / Office ...) ----------

@Composable
fun AttendanceFilterChipItem(filter: FilterModel, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) FluentBlue else CardWhite
    val fg = if (selected) Color.White else Color.Black
    BasicCard(
        cardType = if (selected) CardType.Elevated else CardType.Outlined,
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.ds_radius_sm)))
            .noRippleClickable(onClick)
    )  {
        Row(
            Modifier
                .background(bg)
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._10sdp),
                    vertical = dimensionResource(SdpR.dimen._7sdp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
                )
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
            }
            Text(
                filter.title ?: "",
                color = fg,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                fontWeight = FontWeight.Medium
            )
            if (!filter.count.isNullOrBlank()) {
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
                Box(
                    Modifier
                        .background(
                            if (selected) Color.White.copy(alpha = 0.25f) else ScreenBg,
                            RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp))
                        )
                        .padding(
                            horizontal = dimensionResource(SdpR.dimen._5sdp),
                            vertical = dimensionResource(SdpR.dimen._2sdp)
                        )
                ) {
                    Text(
                        filter.count ?: "0",
                        color = fg,
                        fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceFilterRow(
    filters: List<FilterModel>,
    selectedId: String,
    onFilterClick: (FilterModel) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
    ) {
        filters.forEach { filter ->
            AttendanceFilterChipItem(
                filter = filter,
                selected = filter.id == selectedId,
                onClick = { onFilterClick(filter) }
            )
        }
    }
}

// ---------- One attendance row (Date | Hrs | Status) ----------

@Composable
fun AttendanceDetailRow(item: AttendanceDetail) {
    var expanded by remember(item.date, item.employee_name) { mutableStateOf(false) }

    val displayStatus = item.display_status.orEmpty()
    val rawStatus = item.status.orEmpty()
    val statusLabel = displayStatus.ifBlank { rawStatus }
    val location = item.attendance_location.orEmpty()
    val dateText = item.date.orEmpty()
    val shift = item.shift.orEmpty()
    val shiftTimings = item.shift_timings.orEmpty()

    val actualIn = item.in_time.orEmpty().let {
        if (it.contains("missing", ignoreCase = true)) "" else it
    }
    val expectedIn = item.expected_in.orEmpty().let {
        if (it.contains("missing", ignoreCase = true)) "" else it
    }
    val actualOut = item.out_time.orEmpty().let {
        if (it.contains("missing", ignoreCase = true)) "" else it
    }
    val expectedOut = item.expected_out.orEmpty().let {
        if (it.contains("missing", ignoreCase = true)) "" else it
    }

    BasicCard(
        cardType = CardType.Elevated,
        modifier = Modifier.clip(RoundedCornerShape(dimensionResource(R.dimen.ds_radius_sm)))
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .noRippleClickable { expanded = !expanded }
                    .padding(
                        horizontal = dimensionResource(SdpR.dimen._12sdp),
                        vertical = dimensionResource(SdpR.dimen._12sdp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1.2f)) {
                    Text(
                        dateText,
                        fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (location.isNotBlank()) {
                        Text(
                            location,
                            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                            color = TextGrey
                        )
                    }
                }
                Row(Modifier.weight(0.8f), horizontalArrangement = Arrangement.Center) {
                    Text(
                        if (item.working_hours > 0) "%.2f hrs".format(item.working_hours) else "—",
                        fontSize = dimensionResource(SspR.dimen._13ssp).value.sp
                    )
                }
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttendanceStatusChip(statusLabel)
                    Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
                    Icon(
                        imageVector = if (expanded)
                            Icons.Filled.KeyboardArrowUp
                        else
                            Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextGrey
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(SdpR.dimen._5sdp))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(dimensionResource(SdpR.dimen._1sdp))
                            .background(BorderGrey)
                    )
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))

                    AttendanceDetailField(label = "SHIFT", value = shift)
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))
                    AttendanceDetailField(label = "SHIFT TIMINGS", value = shiftTimings)
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))

                    Row(Modifier.fillMaxWidth()) {
                        AttendanceDetailField(
                            label = "ACTUAL IN",
                            value = actualIn,
                            valueColor = AbsentRed,
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceDetailField(
                            label = "EXPECTED IN",
                            value = expectedIn,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))

                    Row(Modifier.fillMaxWidth()) {
                        AttendanceDetailField(
                            label = "ACTUAL OUT",
                            value = actualOut,
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceDetailField(
                            label = "EXPECTED OUT",
                            value = expectedOut,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceDetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Black
) {
    Column(modifier.padding(dimensionResource(SdpR.dimen._5sdp))) {
        Text(
            label,
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
            color = TextGrey
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))
        Text(
            value.ifBlank { "—" },
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------- Full screen content (everything below the Week/Month/Custom row) ----------

@Composable
fun AttendanceDetailContent(
    startDate: String,
    endDate: String,
    filters: List<FilterModel>,
    selectedFilterId: String,
    attendanceList: List<AttendanceDetail>,
    onFilterClick: (FilterModel) -> Unit,
    onStartDateClick: () -> Unit = {},
    onEndDateClick: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = dimensionResource(SdpR.dimen._10sdp))
    ) {
        AttendanceDateRangeRow(
            startDate = startDate,
            endDate = endDate,
            onStartClick = onStartDateClick,
            onEndClick = onEndDateClick
        )

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))

        AttendanceFilterRow(
            filters = filters,
            selectedId = selectedFilterId,
            onFilterClick = onFilterClick
        )

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))

        if (attendanceList.isEmpty()) {
            ExpenseEmptyState("No attendance records found")
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
            ) {
                items(attendanceList) { item ->
                    AttendanceDetailRow(item = item)
                }
            }
        }
    }
}